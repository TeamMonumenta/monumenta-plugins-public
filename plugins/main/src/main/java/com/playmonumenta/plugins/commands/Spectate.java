package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class Spectate extends GenericCommand implements Listener {
	public static final String SPECTATE_METAKEY = "MonumentaSpectateMetakey";

	private Plugin mPlugin;

	private static class SpectateContext {
		private final Plugin mPlugin;
		private final Location mLoc;
		private final GameMode mMode;
		private final boolean mIsFlying;

		private SpectateContext(Plugin plugin, Player player) {
			mPlugin = plugin;
			mLoc = player.getLocation();
			mMode = player.getGameMode();
			mIsFlying = player.isFlying();

			player.setGameMode(GameMode.SPECTATOR);
			player.setMetadata(SPECTATE_METAKEY, new FixedMetadataValue(plugin, this));
		}

		// Put player back and remove this metadata from them
		private void restore(Player player) {
			player.teleport(mLoc);
			player.setGameMode(mMode);
			player.setFlying(mIsFlying);
			player.removeMetadata(SPECTATE_METAKEY, mPlugin);
		}
	}

	public Spectate(Plugin plugin) {
		mPlugin = plugin;

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		/* No-argument variant which just is the sender (if they are a player) */
		CommandAPI.getInstance().register("spectate",
		                                  CommandPermission.fromString("monumenta.command.spectate"),
		                                  arguments,
		                                  (sender, args) -> {
											  if (sender instanceof Player) {
												  run(plugin, (Player)sender);
											  } else {
												  CommandAPI.fail(ChatColor.RED + "This command must be run by a player!");
											  }
		                                  });
	}

	private static void run(Plugin plugin, Player player) throws CommandSyntaxException {
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			if (player.hasMetadata(SPECTATE_METAKEY)) {
				// Put player back where they were before when they log out
				((SpectateContext)player.getMetadata(SPECTATE_METAKEY).get(0).value()).restore(player);
			} else {
				CommandAPI.fail(ChatColor.RED + "You can not use this command in spectator mode");
			}
		} else if (plugin.mSafeZoneManager.getLocationType(player).equals(SafeZoneManager.LocationType.Capital)
		           || plugin.mSafeZoneManager.getLocationType(player).equals(SafeZoneManager.LocationType.SafeZone)) {
			// Move player to spectator, remember coordinates
			new SpectateContext(plugin, player);
		} else {
			CommandAPI.fail(ChatColor.RED + "You can only use this command from within a safezone");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();

			// If the player switches out of spectator for
			if (!event.getNewGameMode().equals(GameMode.SPECTATOR)) {
				player.removeMetadata(SPECTATE_METAKEY, mPlugin);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Put player back where they were before when they log out
		if (player.hasMetadata(SPECTATE_METAKEY)) {
			((SpectateContext)player.getMetadata(SPECTATE_METAKEY).get(0).value()).restore(player);
		}
	}
}
