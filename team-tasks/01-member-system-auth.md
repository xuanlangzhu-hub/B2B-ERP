# 成员 1 任务书：项目骨架、登录权限与系统设置

> 分支：`feature/system`  
> 后端模块：`common`、`security`、`system`  
> 前端目录：`layouts`、`router`、`stores`、`views/system`  
> 最终责任：公共工程可运行、其他四人的模块可以顺利合并

## 1. 本人交付目标

你负责整个项目的公共底座。第 1 天下午前必须让其他成员拿到一个能启动、能访问数据库、能添加新模块的 Spring Boot + Vue 3 工程。

一周内必须交付：

- Spring Boot 3 后端项目骨架。
- Vue 3 + TypeScript + Vite 前端项目骨架。
- 统一返回体、分页、异常处理、参数校验和跨域配置。
- 登录、JWT、获取当前用户、退出。
- 用户、角色、菜单基础管理。
- 后台整体布局、侧边菜单、路由守卫、用户状态管理。
- 个人信息、修改密码、操作日志页面。
- OpenAPI/Knife4j 接口文档入口。
- 第 5 天负责第一次全量合并，第 7 天协助发布最终版本。

本周不做：

- OAuth、短信登录、验证码登录。
- 完整 RBAC 数据权限引擎。
- 动态按钮权限生成器。
- 组织树、部门树和审批流。
- Redis Token 黑名单；退出时前端删除 Token 即可。

## 2. 依赖与技术要求

### 后端

- Java 17。
- Spring Boot 3.x。
- Spring Security 6。
- JWT。
- MyBatis-Plus。
- MySQL 8。
- Jakarta Validation。
- Lombok。
- springdoc-openapi 或 Knife4j。
- Maven。

### 前端

- Vue 3。
- TypeScript。
- Vite。
- Element Plus。
- Vue Router。
- Pinia。
- Axios。

## 3. 数据库负责范围

沿用 `sql/V1__init_schema.sql` 中已有表，不另外创建同义表：

| 表 | 本周用途 |
| --- | --- |
| `sys_user` | 用户和登录信息 |
| `sys_role` | 角色 |
| `sys_menu` | 菜单和权限标识 |
| `sys_user_role` | 用户角色关系 |
| `sys_role_menu` | 角色菜单关系 |
| `sys_operation_log` | 操作日志 |
| `sys_document_sequence` | 公共单据编号，可给其他成员调用 |
| `org_enterprise` | 企业基本信息，先保留默认企业 |
| `org_store` | 门店选项，简化维护 |

### 必须准备的初始化数据

- 一个默认企业。
- 一个默认门店。
- 一个 `ADMIN` 角色。
- 一个管理员用户。
- 首页、资料、销售、采购、库存、资金、报表、设置菜单。
- 管理员拥有全部菜单。

密码必须使用 BCrypt 哈希，SQL 中不能出现真实日常密码。演示密码放在 README 的“演示账号”章节，并注明仅用于课程演示。

## 4. 后端目录和类清单

```text
erp-server/src/main/java/.../
├─ common/
│  ├─ api/Result.java
│  ├─ api/PageResult.java
│  ├─ exception/BusinessException.java
│  ├─ exception/GlobalExceptionHandler.java
│  ├─ constant/ErrorCode.java
│  └─ config/MybatisPlusConfig.java
├─ security/
│  ├─ JwtTokenProvider.java
│  ├─ JwtAuthenticationFilter.java
│  ├─ SecurityConfig.java
│  ├─ LoginUser.java
│  └─ UserDetailsServiceImpl.java
└─ system/
   ├─ controller/AuthController.java
   ├─ controller/UserController.java
   ├─ controller/RoleController.java
   ├─ controller/MenuController.java
   ├─ controller/ProfileController.java
   ├─ controller/OperationLogController.java
   ├─ dto/LoginRequest.java
   ├─ dto/UserCreateRequest.java
   ├─ dto/UserUpdateRequest.java
   ├─ dto/PasswordChangeRequest.java
   ├─ vo/LoginResponse.java
   ├─ vo/CurrentUserVO.java
   ├─ entity/SysUser.java
   ├─ entity/SysRole.java
   ├─ entity/SysMenu.java
   ├─ mapper/
   └─ service/
```

