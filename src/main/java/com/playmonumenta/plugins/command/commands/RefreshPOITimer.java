package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.managers.POIManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.plugin.Plugin;

public class RefreshPOITimer extends AbstractCommand {
    private final POIManager mPOIManager;
	
	public RefreshPOITimer(Plugin plugin, POIManager poiManager) {
        super(
            "refreshPOITimer",
            "refresh an internal score from the scoreboard value.",
            plugin
        );
        this.mPOIManager = poiManager;
    }

    @Override
    protected void configure(ArgumentParser parser) {
        parser.addArgument("name")
            .help("name of the POI");
        parser.addArgument("value")
            .help("value to be set")
            .type(Integer.class);
    }

    @Override
    protected boolean run(CommandContext context) {
        final String name = context.getNamespace().getString("name");
        final Integer value = context.getNamespace().getInt("value");

        mPOIManager.refreshPOI(name, value);

        return true;
    }
}
