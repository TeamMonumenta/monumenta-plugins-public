package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class MudandMireCS extends WitheringGazeCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"To drown in mud is a terrible way to go out.",
			"A shame then, for your enemies."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_SAND;
	}

	@Override
	public @Nullable String getName() {
		return "Mud and Mire";
	}

	@Override
	public void onCast(Player player, double radius, double angle) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1f, 1.4f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1f, 1.05f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1f, 0.7f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1f, 1.33f), 8);

		new BukkitRunnable() {
			double mCurrentRadius = 1.5;

			@Override
			public void run() {
				Location loc = player.getLocation().add(0, 0.25, 0);
				double degree = 90 - angle;

				int degreeSteps = ((int) (2 * angle)) / 8;
				double degreeStep = 2 * angle / degreeSteps;
				for (int step = 0; step < degreeSteps + 1; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					Vector vec = new Vector(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
					Location l1 = loc.clone().add(vec);

					vec = new Vector(FastUtils.cos(radian1) * (mCurrentRadius - 0.5), 0, FastUtils.sin(radian1) * (mCurrentRadius - 0.5));
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
					Location l2 = loc.clone().add(vec);

					if (player.hasLineOfSight(l1)) {
						new PPCircle(Particle.SOUL, l1, 0.25).directionalMode(true).delta(0, 1, 0)
							.extraRange(0.05, 0.10).count(2).spawnAsPlayerActive(player);
						new PartialParticle(Particle.DAMAGE_INDICATOR, l1.clone().subtract(0, 0.5, 0), 1).directionalMode(true).delta(0, 1, 0).extra(0.5).spawnAsPlayerActive(player);
						new PartialParticle(Particle.BLOCK_CRACK, l1, 6).data(Material.SOUL_SOIL.createBlockData()).delta(0.35).spawnAsPlayerActive(player);
					}

					if (player.hasLineOfSight(l2)) {
						new PPCircle(Particle.SOUL, l2, 0.25).directionalMode(true).delta(0, 1, 0)
							.extraRange(0.05, 0.10).count(2).spawnAsPlayerActive(player);
						new PartialParticle(Particle.DAMAGE_INDICATOR, l2.clone().subtract(0, 0.5, 0), 1).directionalMode(true).delta(0, 1, 0).extra(0.5).spawnAsPlayerActive(player);
						new PartialParticle(Particle.BLOCK_CRACK, l2, 6).data(Material.SOUL_SOIL.createBlockData()).delta(0.35).spawnAsPlayerActive(player);
					}
				}

				if (mCurrentRadius > radius) {
					this.cancel();
				}
				mCurrentRadius += 1;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
