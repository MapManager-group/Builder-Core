# 开发

[返回文档目录](README.md)

## 编译

当前项目版本为 `2.2.1-snapshot`，需要 JDK 21 或更高版本和 Maven。

先在 MapManager v4 源码目录执行：

```bash
mvn -pl api -am install
```

再在 BuilderCore 项目目录执行：

```bash
mvn clean package
```

产物为 `target/BuilderCore-2.2.1-snapshot.jar`。`mapmanager-api:4.0.0-SNAPSHOT` 为 provided 依赖，不打入 JAR；运行时 MapManager-Core 可选。

## 检查

命令应同时核对 [`plugin.yml`](../src/main/resources/plugin.yml)、`BuilderCore.registerCommands()` 和执行器内部权限；菜单命令和额外 WorldEdit 快捷命令由 `BuilderTools.register()` 注册。权限包或命令声明改变后需重启服务端。

```bash
python3 src/test/check_teleport_access.py
```

权限矩阵脚本要求已缓存 Maven 依赖，会离线编译并运行 `SelectedFixesCheck`（旧公开方法、API 可变返回值与覆盖写入、自由名称/副手操作兼容、全部命令的补全注册/用法、前缀/权限/隐身过滤、否定配额、别名冲突/重载/删除、损坏 YAML 保留、返回失败重试、非有限坐标、序列化错误、协议等待/失败/成功与请求清理，以及名称保存/重载、传送点删除、只读预览、速度、时间/天气、元数据与龙蛋开关）及 `TeleportAccessCheck` 的 Java 断言，覆盖角色、开关、服务未就绪/撤销、未登记世界和无 MMC API 的路径。脚本也运行 `CushionPlacementCheck`，覆盖右键放置、方块交互单独禁止、完整事件取消、样式与放置事件拦截；并运行 `CushionToolsCheck`，检查选区边界、地面搜索顺序、权限和背包备份/恢复（含损坏文件及保存失败）；换用已缓存的 Paper 26.3 API 及对应 Adventure 运行库，以 `java -ea` 运行并传入 `26.3` 参数，可额外检查三轴移动、左右键事件去重及 Paper 拆除事件拦截。坐垫实现集中在 `features/cushiontools/`，原 `CushionListener` 已替换。`mvn test` 不会自动执行这些 `*Check` 主方法。以上检查不替代真实 Paper 服务端上的命令、事件与其他插件集成验证。

隔离 Paper 服务端可用 `-ea` 运行测试插件，在 BuilderCore 启用后调用 `ProtectionEventsCheck.run(core)`；该检查使用真实方块、物品和实体测试取消链路、双手、光源权限、长距离半砖及双击移除。玩家使用边界替身，不能代替客户端测试。本次服务端检查结果见[审查记录](audit.md#验证)。

## 数据文件

家保存到 `plugins/BuilderCore/userdata/<UUID>.yml` 的 `Home` 节点，公共传送点保存到 `plugins/BuilderCore/warps.yml` 的 `Warps`、`Aliases` 节点。返回历史、传送屏蔽和能力开关等只保存在内存中；备份位置数据时需包含上述 YAML 文件。名称中的点号作为字面键保存，已有正常 YAML 结构保持兼容；旧版本已按点号拆坏的数据不会自动修复。

## 数据写入与错误处理

家和传送点在命令返回成功前完成保存，使用临时文件替换原文件，避免多个异步任务打乱写入顺序。读取 YAML 失败时中止加载，保留原文件；保存失败会提示并记录日志，不更新内存状态。文件内容保留原有节点格式。位置命令频率较低，仍在主线程执行文件写入；大量自动化调用可能占用 tick 时间。

别名统一按“别名 → 正式名称”保存；重载、列表和按别名删除采用相同方向，名称与别名不能冲突。位置数据在读取时重新解析世界，支持世界晚于玩家数据加载。

## 未开放的功能

重启投票 `RestartVoteCommand` 和卸下鹦鹉 `DismountParrotCommand` 只有源码，未在 `plugin.yml` 声明或启动时注册，不能作为可用命令使用。背包和位置数据库方法尚未接入玩家保存/恢复事件。

## JDBC 打包检查

JAR 内包含 MySQL Connector/J 9.6.0 和 SQLite JDBC 3.53.0.0，合并 `META-INF/services/java.sql.Driver` 服务声明。驱动通过本插件类加载器初始化，原有连接参数、表结构和读写流程保持不变。驱动打包方式参考 [SQLite JDBC](https://github.com/xerial/sqlite-jdbc#hint-for-maven-shade-plugin) 和 [MySQL 驱动初始化](https://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-connect-drivermanager.html)。

打包后可运行以下检查，验证两个驱动的服务声明，以及 SQLite 原生库的内存读写；该检查不连接 MySQL 实例：

```bash
java -ea -cp target/test-classes:target/BuilderCore-2.2.1-snapshot.jar work.alsace.buildercore.JdbcPackagingCheck
```