## 5. 公共接口规范

### 5.1 请求基础路径

所有接口以 `/api/v1` 开头。

### 5.2 统一返回体

成功：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {}
}
```

失败：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "用户名不能为空",
  "data": null
}
```

### 5.3 分页返回

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

分页请求统一使用 `page`、`size`；默认 `page=1`、`size=10`，最大 `size=100`。

### 5.4 HTTP 状态与业务码

| 场景 | HTTP | 业务码 |
| --- | ---: | --- |
| 成功 | 200 | `SUCCESS` |
| 参数错误 | 400 | `VALIDATION_ERROR` |
| 未登录或 Token 失效 | 401 | `UNAUTHORIZED` |
| 无权限 | 403 | `FORBIDDEN` |
| 数据不存在 | 404 | `NOT_FOUND` |
| 状态冲突或重复操作 | 409 | `BUSINESS_CONFLICT` |
| 未处理异常 | 500 | `INTERNAL_ERROR` |

## 6. 本人接口文档

### 6.1 登录

`POST /api/v1/auth/login`

请求：

```json
{
  "username": "admin",
  "password": "演示密码"
}
```

响应 `data`：

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": "1",
    "username": "admin",
    "realName": "系统管理员",
    "roles": ["ADMIN"]
  }
}
```

规则：用户不存在、密码错误或用户被禁用时统一提示“用户名或密码错误”，不要暴露具体原因。

### 6.2 当前用户

`GET /api/v1/auth/me`

请求头：`Authorization: Bearer <token>`

响应 `data`：

```json
{
  "id": "1",
  "username": "admin",
  "realName": "系统管理员",
  "phone": "",
  "roles": ["ADMIN"],
  "permissions": ["system:user:list", "sales:order:list"],
  "menus": []
}
```

### 6.3 用户管理接口

| 方法 | 地址 | 功能 | 主要参数 |
| --- | --- | --- | --- |
| GET | `/api/v1/users` | 用户分页 | `username`、`realName`、`status`、`page`、`size` |
| GET | `/api/v1/users/{id}` | 用户详情 | 路径 ID |
| POST | `/api/v1/users` | 新增用户 | 用户名、初始密码、姓名、手机号、角色 ID |
| PUT | `/api/v1/users/{id}` | 修改用户 | 姓名、手机号、状态、角色 ID |
| DELETE | `/api/v1/users/{id}` | 逻辑删除 | 路径 ID |
| PUT | `/api/v1/users/{id}/status` | 启用/禁用 | `status` |
| POST | `/api/v1/users/{id}/reset-password` | 重置密码 | `newPassword` |
| PUT | `/api/v1/users/{id}/roles` | 分配角色 | `roleIds` |

新增用户请求：

```json
{
  "username": "sales01",
  "password": "Initial@123",
  "realName": "销售员一",
  "phone": "",
  "status": 1,
  "roleIds": [2]
}
```

校验：用户名 3～30 位且唯一；密码至少 6 位；姓名必填；管理员账号不能删除自己。

### 6.4 角色和菜单接口

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/roles` | 角色列表或分页 |
| POST | `/api/v1/roles` | 新增角色 |
| PUT | `/api/v1/roles/{id}` | 修改角色 |
| DELETE | `/api/v1/roles/{id}` | 删除角色 |
| GET | `/api/v1/menus/tree` | 菜单树 |
| GET | `/api/v1/roles/{id}/menus` | 角色已有菜单 |
| PUT | `/api/v1/roles/{id}/menus` | 保存角色菜单 |

角色请求：

```json
{
  "roleCode": "SALES",
  "roleName": "销售员",
  "status": 1,
  "menuIds": [10, 11, 12]
}
```

