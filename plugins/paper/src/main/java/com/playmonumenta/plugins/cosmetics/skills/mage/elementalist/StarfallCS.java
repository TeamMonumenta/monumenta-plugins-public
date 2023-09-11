package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

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

public class StarfallCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.STARFALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	public void starfallCastEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.4f, 2.0f);
		new PartialParticle(Particle.LAVA, loc, 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(player);
	}

	public void starfallCastTrail(Location loc, Player player) {
		new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
	}

	public void starfallFallEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.FLAME, loc, 25, 0.25, 0.25, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 5, 0.25, 0.25, 0.25, 0.1).spawnAsPlayerActive(player);
	}

	public void starfallLandEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 0.1f);
		new PartialParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
	}
}
