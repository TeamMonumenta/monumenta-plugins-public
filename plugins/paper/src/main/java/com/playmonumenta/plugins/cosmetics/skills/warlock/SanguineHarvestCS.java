package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SanguineHarvestCS implements CosmeticSkill {
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(179, 0, 0), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SANGUINE_HARVEST;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	public void onCast(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.9f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 0.1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_SWIM, SoundCategory.PLAYERS, 0.1f, 2.0f);
	}

	public void onEnhancementCast(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1, 1);
	}

	public void projectileParticle(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).countPerMeter(18).delta(0.15).extra(0.075).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(30).delta(0.2).extra(0.1).data(COLOR).spawnAsPlayerActive(player);
	}

	public void onExplode(Player player, World world, Location loc, double radius) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 25, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.1, COLOR).spawnAsPlayerActive(player);

		new PartialParticle(Particle.REDSTONE, loc, 75, radius, radius, radius, 0.25, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, loc, 75, radius, radius, radius, Material.RED_CONCRETE.createBlockData()).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 55, radius, radius, radius, 0.25).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.4f);
	}

	public void atMarkedLocation(Player player, Location loc) {
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.25, 0, 0.25, 0.1, COLOR).spawnAsPlayerActive(player);
	}

	public void entityGainEffect(Entity entity) {
		Location loc = LocationUtils.getEntityCenter(entity);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 30, 0.25, 0.5, 0.25, 0.02).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 30, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsEnemyBuff();
	}

	public void entityTickEffect(Entity mob, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = LocationUtils.getEntityCenter(mob);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02).spawnAsEnemyBuff();
		new PartialParticle(Particle.CRIMSON_SPORE, loc, 4, 0.25, 0.5, 0.25, 0).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsEnemyBuff();
	}

	public void onHurt(LivingEntity mob, Player player) {
		Location loc = LocationUtils.getEntityCenter(mob);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 60, 0.45, 0.7, 0.45, 0.02).spawnAsEnemyBuff();
		new PartialParticle(Particle.CRIMSON_SPORE, loc, 60, 0.45, 0.7, 0.45, 0).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 60, 0.4, 0.4, 0.4, 0.1, COLOR).spawnAsEnemyBuff();

		new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.PLAYERS, 1.0f, 0.8f);
	}

	public void onDeath(LivingEntity mob, Player player) {
		new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.PLAYERS, 1.0f, 0.8f);
	}
}
