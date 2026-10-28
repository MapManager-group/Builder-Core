# 插件集成

[返回文档目录](README.md)

## MapManager v4

关闭 `teleport.check-mmc-permissions` 或未启用 MapManager-Core 时，TP/TPA 只执行原有命令检查。开启且 MapManager-Core 已启用时：

- 直接 TP（玩家 TP、坐标 TP、`/tphere`）要求实际移动玩家的准入结果为允许，且有效角色为 `ADMIN` 或 `BUILDER`。
- TPA/TPAHere 要求实际移动玩家的准入结果为允许，可用于 MMC 允许访问的公开地图。
- 同世界传送也检查；未登记为 MMC 地图的世界允许。MMC 服务尚未就绪、已撤销或检查异常时拒绝。
- 玩家 TP 在延迟执行时重查；TPA 在接受时重查准入，并检查请求发起者仍具有 TPA 命令权限。

此检查只覆盖 TP/TPA 相关命令，不构成所有世界进入途径的权限保护。BuilderCore 不再检查 `multiverse.access.*` 节点。

## PlaceholderAPI 与代理消息

启用 PlaceholderAPI 后注册 `%buildercore_afk%`：暂离时返回 `afk.placeholder`，否则返回空字符串。自动 AFK 同样在启用 PlaceholderAPI 时注册，根据移动事件更新活动时间，移动或转动视角会取消暂离；聊天和执行命令不更新该计时。`buildercore.afk.bypass` 默认 OP 拥有。手动 `/afk` 始终可用并广播状态变化，自动 AFK 不广播。

同时每秒通过插件消息通道 `alsace:meta` 依次发送三个 UTF 字符串（`%multiverse_world_alias%`、`%vault_prefix%`、`%vault_suffix%` 的解析结果）及一个 AFK 布尔值。服务端本插件没有直接修改 Tab 列表，代理端需另有接收实现；相关占位符还需对应的 PlaceholderAPI 扩展及数据提供插件。

## 用户协议

默认关闭。启用前填写协议标题/URL 和 MySQL 连接信息，并确认账号具有数据库权限。MySQL 与 SQLite JDBC 驱动已打包到插件 JAR，无需另装驱动。

启用后，未同意玩家会收到协议链接及按钮，禁止移动坐标、聊天和其他命令，允许转动视角；查询期间也保持限制；`/agree` 等待同意记录保存成功后才解除限制，保存失败会提示重试，`/disagree` 删除记录并踢出玩家。协议确认记录保存在 MySQL 的 `player_agreement` 表。

启用协议也会连接本地 `inventory.db` 并建表，但当前没有将背包/位置保存与恢复接入玩家事件，不能据此宣称支持背包同步或跨服背包恢复。

## WorldEdit 与 floodgate

WorldEdit 快捷命令需要服务端提供对应命令，见[快捷命令](shortcuts.md)。`floodgate` 仅声明为软依赖，当前没有专用集成逻辑。
