package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class RendingRazorCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.RENDING_RAZOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHEARS;
	}

	public void razorCast(final Player player) {
		final World world = player.getWorld();
		final Location playerLoc = player.getLocation();
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(playerLoc, "minecraft:entity.breeze.charge", SoundCategory.PLAYERS, 1, 1.4f);
	}

	public void razorProjectileEffects(final Player player, final Location location) {
		new PartialParticle(Particle.SWEEP_ATTACK, location).minimumCount(1).count(2).delta(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, location).count(10).delta(0.2).extra(0.1).spawnAsPlayerActive(player);
	}

	public void razorTravelSound(final Player player, final Location location) {
		location.getWorld().playSound(location, "minecraft:entity.breeze.charge", SoundCategory.PLAYERS, 1, 1);
	}

	public void razorHit(final Player player, final Location location) {
		final World world = player.getWorld();
		new PartialParticle(Particle.CRIT, location).count(10).delta(0.2).extra(0.1).spawnAsPlayerActive(player);
		world.playSound(location, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1, 0.8f);
		world.playSound(location, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 0.8f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(location, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1, 2);
	}

	public void razorRetrieveSound(final Player player, final Location location) {
		final World world = location.getWorld();
		world.playSound(location, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1, 1.4f);
		world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.3f, 1.4f);
		world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1, 0.7f);
	}

	public void razorRetrieve(final Player player, final Location location) {
		new PartialParticle(Particle.SWEEP_ATTACK, location).minimumCount(1).count(2).delta(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, location).count(5).delta(0.3).extra(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.DAMAGE_INDICATOR, location).count(5).delta(0.3).extra(0.1).spawnAsPlayerActive(player);
	}

	public void razorPierce(final Location location) {
		location.getWorld().playSound(location, Sound.ENTITY_BREEZE_DEATH, 0.6f, 1.6f);
		location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 0.6f, 0.7f);
	}

	public void razorReturned(final Location playerLoc) {
		final World world = playerLoc.getWorld();
		world.playSound(playerLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.4f, 1.6f);
		world.playSound(playerLoc, "minecraft:entity.breeze.charge", 1, 1.5f);
		world.playSound(playerLoc, Sound.ENTITY_WITCH_THROW, 1, 0.7f);
	}
}
