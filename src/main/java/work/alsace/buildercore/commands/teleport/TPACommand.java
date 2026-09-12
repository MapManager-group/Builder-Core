package work.alsace.buildercore.commands.teleport;

import work.alsace.buildercore.BuilderCore;

public class TPACommand extends AbstractTPACommand {

    private static final String USAGE = "<gray>正确指令:\n<white>/tpa <玩家></white> - 请求传送到该玩家的位置</gray>";

    public TPACommand(BuilderCore plugin) {
        super(plugin, false, USAGE);
    }
}