package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ToggleTrail extends GenericCommand implements Listener {

	private static final Map<UUID, Material> mTrackedPlayers = new HashMap<>();
	private static @Nullable BukkitRunnable mRunnable = null;

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.toggletrail");

		List<Argument> arguments = new ArrayList<>();

		new CommandAPICommand("toggletrail")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					run((Player) sender, null);
				}
			})
			.register();

		arguments.clear();
		arguments.add(new StringArgument("material"));

		new CommandAPICommand("toggletrail")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					run((Player) sender, (String) args[0]);
				}
			})
			.register();
	}

	private static void run(Player player, @Nullable String material) {
		Material block = Material.SPONGE;
		if (material != null) {
			block = Material.getMaterial(material.toUpperCase());
		}

		if (!mTrackedPlayers.containsKey(player.getUniqueId())) {
			player.sendMessage("Toggled trail ON.");
			mTrackedPlayers.put(player.getUniqueId(), block);
			if (mTrackedPlayers.size() == 1) {
				mRunnable = new BukkitRunnable() {

					@Override
					public void run() {
						for (Map.Entry<UUID, Material> entry : mTrackedPlayers.entrySet()) {
							Player p = Bukkit.getPlayer(entry.getKey());
							if (p != null) {
								p.getLocation().getBlock().setType(entry.getValue());
								CoreProtectIntegration.logPlacement(p, p.getLocation(), entry.getValue(), entry.getValue().createBlockData());
							}
						}
					}
				};

				mRunnable.runTaskTimer(Plugin.getInstance(), 0, 2);
			}
		} else {
			player.sendMessage("Toggled trail OFF.");
			mTrackedPlayers.remove(player.getUniqueId());
			if (mTrackedPlayers.isEmpty() && mRunnable != null) {
				mRunnable.cancel();
				mRunnable = null;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuit(PlayerQuitEvent event) {
		//Remove from active players
		CharmManager.getInstance().onQuit(event.getPlayer());
		mTrackedPlayers.remove(event.getPlayer().getUniqueId());
		if (mTrackedPlayers.isEmpty() && mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}
}