### 6.5 个人信息与日志

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/profile` | 当前用户个人资料 |
| PUT | `/api/v1/profile` | 修改姓名、手机号 |
| PUT | `/api/v1/profile/password` | 修改密码 |
| GET | `/api/v1/operation-logs` | 日志分页 |

修改密码请求：

```json
{
  "oldPassword": "old",
  "newPassword": "new-password",
  "confirmPassword": "new-password"
}
```

## 7. 前端目录和页面

```text
erp-web/src/
├─ api/auth.ts
├─ api/system/user.ts
├─ api/system/role.ts
├─ api/system/log.ts
├─ layouts/MainLayout.vue
├─ router/index.ts
├─ stores/auth.ts
├─ utils/request.ts
├─ views/login/index.vue
├─ views/system/users/index.vue
├─ views/system/roles/index.vue
├─ views/system/logs/index.vue
└─ views/profile/index.vue
```

### 页面验收细节

- 登录按钮有 loading，防止重复提交。
- Axios 自动添加 Token。
- 收到 401 时清理登录状态并跳转登录页。
- 登录后刷新页面仍能恢复用户和菜单。
- 用户列表支持筛选、分页、新增、编辑、启停、重置密码。
- 角色页面支持基础 CRUD；菜单树来不及时可只做固定角色。
- 操作日志支持按操作人、模块、日期查询。
- 所有删除操作必须二次确认。

## 8. 七天安排

| 日期 | 必须完成 |
| --- | --- |
| 第 1 天上午 | 创建前后端工程、依赖、目录、开发环境配置模板 |
| 第 1 天下午 | 返回体、异常、分页、Axios、路由、后台布局；给全组示例代码 |
| 第 2 天 | JWT 登录、当前用户、登录页、路由守卫 |
| 第 3 天 | 用户和角色接口、用户角色页面 |
| 第 4 天 | 菜单、个人信息、修改密码、操作日志、接口文档 |
| 第 5 天 | 合并五人代码，处理公共配置、菜单、路由、依赖冲突 |
| 第 6 天 | 修复认证问题，检查所有接口 Token，整理演示账号 |
| 第 7 天 | 锁定版本，协助构建部署，补 README 和接口入口 |

## 9. 本人代码规范

- Controller 只接收参数和返回结果，不写 SQL、不堆业务判断。
- Service 负责业务校验和事务。
- Mapper 只负责数据库访问。
- DTO 用于请求，VO 用于响应，禁止直接把 Entity 全字段返回前端。
- 类名使用 PascalCase，方法和变量使用 camelCase，常量使用 UPPER_SNAKE_CASE。
- REST 地址使用复数和短横线，不使用动词式中文拼音。
- 所有新增、修改请求加 `@Valid`。
- 不使用 `System.out.println`，统一使用 SLF4J 日志。
- 不捕获异常后静默忽略。
- 不在代码中写数据库密码、JWT 密钥和服务器地址。
- 前端统一使用 Composition API 和 `<script setup lang="ts">`。
- Vue 页面请求放在 `api/`，不在组件里直接创建 Axios 实例。
- TypeScript 不滥用 `any`；表单、列表和响应建立类型。
- 公共菜单、路由、依赖版本只能由你统一修改。

## 10. Git 规范

- 只在 `feature/system` 开发。
- 小步提交，建议提交：
  - `chore: initialize backend and frontend projects`
  - `feat(system): add jwt authentication`
  - `feat(system): add user and role management`
  - `fix(system): handle expired token correctly`
- 不提交 IDE 配置、构建产物、本地配置和数据库密码。
- 合并其他成员分支前先备份可运行版本。
- 第 5 天以后不进行大规模目录重构。

## 11. 联调交接

你需要在第 1 天下午向全组交付：

- 工程启动命令。
- 后端模块示例。
- 前端页面和路由接入示例。
- 统一返回格式。
- Axios 使用方式。
- SQL 文件命名规则。
- Git 分支规则。

## 12. 测试清单

- 正确账号登录成功。
- 错误密码登录失败。
- 禁用用户不能登录。
- 无 Token 访问业务接口返回 401。
- 过期 Token 返回 401。
- 用户名重复时新增失败。
- 修改密码后旧密码不能登录。
- 删除、禁用和重置密码按钮工作正常。
- 前端生产构建通过。
- Knife4j 页面可访问并能携带 Token 调试。

## 13. 完成定义

- 后端和前端均可启动。
- 登录到后台主界面完整可演示。
- 用户、角色至少完成可用 CRUD。
- 公共规范已被其他成员采用。
- 自动化测试或手工测试记录齐全。
- 代码已合入集成分支且无阻断问题。

