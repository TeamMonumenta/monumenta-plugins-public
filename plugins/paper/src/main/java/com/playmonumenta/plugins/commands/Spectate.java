package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

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

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class Spectate implements Listener {
	public static final String SPECTATE_METAKEY = "MonumentaSpectateMetakey";

	private Plugin mPlugin;

	protected static class SpectateContext {
		private final Plugin mPlugin;
		private final Location mLoc;
		private final GameMode mMode;
		private final boolean mIsFlying;

		protected SpectateContext(Plugin plugin, Player player) {
			mPlugin = plugin;
			mLoc = player.getLocation();
			mMode = player.getGameMode();
			mIsFlying = player.isFlying();

			player.setGameMode(GameMode.SPECTATOR);
			player.setMetadata(SPECTATE_METAKEY, new FixedMetadataValue(plugin, this));
		}

		// Put player back and remove this metadata from them
		protected void restore(Player player) {
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
		new CommandAPICommand("spectate")
			.withPermission(CommandPermission.fromString("monumenta.command.spectate"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					run(plugin, (Player)sender);
				} else {
					CommandAPI.fail(ChatColor.RED + "This command must be run by a player!");
				}
			})
			.register();
	}

	public static boolean run(Plugin plugin, Player player) throws WrapperCommandSyntaxException {
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			if (player.hasMetadata(SPECTATE_METAKEY)) {
				// Put player back where they were before when they log out
				((SpectateContext)player.getMetadata(SPECTATE_METAKEY).get(0).value()).restore(player);
			} else {
				CommandAPI.fail(ChatColor.RED + "You can not use this command in spectator mode");
			}
		} else if (ZoneUtils.hasZoneProperty(player, ZoneProperty.SPECTATE_AVAILABLE)) {
			// Move player to spectator, remember coordinates
			new SpectateContext(plugin, player);
			// Succeeded in making this player a spectator
			return true;
		} else {
			CommandAPI.fail(ChatColor.RED + "You can only use this command from within a safezone");
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();

			// If the player switches out of spectator
			if (!event.getNewGameMode().equals(GameMode.SPECTATOR)) {
				player.removeMetadata(SPECTATE_METAKEY, mPlugin);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Put player back where they were before when they log out
		if (player.hasMetadata(SPECTATE_METAKEY)) {
			((SpectateContext)player.getMetadata(SPECTATE_METAKEY).get(0).value()).restore(player);
		}
	}
}
