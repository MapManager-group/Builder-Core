# 命令

[返回文档目录](README.md)

方括号表示可选参数。表中包含命令入口及执行器内的权限检查；操作他人还需保留基础命令权限。显式填写自己的名字时，部分命令也会进入指定玩家分支。

大部分命令仅供玩家使用，`/time` 只允许玩家调整当前世界。控制台可使用 `/buildercore`、`/whois`，以及指定目标的 `/fly`、`/gamemode`、`/speed`、`/ptime`、`/tp`；控制台 TP 限两个玩家名或玩家名加三个坐标。

## 建筑工具

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/buildertools [ability\|armorcolor\|banner\|special\|rc]`（`/btools`） | 打开能力、护甲配色、旗帜、特殊物品菜单，或刷新周边已加载区块；默认 `ability` | 分别为 `buildercore.menu.ability`、`buildercore.menu.armorcolor`、`buildercore.menu.banner`、`buildercore.menu.special`、`buildercore.commands.rc` |
| `/cushiontools` | 切换坐垫工具模式；再次执行恢复原背包，详见[坐垫工具](features.md#坐垫修改工具) | `buildercore.commands.cushiontools`，另需 `buildercore.protect.interact`、`.place`、`.break` |
| `/reach [距离]` | 设置方块交互距离，范围 0–64；不填重置为 4.5 | `buildercore.commands.reach` |
| `/slab`（`/sb`） | 切换双层半砖的半层破坏 | `buildercore.commands.slab` |
| `/armorcolor`、`/banner` | 直接打开相应菜单 | `buildercore.menu.armorcolor` / `buildercore.menu.banner` |
| `/debugstick`（`/dbs`）、`/head <玩家>`（`/skull`） | 获取调试棒 / 玩家头颅 | `buildercore.commands.debugstick` / `buildercore.commands.head` |
| `/hat` | 交换主手物品与头盔栏物品 | `buildercore.commands.hat` |
| `/itemname <名称>`（`/iname`） | 修改主手物品名称，支持 `&` 颜色代码；名称限一个参数 | `buildercore.commands.itemname` |
| `/itemcolor <十六进制颜色>`（`/icolor`）、`/itemnbt`（`/inbt`） | 设置皮革物品六位 RGB 颜色（可带 `#`）/ 查看物品元数据并点击复制 | `buildercore.commands.itemcolor` / `buildercore.commands.itemnbt` |

能力菜单可切换铁门、陶瓦、半砖、夜视、穿墙和进阶飞行，具体交互见[非命令功能](features.md)。

- 护甲配色：RGB 左键加 8、右键减 8、Shift 点击随机，点击护甲领取。
- 旗帜编辑：选择底色、颜色和图案，支持随机、撤销、重置、领取，最多 16 层。
- 特殊物品：点击领取配置物品。上述菜单领取的物品在背包满时掉到脚下。
- 区块刷新：`/buildertools rc` 只刷新已加载区块，半径为客户端视距、世界视距和 8 的最小值。

## 玩家状态

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/fly [玩家]` | 切换自己或指定玩家的飞行 | 自己：`buildercore.commands.fly`；他人：`buildercore.commands.fly.other` |
| `/speed <速度>`、`/speed <walk\|fly> <速度> [玩家]` | 设置步行或飞行速度，常规范围 0.1–10 | 自己：`buildercore.commands.speed`；他人：`buildercore.commands.speed.other` |
| `/gamemode <模式> [玩家]`（`/gm`） | 设置游戏模式，支持 `creative/survival/adventure/spectator` 及简写 | 自己：`buildercore.commands.gamemode`；他人：`buildercore.commands.gamemode.other` |
| `/advfly`（`/af`） | 切换进阶飞行，减少停止时的水平惯性 | `buildercore.commands.advfly` |
| `/noclip`（`/nc`） | 切换自动穿墙 | `buildercore.commands.noclip` |
| `/scale [倍数]` | 调整自身尺寸，范围 0.0625–16；不填重置为 1 | `buildercore.commands.scale` |

`/speed <速度>` 按当前是否飞行选择速度类型，使用 0.1–10 范围。模式支持 `creative/c/1`、`survival/s/0`、`adventure/a/2`、`spectator/sp/3`。

自动穿墙检查附近实心方块，在创造与旁观间切换；创造模式在实心方块上潜行也可进入旁观，离开后恢复创造。进阶飞行减少停止时的水平惯性，不替代飞行许可。铁门/陶瓦开关、菜单会话、穿墙和进阶飞行状态在离线后清理。

## 家与公共传送点

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/sethome <名称>`、`/delhome <名称>` | 设置 / 删除自己的家 | `buildercore.commands.sethome` / `buildercore.commands.delhome` |
| `/home <名称>`、`/homes` | 传送到家 / 列出家 | `buildercore.commands.home` / `buildercore.commands.homes` |
| `/sethome <玩家>:<名称>`、`/delhome <玩家>:<名称>`、`/home <玩家>:<名称>` | 管理或访问其他玩家的家 | 各命令基础权限 + 对应的 `buildercore.commands.sethome.other` / `buildercore.commands.delhome.other` / `buildercore.commands.home.other` |
| `/warps`、`/warp <名称>` | 按世界列出传送点 / 前往传送点 | `buildercore.commands.warps` / `buildercore.commands.warp` |
| `/setwarp <名称> [别名]`、`/delwarp <名称>` | 设置 / 删除公共传送点 | `buildercore.commands.setwarp` / `buildercore.commands.delwarp` |
| `/back [步数]` | 返回历史位置；默认返回上一个位置，记录数由 `backHistory` 控制 | `buildercore.commands.back` |

