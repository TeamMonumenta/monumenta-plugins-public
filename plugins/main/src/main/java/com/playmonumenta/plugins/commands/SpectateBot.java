package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class SpectateBot extends GenericCommand implements Listener {
	public static final int MAX_RADIUS = 17;
	public static final int TICK_PERIOD = 1;

	private static class SpectateContext {
		public final double yawVelocity = 0.1;
		public final double pitchVelocity = 0.05;

		public double yaw = 0;
		public double pitch = 180;
		public Player target = null;
		public Location lastTargetLoc;
		public final Player spectator;

		public SpectateContext(Player player) {
			spectator = player;
			lastTargetLoc = player.getLocation();
		}
	}

	public final Map<Player, SpectateContext> mSpectators = new HashMap<Player, SpectateContext>();
	public BukkitRunnable mRunnable = null;

	public SpectateBot(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		/* No-argument variant which just is the sender (if they are a player) */
		CommandAPI.getInstance().register("spectatebot",
		                                  CommandPermission.fromString("monumenta.command.spectatebot"),
		                                  arguments,
		                                  (sender, args) -> {
											  if (sender instanceof Player && ((Player)sender).getGameMode().equals(GameMode.SPECTATOR)) {
												  run(plugin, (Player)sender);
											  } else {
												  CommandAPI.fail(ChatColor.RED + "This command must be run by a player in spectator mode!");
											  }
		                                  });
	}

	private static Player getPlayerToSpectate(Player spectator) {
		List<Player> players = new ArrayList<Player>(Bukkit.getOnlinePlayers());
		players.removeIf((p) -> p.equals(spectator) || p.getGameMode().equals(GameMode.SPECTATOR));
		if (players.size() <= 0) {
			return null;
		}
		Collections.shuffle(players);
		return players.get(0);
	}

	private static Location computeLoc(double yaw, double pitch, Location targetLoc) {
		Location cameraLoc = targetLoc.clone();
		cameraLoc = cameraLoc.add(MAX_RADIUS * -Math.sin(yaw*(Math.PI/180)) * Math.cos((pitch)*(Math.PI/180)),
								  MAX_RADIUS * -Math.sin((pitch)*(Math.PI/180)),
								  -MAX_RADIUS * Math.cos((yaw)*(Math.PI/180)) * Math.cos((pitch)*(Math.PI/180)));
		return cameraLoc;
	}

	private void run(Plugin plugin, Player player) throws CommandSyntaxException {
		if (mSpectators.containsKey(player)) {
			player.sendMessage(ChatColor.RED + "You are no longer spectate botting");
			mSpectators.remove(player);
		} else {
			mSpectators.put(player, new SpectateContext(player));

			if (mRunnable == null) {
				mRunnable = new BukkitRunnable() {

					@Override
					public void run() {
						Iterator<Map.Entry<Player, SpectateContext>>it = mSpectators.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<Player, SpectateContext> entry = it.next();
							SpectateContext ctx = entry.getValue();

							if (ctx.target == null || !ctx.target.isOnline()) {
								ctx.target = getPlayerToSpectate(ctx.spectator);
								if (ctx.target == null) {
									ctx.spectator.sendMessage(ChatColor.RED + "No player to spectate");
									it.remove();
									continue;
								} else {
									ctx.spectator.sendMessage(ChatColor.RED + "Now spectating: " + ctx.target.getName());
								}
							}

							/* Rolling average target location - 49 parts previous location, 1 part new location */
							Location targetLoc = ctx.lastTargetLoc.multiply(49.0d).add(ctx.target.getLocation()).multiply(0.02d);

							/* Compute the camera aiming location */
							Location cameraLoc = computeLoc(ctx.yaw, ctx.pitch, targetLoc);

							/* Compute where the camera should look */
							Vector lookVect = targetLoc.toVector().subtract(cameraLoc.toVector()).normalize();
							cameraLoc.setDirection(lookVect);

							/* Move the spectator and adjust their facing direction */
							ctx.spectator.teleport(cameraLoc);

							/* Compute the next location */
							ctx.yaw += ctx.yawVelocity;
							ctx.pitch += ctx.pitchVelocity;

							/* Adjust the player's velocity towards the next location to try to reduce screen jitter */
							Location nextCameraLoc = computeLoc(ctx.yaw, ctx.pitch, targetLoc);
							ctx.spectator.setVelocity(nextCameraLoc.toVector().subtract(cameraLoc.toVector()).multiply(10.0d * TICK_PERIOD / 20.0d));
						}

						/* Cancel this runnable if there is nothing to do */
						if (mSpectators.isEmpty()) {
							this.cancel();
							mRunnable = null;
						}
					}
				};

				mRunnable.runTaskTimer(plugin, 0, TICK_PERIOD);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();

			// If the player switches out of spectator remove them from the list
			if (!event.getNewGameMode().equals(GameMode.SPECTATOR)) {
				mSpectators.remove(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// If the player leaves the game remove them from the list
		mSpectators.remove(player);
	}
}
