package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

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

public class StarfallCS implements CosmeticSkill {

	public static final ImmutableMap<String, StarfallCS> SKIN_LIST = ImmutableMap.<String, StarfallCS>builder()
		.put(PrestigiousStarfallCS.NAME, new PrestigiousStarfallCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.STARFALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	@Override
	public String getName() {
		return null;
	}

	public void starfallCastEffect(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0.85f);
		new PartialParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
	}

	public void starfallCastTrail(Location loc, Player mPlayer) {
		new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
	}

	public void starfallFallEffect(World world, Player mPlayer, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);
	}

	public void starfallLandEffect(World world, Player mPlayer, Location loc) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0);
		new PartialParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F).spawnAsPlayerActive(mPlayer);
	}
}
