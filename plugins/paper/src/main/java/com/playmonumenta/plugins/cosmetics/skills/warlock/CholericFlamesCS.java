package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class CholericFlamesCS implements CosmeticSkill {

	public static final ImmutableMap<String, CholericFlamesCS> SKIN_LIST = ImmutableMap.<String, CholericFlamesCS>builder()
		.put(InfernalFlamesCS.NAME, new InfernalFlamesCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.CHOLERIC_FLAMES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	@Override
	public String getName() {
		return null;
	}

	public void flameParticle(Player mPlayer, Location mLoc, double mRadius) {
		new PPCircle(Particle.FLAME, mLoc, mRadius).ringMode(true).count(40).extra(0.125).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SOUL_FIRE_FLAME, mLoc, mRadius).ringMode(true).count(40).extra(0.125).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SMOKE_NORMAL, mLoc, mRadius).ringMode(true).count(20).extra(0.15).spawnAsPlayerActive(mPlayer);
	}

	public void flameEffects(Player mPlayer, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.35f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 30, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
	}
}
