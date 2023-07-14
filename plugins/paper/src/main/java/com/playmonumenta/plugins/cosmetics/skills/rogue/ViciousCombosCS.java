package com.playmonumenta.plugins.cosmetics.skills.rogue;

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

public class ViciousCombosCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	public void comboOnKill(World world, Location loc, Player player, double range, LivingEntity target) {
		world.playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.2f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.4f, 0.9f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.3f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.3f, 0.7f);
		new PartialParticle(Particle.CRIT, loc, 50, range, range, range, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 50, range, range, range, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 30, range, range, range, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc, 30, range, range, range, 0.001).spawnAsPlayerActive(player);
	}

	public void comboOnElite(World world, Location loc, Player player, double range, LivingEntity target) {
		world.playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.3f, 1.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.7f, 0.9f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.3f, 2.0f);
		new PartialParticle(Particle.CRIT, loc, 500, range, range, range, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 500, range, range, range, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 350, range, range, range, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB, loc, 350, range, range, range, 0.001).spawnAsPlayerActive(player);
	}
}
