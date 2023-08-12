package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CounterStrikeCS implements CosmeticSkill {
	private static final Color RED_COLOR = Color.fromRGB(240, 54, 10);
	private static final Color YELLOW_COLOR = Color.fromRGB(252, 186, 10);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.COUNTER_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CACTUS;
	}

	public void onPrime(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.PLAYERS, 0.8f, 0.5f);
		player.playSound(loc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		player.playSound(loc, Sound.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.PLAYERS, 0.6f, 0.3f);
	}

	public void onCounterStrike(Player player, LivingEntity enemy, Location enemyLoc) {
		player.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.8f, 0.1f);
		player.playSound(enemyLoc, Sound.ENTITY_PLAYER_BIG_FALL, SoundCategory.PLAYERS, 0.8f, 0.1f);
		player.playSound(enemyLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.8f, 0.6f);
		player.playSound(enemyLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.6f, 0.8f);
		player.playSound(enemyLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.4f, 2.0f);
		new PartialParticle(Particle.SWEEP_ATTACK, enemyLoc, 3, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, enemyLoc, 8, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(player);
	}

	public void onPrimedMobTick(Player player, Location enemyLoc) {
		passiveParticles(player, enemyLoc, RED_COLOR);
	}

	public void onAbsorptionMobTick(Player player, Location enemyLoc) {
		passiveParticles(player, enemyLoc, YELLOW_COLOR);
	}

	private void passiveParticles(Player player, Location enemyLoc, Color color) {
		new PartialParticle(Particle.REDSTONE, enemyLoc.add(0, 1, 0), 4, 0.25, 0.4, 0.25, new Particle.DustOptions(color, 0.5f)).spawnForPlayer(ParticleCategory.OWN_PASSIVE, player);
	}
}
