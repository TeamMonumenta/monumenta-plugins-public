package com.playmonumenta.bossfights.spells;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellDetectionCircle implements Spell {
	private Plugin mPlugin;
	private int mRadius;
	private int mDuration;
	private Location mCenter;
	private Location mTarget;

	public static void registerCommand(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("detection_circle", new LiteralArgument("detection_circle"));
		arguments.put("center_pos", new LocationArgument());
		arguments.put("radius", new IntegerArgument(1, 2000));
		arguments.put("duration", new IntegerArgument(1, Integer.MAX_VALUE));
		arguments.put("redstone_pos", new LocationArgument());

		CommandAPI.getInstance().register("mobspell",
		                                  new CommandPermission("mobspell.detectioncircle"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      new SpellDetectionCircle(plugin, (Location)args[0], (Integer)args[1],
		                                                               (Integer)args[2], (Location)args[3]).run();
		                                  }
		);
	}

	public SpellDetectionCircle(Plugin plugin, Location center, int radius, int duration, Location target) {
		mPlugin = plugin;
		mRadius = radius;
		mDuration = duration;
		mCenter = center;
		mTarget = target;
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			private int runs_left = mDuration;
			private Random mRand = new Random();

			@Override
			public void run() {
				int  n = mRand.nextInt(40) + 50;
				double precision = n;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(mCenter.getWorld(), 0, mCenter.getY() + 5, 0);
				double rad = mRadius;
				double angle = 0;
				for (int j = 0; j < precision; j++) {
					angle = (double)j * increment;
					particleLoc.setX(mCenter.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(mCenter.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(mCenter.getY() + 5 * (double)(mRand.nextInt(120) - 60) / (60));
					particleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 0.02, 0.02, 0);
				}

				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					if (player.getLocation().distance(mCenter) < mRadius &&
						(player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
						mTarget.getBlock().setType(Material.REDSTONE_BLOCK);
						this.cancel();
						break;
					}
				}
				if (runs_left <= 0) {
					this.cancel();
				}
				runs_left -= 5;
			}
		}.runTaskTimer(mPlugin, 1, 5);
	}

	@Override
	public int duration() {
		return 1;
	}
}
