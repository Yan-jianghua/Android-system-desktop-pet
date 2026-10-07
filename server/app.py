#!/usr/bin/env python3
"""QiuQiu 3.0 self-hosted sync server using only the Python standard library."""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import secrets
import shutil
import sqlite3
import threading
import time
import uuid
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


DATA_DIR = Path(os.environ.get("QIUQIU_DATA_DIR", "/data")).resolve()
DB_PATH = DATA_DIR / "qiuqiu.db"
ATTACHMENTS = DATA_DIR / "attachments"
BACKUPS = DATA_DIR / "backups"
ADMIN_TOKEN = os.environ.get("QIUQIU_ADMIN_TOKEN", "").strip()
HOST = os.environ.get("QIUQIU_HOST", "0.0.0.0")
PORT = int(os.environ.get("QIUQIU_PORT", "8080"))
MAX_BODY = int(os.environ.get("QIUQIU_MAX_BODY", str(12 * 1024 * 1024)))
INVITE_TTL = int(os.environ.get("QIUQIU_INVITE_TTL", "900"))
LOCK = threading.RLock()


def now_ms() -> int:
    return int(time.time() * 1000)


def sha(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def password_hash(password: str, salt: str) -> str:
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), bytes.fromhex(salt), 180000).hex()


def canonical_json(value) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)


def connect() -> sqlite3.Connection:
    db = sqlite3.connect(DB_PATH, timeout=30)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA foreign_keys=ON")
    db.execute("PRAGMA journal_mode=WAL")
    return db


def init_db() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    ATTACHMENTS.mkdir(parents=True, exist_ok=True)
    BACKUPS.mkdir(parents=True, exist_ok=True)
    with connect() as db:
        db.executescript(
            """
            CREATE TABLE IF NOT EXISTS spaces(
              space_id TEXT PRIMARY KEY, pet_name TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'active',
              created_at INTEGER NOT NULL, schema_version INTEGER NOT NULL DEFAULT 1
            );
            CREATE TABLE IF NOT EXISTS accounts(
              account_id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE,
              password_salt TEXT NOT NULL, password_hash TEXT NOT NULL,
              display_name TEXT NOT NULL, account_token_hash TEXT NOT NULL UNIQUE,
              space_id TEXT, actor_id TEXT, created_at INTEGER NOT NULL,
              FOREIGN KEY(space_id) REFERENCES spaces(space_id)
            );
            CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_actor ON accounts(actor_id) WHERE actor_id IS NOT NULL;
            CREATE TABLE IF NOT EXISTS members(
              actor_id TEXT PRIMARY KEY, space_id TEXT NOT NULL, display_name TEXT NOT NULL,
              role TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'active', created_at INTEGER NOT NULL,
              FOREIGN KEY(space_id) REFERENCES spaces(space_id)
            );
            CREATE TABLE IF NOT EXISTS devices(
              device_id TEXT PRIMARY KEY, actor_id TEXT NOT NULL, token_hash TEXT NOT NULL UNIQUE,
              device_name TEXT NOT NULL, created_at INTEGER NOT NULL, last_seen INTEGER NOT NULL,
              revoked_at INTEGER, FOREIGN KEY(actor_id) REFERENCES members(actor_id)
            );
            CREATE TABLE IF NOT EXISTS invites(
              invite_id TEXT PRIMARY KEY, space_id TEXT NOT NULL, creator_actor_id TEXT NOT NULL,
              token_hash TEXT NOT NULL UNIQUE, expires_at INTEGER NOT NULL, used_at INTEGER,
              revoked_at INTEGER, created_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS events(
              server_seq INTEGER PRIMARY KEY AUTOINCREMENT, op_id TEXT NOT NULL UNIQUE,
              space_id TEXT NOT NULL, actor_id TEXT NOT NULL, device_id TEXT NOT NULL,
              entity_type TEXT NOT NULL, entity_id TEXT NOT NULL, action_type TEXT NOT NULL,
              payload TEXT NOT NULL, hlc TEXT NOT NULL, base_version INTEGER NOT NULL DEFAULT 0,
              schema_version INTEGER NOT NULL DEFAULT 1, created_at_local INTEGER NOT NULL,
              accepted_at INTEGER NOT NULL, tombstone INTEGER NOT NULL DEFAULT 0,
              result TEXT NOT NULL DEFAULT 'accepted'
            );
            CREATE INDEX IF NOT EXISTS idx_events_space_seq ON events(space_id, server_seq);
            CREATE INDEX IF NOT EXISTS idx_events_entity ON events(space_id, entity_type, entity_id, server_seq);
            CREATE TABLE IF NOT EXISTS entity_versions(
              space_id TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id TEXT NOT NULL,
              field_name TEXT NOT NULL, value_json TEXT, hlc TEXT NOT NULL, device_id TEXT NOT NULL,
              tombstone INTEGER NOT NULL DEFAULT 0, server_seq INTEGER NOT NULL,
              PRIMARY KEY(space_id, entity_type, entity_id, field_name)
            );
            CREATE TABLE IF NOT EXISTS reward_claims(
              space_id TEXT NOT NULL, reward_key TEXT NOT NULL, op_id TEXT NOT NULL,
              server_seq INTEGER, PRIMARY KEY(space_id, reward_key)
            );
            CREATE TABLE IF NOT EXISTS conflicts(
              conflict_id TEXT PRIMARY KEY, space_id TEXT NOT NULL, op_id TEXT NOT NULL,
              policy TEXT NOT NULL, result TEXT NOT NULL, detail TEXT NOT NULL, created_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS attachments(
              attachment_id TEXT PRIMARY KEY, space_id TEXT NOT NULL, actor_id TEXT NOT NULL,
              content_hash TEXT NOT NULL, mime TEXT NOT NULL, size INTEGER NOT NULL,
              privacy_scope TEXT NOT NULL, storage_path TEXT NOT NULL, created_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS audit_log(
              audit_id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL,
              actor TEXT NOT NULL, detail TEXT NOT NULL, created_at INTEGER NOT NULL
            );
            """
        )


