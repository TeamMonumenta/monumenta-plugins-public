package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.classes.ClassAbility;
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

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.GLORIOUS_BATTLE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public void gloryStart(World world, Player player, Location location, int duration) {
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(location, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.2f, 0.8f);
		new PartialParticle(Particle.CRIMSON_SPORE, location, 25, 1, 0, 1, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, location, 15, 1, 0, 1, 0).spawnAsPlayerActive(player);
	}

	public void gloryTick(Player player, int tick) {
		//Nope!
	}

	public void gloryOnDamage(World world, Player player, LivingEntity target) {
		world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		new PartialParticle(Particle.SWEEP_ATTACK, target.getLocation(), 2).spawnAsPlayerActive(player);
	}

	public void gloryOnLand(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 0.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.3f, 2.0f);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 20, 1, 0, 1, 0).spawnAsPlayerActive(player);
	}
}
