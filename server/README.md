# 球球 3.0 自托管服务端

## Windows 启动

1. 安装并启动 Docker Desktop，确认 `docker version` 可用。
2. 在项目根目录运行 `server/start-server.ps1`。
3. 打开 `http://localhost:8080/admin`，输入根目录 `.env` 中的管理员令牌。

首次启动会创建 Docker 数据卷 `qiuqiu-data`。停止容器不会删除数据；只有显式执行 `docker compose down -v` 才会删除数据卷。

手机和电脑在同一局域网时，手机端填写 `http://电脑局域网IP:8080`。异地访问必须自行配置 HTTPS 反向代理或安全隧道，不要直接把明文 HTTP 暴露到公网。

## Ubuntu / Docker 部署

当前虚拟机部署目录为 `/home/yanjianghua/qiuqiu-server`，服务地址为
`http://192.168.130.131:8080`。常用操作：

```bash
cd /home/yanjianghua/qiuqiu-server
docker compose ps
docker compose logs --tail=100
docker compose restart
grep '^QIUQIU_ADMIN_TOKEN=' .env
```

`.env` 权限应保持为 `600`。管理员令牌只用于创建首个双人空间，不要通过聊天、截图或公开链接发送。停止或升级容器不会删除 `qiuqiu-data` 数据卷；不要执行
`docker compose down -v`，除非明确需要删除全部同步数据。

VMware NAT 模式下，Windows 宿主机可以访问上述地址，但实体手机通常不能直接访问虚拟机。手机接入前需要将虚拟机改为桥接模式，或在 VMware/Windows 上配置端口转发。服务仅使用 HTTP，因此只应在可信局域网内使用；公网访问应增加 HTTPS 反向代理或安全隧道。

## API

- `GET /v1/health`：健康检查
- `POST /v1/spaces`：管理员创建共享空间和首台设备
- `POST /v1/invites`、`POST /v1/invites/accept`：双人邀请
- `POST /v1/sync/push`、`GET /v1/sync/pull`：离线事件同步
- `GET /v1/snapshot`：实体字段快照
- `POST /v1/attachments`：受限附件上传
- `DELETE /v1/devices/{id}`：撤销设备
- `GET /v1/admin/stats`、`POST /v1/admin/backup`：管理和备份

所有设备接口使用 `Authorization: Bearer <deviceToken>`。管理员接口使用 `.env` 中的管理员令牌。
