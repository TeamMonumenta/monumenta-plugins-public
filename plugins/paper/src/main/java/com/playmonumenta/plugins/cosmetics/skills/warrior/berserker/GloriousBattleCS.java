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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GloriousBattleCS implements CosmeticSkill {

	public static final ImmutableMap<String, GloriousBattleCS> SKIN_LIST = ImmutableMap.<String, GloriousBattleCS>builder()
		.put(GloryExecutionCS.NAME, new GloryExecutionCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.GLORIOUS_BATTLE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	@Override
	public String getName() {
		return null;
	}

	public void gloryStart(World world, Player mPlayer, Location location, int duration) {
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2f, 0.5f);
		new PartialParticle(Particle.CRIMSON_SPORE, location, 25, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, location, 15, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
	}

	public void gloryTick(Player mPlayer, int tick) {
		//Nope!
	}

	public void gloryOnDamage(World world, Player mPlayer, LivingEntity target) {
		//Nope!
	}

	public void gloryOnLand(World world, Player mPlayer, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.65f);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 20, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
	}
}
