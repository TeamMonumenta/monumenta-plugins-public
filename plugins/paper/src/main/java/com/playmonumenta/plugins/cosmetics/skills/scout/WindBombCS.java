package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WindBombCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WIND_BOMB;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	public void onThrow(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
	}

	public void onLand(Plugin plugin, Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.1f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 0.1f);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (double j = 0; j < 360; j += 6) {
				double radian = Math.toRadians(j);
				Location angleLoc = loc.clone().add(FastUtils.cos(radian) * radius, 0.15, FastUtils.sin(radian) * radius);
				new PartialParticle(Particle.CLOUD, angleLoc, 3, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
			}
		}, 1);
	}

	public void onVortexSpawn(Player player, World world, Location loc) {
		new PartialParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 1);
	}

	public void onVortexTick(Player player, Location loc) {
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15).spawnAsPlayerActive(player);
	}

	public String getProjectileName() {
		return "Wind Bomb";
	}

	public @Nullable Particle getProjectileParticle() {
		return Particle.CLOUD;
	}
}
