# 安装与配置

[返回目录](../README.md)

## 安装

插件按 Java 21、Paper 1.21 API 编译，运行需要 Java 21 或更高版本，且须满足所选 Paper 服务端的 Java 要求。坐垫工具另需 Paper 26.3 的坐垫实体和拆除事件 API；`api-version: '1.20'` 不是完整的版本兼容承诺。

1. 将构建得到的 JAR 放入服务端 `plugins` 目录并启动，编译方法见[开发](development.md#编译)。
2. 编辑 `plugins/BuilderCore/config.yml`；按需安装下方列出的集成插件后，重启服务端。
3. 授予[权限包](permissions.md)，另行配置家数量。
4. 执行 `/buildercore reload`；协议开关、数据库连接或 WorldEdit 快捷命令开关有变更时重启服务端。

## 配置与重载

完整默认配置见 [config.yml](../src/main/resources/config.yml)。重载会读取配置、重新加载在线玩家的家和公共传送点，不重新注册命令或初始化数据库。

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `backHistory` | `5` | 返回历史容量，至少保留 1 条 |
| `teleport.check-mmc-permissions` | `true` | TP/TPA 检查 MapManager v4 权限 |
| `afk.threshold` | `300` | 自动暂离阈值，单位秒；启用 PlaceholderAPI 时每 60 秒检查一次 |
| `afk.placeholder` | `[AFK]` | 暂离占位符文本，支持 `&` 颜色代码 |
| `agree.agreement` | `false` | 是否要求同意用户协议；修改后重启 |
| `agree.url-title`、`agree.url` | 示例标题、地址 | 协议链接，启用前替换 |
| `database.host` | `your_mysql_host:3306` | MySQL 地址，端口写在此字段中 |
| `database.database`、`database.username`、`database.password` | 示例值 | 协议数据库及凭据；修改后重启 |
| `special-items` | 物品列表 | 特殊物品菜单，最多 54 槽；支持服务端物品语法，无效项写入日志 |
| `worldedit-extra-aliases` | `true` | 启用 `//p1`、`//p2`、`//pa`、`//s`、`//r`、`//f`、`//c`；修改后重启 |

`building-settings` 下的开关可重载：

| 配置项 | 默认值 | 拦截范围 |
| --- | --- | --- |
| `disable-redstone` | `false` | 部分红石方块的物理事件 |
| `disable-gravity-physics` | `false` | 变化材质具有重力的物理事件 |
| `disable-physics` | `false` | 其他方块物理事件 |
| `disable-entity-explode` | `true` | 实体爆炸及对应爆炸伤害 |
| `disable-block-explode` | `true` | 方块爆炸及对应爆炸伤害 |
| `disable-leaves-decay` | `true` | 树叶衰减 |
| `disable-farmland-trample` | `true` | 玩家踩踏耕地 |
| `disable-dragon-egg-teleport` | `true` | 龙蛋传送 |

三个物理开关分别生效，楼梯、栅栏、栅栏门、墙、门和箱子类放行；它们不等同于停用全部红石或重力机制。耕地开关关闭后仍受保护权限限制。固定保护行为见[建筑交互](features.md#环境与保护)。

## 可选集成

| 插件 | 功能 |
| --- | --- |
| MapManager-Core v4 | 检查 TP/TPA 的世界准入和建筑角色 |
| PlaceholderAPI | `%buildercore_afk%`、自动暂离及代理元数据消息；具体占位符还需相应扩展 |
| WorldEdit | 执行[快捷命令](commands.md#worldedit-快捷命令)，仍需 WorldEdit 自身权限 |
| floodgate | 仅声明软依赖，没有专用集成逻辑 |

自动暂离只以移动或转动视角判断活动，聊天和命令不刷新计时；移动会解除暂离。`%buildercore_afk%` 在非暂离时为空。

### MapManager v4

开启检查且 MapManager-Core 已启用时，直接 TP 要求实际移动玩家获准进入目标地图，且角色为 `ADMIN` 或 `BUILDER`；TPA 只要求获准进入。未登记的世界放行，MMC 服务未就绪、已撤销或检查失败时拒绝。关闭检查或未启用 MMC 时，只检查命令权限。

该规则覆盖玩家 TP、坐标 TP、`/tphere` 和 TPA，不覆盖家、传送点、返回或其他插件的传送。全路径世界准入应由世界管理插件负责；BuilderCore 不检查 `multiverse.access.*`。

### 用户协议

启用前填写 `agree` 和 MySQL 配置。玩家在查询和确认期间不能移动坐标、聊天或使用其他命令，可以转动视角。`/agree` 保存成功后解除限制；`/disagree` 尝试删除同意记录，随后踢出玩家。数据保存在 MySQL 的 `player_agreement` 表，JDBC 驱动已随 JAR 打包。

当前 MySQL 连接关闭 SSL 并允许公钥检索，应放在可信网络或加密隧道内。启用协议还会创建本地 `inventory.db`，但背包/位置同步未接入玩家事件。

## 备份与恢复

| 数据 | 位置 |
| --- | --- |
| 配置 | `plugins/BuilderCore/config.yml` |
| 家 | `plugins/BuilderCore/userdata/<UUID>.yml` |
| 公共传送点及别名 | `plugins/BuilderCore/warps.yml` |
| 坐垫模式的原背包 | `plugins/BuilderCore/cushiontools/inventories/<UUID>.dat` |
| 用户协议记录 | MySQL 的 `player_agreement` 表 |

位置文件保存失败会提示并写日志，原文件保留。坐垫背包恢复失败时保留备份并锁定工具操作，可再次执行 `/cushiontools` 重试；不要删除备份文件。返回历史、传送屏蔽和菜单会话为内存状态，离线后清除。
