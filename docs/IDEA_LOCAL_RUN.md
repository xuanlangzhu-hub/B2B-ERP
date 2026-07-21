# IDEA 本地启动后端

## 推荐启动方式

1. 用 IDEA 将 `erp-server` 目录作为后端项目打开，等待 Maven 依赖加载完成。
2. 确认 **Project SDK** 和 **Maven Runner JRE** 都是 JDK 21。
3. 在右上角运行配置中选择仓库自带的 `ErpApplication`。
4. 先确认本地 MySQL 已启动，并存在 `group_project` 数据库。
5. 运行后看到 `Started ErpApplication` 即表示启动成功，后端地址为 `http://localhost:8080`。

仓库里的共享运行配置位于 `erp-server/.run/ErpApplication.run.xml`。它已将 IDEA 的命令行缩短方式设为 `JAR manifest`，可以避开 Windows 中文路径导致的主类加载失败。

## 本地数据库默认配置

默认配置来自 `erp-server/src/main/resources/application.yml`：

- 数据库：`group_project`
- 用户名：`erp_local`
- 密码：`ErpLocal@2026`
- 地址：`127.0.0.1:3306`

如本机账号不同，在 IDEA 的运行配置中增加环境变量覆盖：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/group_project?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=你的用户名
DB_PASSWORD=你的密码
```

## 常见问题

### `com.sun.tools.javac.code.TypeTag :: UNKNOWN`

通常是 IDEA 使用了与项目不一致的 JDK。把 Project SDK、Module SDK、Maven Runner JRE 都统一为 JDK 21，然后重新加载 Maven。

### `找不到或无法加载主类 com.erp.ErpApplication`

本项目所在路径含中文，Windows 下直接执行 Maven 的 `spring-boot:run` 可能把参数文件中的路径编码错误。请使用右上角共享的 `ErpApplication` 配置，不要用 Maven 面板里的 `spring-boot:run`。

### JAR 无法重命名或删除

说明旧 Java 进程仍占用 `target/erp-server.jar`。先停止 IDEA 运行窗口里的旧后端，再重新构建。不要同时启动两个后端，否则还会出现 8080 端口占用。

### 数据库连接失败

先用 Navicat 或命令行确认上述本地账号能连接 `group_project`。当前本地启动不需要 SSH 隧道；只有连接服务器数据库时才需要隧道并通过环境变量覆盖数据库地址。
