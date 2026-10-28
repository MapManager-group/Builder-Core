# 安装

[返回文档目录](README.md)

## 运行要求

项目按 Java 21、Paper 1.21 API 编译。`plugin.yml` 的 `api-version: '1.20'` 不代表已验证全部 1.20 服务端兼容性。坐垫工具另需支持对应实体和事件 API 的 Paper 26.3 服务端。

## 安装步骤

1. 将编译得到的 `BuilderCore-2.2.1-snapshot.jar` 放入服务端 `plugins` 目录，编译方法见[开发](development.md#编译)。
2. 启动后编辑 `plugins/BuilderCore/config.yml`。
3. 用权限插件授予所需[权限包](permissions.md)，另行设置家数量。
4. 安装所需的[集成插件](integrations.md)。

## 配置与重载

默认配置见 [`src/main/resources/config.yml`](../src/main/resources/config.yml)。`/buildercore reload` 调用配置加载方法，并重新加载在线玩家的家和公共传送点缓存；不重新注册命令、监听器或初始化数据库。

| 配置项 | 默认值 | 含义与生效方式 |
| --- | --- | --- |
| `backHistory` | `5` | `/back` 内存历史上限，实际至少为 1；重载后生效 |
| `teleport.check-mmc-permissions` | `true` | TP/TPA 的 MMC v4 权限检查；重载后生效 |
| `afk.threshold` | `300` | 自动 AFK 阈值，单位秒；任务每 60 秒检查一次，沿用原有 PlaceholderAPI 启用条件 |
| `afk.placeholder` | `[AFK]` | `%buildercore_afk%` 的暂离文本，支持 `&` 颜色代码；重载后生效 |
| `agree.agreement` | `false` | 用户协议功能；切换后需重启，以同步命令注册和数据库初始化 |
| `agree.url-title` / `agree.url` | 示例标题 / 示例 URL | 协议链接标题及地址，启用前替换 |
| `database.host` | `your_mysql_host:3306` | MySQL 地址，端口写在此字段中；注释里的 `port` 不被读取 |
| `database.database` / `username` / `password` | 示例值 | 协议数据库名称及凭据，修改连接信息后重启 |
| `special-items` | 材质与物品字符串列表 | 特殊物品菜单内容，支持服务端可解析的物品语法；最多 54 个槽，无分页；无效项写日志并跳过 |
| `worldedit-extra-aliases` | `true` | 启动时注册 `//p1`、`//p2`、`//pa`、`//s`、`//r`、`//f`、`//c` 执行器；修改后重启，不控制 deform/选区/undo 快捷命令 |

旧配置中的 `illegal-characters` 已不再读取，可删除。

## 建筑环境

`building-settings` 中的开关均可通过重载读取：

| 子项 | 默认值 | 实际拦截范围 |
| --- | --- | --- |
| `disable-redstone` | `false` | 部分红石相关方块的物理事件，不等同于关闭所有红石机制 |
| `disable-gravity-physics` | `false` | 变化材质具有重力的方块物理事件 |
| `disable-physics` | `false` | 其他方块物理事件；红石、重力走各自开关，楼梯、栅栏、栅栏门、墙、门及箱子类明确放行 |
| `disable-entity-explode` | `true` | 实体爆炸事件及对应爆炸伤害 |
| `disable-block-explode` | `true` | 方块爆炸事件及对应爆炸伤害 |
| `disable-leaves-decay` | `true` | 树叶衰减 |
| `disable-farmland-trample` | `true` | 玩家踩踏耕地；关闭后仍受保护权限约束 |
| `disable-dragon-egg-teleport` | `true` | 禁止龙蛋传送；设为 `false` 后本插件不再拦截 |
