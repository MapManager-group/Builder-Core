package work.alsace.buildercore.commands.teleport;

import work.alsace.buildercore.BuilderCore;

public class TPADenyCommand extends AbstractTPAResponseCommand {

    public TPADenyCommand(BuilderCore plugin) {
        super(plugin, plugin.getTPAHandler()::denyTPA);
    }
}