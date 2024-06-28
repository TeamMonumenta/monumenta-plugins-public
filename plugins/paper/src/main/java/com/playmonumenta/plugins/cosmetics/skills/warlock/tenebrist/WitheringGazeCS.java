package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WitheringGazeCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WITHERING_GAZE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	public void onCast(Player player, double radius, double angle) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1f, 1f);

		new BukkitRunnable() {
			double mCurrentRadius = 1.15;

			@Override
			public void run() {
				Location loc = player.getLocation().add(0, 0.65, 0);
				double degree = 90 - angle;
				// particles about every 10 degrees
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
						new PartialParticle(Particle.SPELL_WITCH, l1, 3, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(player);
						new PartialParticle(Particle.SPELL_MOB, l1, 3, 0.15, 0.15, 0.15, 0).spawnAsPlayerActive(player);
						new PartialParticle(Particle.SMOKE_NORMAL, l1, 2, 0.15, 0.15, 0.15, 0.05).spawnAsPlayerActive(player);
					}
					if (player.hasLineOfSight(l2)) {
						new PartialParticle(Particle.SPELL_WITCH, l2, 3, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(player);
						new PartialParticle(Particle.SPELL_MOB, l2, 3, 0.15, 0.15, 0.15, 0).spawnAsPlayerActive(player);
						new PartialParticle(Particle.SMOKE_NORMAL, l2, 2, 0.15, 0.15, 0.15, 0.05).spawnAsPlayerActive(player);
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
