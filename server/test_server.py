import importlib.util
import json
import os
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
import shutil
import uuid
from pathlib import Path


class ServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        workspace_tmp = Path(__file__).resolve().parents[1] / ".tools" / "test-tmp"
        workspace_tmp.mkdir(parents=True, exist_ok=True)
        cls.tmp_path = workspace_tmp / ("run-" + uuid.uuid4().hex)
        cls.tmp_path.mkdir(parents=True)
        os.environ["QIUQIU_DATA_DIR"] = str(cls.tmp_path)
        os.environ["QIUQIU_ADMIN_TOKEN"] = "test-admin-token-32-characters-ok"
        spec = importlib.util.spec_from_file_location("qiuqiu_server", Path(__file__).with_name("app.py"))
        cls.app = importlib.util.module_from_spec(spec); spec.loader.exec_module(cls.app)
        cls.app.init_db()
        cls.httpd = cls.app.ThreadingHTTPServer(("127.0.0.1", 0), cls.app.Handler)
        cls.base = f"http://127.0.0.1:{cls.httpd.server_address[1]}"
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True); cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown(); cls.httpd.server_close(); shutil.rmtree(cls.tmp_path, ignore_errors=True)

    def request(self, method, path, body=None, token=None):
        data = None if body is None else json.dumps(body).encode()
        req = urllib.request.Request(self.base+path, data=data, method=method, headers={"Content-Type":"application/json", **({"Authorization":"Bearer "+token} if token else {})})
        with urllib.request.urlopen(req, timeout=3) as res: return res.status, json.loads(res.read())

    def test_end_to_end_and_idempotency(self):
        status, health = self.request("GET", "/v1/health"); self.assertEqual(status, 200); self.assertEqual(health["status"], "ok")
        _, account_a = self.request("POST", "/v1/accounts/register", {"username":"owner_a","password":"password-a","displayName":"A"})
        _, account_b = self.request("POST", "/v1/accounts/register", {"username":"owner_b","password":"password-b","displayName":"B"})
        _, a = self.request("POST", "/v1/spaces", {"petName":"球球","displayName":"A","deviceName":"A phone"}, account_a["accountToken"])
        _, invite = self.request("POST", "/v1/invites", {}, a["deviceToken"])
        _, b = self.request("POST", "/v1/invites/accept", {"inviteToken":invite["inviteToken"],"accountToken":account_b["accountToken"],"displayName":"B","deviceName":"B phone"})
        event = {"opId":"op-1","entityType":"care","entityId":"pet","actionType":"feed","payload":{"food":"猫粮"},"hlc":"1000-0-a","createdAtLocal":1000}
        _, pushed = self.request("POST", "/v1/sync/push", {"events":[event]}, a["deviceToken"])
        self.assertEqual(pushed["results"][0]["status"], "accepted")
        _, duplicate = self.request("POST", "/v1/sync/push", {"events":[event]}, a["deviceToken"])
        self.assertEqual(duplicate["results"][0]["status"], "duplicate")
        _, pulled = self.request("GET", "/v1/sync/pull?after=0", token=b["deviceToken"])
        self.assertEqual(len(pulled["events"]), 1); self.assertEqual(pulled["events"][0]["payload"]["food"], "猫粮")
        _, devices = self.request("GET", "/v1/devices", token=a["deviceToken"])
        self.assertEqual(len(devices["devices"]), 2)
        _, relogin = self.request("POST", "/v1/accounts/login", {"username":"owner_a","password":"password-a","deviceName":"A second phone"})
        self.assertEqual(relogin["spaceId"], a["spaceId"]); self.assertIn("deviceToken", relogin)
        _, frozen = self.request("POST", "/v1/relation/freeze", {}, a["deviceToken"]); self.assertEqual(frozen["status"], "frozen")
        private_event = {"opId":"feature-frozen","entityType":"feature","entityId":"note","actionType":"upsert","payload":{"title":"x"},"hlc":"1001-0-a","createdAtLocal":1001}
        _, rejected = self.request("POST", "/v1/sync/push", {"events":[private_event]}, a["deviceToken"])
        self.assertEqual(rejected["results"][0]["status"], "rejected")
        self.request("POST", "/v1/relation/resume", {}, a["deviceToken"])

    def test_field_conflict_and_reward(self):
        _, a = self.request("POST", "/v1/spaces", {"petName":"球球","displayName":"A","deviceName":"A"}, "test-admin-token-32-characters-ok")
        events = [
          {"opId":"set-new","entityType":"pet","entityId":"pet","actionType":"set","payload":{"name":"新名字"},"hlc":"2000-0-a","createdAtLocal":2000},
          {"opId":"set-old","entityType":"pet","entityId":"pet","actionType":"set","payload":{"name":"旧名字"},"hlc":"1000-0-a","createdAtLocal":1000},
          {"opId":"reward-1","entityType":"reward","entityId":"daily","actionType":"claim_reward","payload":{"rewardKey":"2026-10-07"},"hlc":"2001-0-a","createdAtLocal":2001},
          {"opId":"reward-2","entityType":"reward","entityId":"daily","actionType":"claim_reward","payload":{"rewardKey":"2026-10-07"},"hlc":"2002-0-a","createdAtLocal":2002},
        ]
        _, result = self.request("POST", "/v1/sync/push", {"events":events}, a["deviceToken"])
        self.assertEqual([x["status"] for x in result["results"]], ["accepted","compensated","accepted","duplicate"])
        _, conflicts = self.request("GET", "/v1/conflicts", token=a["deviceToken"])
        self.assertGreaterEqual(len(conflicts["conflicts"]), 1)

    def test_account_claims_existing_pet_and_cannot_create_second(self):
        _, pet = self.request("POST", "/v1/spaces", {"petName":"球球","displayName":"旧主人","deviceName":"旧手机"}, "test-admin-token-32-characters-ok")
        _, account = self.request("POST", "/v1/accounts/register", {"username":"legacy_owner","password":"password-legacy","displayName":"旧主人","deviceToken":pet["deviceToken"]})
        self.assertTrue(account["hasPet"])
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.request("POST", "/v1/spaces", {"petName":"另一只","displayName":"旧主人","deviceName":"手机"}, account["accountToken"])
        self.assertEqual(error.exception.code, 409)


if __name__ == "__main__": unittest.main()
