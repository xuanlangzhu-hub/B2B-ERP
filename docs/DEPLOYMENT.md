# B2B 云进销存 ERP 部署说明

本文以 Ubuntu + MySQL 8 + JDK 21 + Nginx 为例。正式配置中的密码、JWT 密钥和 SSH 私钥不得提交到 Git。

## 1. 推荐架构

```text
浏览器 --80/443--> Nginx --/api/--> Spring Boot :8080 --> MySQL 127.0.0.1:3306
                         |
                         +--> Vue dist 静态文件
```

MySQL 与后端在同一台服务器时，MySQL 只监听 `127.0.0.1`，阿里云安全组不开放 3306。组员需要管理数据库时，通过 SSH 隧道把本机端口转发到服务器的 3306。

## 2. 环境要求

- Ubuntu 22.04/24.04
- JDK 21
- MySQL 8.0
- Nginx
- 构建机 Node.js 20+、Maven 3.9+

本次验证环境为 JDK 21.0.11、Maven 3.9.9、Node.js 24.18.0。前端项目要求 Node.js 20 或更高版本。

## 3. 数据库初始化

创建目标数据库和专用用户后，按顺序执行：

```text
sql/V1__init_schema.sql
sql/V2__seed_demo_data.sql
sql/V3__align_permissions.sql
sql/V4__finance_closure.sql
sql/V5__inventory_operations.sql
sql/V6__inventory_borrow.sql
sql/V7__master_data_core.sql
sql/V8__master_data_metadata.sql
```

演示数据脚本包含 `admin / Admin@123456`，部署后应立即修改演示密码。

## 4. 构建产物

后端：

```bash
cd erp-server
mvn clean package
```

产物为 `erp-server/target/erp-server.jar`。

前端：

```bash
cd erp-web
npm ci
npm run build
```

产物位于 `erp-web/dist/`。

## 5. 后端环境变量

创建 `/etc/b2b-erp/erp-server.env`，权限设为仅管理员和服务用户可读：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/group_project?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=<数据库用户>
DB_PASSWORD=<数据库密码>
JWT_SECRET=<至少32字节的随机字符串>
SERVER_PORT=8080
```

应用读取的是完整 `DB_URL`，这样本地数据库、SSH 隧道和服务器本机数据库都可以使用同一份程序，只需切换配置。

## 6. systemd 服务

建议目录：

```text
/opt/b2b-erp/server/erp-server.jar
/var/www/b2b-erp/
/etc/b2b-erp/erp-server.env
```

创建 `/etc/systemd/system/b2b-erp.service`：

```ini
[Unit]
Description=B2B ERP Spring Boot Service
After=network.target mysql.service

[Service]
Type=simple
User=erp
Group=erp
WorkingDirectory=/opt/b2b-erp/server
EnvironmentFile=/etc/b2b-erp/erp-server.env
ExecStart=/usr/bin/java -jar /opt/b2b-erp/server/erp-server.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

加载并启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now b2b-erp
sudo systemctl status b2b-erp
```

查看日志：

```bash
sudo journalctl -u b2b-erp -f
```

## 7. Nginx 配置

将前端 `dist` 中的文件部署到 `/var/www/b2b-erp/`，创建 `/etc/nginx/sites-available/b2b-erp`：

```nginx
server {
    listen 80;
    server_name <域名或服务器公网IP>;

    root /var/www/b2b-erp;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }
}
```

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

前端当前使用 Hash 路由，刷新页面不会依赖服务器识别业务路径；保留 `try_files` 可以兼容以后切换 History 路由。

## 8. 阿里云安全组与 MySQL

推荐只开放：

- SSH 实际端口，例如 2222，只允许可信 IP 更好。
- HTTP 80。
- HTTPS 443。

不要向公网开放 3306。`mysqld.cnf` 保持：

```ini
bind-address = 127.0.0.1
mysqlx-bind-address = 127.0.0.1
max_connect_errors = 10000
```

修改配置后先检查，再重启：

```bash
sudo mysqld --validate-config
sudo systemctl restart mysql
sudo systemctl status mysql
```

开发电脑需要连接服务器 MySQL 时建立 SSH 隧道：

```bash
ssh -N -L 13306:127.0.0.1:3306 -p <SSH端口> <SSH用户>@<服务器IP>
```

Navicat 或本地后端连接 `127.0.0.1:13306`。服务器上的 Spring Boot 直接连接 `127.0.0.1:3306`，不需要 SSH 隧道。

## 9. 发布检查

发布前：

1. `mvn test` 通过。
2. `mvn clean package` 通过。
3. `npm run build` 通过。
4. 数据库已经备份。
5. 环境文件不在 Git 中。

发布后：

1. `systemctl status b2b-erp` 为 active。
2. `curl http://127.0.0.1:8080/v3/api-docs` 能返回内容。
3. 浏览器能打开登录页。
4. admin 能登录并访问首页。
5. 库存、流水、首页和库存报表接口返回成功。
6. 服务器重启后 systemd 能自动恢复服务。

## 10. 数据库备份与恢复

备份：

```bash
mysqldump --single-transaction --routines --triggers -u <用户> -p group_project > group_project_YYYYMMDD.sql
```

恢复到空数据库：

```bash
mysql -u <用户> -p group_project < group_project_YYYYMMDD.sql
```

备份文件包含业务数据，应存放在受控目录并限制读取权限，不要提交到仓库。

## 11. 回滚方法

发布前保留上一个版本的 JAR 和前端目录。出现严重问题时：

1. 停止 `b2b-erp` 服务。
2. 将 JAR 和前端静态目录切回上一版本。
3. 如果本次发布包含不兼容数据库变更，按发布前备份恢复。
4. 启动后端并重载 Nginx。
5. 重新执行登录、首页、库存查询和核心出入库验证。

本项目的 SQL 脚本目前以初始化和补丁顺序管理，正式长期维护时建议引入 Flyway，对每次数据库变更进行版本化。
