package work.alsace.buildercore.commands.teleport;

import work.alsace.buildercore.BuilderCore;

public class TPAHereCommand extends AbstractTPACommand {

    private static final String USAGE = "<gray>正确指令:\n<white>/tpahere <玩家></white> - 请求该玩家传送到你的位置</gray>";

    public TPAHereCommand(BuilderCore plugin) {
        super(plugin, true, USAGE);
    }
}