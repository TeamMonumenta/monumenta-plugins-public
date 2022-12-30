package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BodkinBlitzCS implements CosmeticSkill {

	public static final ImmutableMap<String, BodkinBlitzCS> SKIN_LIST = ImmutableMap.<String, BodkinBlitzCS>builder()
		.put(PrestigiousBlitzCS.NAME, new PrestigiousBlitzCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BODKIN_BLITZ;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void blitzStartSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 2f);
	}

	public void blitzTrailEffect(Player mPlayer, Location loc, Vector dir) {
		new PartialParticle(Particle.FALLING_DUST, loc, 5, 0.15, 0.45, 0.1,
			Bukkit.createBlockData("gray_concrete")).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, loc, 4, 0.25, 0.5, 0.25, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 5, 0.15, 0.45, 0.15, 0.01).spawnAsPlayerActive(mPlayer);
	}

	public void blitzEndEffect(World world, Player mPlayer, Location tpLoc) {
		world.playSound(tpLoc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(tpLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(tpLoc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(tpLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 1f);

		new PartialParticle(Particle.SMOKE_LARGE, tpLoc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.18).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, tpLoc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.04).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_WITCH, tpLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, tpLoc.clone().add(0, 1, 0), 50, 0.75, 0.5, 0.75, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, tpLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.3).spawnAsPlayerActive(mPlayer);
	}

	public void blitzBuffEffect(Player mPlayer) {
		new PartialParticle(Particle.FALLING_DUST, mPlayer.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, Bukkit.createBlockData("gray_concrete")).spawnAsPlayerActive(mPlayer);
	}

	public void blitzOnDamage(World world, Player mPlayer, Location entityLoc) {
		world.playSound(entityLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 2f);
		world.playSound(entityLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 2f);
		new PartialParticle(Particle.FALLING_DUST, entityLoc, 35, 0.35, 0.5, 0.35, Bukkit.createBlockData("gray_concrete")).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, entityLoc, 20, 0.25, 0.25, 0.25, 1, Bukkit.createBlockData("redstone_block")).spawnAsPlayerActive(mPlayer);
	}
}
