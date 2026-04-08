package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpiritcatcherOrbsCS implements CosmeticSkill {
	private static final BlockData FALLING_DUST_DATA = Material.PURPLE_GLAZED_TERRACOTTA.createBlockData();

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SPIRITCATCHER_ORBS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_LANTERN;
	}

	public void createOrbs(Location standLocation) {
		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_RAVAGER_ATTACK, SoundCategory.PLAYERS, 0.8f, 2f);
		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 0.7f);
	}

	public void orbTick(int ticks, Location orbLocation, Player player) {
		if (ticks % 5 != 0) {
			return;
		}

		new PartialParticle(Particle.DRAGON_BREATH, orbLocation, 3, 0, 0, 0, 0.005).spawnAsPlayerActive(player);
		new PPPeriodic(Particle.FALLING_DUST, orbLocation).count(1).delta(0.2).data(FALLING_DUST_DATA).manualTimeOverride(ticks).spawnAsPlayerActive(player);
	}

	public void orbPickup(Location orbLocation, Player player) {
		World world = orbLocation.getWorld();
		world.playSound(orbLocation, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.65f);
		world.playSound(orbLocation, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.6f);
		world.playSound(orbLocation, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 0.6f);
		new PartialParticle(Particle.BLOCK_CRACK, orbLocation, 30, 0.15, 0.15, 0.15, 0.75F, FALLING_DUST_DATA).spawnAsPlayerActive(player);
	}

	public void spiritflamesApplied(LivingEntity entity, Player player) {
		Location enemyLoc = entity.getLocation().add(0, 0.5 * entity.getHeight(), 0);
		entity.getWorld().playSound(enemyLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 2f, 1.2f);
		entity.getWorld().playSound(enemyLoc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.7f, 0.7f);
		entity.getWorld().playSound(enemyLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.5f, 1f);
		entity.getWorld().playSound(enemyLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.5f, 1.4f);
		entity.getWorld().playSound(enemyLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.4f);
		entity.getWorld().playSound(enemyLoc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 2f, 0.5f);
		new PartialParticle(Particle.SCULK_SOUL, enemyLoc, 12, 0.1, 0.2, 0.1, 0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL, enemyLoc, 12, 0.1, 0.2, 0.1, 0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, enemyLoc, 30, 0.1, 0.2, 0.1, 0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SCULK_CHARGE_POP, enemyLoc, 8, 0.1, 0.2 * entity.getHeight(), 0.1, 0.05).spawnAsPlayerActive(player);
	}

	public void spiritflamesFlameVisuals(LivingEntity entity, Player player) {
		Location enemyLoc = entity.getLocation();
		new PPCircle(Particle.SOUL_FIRE_FLAME, enemyLoc.add(0, 0.5 * entity.getHeight(), 0), 1.2 * entity.getWidth()).ringMode(true).countPerMeter(0.6).delta(0).spawnAsPlayerActive(player);
		entity.getWorld().playSound(enemyLoc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 0.5f, 1.6f);
	}

	public Material getOrbItemMaterial() {
		return Material.PEARLESCENT_FROGLIGHT;
	}

	public String getOrbName() {
		return "SpiritcatcherOrb";
	}
}
