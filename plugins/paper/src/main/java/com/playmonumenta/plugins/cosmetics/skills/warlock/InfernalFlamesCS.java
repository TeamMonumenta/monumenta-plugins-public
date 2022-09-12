package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class InfernalFlamesCS extends CholericFlamesCS {
	//Twisted theme

	public static final String NAME = "Infernal Flames";

	private static final Particle.DustOptions TWIST_COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);
	private static final Particle.DustOptions INFERNAL_COLOR = new Particle.DustOptions(Color.fromRGB(127, 80, 0), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.CHOLERIC_FLAMES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_CAMPFIRE;
	}

	@Override
	public void flameParticle(Player mPlayer, Location mLoc, double mRadius) {
		new PPCircle(Particle.REDSTONE, mLoc, mRadius).ringMode(true).count(80).delta(0.1).extra(0.05).data(INFERNAL_COLOR).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, mLoc, mRadius).ringMode(true).count(40).delta(0.1).extra(0.05).data(TWIST_COLOR).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SOUL_FIRE_FLAME, mLoc, mRadius).ringMode(true).count(80).extra(0.125).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SMOKE_LARGE, mLoc, mRadius).ringMode(true).count(20).extra(0.1).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void flameSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.75f, 0.5f);
	}
}
