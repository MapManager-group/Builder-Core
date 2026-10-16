# 权限配置

[返回文档目录](README.md)

`plugin.yml` 提供下列权限包；建议在权限插件中授予权限包，再按需增加细分权限。

| 权限包 | 包含的主要能力 |
| --- | --- |
| `buildercore.pack.teleport` | 家、返回、传送点、传送屏蔽，以及 `/tp` 入口/坐标权限、TPA；不含 `.tp.player` |
| `buildercore.pack.warpset` | 设置、删除公共传送点 |
| `buildercore.pack.public` | 建筑保护、传送包、建筑工具、个人天气/时间、自己的末影箱入口；不含 `.tp.player` |
| `buildercore.pack.private` | 同类建筑工具与传送能力，另含 `.tp.player`、世界时间/天气；缺少末影箱入口 |
| `buildercore.admin` | 管理命令、操作其他玩家、AFK/命令限制绕过及区块刷新，默认仅 OP |

## 常用细分权限

| 节点 | 用途 |
| --- | --- |
| `buildercore.protect.*` | 包含 `break`、`place`、`interact`、`lectern`、`ignite`、`sign`、`item_frame`、`farmland`、`armor_stand` 九项保护权限 |
| `buildercore.spectate.player` / `.entity` / `.teleport` | 旁观玩家、实体及旁观传送；三个节点分别默认授予所有玩家，没有声明 `buildercore.spectate.*` 通配节点 |
| `buildercore.event.spectate` | 另一监听器对旁观传送的额外检查，公共和私人包均未包含 |
| `buildercore.service.quickopen` | 持潜影盒右键空气只读预览，或快速打开自己的末影箱 |
| `buildercore.afk.bypass` | 绕过自动 AFK，默认 OP 拥有 |
| `buildercore.blocked.bypass` | 绕过受限命令拦截，默认 OP 拥有 |
| `buildercore.commands.cushiontools` | 进入坐垫工具模式，默认 OP；公共/私人包包含。操作另需 `buildercore.protect.interact`、`.place`、`.break`，退出恢复背包不受权限撤销影响 |
| `buildercore.commands.rc` | 刷新周边已加载区块，默认 OP 拥有 |

每条命令的权限见[命令表](commands.md)和[快捷命令表](shortcuts.md)。未显式定义的节点由权限插件按需授予。

## 授权注意事项

- 公共和私人包都含建筑工具、WorldEdit 快捷操作、帽子及个人时间/天气；公共包不含世界时间/天气和玩家 TP 子权限。
- 末影箱既要入口 `buildercore.commands.endersee`，又要 `.self` 或 `.other`。公共包包含入口与 `.self`；私人包只有 `.self`，管理员包也未显式补齐入口，单独授予这些包时需检查入口权限。
- 操作他人的 `.other` 节点、区块刷新、AFK 和命令限制绕过属于管理员包，不授予公共/私人包。
- 旁观传送同时受 `buildercore.spectate.teleport` 和 `buildercore.event.spectate` 检查；只有前者仍可能被拦截。
- BuilderCore 不再检查 `multiverse.access.*`。修改插件声明的权限包后需重启服务端。

## 家数量

设置自己的家之前会重新计算配额，权限变更立即用于下一次设置；现有家不会因配额减少而被删除。家数量需要另外授予 `buildercore.home.max.<数量>`（例如 `buildercore.home.max.3`）或 `buildercore.home.max.unlimited`。没有数量节点时，源码计算结果为 0；权限包不默认设定容量。
