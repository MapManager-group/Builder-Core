# 快捷命令

[返回文档目录](README.md)

以下命令转发到 WorldEdit，服务端需提供相应命令，玩家同时需要 WorldEdit 的操作权限。

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `//p1`（`//1`）、`//p2`（`//2`） | 设置第一、第二选区点 | `buildercore.aliases` |
| `//pa`、`//s`、`//r`、`//f`、`//c` | 转发到 WorldEdit 的 paste、set、replace、flip、copy | `buildercore.aliases` |
| `//twist <轴> <角度>`、`//derot <轴> <角度>`、`//scale <数值>` | WorldEdit deform 快捷操作 | 对应 `buildercore.commands.twist` / `.derot` / `.scale`，源码还检查 `buildercore.aliases` |
| `//cuboid`（`//cub`）、`//convex`（`//con`）、`/undo`（`/u`） | 切换 WorldEdit 选区 / 撤销 | 对应 `buildercore.commands.cuboid` / `.convex` / `.undo`；源码还检查 `buildercore.aliases` |

`//twist` 沿轴扭转，`//derot` 绕轴旋转，轴支持 `x`、`y`、`z`，角度为整数。`//scale` 缩放选区内容；单斜杠 `/scale` 调整玩家尺寸。`/undo` 撤销一次，不转发额外参数。

`worldedit-extra-aliases` 控制前两行命令的执行器注册，默认开启，修改后重启；不控制形变、选区模式和撤销快捷命令。普通命令的别名（如 `/gm`、`/nv`）直接列在[命令表](commands.md)中。
