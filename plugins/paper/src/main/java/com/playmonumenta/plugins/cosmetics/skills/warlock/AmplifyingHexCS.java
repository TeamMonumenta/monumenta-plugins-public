package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class AmplifyingHexCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void amplifyingParticle(Player mPlayer, Location l) {
		new PartialParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void amplifyingEffects(Player mPlayer, World world, Location soundLoc) {
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1.0f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.65f);
	}

	public double amplifyingAngle(double angle, double radius) {
		return 0;
	}

	public double amplifyingHeight(double radius, double max) {
		return 0;
	}
}
