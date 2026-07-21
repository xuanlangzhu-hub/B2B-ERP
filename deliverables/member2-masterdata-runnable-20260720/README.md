# B2B 云进销存 ERP

一周实训版本，采用 Spring Boot 3 + Vue 3，实现商品、客户、供应商、采购、销售、库存、资金和基础权限管理。

## 项目结构

```text
erp-server/   Spring Boot 后端
erp-web/      Vue 3 前端
sql/          数据库结构、演示数据和权限补丁
docs/         数据库设计说明
team-tasks/   五名组员的详细任务单
```

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+
- MySQL 8.0+

## 1. 准备数据库

在目标数据库中按顺序执行：

1. `sql/V1__init_schema.sql`
2. `sql/V2__seed_demo_data.sql`
3. `sql/V3__align_permissions.sql`
4. `sql/V4__finance_closure.sql`

本机开发默认连接 `127.0.0.1:3306/group_project`，默认本地账号为
`erp_local / ErpLocal@2026`。首次搭建时需要创建数据库和用户，再依次执行上面的三个脚本。

如果需要改为连接服务器 MySQL，并且服务器只允许 SSH 隧道访问，先建立本地端口转发：

```powershell
ssh -N -L 13306:127.0.0.1:3306 -p <SSH端口> <SSH用户>@<服务器IP>
```

保持这个窗口运行。此后，本机程序通过 `127.0.0.1:13306` 连接服务器上的 MySQL。

## 2. 启动后端

直接启动时会使用本地数据库默认配置：

```powershell
cd erp-server
mvn spring-boot:run
```

需要连接服务器或 SSH 隧道时，使用环境变量覆盖默认值。服务器数据库密码和正式 JWT 密钥不要提交到 Git：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:13306/group_project?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
$env:DB_USERNAME='team_user'
$env:DB_PASSWORD='<服务器数据库密码>'
$env:JWT_SECRET='<至少32字节的随机字符串>'
mvn spring-boot:run
```

后端启动后：

- API 根路径：`http://localhost:8080/api/v1`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

演示账号由 V2 脚本创建：`admin / Admin@123456`。仅用于实训演示，部署后应立即修改。

## 3. 启动前端

```powershell
cd erp-web
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。开发服务器会把 `/api` 请求代理到后端的 `8080` 端口。

## 4. 提交前检查

```powershell
cd erp-server
mvn test

cd ../erp-web
npm run build
```

当前自动化检查覆盖 JWT、演示密码兼容性和前端 TypeScript 类型检查。退货链路已在本地库完成“采购入库 → 销售出库 → 销售退货入库 → 采购退货出库”的端到端回归。联调时还应至少验证登录、首页、商品、采购、销售、库存和资金列表。

退货接口与限制见 `docs/RETURN_API.md`，财务接口见 `docs/FINANCE_API.md`，核心状态规则见 `docs/BUSINESS_RULES.md`。

## 协作约定

- 每个人在自己的功能分支开发，不直接向 `main` 推送未验证代码。
- 接口统一使用 `/api/v1` 前缀，响应统一使用 `Result<T>`。
- 数据库字段使用 `snake_case`，Java/TypeScript 字段使用 `camelCase`。
- 密码、私钥、生产配置和日志禁止提交；仓库中的 `.gitignore` 已屏蔽常见敏感文件和构建产物。
- 具体分工与接口范围见 `team-tasks/` 和 `ONE_WEEK_TASKS.md`。
