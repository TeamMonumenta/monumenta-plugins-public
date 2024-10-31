package com.playmonumenta.velocity.handlers;

import com.playmonumenta.velocity.MonumentaVelocity;
import com.playmonumenta.velocity.integrations.PremiumVanishIntegration;
import com.playmonumenta.velocity.network.VelocityClientModHandler;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class JoinLeaveHandler {
	final boolean mVanishEnabled;

	final MonumentaVelocity mPlugin;

	/* Keeps track of players that have had their join message sent */
	private final Set<UUID> mOnlinePlayers = new ConcurrentSkipListSet<UUID>();

	public JoinLeaveHandler(MonumentaVelocity main) {
		mPlugin = main;

		if (!PremiumVanishIntegration.mDisabled) {
			mPlugin.mLogger.info("Vanish support enabled - PremiumVanish plugin detected");
			mVanishEnabled = true;
		} else {
			mPlugin.mLogger.info("Vanish support disabled - no plugin detected");
			mVanishEnabled = false;
		}
	}

	private void joinLeaveEvent(Player player, String operation, boolean isVanished) {
		Collection<Player> players = mPlugin.mServer.getAllPlayers();

		Component msg = Component.text(player.getUsername())
			.color(NamedTextColor.AQUA)
			.append(Component.text(operation).color(NamedTextColor.YELLOW));
		if (!isVanished) {
			/* No vanish - send everyone the login message */
			for (Player p : players) {
				p.sendMessage(msg);
			}
		} else {
			/*
			 * Vanish is enabled and player joined vanished
			 * Only send login message to other players that have perms to see it
			 */
			int useLevel = 0;
			for (int i = 5; i > 0; i--) {
				if (player.hasPermission("pv.use.level" + Integer.toString(i))) {
					useLevel = i;
					break;
				}
			}

			msg = msg.append(Component.text(" vanished").color(NamedTextColor.RED));

			for (Player p : players) {
				int seeLevel = -1;
				for (int i = 5; i > 0; i--) {
					if (p.hasPermission("pv.see.level" + Integer.toString(i))) {
						seeLevel = i;
						break;
					}
				}
				if (seeLevel >= useLevel) {
					p.sendMessage(msg);
				}
			}
		}
	}

	@Subscribe(order = PostOrder.LATE)
	public void serverPostConnectEvent(ServerPostConnectEvent event) {
		Player player = event.getPlayer();

		if (!mOnlinePlayers.contains(player.getUniqueId())) {
			//first login, send server info to the client.
			VelocityClientModHandler.sendServerInfoPacket(player);

			/* This player is not already online - send join message */
			mOnlinePlayers.add(player.getUniqueId());
			if (mPlugin.mConfig.mJoinMessagesEnabled) {
				joinLeaveEvent(player, " joined the game",
					mVanishEnabled && PremiumVanishIntegration.isInvisible(player));
			}
		}
	}

	@Subscribe(order = PostOrder.EARLY)
	public void disconnectEvent(DisconnectEvent event) {
		if (!event.getLoginStatus().equals(LoginStatus.SUCCESSFUL_LOGIN)) {
			return;
		}
		Player player = event.getPlayer();

		if (mOnlinePlayers.contains(player.getUniqueId())) {
			/* This player was online - send leave message */
			mOnlinePlayers.remove(player.getUniqueId());

			if (mPlugin.mConfig.mJoinMessagesEnabled) {
				joinLeaveEvent(player, " left the game",
					mVanishEnabled && PremiumVanishIntegration.isInvisible(player));
			}
		}
	}
}
