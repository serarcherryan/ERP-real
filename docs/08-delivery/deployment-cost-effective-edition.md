# 性价比版部署指南（All-in-One 单节点架构）

本指南详细说明了在单台云服务器上使用 Docker Compose 完成本 ERP 系统（前后端、数据库以及 EMQX 消息队列）部署的全过程。适合项目起步、单院区、对成本极其敏感的场景。

## 1. 准备工作

- **云服务器**：推荐 Ubuntu 22.04 LTS，配置建议 4核 8G 及以上，需要在云厂商控制台配置安全组，开放 **80 (HTTP)**, **443 (HTTPS)**, **1883 (MQTT)** 端口。
- **域名准备**：请确保域名已解析到该服务器的公网 IP，例如：
  - 前端与 API 访问：`erp.yourdomain.com`
  - MQTT 消息 WebSocket 访问：`mqtt.yourdomain.com`

## 2. 基础环境安装

通过 SSH 登录服务器，更新系统并安装基础组件：

```bash
sudo apt update && sudo apt upgrade -y

# 安装 Docker 与 Docker Compose 插件
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Certbot（用于自动化申请 Let's Encrypt 免费 HTTPS 证书）
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
```
*(注：执行 `usermod` 后需退出 SSH 重新登录，使 Docker 权限生效)*

## 3. 申请 HTTPS 证书

生成免费且可自动续期的 HTTPS 证书：

```bash
sudo certbot certonly --standalone -d erp.yourdomain.com -d mqtt.yourdomain.com
```
*（期间按提示输入邮箱即可。证书默认存放在 `/etc/letsencrypt/live/erp.yourdomain.com/` 中）*

## 4. 创建部署目录结构

规划并在服务器上创建数据卷目录：

```bash
sudo mkdir -p /opt/erp-real/{nginx,postgres/data,emqx/data,frontend}
sudo chown -R $USER:$USER /opt/erp-real
cd /opt/erp-real
```

## 5. 编写 docker-compose.yml

在 `/opt/erp-real` 目录下新建 `docker-compose.yml` 文件：

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: erp-postgres
    restart: always
    environment:
      POSTGRES_DB: erp_real
      POSTGRES_USER: erp
      POSTGRES_PASSWORD: erp_prod_password
    volumes:
      - ./postgres/data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U erp -d erp_real"]
      interval: 10s
      timeout: 5s
      retries: 5

  emqx:
    image: emqx/emqx:5.3.0
    container_name: erp-emqx
    restart: always
    ports:
      - "1883:1883"     # 设备端直接通过 MQTT(TCP) 协议连接
      - "8083:8083"     # Nginx 代理 WebSocket 需要
      # - "18083:18083" # Dashboard 建议仅在需要时开放，或使用 Nginx 内网代理并加持 BasicAuth
    volumes:
      - ./emqx/data:/opt/emqx/data

  backend:
    # 填入你构建的后端镜像地址。若是本地构建，可以先在本地 docker save，然后传到服务器上 docker load
    image: erpreal/backend:latest 
    container_name: erp-backend
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/erp_real
      SPRING_DATASOURCE_USERNAME: erp
      SPRING_DATASOURCE_PASSWORD: erp_prod_password
      # 视业务配置项而定，指向内网 EMQX
      EMQX_HOST: tcp://emqx:1883
    depends_on:
      postgres:
        condition: service_healthy

  nginx:
    image: nginx:alpine
    container_name: erp-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./frontend:/usr/share/nginx/html:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro # 挂载主机的 SSL 证书
    depends_on:
      - backend
```

## 6. 编写 Nginx 反向代理配置

在 `/opt/erp-real/nginx/` 目录下新建 `nginx.conf`：

```nginx
events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    # 1. 强制 HTTP 重定向 HTTPS
    server {
        listen 80;
        server_name erp.yourdomain.com mqtt.yourdomain.com;
        return 301 https://$host$request_uri;
    }

    # 2. 业务系统与后台接口 (Web-Admin & Backend API)
    server {
        listen 443 ssl;
        server_name erp.yourdomain.com;

        ssl_certificate /etc/letsencrypt/live/erp.yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/erp.yourdomain.com/privkey.pem;

        # 部署前端 Web-Admin 静态文件 (React/Vue 单页应用标准配置)
        location / {
            root /usr/share/nginx/html;
            index index.html index.htm;
            try_files $uri $uri/ /index.html;
        }

        # 代理到后端 Spring Boot API
        location /api/ {
            proxy_pass http://backend:8080/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }

    # 3. MQTT over WebSocket (WSS) 安全代理
    server {
        listen 443 ssl;
        server_name mqtt.yourdomain.com;

        ssl_certificate /etc/letsencrypt/live/erp.yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/erp.yourdomain.com/privkey.pem;

        # Web / 小程序端通过 WSS 连接 MQTT
        location /mqtt {
            proxy_pass http://emqx:8083/mqtt;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "Upgrade";
            proxy_set_header Host $host;
        }
    }
}
```

## 7. 部署并启动系统

1. **部署前端**：在本地执行 `npm run build` 打包 `web-admin`，将生成的 `dist/` 目录中的所有文件上传至服务器的 `/opt/erp-real/frontend` 目录。
2. **部署后端**：将 Spring Boot 后端项目打包成 Docker 镜像并推送到镜像仓库，或上传到服务器通过 `docker load` 加载。
3. **启动容器**：在 `/opt/erp-real` 目录执行一键拉起：

```bash
docker-compose up -d
```
通过 `docker-compose logs -f` 查看启动日志，确认数据库初始化完成（Flyway 自动执行表结构及 Seed SQL 等）。

## 8. 保底运维：配置定时备份脚本

单节点最大的风险是硬盘损坏或误删库，必须设置自动冷备兜底策略。
在 `/opt/erp-real` 创建 `backup.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/opt/erp-real/postgres/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# 备份 erp_real 数据库并压缩
docker exec -t erp-postgres pg_dump -U erp erp_real | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# 自动清理超过 14 天的过期备份
find $BACKUP_DIR -type f -mtime +14 -name '*.gz' -delete

# 【强烈推荐】：加入 rclone 命令，将本地备份推送到 S3 或阿里云 OSS 
# rclone copy $BACKUP_DIR/db_$DATE.sql.gz remote:my-bucket/db-backups/
```

添加执行权限并写入系统 crontab，每天凌晨 3 点执行：

```bash
chmod +x /opt/erp-real/backup.sh
(crontab -l 2>/dev/null; echo "0 3 * * * /opt/erp-real/backup.sh") | crontab -
```
至此，极简高性价比生产环境已全部就绪。
