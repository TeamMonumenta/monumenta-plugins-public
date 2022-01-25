package com.playmonumenta.bungeecord.listeners;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import com.playmonumenta.bungeecord.Main;

import de.myzelyam.api.vanish.BungeeVanishAPI;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
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

			BaseComponent[] msg = new ComponentBuilder(player.getName())
				.color(ChatColor.AQUA)
				.append(operation)
				.color(ChatColor.YELLOW)
				.create();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
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

			BaseComponent[] msg = new ComponentBuilder(player.getName())
				.color(ChatColor.AQUA)
				.append(operation)
				.color(ChatColor.YELLOW)
				.append(" vanished")
				.color(ChatColor.RED)
				.create();

			for (ProxiedPlayer p : mMain.getProxy().getPlayers()) {
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void serverSwitchEvent(ServerSwitchEvent event) {
		ProxiedPlayer player = event.getPlayer();

		if (mMain.mJoinMessagesEnabled && !mOnlinePlayers.contains(player.getUniqueId())) {
			/* This player is not already online - send join message */
			mOnlinePlayers.add(player.getUniqueId());
			joinLeaveEvent(player, " joined the game",
			                mVanishEnabled && BungeeVanishAPI.isInvisible(player));
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();

		if (mMain.mJoinMessagesEnabled && mOnlinePlayers.contains(player.getUniqueId())) {
			/* This player was online - send leave message */
			mOnlinePlayers.remove(player.getUniqueId());
			joinLeaveEvent(player, " left the game",
			                mVanishEnabled && BungeeVanishAPI.isInvisible(player));
		}
	}
}
