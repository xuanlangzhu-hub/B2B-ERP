# 成员 2 基础资料模块运行与测试说明

这个压缩包保留了完整的公共框架、登录、JWT、权限、动态菜单及当前业务源码，因此解压后可以直接作为完整项目运行。成员 2 只需重点修改本文列出的基础资料目录，不要删除其他模块。

打包前验证结果：后端 `mvn test` 通过，前端 `npm run build` 通过；打包目录中的前后端源码与当前主工程逐文件哈希一致。

## 1. 压缩包内容

```text
erp-server/       Spring Boot 3 后端源码
erp-web/          Vue 3 前端源码
sql/              MySQL 建表、演示数据和补丁
docs/             当前接口和业务规则
MEMBER2-TASK.md   成员 2 当前进度、剩余任务和验收清单
README.md         整体项目说明
```

压缩包没有包含：

- `.git` 和个人分支记录。
- IDEA、VS Code 等个人配置。
- `node_modules`、`target`、`dist` 等可重新生成的构建产物。
- 后端运行日志。
- 服务器数据库账号、SSH 私钥或隧道配置。

## 2. 环境要求

- JDK 21
- Maven 3.9 或更高
- Node.js 20 或更高
- MySQL 8.0 或更高
- IDEA 与 Navicat 可选

检查环境：

```powershell
java -version
mvn -version
node -v
npm -v
mysql --version
```

## 3. 初始化本地数据库

建议使用本地数据库，不连接小组服务器数据库，避免互相覆盖数据。

### 方式 A：使用 Navicat

1. 新建数据库 `group_project`。
2. 字符集选择 `utf8mb4`，排序规则选择 `utf8mb4_0900_ai_ci`；若本机不支持该排序规则，可使用 `utf8mb4_unicode_ci`。
3. 选中 `group_project`，按顺序运行：

```text
sql/V1__init_schema.sql
sql/V2__seed_demo_data.sql
sql/V3__align_permissions.sql
sql/V4__finance_closure.sql
```

### 方式 B：使用 MySQL 命令行

先登录 MySQL：

```powershell
mysql -u root -p
```

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS group_project
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'erp_local'@'localhost'
  IDENTIFIED BY 'ErpLocal@2026';

GRANT ALL PRIVILEGES ON group_project.* TO 'erp_local'@'localhost';
FLUSH PRIVILEGES;
```

退出 MySQL，然后在项目根目录依次导入：

```powershell
Get-Content -Raw -Encoding UTF8 .\sql\V1__init_schema.sql | mysql -u erp_local -p group_project
Get-Content -Raw -Encoding UTF8 .\sql\V2__seed_demo_data.sql | mysql -u erp_local -p group_project
Get-Content -Raw -Encoding UTF8 .\sql\V3__align_permissions.sql | mysql -u erp_local -p group_project
Get-Content -Raw -Encoding UTF8 .\sql\V4__finance_closure.sql | mysql -u erp_local -p group_project
```

每次提示密码时输入：`ErpLocal@2026`。

## 4. 数据库配置

后端默认配置已经指向：

```text
地址：127.0.0.1:3306
数据库：group_project
用户名：erp_local
密码：ErpLocal@2026
```

如果不想创建该用户，可在启动后端前临时覆盖配置：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/group_project?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='<你自己的本地 MySQL root 密码>'
```

不要把个人密码写回 `application.yml`，也不要提交到 Git。

## 5. 后端测试与启动

打开第一个终端：

```powershell
cd erp-server
mvn test
mvn spring-boot:run
```

看到 `Started ErpApplication` 表示后端启动成功。

访问地址：

- API：`http://localhost:8080/api/v1`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

如果 IDEA 使用了错误的 JDK：

1. `File -> Project Structure -> Project SDK` 选择 JDK 21。
2. `Settings -> Build Tools -> Maven -> Runner -> JRE` 选择 JDK 21。
3. 重新加载 Maven 项目。

## 6. 前端安装、测试与启动

打开第二个终端：

```powershell
cd erp-web
npm install
npm run build
npm run dev
```

浏览器访问：`http://localhost:5173`

演示账号：

```text
用户名：admin
密码：Admin@123456
```

前端开发服务器已经把 `/api` 代理到 `http://localhost:8080`，不需要手动配置跨域。

## 7. 成员 2 重点目录

后端重点修改：

```text
erp-server/src/main/java/com/erp/masterdata/
erp-server/src/main/java/com/erp/system/controller/WarehouseController.java
erp-server/src/main/java/com/erp/system/entity/OrgWarehouse.java
erp-server/src/main/java/com/erp/system/mapper/OrgWarehouseMapper.java
erp-server/src/main/java/com/erp/system/service/OrgWarehouseService.java
```

前端重点修改：

```text
erp-web/src/views/master/product/index.vue
erp-web/src/views/master/customer/index.vue
erp-web/src/views/master/supplier/index.vue
erp-web/src/views/master/warehouse/index.vue
erp-web/src/api/masterdata.ts
```

演示数据：

```text
sql/V2__seed_demo_data.sql
```

完整剩余任务和验收标准见：`MEMBER2-TASK.md`。

## 8. 推荐测试顺序

登录后依次检查：

1. 基础资料 -> 商品管理。
2. 基础资料 -> 客户管理。
3. 基础资料 -> 供应商管理。
4. 基础资料 -> 仓库管理。
5. 新增一件测试商品，检查分类、单位、价格和状态。
6. 编辑该商品，刷新页面检查回显。
7. 删除该商品，检查列表不再显示。
8. 打开销售单，检查客户、商品、仓库下拉。
9. 打开采购单，检查供应商、商品、仓库下拉。
10. 执行 `mvn test` 和 `npm run build`。

建议测试编码使用自己的前缀，例如：

```text
商品：M2-P001
客户：M2-C001
供应商：M2-S001
仓库：M2-W001
```

## 9. 当前已知缺口

这些是需要成员 2 继续完成的内容，不是启动故障：

- 商品分类、计量单位暂时没有前端维护页。
- 商品演示数据为 8 条，还差 2 条。
- 基础资料列表、详情、修改和删除仍需全面补齐企业隔离。
- 后端请求 DTO、`@Valid` 和字段校验不完整。
- options 返回字段需要补全。
- 缺少基础资料专项自动化测试。

优先顺序：企业隔离 -> 后端校验 -> 分类/单位页面 -> options 字段 -> 测试。

## 10. 常见启动问题

### 后端提示数据库连接失败

- 检查 MySQL 服务是否启动。
- 检查 `group_project` 是否存在。
- 检查四个 SQL 是否按顺序执行。
- 检查用户名和密码。
- 确认使用本地 `3306`，不需要 SSH 隧道。

### 登录失败

- 确认已执行 V2 演示数据。
- 使用 `admin / Admin@123456`，注意大小写。
- 浏览器旧 token 可能失效，可以清理站点 Local Storage 后重新登录。

### 前端白屏或接口 404

- 确认后端 8080 正在运行。
- 确认前端通过 `npm run dev` 启动在 5173。
- 打开浏览器开发者工具查看 Console 和 Network。

### Maven 出现 `TypeTag :: UNKNOWN`

- 项目必须使用 JDK 21。
- IDEA Project SDK、Maven Runner JRE 和终端 `java -version` 要保持一致。

## 11. 提交前检查

```powershell
cd erp-server
mvn test

cd ..\erp-web
npm run build
```

提交前确认没有包含：数据库密码修改、日志、`target`、`dist`、`node_modules`、`.idea`、私钥或个人 SSH 配置。
