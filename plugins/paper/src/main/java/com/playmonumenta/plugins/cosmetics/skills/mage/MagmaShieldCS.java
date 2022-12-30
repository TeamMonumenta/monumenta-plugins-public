package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
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
import org.jetbrains.annotations.Nullable;

public class MagmaShieldCS implements CosmeticSkill {

	public static final ImmutableMap<String, MagmaShieldCS> SKIN_LIST = ImmutableMap.<String, MagmaShieldCS>builder()
		.put(VolcanicBurstCS.NAME, new VolcanicBurstCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MAGMA_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_CREAM;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void magmaEffects(World world, Player mPlayer, double radius, double angle) {

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 1.25f);
		new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				double degree = 90 - angle;
				// particles about every 10 degrees
				int degreeSteps = ((int) (2 * angle)) / 10;
				double degreeStep = 2 * angle / degreeSteps;
				for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				}

				if (mRadius >= radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}
}
