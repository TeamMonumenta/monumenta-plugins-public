package com.playmonumenta.plugins.cosmetics.skills.warlock;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AmplifyingHexCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void onCast(Player player, double radius, double angle) {
		new BukkitRunnable() {
			final Location mLoc = player.getLocation();
			double mRadiusIncrement = 0.5;

			@Override
			public void run() {
				if (mRadiusIncrement == 0.5) {
					mLoc.setDirection(player.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadiusIncrement += 1.25;
				double degree = 90 - angle;
				// particles about every 10 degrees
				int degreeSteps = ((int) (2 * angle)) / 10;
				double degreeStep = 2 * angle / degreeSteps;
				for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement,
						0.15,
						FastUtils.sin(radian1) * mRadiusIncrement);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);

					new PartialParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(player);
				}

				if (mRadiusIncrement >= radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		World world = player.getWorld();
		final Location soundLoc = player.getLocation();
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.3f, 0.9f);
		world.playSound(soundLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.3f, 1.4f);
		world.playSound(soundLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public void onHit(Player player, LivingEntity mob) {

	}
}
