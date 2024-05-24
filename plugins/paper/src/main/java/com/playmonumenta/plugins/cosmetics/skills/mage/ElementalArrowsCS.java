package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
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
import org.bukkit.entity.Projectile;

public class ElementalArrowsCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ELEMENTAL_ARROWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	public void thunderEffect(Player player, LivingEntity enemy) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();
		new PartialParticle(Particle.END_ROD, loc.clone().add(0, 0.5*enemy.getHeight(), 0), 15, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 1.0f, 2.0f);
	}

	public void iceEffect(Player player, LivingEntity enemy) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();
		new PartialParticle(Particle.CLOUD, loc.clone().add(0, 0.5*enemy.getHeight(), 0), 10, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 1.2f);
	}

	public void fireEffect(Player player, LivingEntity enemy) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();
		new PartialParticle(Particle.FLAME, loc.clone().add(0, 0.5*enemy.getHeight(), 0), 5, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMALL_FLAME, loc.clone().add(0, 0.5*enemy.getHeight(), 0), 5, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2.0f, 0.8f);
	}

	public void thunderProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.END_ROD);
	}

	public void iceProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.SNOW_SHOVEL);
	}

	public void fireProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.FLAME);
	}
}
