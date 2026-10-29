# 权限配置

[返回目录](../README.md)

权限声明见 [plugin.yml](../src/main/resources/plugin.yml)。先按用途授予权限包，再补充数量或管理节点。

## 权限包

| 权限包 | 内容 |
| --- | --- |
| `buildercore.pack.teleport` | 家、返回、传送点、TPA、传送屏蔽及 TP 入口/坐标权限；不含 `.tp.player` |
| `buildercore.pack.warpset` | 设置、删除公共传送点 |
| `buildercore.pack.public` | 建筑保护、传送包、建筑工具、个人时间/天气及自己的末影箱入口 |
| `buildercore.pack.private` | 建筑保护、传送包、建筑工具，另含 `.tp.player` 和世界时间/天气 |
| `buildercore.admin` | 私人建筑包、传送点管理、管理命令、操作他人和绕过限制；默认 OP |

公共包不含世界时间/天气和 `.tp.player`。私人包只有 `buildercore.commands.endersee.self`，缺少入口 `buildercore.commands.endersee`；管理员包也未显式补齐该入口，需要时另授。以上权限包都不设置家数量。

## 命令权限

普通命令的入口通常为 `buildercore.commands.<命令名>`，例如 `/fly` 对应 `buildercore.commands.fly`。别名沿用原命令权限。例外及额外检查如下：

| 命令或功能 | 权限 |
| --- | --- |
| `/buildercore` | `buildercore.admin` |
| `/night` | `buildercore.commands.nightvision` |
| `/tpa`、`/tpahere`、`/tpaccept`、`/tpdeny` | `buildercore.commands.tpa` |
| `/buildertools ability\|armorcolor\|banner\|special` | 对应 `buildercore.menu.<子命令>`；`/armorcolor`、`/banner` 相同 |
| `/buildertools rc` | `buildercore.commands.rc`，默认 OP |
| `/cushiontools` | `buildercore.commands.cushiontools`，另需 `buildercore.protect.interact`、`buildercore.protect.place`、`buildercore.protect.break` |
| `/tp <玩家>` | 基础权限 + `buildercore.commands.tp.player` |
| `/tp <x> <y> <z>` | 基础权限 + `buildercore.commands.tp.location` |
| `/tp` 移动其他玩家 | 上述对应权限 + `buildercore.commands.tp.other` |
| `/endersee` | 入口权限 + `buildercore.commands.endersee.self` |
| `/endersee <玩家>` | 入口权限 + `buildercore.commands.endersee.other` |
| `//p1`、`//p2`、`//pa`、`//s`、`//r`、`//f`、`//c` | `buildercore.aliases` + WorldEdit 对应权限 |
| `//twist`、`//derot`、`//scale`、`//cuboid`、`//convex`、`/undo` | 对应 `buildercore.commands.<名称>` + `buildercore.aliases` + WorldEdit 对应权限 |
| `/afk`、`/agree`、`/disagree` | 无额外命令权限 |

`fly`、`speed`、`gamemode`、`ptime`、`pweather`、`home`、`sethome`、`delhome` 操作指定玩家时，还需相应的 `buildercore.commands.<名称>.other`，并保留入口权限。部分命令显式指定自己时也进入该分支。

`invsee` 和 `endersee` 可以修改实际库存，`whois` 可查看 IP、UUID 和位置，相关管理权限应按需授予。

## 家数量

授予 `buildercore.home.max.<数量>`（如 `buildercore.home.max.3`）或 `buildercore.home.max.unlimited`。取有效授权的最大值，否定节点不增加容量；没有数量节点时为 0。设置自己的家和查看列表时刷新配额，已有家不因配额减少而删除。为他人设置家不检查其配额。

## 建筑与旁观

| 节点 | 功能 |
| --- | --- |
| `buildercore.protect.*` | 包含 `break`、`place`、`interact`、`lectern`、`ignite`、`sign`、`item_frame`、`farmland`、`armor_stand` |
| `buildercore.ability.irondoor` / `buildercore.ability.terracotta` | 铁门开关 / 釉面陶瓦旋转，还需交互、放置和破坏权限 |
| `buildercore.service.quickopen` | 潜影盒只读预览、自己的末影箱快捷打开 |
| `buildercore.spectate.player` / `buildercore.spectate.entity` | 旁观玩家 / 其他实体，默认允许 |
| `buildercore.spectate.teleport` + `buildercore.event.spectate` | 旁观传送需同时通过两项检查；前者默认允许，后者未包含在公共/私人包中 |
| `buildercore.afk.bypass` | 绕过自动 AFK，默认 OP |
| `buildercore.blocked.bypass` | 绕过受限命令拦截，默认 OP |

修改 `plugin.yml` 的命令或权限包声明后需重启服务端。
