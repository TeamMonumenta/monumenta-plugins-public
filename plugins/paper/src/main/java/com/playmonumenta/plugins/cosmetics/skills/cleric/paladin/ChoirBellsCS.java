package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChoirBellsCS implements CosmeticSkill {

	public static final ImmutableMap<String, ChoirBellsCS> SKIN_LIST = ImmutableMap.<String, ChoirBellsCS>builder()
		.build();

	private static final float[] CHOIR_BELLS_PITCHES = {0.6f, 0.8f, 0.6f, 0.8f, 1f};

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.CHOIR_BELLS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BELL;
	}

	@Override
	public String getName() {
		return null;
	}

	public void bellsCastEffect(Player mPlayer, double range) {
		new BukkitRunnable() {
			final Location mCenter = mPlayer.getLocation();
			final double mDelta = 0.8;
			int mTicks = 0;

			@Override
			public void run() {
				double radius = (mTicks + 1.5) * mDelta;
				double height = (FastUtils.cos(mTicks * 3.1416 / 4) + 1) * Math.exp(-0.06 * mTicks) * 1.25;
				int units = (mTicks + 3) * 3;
				ParticleUtils.drawCurve(mCenter, 1, units,
					new Vector(0, 0, 1), new Vector(1, 0, 0), new Vector(0, 1, 0),
					t -> radius * FastUtils.cos(3.1416 * 2 * t / units),
					t -> radius * FastUtils.sin(3.1416 * 2 * t / units),
					t -> height,
					(loc, t) -> {
						new PartialParticle(Particle.VILLAGER_HAPPY, loc, 1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 0.25, 0), 1, 0, 0.1).spawnAsPlayerActive(mPlayer);
					});

				if (++mTicks > range / mDelta) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, CHOIR_BELLS_PITCHES[mTicks]);
				if (++mTicks >= CHOIR_BELLS_PITCHES.length) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	public void bellsApplyEffect(Player mPlayer, LivingEntity mob) {
		//Nope!
	}
}
