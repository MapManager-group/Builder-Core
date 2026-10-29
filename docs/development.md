# 开发

[返回目录](../README.md)

## 编译

需要 JDK 21 或更高版本及 Maven。先在 MapManager v4 源码目录安装 API，再构建 BuilderCore：

```bash
# MapManager v4 目录
mvn -pl api -am install

# BuilderCore 目录
mvn clean package
```

当前产物为 `target/BuilderCore-2.2.1-snapshot.jar`，版本以 [pom.xml](../pom.xml) 为准。MapManager API、Paper API 和 PlaceholderAPI 为 provided 依赖；JAR 内打包 MySQL/SQLite JDBC，并合并驱动服务声明。

## 检查

已缓存 Maven 依赖后，在项目根目录执行：

```bash
python3 src/test/check_teleport_access.py
```

脚本离线编译后运行 Java 断言，覆盖命令及 API 兼容、位置读写、权限、协议状态、传送、菜单和坐垫备份/恢复。`mvn test` 不会自动运行这些 `*Check` 主方法。

打包后检查 JDBC 服务声明、MySQL 驱动加载和 SQLite 实际读写：

```bash
java -ea -cp target/test-classes:target/BuilderCore-2.2.1-snapshot.jar work.alsace.buildercore.JdbcPackagingCheck
```

隔离 Paper 服务端可在 BuilderCore 启用后调用 `ProtectionEventsCheck.run(core)`，以 `-ea` 检查真实方块、实体和事件链。玩家仍使用替身；线上 MySQL、真实客户端和其他插件组合需另测。坐垫原生事件测试另需对应 Paper 26.3 API 和 Adventure 运行库，向 `CushionToolsCheck`、`CushionPlacementCheck` 传入 `26.3` 参数。

## 修改入口

| 内容 | 代码位置 |
| --- | --- |
| 启动、命令/监听器注册、配置重载 | [BuilderCore.java](../src/main/java/work/alsace/buildercore/BuilderCore.java) |
| 命令参数与权限 | `commands/`、[plugin.yml](../src/main/resources/plugin.yml) |
| 补全与帮助 | `commands/CommandCompletion.java`、`commands/CommandHelp.java` |
| 建筑菜单与环境开关 | `features/BuilderTools.java`、`features/BuildingSettings.java` |
| 坐垫工具与背包恢复 | `features/cushiontools/` |
| 家、传送点、TPA、MMC 准入 | `service/` |
| 保护与玩家事件 | `listeners/` |

上表 Java 路径均相对 `src/main/java/work/alsace/buildercore/`。修改命令需同时核对声明、注册和执行器；菜单及额外 WorldEdit 快捷命令由 `BuilderTools.register()` 注册，补全统一在 `BuilderCore.registerCompletions()` 安装。

## 数据与接口约定

- 家和传送点在主线程同步保存，成功后才更新内存；临时文件替换原文件，读取失败时中止，不用空配置覆盖损坏数据。大量自动化写入会占用 tick 时间。
- 家位于 YAML 的 `Home` 节点，传送点和别名位于 `Warps`、`Aliases`；别名方向为“别名 → 正式名称”。点号作为字面键，已有按点号拆坏的数据不自动迁移。
- `getHomes()` 返回可变实时视图，`getWarps()` 返回可变快照；位置 getter 返回原对象，`addWarp` 支持覆盖写入。命令入口另行拒绝重名。
- 旧背包序列化 API 保留原格式，损坏数据通过 `IOException` 报告，只能读取可信本地数据。

`RestartVoteCommand` 和 `DismountParrotCommand` 尚未声明或注册，不是可用命令；背包/位置数据库 API 也未接入玩家保存/恢复事件。

## 代理消息

启用 PlaceholderAPI 后每秒通过 `alsace:meta` 依次发送三个 UTF 字符串：`%multiverse_world_alias%`、`%vault_prefix%`、`%vault_suffix%` 的解析结果，然后发送 AFK 布尔值。代理端需实现接收；BuilderCore 不直接修改 Tab 列表。