def audit(db, action: str, actor: str, detail: dict) -> None:
    safe = {k: v for k, v in detail.items() if k not in {"token", "content", "payload"}}
    db.execute("INSERT INTO audit_log(action,actor,detail,created_at) VALUES(?,?,?,?)",
               (action, actor, canonical_json(safe), now_ms()))


def make_device(db, actor_id: str, device_name: str) -> tuple[str, str]:
    device_id = str(uuid.uuid4())
    token = secrets.token_urlsafe(48)
    db.execute(
        "INSERT INTO devices(device_id,actor_id,token_hash,device_name,created_at,last_seen) VALUES(?,?,?,?,?,?)",
        (device_id, actor_id, sha(token), device_name[:80], now_ms(), now_ms()),
    )
    return device_id, token


def auth_device(db, header: str | None):
    if not header or not header.startswith("Bearer "):
        return None
    token_hash = sha(header[7:].strip())
    row = db.execute(
        "SELECT d.*,m.space_id,m.display_name,m.status member_status FROM devices d "
        "JOIN members m ON m.actor_id=d.actor_id WHERE d.token_hash=? AND d.revoked_at IS NULL",
        (token_hash,),
    ).fetchone()
    if row and row["member_status"] == "active":
        db.execute("UPDATE devices SET last_seen=? WHERE device_id=?", (now_ms(), row["device_id"]))
        return row
    return None


def auth_account_token(db, token: str | None):
    if not token:
        return None
    return db.execute("SELECT * FROM accounts WHERE account_token_hash=?", (sha(token.strip()),)).fetchone()


def auth_account(db, header: str | None):
    if not header or not header.startswith("Bearer "):
        return None
    return auth_account_token(db, header[7:])


