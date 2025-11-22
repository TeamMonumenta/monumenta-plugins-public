package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MeteorSlamCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.METEOR_SLAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	public void slamCastEffect(World world, Location location, Player mPlayer) {
		world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.LAVA, location, 15, 1, 0f, 1, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, location)
			.count(30)
			.delta(3, 0, 3)
			.deltaVariance(true, false, true)
			.extra(0.2)
			.extraVariance(0.1)
			.directionalMode(true)
			.spawnAsPlayerActive(mPlayer);
	}

	public void slamAttackEffect(World world, Location location, Player mPlayer, double radius, double fallDistance) {
		float volumeScale = (float) Math.min(0.1 + fallDistance / 16 * 0.9, 1);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 1.3f, 0);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 2, 1.25F);
		new PartialParticle(Particle.FLAME, location, 30, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.LAVA, location, radius - 0.5)
			.delta(0.5, 0.5, 0.5)
			.countPerMeter(4)
			.spawnAsPlayerActive(mPlayer);

		new PPCircle(Particle.SMOKE_NORMAL, location.clone().add(0, 0.25, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extra(radius)
			.count(20)
			.spawnAsPlayerActive(mPlayer);
	}
}
