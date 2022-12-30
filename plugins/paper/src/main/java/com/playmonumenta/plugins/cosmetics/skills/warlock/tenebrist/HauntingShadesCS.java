package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class HauntingShadesCS implements CosmeticSkill {

	public static final ImmutableMap<String, HauntingShadesCS> SKIN_LIST = ImmutableMap.<String, HauntingShadesCS>builder()
		.put(PrestigiousShadesCS.NAME, new PrestigiousShadesCS())
		.build();

	private static final String AS_NAME = "HauntingShade";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HAUNTING_SHADES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public String getAsName() {
		return AS_NAME;
	}

	public void shadesStartSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.65f);
	}

	public void shadesTrailParticle(Player mPlayer, Location bLoc, Vector dir, double distance) {
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsPlayerActive(mPlayer);
	}

	public void shadesTickEffect(Plugin mPlugin, World world, Player mPlayer, Location bLoc, double mAoeRadius, int mT) {
		if (mT % 10 == 0) {
			new BukkitRunnable() {
				double mRadius = 0;
				final Location mLoc = bLoc.clone().add(0, 0.15, 0);

				@Override
				public void run() {
					mRadius += 1.25;
					new PPCircle(Particle.REDSTONE, mLoc, mRadius).ringMode(true).count(36).delta(0.2).extra(0.1).data(COLOR).spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.SMOKE_NORMAL, mLoc, mRadius).ringMode(true).count(12).extra(0.15).spawnAsPlayerActive(mPlayer);
					if (mRadius >= mAoeRadius + 1) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		if (mT % 20 == 0) {
			world.playSound(bLoc, Sound.ENTITY_BLAZE_HURT, 0.3f, 0.5f);
		}
	}

	public void shadesEndEffect(World world, Player mPlayer, Location bLoc, double radius) {
		new PartialParticle(Particle.REDSTONE, bLoc, 45, 0.2, 1.1, 0.2, 0.1, COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 40, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(mPlayer);
		world.playSound(bLoc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
	}
}