def event_dict(row) -> dict:
    return {
        "serverSeq": row["server_seq"], "opId": row["op_id"], "spaceId": row["space_id"],
        "actorId": row["actor_id"], "deviceId": row["device_id"], "entityType": row["entity_type"],
        "entityId": row["entity_id"], "actionType": row["action_type"],
        "payload": json.loads(row["payload"]), "hlc": row["hlc"], "baseVersion": row["base_version"],
        "schemaVersion": row["schema_version"], "createdAtLocal": row["created_at_local"],
        "acceptedAt": row["accepted_at"], "tombstone": bool(row["tombstone"]), "result": row["result"],
    }


def resolve_event(db, device, item: dict) -> dict:
    required = ["opId", "entityType", "entityId", "actionType", "payload", "hlc", "createdAtLocal"]
    if any(k not in item for k in required):
        return {"opId": item.get("opId"), "status": "rejected", "reason": "missing_fields"}
    op_id = str(item["opId"])
    duplicate = db.execute("SELECT server_seq,result FROM events WHERE op_id=?", (op_id,)).fetchone()
    if duplicate:
        return {"opId": op_id, "status": "duplicate", "serverSeq": duplicate["server_seq"]}
    if len(canonical_json(item.get("payload"))) > 2 * 1024 * 1024:
        return {"opId": op_id, "status": "rejected", "reason": "payload_too_large"}
    entity_type = str(item["entityType"])[:64]
    entity_id = str(item["entityId"])[:128]
    action_type = str(item["actionType"])[:64]
    payload = item.get("payload") if isinstance(item.get("payload"), dict) else {"value": item.get("payload")}
    space_status = db.execute("SELECT status FROM spaces WHERE space_id=?", (device["space_id"],)).fetchone()[0]
    if space_status != "active" and entity_type in {"feature", "memory", "attachment"}:
        return {"opId": op_id, "status": "rejected", "reason": "relation_not_active"}
    hlc = str(item["hlc"])[:96]
    tombstone = 1 if item.get("tombstone") or action_type == "delete" else 0
    result = "accepted"
    reason = None

    if action_type == "claim_reward":
        reward_key = str(payload.get("rewardKey", ""))[:128]
        if not reward_key:
            return {"opId": op_id, "status": "rejected", "reason": "reward_key_required"}
        try:
            db.execute("INSERT INTO reward_claims(space_id,reward_key,op_id) VALUES(?,?,?)",
                       (device["space_id"], reward_key, op_id))
        except sqlite3.IntegrityError:
            result, reason = "duplicate", "reward_already_claimed"

    db.execute(
        "INSERT INTO events(op_id,space_id,actor_id,device_id,entity_type,entity_id,action_type,payload,hlc,base_version,schema_version,created_at_local,accepted_at,tombstone,result) "
        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (op_id, device["space_id"], device["actor_id"], device["device_id"], entity_type, entity_id,
         action_type, canonical_json(payload), hlc, int(item.get("baseVersion", 0)),
         int(item.get("schemaVersion", 1)), int(item["createdAtLocal"]), now_ms(), tombstone, result),
    )
    seq = db.execute("SELECT server_seq FROM events WHERE op_id=?", (op_id,)).fetchone()[0]

    # Field-level HLC register. Delete is represented by a versioned tombstone.
    if action_type in {"set", "upsert", "delete"}:
        fields = payload if action_type != "delete" else {"__deleted__": True}
        for field, value in fields.items():
            current = db.execute(
                "SELECT hlc,device_id FROM entity_versions WHERE space_id=? AND entity_type=? AND entity_id=? AND field_name=?",
                (device["space_id"], entity_type, entity_id, str(field)),
            ).fetchone()
            wins = not current or (hlc, device["device_id"]) > (current["hlc"], current["device_id"])
            if wins:
                db.execute(
                    "INSERT INTO entity_versions(space_id,entity_type,entity_id,field_name,value_json,hlc,device_id,tombstone,server_seq) "
                    "VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(space_id,entity_type,entity_id,field_name) DO UPDATE SET "
                    "value_json=excluded.value_json,hlc=excluded.hlc,device_id=excluded.device_id,tombstone=excluded.tombstone,server_seq=excluded.server_seq",
                    (device["space_id"], entity_type, entity_id, str(field), canonical_json(value), hlc,
                     device["device_id"], tombstone, seq),
                )
            else:
                result, reason = "compensated", "older_field_version"
                db.execute("UPDATE events SET result=? WHERE op_id=?", (result, op_id))
                db.execute(
                    "INSERT INTO conflicts(conflict_id,space_id,op_id,policy,result,detail,created_at) VALUES(?,?,?,?,?,?,?)",
                    (str(uuid.uuid4()), device["space_id"], op_id, "field_hlc", result,
                     canonical_json({"entityType": entity_type, "entityId": entity_id, "field": field}), now_ms()),
                )
    if action_type == "claim_reward" and result == "accepted":
        db.execute("UPDATE reward_claims SET server_seq=? WHERE space_id=? AND op_id=?", (seq, device["space_id"], op_id))
    response = {"opId": op_id, "status": result, "serverSeq": seq}
    if reason:
        response["reason"] = reason
    return response


