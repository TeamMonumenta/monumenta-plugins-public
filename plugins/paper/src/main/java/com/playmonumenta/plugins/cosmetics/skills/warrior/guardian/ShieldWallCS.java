package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

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

public class ShieldWallCS implements CosmeticSkill {

	public static final ImmutableMap<String, ShieldWallCS> SKIN_LIST = ImmutableMap.<String, ShieldWallCS>builder()
		.put(PrestigiousShieldCS.NAME, new PrestigiousShieldCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SHIELD_WALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.COBBLESTONE_WALL;
	}

	@Override
	public String getName() {
		return null;
	}

	public void shieldStartEffect(World world, Player mPlayer, double radius) {
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1.5f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1, 0.8f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 70, 0, 0, 0, 0.3f).spawnAsPlayerActive(mPlayer);
	}

	public void shieldWallDot(Player mPlayer, Location l, double degree, double angle, int y, int height) {
		new PartialParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	public void shieldOnBlock(World world, Location eLoc, Player mPlayer) {
		world.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 5, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
	}

	public void shieldOnHit(World world, Location eLoc, Player mPlayer) {
		world.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, eLoc, 50, 0, 0, 0, 0.35f).spawnAsPlayerActive(mPlayer);
	}
}
