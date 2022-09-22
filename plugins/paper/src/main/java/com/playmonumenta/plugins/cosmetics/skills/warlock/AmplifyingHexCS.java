package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class AmplifyingHexCS implements CosmeticSkill {

	public static final ImmutableMap<String, AmplifyingHexCS> SKIN_LIST = ImmutableMap.<String, AmplifyingHexCS>builder()
		.put(AvalanchexCS.NAME, new AvalanchexCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void amplifyingParticle(Player mPlayer, Location l) {
		new PartialParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	public void amplifyingEffects(Player mPlayer, World world, Location soundLoc) {
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.65f);
	}

	public double amplifyingAngle(double angle, double radius) {
		return 0;
	}

	public double amplifyingHeight(double radius, double max) {
		return 0;
	}
}