家名称按小写保存，`/homes` 可点击传送。自己的家受[数量权限](permissions.md#家数量)限制；为他人设置家不检查其配额，保存执行者的位置。同名家和传送点不能直接覆盖。

`/warps` 只列出世界已加载的传送点，不按 MMC 或 Multiverse 权限过滤。家、返回和传送点不调用 TP/TPA 的 MMC 检查，其他插件仍可拦截传送。

`/back` 默认最多保留 5 条传送前位置，实际容量至少为 1；指定步数会弹出相应记录，不单独记录死亡位置，离线清空。

家和传送点名称不作格式限制，中文、点号等按完整名称保存；仍按单个命令参数和 `玩家:名称` 语法解析。`/delwarp` 可删除目标世界未加载的记录。别名读写与清理的既有限制保持不变，见[已知问题](development.md#已知问题)。

## 玩家与坐标传送

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/tpa <玩家>`、`/tpahere <玩家>` | 请求传送到对方 / 请求对方传送过来 | `buildercore.commands.tpa` |
| `/tpaccept`、`/tpdeny` | 接受 / 拒绝待处理传送请求 | `buildercore.commands.tpa` |
| `/tp <玩家>` | 传送到在线玩家 | `buildercore.commands.tp` + `buildercore.commands.tp.player` |
| `/tp <玩家A> <玩家B>` | 将 A 传送至 B | `buildercore.commands.tp` + `buildercore.commands.tp.player` + `buildercore.commands.tp.other` |
| `/tp <x> <y> <z>` | 传送自己到坐标，支持 `~` 相对坐标 | `buildercore.commands.tp` + `buildercore.commands.tp.location` |
| `/tp <玩家或@s> <x> <y> <z>` | 将指定玩家传送到坐标 | `buildercore.commands.tp` + `buildercore.commands.tp.location`；目标不是自己时还需 `buildercore.commands.tp.other` |
| `/tphere <玩家>` | 将指定玩家传送到自己身边 | `buildercore.commands.tphere` |
| `/tpignore` | 切换是否屏蔽强制传送 | `buildercore.commands.tpignore` |

TPA 有效期 60 秒，每个接收者保留一条，新请求覆盖旧请求。玩家之间的直接 TP 延迟 1 秒，其余 TP 用法没有该延迟。MMC 检查见[插件集成](integrations.md#mapmanager-v4)。

`/tpignore` 只拦截别人用单参数 `/tp <玩家>` 传送到自己身边，不拦截双玩家 TP、坐标 TP、`/tphere` 或 TPA；离线清除。

坐标支持数值与 `~`，不支持 `^`。四参数 TP 使用被移动玩家的世界，相对坐标以执行玩家位置为基准，控制台执行时以被移动玩家为基准。

## 时间与天气

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/night`（`/nv`、`/n`） | 切换永久夜视 | `buildercore.commands.nightvision` |
| `/ptime <时间> [玩家]` | 设置固定个人时刻，或 `reset` 恢复世界时间 | 自己：`buildercore.commands.ptime`；他人：`buildercore.commands.ptime.other` |
| `/pweather <clear\|rain\|reset> [玩家]` | 强制个人晴天 / 降雨 / 恢复世界天气 | 基础：`buildercore.commands.pweather`；指定玩家还需 `.other` |
| `/time <时间>`、`/time set\|add <数值>` | 设置或增加当前世界时间，仅玩家可用 | `buildercore.commands.time` |
| `/weather <clear\|rain\|thunder>` | 设置所在世界天气 | `buildercore.commands.weather` |

时间支持数值及 `sunrise`、`day`、`morning`、`noon`、`afternoon`、`sunset`、`night`、`midnight`。个人时间固定在指定时刻，`reset` 恢复世界时间；个人与世界命令使用相同关键词数值。

`/pweather clear` 强制个人晴天，`rain` 强制个人降雨，`reset` 恢复跟随世界天气。指定玩家时需要基础权限及 `buildercore.commands.pweather.other`。世界时间和天气设置会影响同世界玩家。

## 管理与协作

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/endersee [玩家]` | 查看自己或在线玩家的末影箱 | 自己：`buildercore.commands.endersee.self`；他人：`buildercore.commands.endersee.other`；命令入口还需 `buildercore.commands.endersee` |
| `/afk` | 手动切换暂离状态 | 无额外要求 |
| `/agree`、`/disagree` | 同意 / 拒绝用户协议；仅在 `agree.agreement: true` 时注册 | 无额外要求 |
| `/vanish`、`/invsee <玩家>`、`/whois <玩家>` | 隐身 / 打开在线玩家背包 / 查询在线玩家信息（含 IP、UUID、位置） | `buildercore.commands.vanish` / `.invsee` / `.whois` |
| `/buildercore info\|reload` | 查看插件信息 / 重载配置 | `buildercore.admin` |

`/invsee` 与 `/endersee` 打开实际库存，可以操作物品。`/vanish` 向其他玩家隐藏自己、添加隐身效果，并尝试执行 `/tpignore`；退出隐身不恢复先前的屏蔽状态。

`/afk`、`/agree`、`/disagree` 无额外命令权限。自动 AFK 与协议的启用条件见[插件集成](integrations.md)。
