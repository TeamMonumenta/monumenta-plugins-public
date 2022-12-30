package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class VoodooBondsCS implements CosmeticSkill {

	public static final ImmutableMap<String, VoodooBondsCS> SKIN_LIST = ImmutableMap.<String, VoodooBondsCS>builder()
		.put(PrestigiousBondsCS.NAME, new PrestigiousBondsCS())
		.build();

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VOODOO_BONDS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.JACK_O_LANTERN;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void bondsStartEffect(World world, Player mPlayer, double maxRadius) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.3f, 0.75f);
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				for (int i = 0; i < 36; i += 1) {
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					//new particles
					new PartialParticle(Particle.SPELL_WITCH, mLoc, 1, 0.15, 0.15, 0.15, 0).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mLoc, 1, 0.15, 0.15, 0.15, 0, COLOR).spawnAsPlayerActive(mPlayer);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);

				}
				if (mRadius >= maxRadius) {
					this.cancel();
				}

			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void bondsApplyEffect(Player mPlayer, Player p) {
		p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 0.75f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01).spawnAsPlayerActive(mPlayer);
	}

	public void bondsSpreadParticle(Player mPlayer, Location mLoc, Location eLoc) {
		new PartialParticle(Particle.SPELL_WITCH, mLoc, 30, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mLoc, 30, 0.5, 0.5, 0.5, 0, COLOR).spawnAsPlayerActive(mPlayer);
	}
}
