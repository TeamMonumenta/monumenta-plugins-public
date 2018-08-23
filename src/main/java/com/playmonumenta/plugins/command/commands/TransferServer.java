package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.ChatColor;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.plugin.Plugin;

public class TransferServer extends AbstractPlayerCommand {
    private final ServerProperties mServerProperties;

	public TransferServer(Plugin plugin, ServerProperties serverProperties) {
        super(
            "transferServer",
            "Transfers players with or without inventory between bungee shards.",
            plugin
        );
        this.mServerProperties = serverProperties;
    }

    @Override
    protected void configure(ArgumentParser parser) {
        parser.addArgument("server")
            .help("the server name")
            .nargs("?");
        parser.addArgument("sendData")
            .help("if true and enabled, sends player data")
            .type(Boolean.class)
            .nargs("?")
            .setDefault();
    }

    @Override
    protected boolean run(CommandContext context) {
        //noinspection OptionalGetWithoutIsPresent - checked before being called
        final Player player = context.getPlayer().get();
        final String server = context.getNamespace().getString("server");
        final Boolean sendData = context.getNamespace().getBoolean("sendData");

        if (server == null) {
            try {
                NetworkUtils.getServerList((com.playmonumenta.plugins.Plugin) mPlugin, player);
            } catch (Exception e) {
                sendErrorMessage(context, "An error occurred while requesting the server list");
            }
            return true;
        }

		// Error if target server is not allowed
		if (!(mServerProperties.mAllowedTransferTargets.isEmpty()
			  || mServerProperties.mAllowedTransferTargets.contains(server))) {
            sendErrorMessage(context, "You may not transfer to that server from here!");
            sendErrorMessage(context, "Allowed servers are: " +
			                   mServerProperties.mAllowedTransferTargets.toString());
			return false;
		}

		// Default to server properties - if properties says false, no way to set to true
		boolean sendPlayerStuff = mServerProperties.getTransferDataEnabled();
		if (sendPlayerStuff && sendData != null) {
		    sendPlayerStuff = sendData;
        }

        return _transferServer(context, player, server, sendPlayerStuff);
	}

	private boolean _transferServer(CommandContext context, Player player, String server, boolean sendPlayerStuff) {
		/* Don't let the player transfer again if their inventory is still locked */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
            sendErrorMessage(context, "Player attempted to transfer with locked inventory!");
            sendErrorMessage(context, "You can fix this by logging out and back in.");
			return false;
		}

		try {
			if (sendPlayerStuff) {
				player.sendMessage(ChatColor.GOLD + "Transferring you to " + server);

				/* Mark this player as inventory locked */
				player.setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(mPlugin, true));

				InventoryUtils.removeSpecialItems(player);

				NetworkUtils.transferPlayerData((com.playmonumenta.plugins.Plugin) mPlugin, player, server);
			} else {
				player.sendMessage(ChatColor.GOLD + "Transferring you " + ChatColor.RED  + "without playerdata" + ChatColor.GOLD + " to " + server);
				NetworkUtils.sendPlayer((com.playmonumenta.plugins.Plugin) mPlugin, player, server);
			}
		} catch (Exception e) {
            sendErrorMessage(context, "Caught exception when transferring players");
			return false;
		}

		return true;
	}
}
