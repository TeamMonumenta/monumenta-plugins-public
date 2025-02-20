package com.playmonumenta.bungeecord.listeners;

import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.network.BungeeClientModHandler;
import de.myzelyam.api.vanish.BungeeVanishAPI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class EventListener implements Listener {
	final boolean mVanishEnabled;

	final Main mMain;

	/* Keeps track of players that have had their join message sent */
	private final Set<UUID> mOnlinePlayers = new ConcurrentSkipListSet<UUID>();

	public EventListener(Main main) {
		mMain = main;

		PluginManager mgr = mMain.getProxy().getPluginManager();

		if (mgr.getPlugin("PremiumVanish") != null) {
			mMain.getLogger().info("Vanish support enabled - PremiumVanish plugin detected");
			mVanishEnabled = true;
		} else {
			mMain.getLogger().info("Vanish support disabled - no plugin detected");
			mVanishEnabled = false;
		}
	}

	private void joinLeaveEvent(ProxiedPlayer player, String operation, boolean isVanished) {

		if (!isVanished) {
			/* No vanish - send everyone the login message */

			TextComponent msg = Component.text()
				.append(Component.text(player.getName(), NamedTextColor.AQUA))
				.append(Component.text(operation, NamedTextColor.YELLOW))
				.build();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
				p.sendMessage(BungeeComponentSerializer.get().serialize(msg));
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

			TextComponent msg = Component.text()
				.append(Component.text(player.getName(), NamedTextColor.AQUA))
				.append(Component.text(operation, NamedTextColor.YELLOW))
				.append(Component.text(" vanished", NamedTextColor.RED))
				.build();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
				int seeLevel = -1;
				for (int i = 5; i > 0; i--) {
					if (p.hasPermission("pv.see.level" + Integer.toString(i))) {
						seeLevel = i;
						break;
					}
				}
				if (seeLevel >= useLevel) {
					p.sendMessage(BungeeComponentSerializer.get().serialize(msg));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void serverSwitchEvent(ServerSwitchEvent event) {
		ProxiedPlayer player = event.getPlayer();

		if (!mOnlinePlayers.contains(player.getUniqueId())) {
			//first login, send server info to the client.
			BungeeClientModHandler.sendServerInfoPacket(player);

			/* This player is not already online - send join message */
			mOnlinePlayers.add(player.getUniqueId());

			if (mMain.mJoinMessagesEnabled) {
				joinLeaveEvent(player, " joined the game",
					mVanishEnabled && BungeeVanishAPI.isInvisible(player));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();

		if (mOnlinePlayers.contains(player.getUniqueId())) {
			/* This player was online - send leave message */
			mOnlinePlayers.remove(player.getUniqueId());

			if (mMain.mJoinMessagesEnabled) {
				joinLeaveEvent(player, " left the game",
					mVanishEnabled && BungeeVanishAPI.isInvisible(player));
			}
		}
	}
}
