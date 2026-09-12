package work.alsace.buildercore.commands.teleport;

import work.alsace.buildercore.BuilderCore;

public class TPAcceptCommand extends AbstractTPAResponseCommand {

    public TPAcceptCommand(BuilderCore plugin) {
        super(plugin, plugin.getTPAHandler()::acceptTPA);
    }
}