ADMIN_HTML = """<!doctype html><html lang=zh-CN><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'>
<title>球球 3.0 服务端</title><style>body{font-family:system-ui;margin:0;background:#f6f1ed;color:#423832}.wrap{max-width:980px;margin:auto;padding:28px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px}.card{background:white;border-radius:18px;padding:18px;box-shadow:0 6px 22px #0001}
h1{margin:0 0 8px}.n{font-size:30px;font-weight:700;color:#9c4f59}button{padding:10px 16px;border:0;border-radius:12px;background:#477293;color:white}
input{padding:10px;border:1px solid #ccc;border-radius:10px;width:280px;max-width:90%}pre{white-space:pre-wrap;background:#fff;padding:16px;border-radius:14px}</style>
<div class=wrap><h1>球球 3.0 自托管服务端</h1><p>只显示运行指标，不展示私人正文。</p><p><input id=t type=password placeholder='管理员令牌'> <button onclick=load()>刷新</button> <button onclick=backup()>立即备份</button></p>
<div id=g class=grid></div><pre id=o>输入 docker-compose.yml 中的管理员令牌。</pre></div><script>
async function api(path,opt={}){opt.headers={...(opt.headers||{}),'Authorization':'Bearer '+t.value};let r=await fetch(path,opt);let j=await r.json();if(!r.ok)throw Error(j.error||r.status);return j}
async function load(){try{let j=await api('/v1/admin/stats');g.innerHTML=Object.entries(j).map(([k,v])=>`<div class=card><div>${k}</div><div class=n>${v}</div></div>`).join('');o.textContent=JSON.stringify(j,null,2)}catch(e){o.textContent=e}}
async function backup(){try{o.textContent=JSON.stringify(await api('/v1/admin/backup',{method:'POST'}),null,2)}catch(e){o.textContent=e}}</script></html>"""

JOIN_HTML = """<!doctype html><html lang=zh-CN><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'>
<title>加入球球双人共养</title><style>body{font-family:system-ui;background:#f8f2ef;color:#443832;display:grid;place-items:center;min-height:100vh}.c{background:#fff;padding:30px;border-radius:22px;max-width:520px;box-shadow:0 8px 30px #0002}a{display:block;text-align:center;padding:14px;background:#9c4f59;color:#fff;border-radius:14px;text-decoration:none;font-weight:700}</style>
<div class=c><h1>一起养球球</h1><p>邀请链接只用于加入指定的私人共享空间，有效期短且只能使用一次。请核对分享者身份后继续。</p><a id=open>打开球球桌面宠物</a><p>如果没有反应，请先安装 3.0 版本，再重新打开此链接。</p></div><script>let q=new URLSearchParams(location.search),t=q.get('token')||'';open.href='qiuqiu://join?server='+encodeURIComponent(location.origin)+'&token='+encodeURIComponent(t)</script></html>"""


