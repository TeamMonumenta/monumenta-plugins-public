package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MeteorSlamCS implements CosmeticSkill {

	public static final ImmutableMap<String, MeteorSlamCS> SKIN_LIST = ImmutableMap.<String, MeteorSlamCS>builder()
		.put(PrestigiousSlamCS.NAME, new PrestigiousSlamCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.METEOR_SLAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	@Override
	public @Nullable String getName() {
		return null;
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
		new PartialParticle(Particle.FLAME, location, 60, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.LAVA, location, (int) (4 * radius * radius), radius * 0.4, 0.25f, radius * 0.4, 0).spawnAsPlayerActive(mPlayer);
	}
}
