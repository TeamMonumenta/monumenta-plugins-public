package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VolcanicBurstCS extends MagmaShieldCS implements DepthsCS {
	// Meteor like magma shield. Depth set: flame
	// It used to be Heavenly Blast. I deeply loved it and a song with the same name.

	public static final String NAME = "Volcanic Burst";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Unleash forth volcanic havoc,",
			"an unstoppable blast from the heavens.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MAGMA_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_FLAME;
	}

	@Override
	public void magmaParticle(Player mPlayer, Location l) {

	}

	@Override
	public void magmaEffects(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 1.5f, 0.65f);

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;
			int mHeight = 0;
			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				mHeight++;
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 0.7f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.85f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 1.5f, 0.75f);
				int flameI = mHeight % 2;
				for (double degree = 30; degree <= 150; degree += 30) {
					double radian1 = FastMath.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);

					Particle flame = flameI % 2 == 0 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
					for (int i = 0; i < mHeight; i++) {
						Location hLoc = l.clone().add(0, i * 0.85, 0);
						new PartialParticle(flame, hLoc, 5, 0.15, 0.15, 0.15, 0.1)
							.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					}
					new PartialParticle(Particle.LAVA, l, 3, 0.2, 0.05, 0.2, 0.075)
						.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 25, 0.1, 0.05, 0.1, 0.1)
						.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

					ParticleUtils.drawParticleCircleExplosion(mPlayer, l, 0, 1, 0, 0, 30, 0.25f,
						true, 0, 0, flame);
					flameI++;
				}

				if (mRadius >= MagmaShield.SIZE) {
					this.cancel();

				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 4);
	}

}