class Handler(BaseHTTPRequestHandler):
    server_version = "QiuQiuSync/4.0"

    def log_message(self, fmt, *args):
        print(f"{self.address_string()} - {fmt % args}")

    def send_json(self, status: int, data: dict):
        body = canonical_json(data).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def read_json(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY:
                raise ValueError("invalid_body_size")
            return json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception as exc:
            raise ValueError("invalid_json") from exc

    def admin_ok(self) -> bool:
        header = self.headers.get("Authorization", "")
        supplied = header[7:].strip() if header.startswith("Bearer ") else ""
        return bool(ADMIN_TOKEN) and hmac.compare_digest(supplied, ADMIN_TOKEN)

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path in {"/", "/admin"}:
            body = ADMIN_HTML.encode("utf-8")
            self.send_response(200); self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body); return
        if parsed.path == "/join":
            body = JOIN_HTML.encode("utf-8")
            self.send_response(200); self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body))); self.send_header("Cache-Control", "no-store")
            self.end_headers(); self.wfile.write(body); return
        if parsed.path == "/v1/health":
            with connect() as db:
                seq = db.execute("SELECT COALESCE(MAX(server_seq),0) FROM events").fetchone()[0]
            return self.send_json(200, {"status": "ok", "version": "4.0.0", "serverTime": now_ms(), "latestServerSeq": seq})
        with connect() as db:
            device = auth_device(db, self.headers.get("Authorization"))
            if parsed.path == "/v1/sync/pull":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                q = parse_qs(parsed.query); after = max(0, int(q.get("after", ["0"])[0])); limit = min(500, max(1, int(q.get("limit", ["200"])[0])))
                rows = db.execute("SELECT * FROM events WHERE space_id=? AND server_seq>? ORDER BY server_seq LIMIT ?", (device["space_id"], after, limit)).fetchall()
                latest = db.execute("SELECT COALESCE(MAX(server_seq),0) FROM events WHERE space_id=?", (device["space_id"],)).fetchone()[0]
                return self.send_json(200, {"events": [event_dict(r) for r in rows], "latestServerSeq": latest, "hasMore": bool(rows and rows[-1]["server_seq"] < latest), "serverTime": now_ms()})
            if parsed.path == "/v1/snapshot":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                versions = db.execute("SELECT * FROM entity_versions WHERE space_id=? ORDER BY entity_type,entity_id,field_name", (device["space_id"],)).fetchall()
                latest = db.execute("SELECT COALESCE(MAX(server_seq),0) FROM events WHERE space_id=?", (device["space_id"],)).fetchone()[0]
                return self.send_json(200, {"spaceId": device["space_id"], "latestServerSeq": latest, "entities": [dict(r) for r in versions], "serverTime": now_ms()})
            if parsed.path == "/v1/devices":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                rows = db.execute("SELECT d.device_id,d.device_name,d.created_at,d.last_seen,d.revoked_at,m.actor_id,m.display_name FROM devices d JOIN members m ON m.actor_id=d.actor_id WHERE m.space_id=? ORDER BY d.created_at", (device["space_id"],)).fetchall()
                return self.send_json(200, {"devices": [dict(r) for r in rows]})
            if parsed.path == "/v1/conflicts":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                rows = db.execute("SELECT conflict_id,op_id,policy,result,detail,created_at FROM conflicts WHERE space_id=? ORDER BY created_at DESC LIMIT 100", (device["space_id"],)).fetchall()
                return self.send_json(200, {"conflicts": [dict(r) for r in rows]})
            if parsed.path == "/v1/admin/stats":
                if not self.admin_ok(): return self.send_json(401, {"error": "admin_auth_required"})
                sizes = sum(p.stat().st_size for p in ATTACHMENTS.glob("*") if p.is_file())
                return self.send_json(200, {
                    "状态": "在线", "共享空间": db.execute("SELECT COUNT(*) FROM spaces WHERE status='active'").fetchone()[0],
                    "有效设备": db.execute("SELECT COUNT(*) FROM devices WHERE revoked_at IS NULL").fetchone()[0],
                    "事件": db.execute("SELECT COUNT(*) FROM events").fetchone()[0],
                    "冲突补偿": db.execute("SELECT COUNT(*) FROM conflicts").fetchone()[0],
                    "附件 MB": round(sizes / 1024 / 1024, 2), "备份": len(list(BACKUPS.glob("*.db"))),
                })
        return self.send_json(404, {"error": "not_found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        try: body = self.read_json() if parsed.path != "/v1/admin/backup" else {}
        except ValueError as exc: return self.send_json(400, {"error": str(exc)})
        with LOCK, connect() as db:
            if parsed.path == "/v1/accounts/register":
                username = str(body.get("username", "")).strip().lower()
                password = str(body.get("password", ""))
                display_name = str(body.get("displayName", username)).strip()[:40]
                if len(username) < 3 or len(username) > 32 or not username.replace("_", "").isalnum():
                    return self.send_json(400, {"error": "username_invalid"})
                if len(password) < 8 or len(password) > 128:
                    return self.send_json(400, {"error": "password_must_be_8_to_128_chars"})
                existing_device = auth_device(db, "Bearer " + str(body.get("deviceToken", ""))) if body.get("deviceToken") else None
                salt, token, account_id = secrets.token_hex(16), secrets.token_urlsafe(48), str(uuid.uuid4())
                try:
                    db.execute("INSERT INTO accounts(account_id,username,password_salt,password_hash,display_name,account_token_hash,space_id,actor_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                               (account_id, username, salt, password_hash(password, salt), display_name or username, sha(token), existing_device["space_id"] if existing_device else None, existing_device["actor_id"] if existing_device else None, now_ms()))
                except sqlite3.IntegrityError:
                    return self.send_json(409, {"error": "username_already_exists"})
                audit(db, "account_registered", account_id, {"username": username})
                return self.send_json(201, {"accountId": account_id, "username": username, "displayName": display_name or username, "accountToken": token, "hasPet": bool(existing_device)})
            if parsed.path == "/v1/accounts/login":
                username, password = str(body.get("username", "")).strip().lower(), str(body.get("password", ""))
                account = db.execute("SELECT * FROM accounts WHERE username=?", (username,)).fetchone()
                if not account or not hmac.compare_digest(account["password_hash"], password_hash(password, account["password_salt"])):
                    return self.send_json(401, {"error": "account_or_password_incorrect"})
                token = secrets.token_urlsafe(48)
                db.execute("UPDATE accounts SET account_token_hash=? WHERE account_id=?", (sha(token), account["account_id"]))
                result = {"accountId": account["account_id"], "username": username, "displayName": account["display_name"], "accountToken": token, "hasPet": bool(account["space_id"])}
                if account["space_id"] and account["actor_id"]:
                    device_id, device_token = make_device(db, account["actor_id"], str(body.get("deviceName", "手机")))
                    result.update({"spaceId": account["space_id"], "actorId": account["actor_id"], "deviceId": device_id, "deviceToken": device_token})
                audit(db, "account_login", account["account_id"], {"username": username})
                return self.send_json(200, result)
            if parsed.path == "/v1/spaces":
                account = auth_account(db, self.headers.get("Authorization"))
                if not account and not self.admin_ok(): return self.send_json(401, {"error": "account_login_required"})
                if account and account["space_id"]: return self.send_json(409, {"error": "account_already_has_pet"})
                space_id, actor_id = str(uuid.uuid4()), str(uuid.uuid4())
                db.execute("INSERT INTO spaces(space_id,pet_name,created_at) VALUES(?,?,?)", (space_id, str(body.get("petName", "球球"))[:40], now_ms()))
                db.execute("INSERT INTO members(actor_id,space_id,display_name,role,created_at) VALUES(?,?,?,?,?)", (actor_id, space_id, str(body.get("displayName", "主人 A"))[:40], "owner", now_ms()))
                device_id, token = make_device(db, actor_id, str(body.get("deviceName", "首台手机")))
                if account: db.execute("UPDATE accounts SET space_id=?,actor_id=?,display_name=? WHERE account_id=?", (space_id, actor_id, str(body.get("displayName", account["display_name"]))[:40], account["account_id"]))
                audit(db, "space_created", actor_id, {"spaceId": space_id, "deviceId": device_id})
                return self.send_json(201, {"spaceId": space_id, "actorId": actor_id, "deviceId": device_id, "deviceToken": token})
            device = auth_device(db, self.headers.get("Authorization"))
            if parsed.path == "/v1/invites":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                raw, invite_id = secrets.token_urlsafe(32), str(uuid.uuid4())
                db.execute("INSERT INTO invites(invite_id,space_id,creator_actor_id,token_hash,expires_at,created_at) VALUES(?,?,?,?,?,?)",
                           (invite_id, device["space_id"], device["actor_id"], sha(raw), now_ms()+INVITE_TTL*1000, now_ms()))
                audit(db, "invite_created", device["actor_id"], {"inviteId": invite_id})
                base = str(body.get("publicBaseUrl", "")).rstrip("/")
                return self.send_json(201, {"inviteId": invite_id, "inviteToken": raw, "expiresAt": now_ms()+INVITE_TTL*1000, "inviteUrl": f"{base}/join?token={raw}" if base else raw})
            if parsed.path == "/v1/invites/accept":
                account = auth_account_token(db, str(body.get("accountToken", "")))
                if not account: return self.send_json(401, {"error": "account_login_required"})
                if account["space_id"]: return self.send_json(409, {"error": "account_already_has_pet"})
                token = str(body.get("inviteToken", "")); invite = db.execute("SELECT * FROM invites WHERE token_hash=?", (sha(token),)).fetchone()
                if not invite or invite["used_at"] or invite["revoked_at"] or invite["expires_at"] < now_ms():
                    return self.send_json(410, {"error": "invite_invalid_or_expired"})
                count = db.execute("SELECT COUNT(*) FROM members WHERE space_id=? AND status='active'", (invite["space_id"],)).fetchone()[0]
                if count >= 2: return self.send_json(409, {"error": "space_already_has_two_members"})
                actor_id = str(uuid.uuid4())
                db.execute("INSERT INTO members(actor_id,space_id,display_name,role,created_at) VALUES(?,?,?,?,?)", (actor_id, invite["space_id"], str(body.get("displayName", "主人 B"))[:40], "owner", now_ms()))
                device_id, token_out = make_device(db, actor_id, str(body.get("deviceName", "手机")))
                db.execute("UPDATE accounts SET space_id=?,actor_id=?,display_name=? WHERE account_id=?", (invite["space_id"], actor_id, str(body.get("displayName", account["display_name"]))[:40], account["account_id"]))
                db.execute("UPDATE invites SET used_at=? WHERE invite_id=?", (now_ms(), invite["invite_id"]))
                audit(db, "invite_accepted", actor_id, {"inviteId": invite["invite_id"], "deviceId": device_id})
                return self.send_json(201, {"spaceId": invite["space_id"], "actorId": actor_id, "deviceId": device_id, "deviceToken": token_out})
            if parsed.path == "/v1/sync/push":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                events = body.get("events", [])
                if not isinstance(events, list) or len(events) > 500: return self.send_json(400, {"error": "events_must_be_array_max_500"})
                results = [resolve_event(db, device, item) for item in events]
                latest = db.execute("SELECT COALESCE(MAX(server_seq),0) FROM events WHERE space_id=?", (device["space_id"],)).fetchone()[0]
                return self.send_json(200, {"results": results, "latestServerSeq": latest, "serverTime": now_ms()})
            if parsed.path == "/v1/relation/freeze":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                db.execute("UPDATE spaces SET status='frozen' WHERE space_id=?", (device["space_id"],)); audit(db,"relation_frozen",device["actor_id"],{"spaceId":device["space_id"]})
                return self.send_json(200,{"status":"frozen","recoverUntil":now_ms()+24*3600*1000})
            if parsed.path == "/v1/relation/resume":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                db.execute("UPDATE spaces SET status='active' WHERE space_id=?", (device["space_id"],)); audit(db,"relation_resumed",device["actor_id"],{"spaceId":device["space_id"]})
                return self.send_json(200,{"status":"active"})
            if parsed.path == "/v1/attachments":
                if not device: return self.send_json(401, {"error": "device_auth_required"})
                try: raw = base64.b64decode(body.get("contentBase64", ""), validate=True)
                except Exception: return self.send_json(400, {"error": "invalid_base64"})
                if not raw or len(raw) > 10*1024*1024: return self.send_json(413, {"error": "attachment_size_invalid"})
                mime = str(body.get("mime", "application/octet-stream"))[:100]
                if not (mime.startswith("image/") or mime == "application/pdf"): return self.send_json(415, {"error": "mime_not_allowed"})
                digest, attachment_id = hashlib.sha256(raw).hexdigest(), str(uuid.uuid4())
                target = ATTACHMENTS / digest; target.write_bytes(raw)
                db.execute("INSERT INTO attachments VALUES(?,?,?,?,?,?,?,?,?)", (attachment_id, device["space_id"], device["actor_id"], digest, mime, len(raw), str(body.get("privacyScope", "shared"))[:20], str(target), now_ms()))
                return self.send_json(201, {"attachmentId": attachment_id, "contentHash": digest, "size": len(raw)})
            if parsed.path == "/v1/admin/backup":
                if not self.admin_ok(): return self.send_json(401, {"error": "admin_auth_required"})
                db.commit(); stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ"); target = BACKUPS / f"qiuqiu-{stamp}.db"
                with sqlite3.connect(target) as out: db.backup(out)
                audit(db, "backup_created", "admin", {"file": target.name})
                return self.send_json(201, {"status": "created", "file": target.name, "bytes": target.stat().st_size})
        return self.send_json(404, {"error": "not_found"})

    def do_DELETE(self):
        parsed = urlparse(self.path)
        with LOCK, connect() as db:
            device = auth_device(db, self.headers.get("Authorization"))
            if not device: return self.send_json(401, {"error": "device_auth_required"})
            if parsed.path.startswith("/v1/devices/"):
                target = parsed.path.rsplit("/", 1)[-1]
                row = db.execute("SELECT d.device_id,m.space_id FROM devices d JOIN members m ON m.actor_id=d.actor_id WHERE d.device_id=?", (target,)).fetchone()
                if not row or row["space_id"] != device["space_id"]: return self.send_json(404, {"error": "device_not_found"})
                db.execute("UPDATE devices SET revoked_at=? WHERE device_id=?", (now_ms(), target))
                audit(db, "device_revoked", device["actor_id"], {"deviceId": target})
                return self.send_json(200, {"status": "revoked", "deviceId": target})
        return self.send_json(404, {"error": "not_found"})


def main() -> None:
    if not ADMIN_TOKEN:
        raise SystemExit("QIUQIU_ADMIN_TOKEN must be set")
    init_db()
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"QiuQiu 3.0 sync server listening on {HOST}:{PORT}; data={DATA_DIR}")
    server.serve_forever()


if __name__ == "__main__":
    main()
