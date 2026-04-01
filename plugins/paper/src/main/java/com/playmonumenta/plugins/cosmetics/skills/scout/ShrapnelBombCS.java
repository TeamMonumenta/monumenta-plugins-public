package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ShrapnelBombCS implements CosmeticSkill {
	private static final Particle.DustOptions GRAY = new Particle.DustOptions(Color.GRAY, 1.5f);
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(70, 70, 70), 0.8f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SHRAPNEL_BOMB;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_SWORD;
	}

	@Override
	public String getName() {
		return "Shrapnel Bomb";
	}

	public void bombShoot(World world, Player player) {
		Location loc = LocationUtils.getHalfHeightLocation(player);

		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 1f, 1f);
	}

	public void bombTick(World world, Player player, Projectile bomb) {
	}

	public Particle getParticle() {
		return Particle.SMOKE_LARGE;
	}

	public void bombExplode(World world, Player player, Location loc, double radius) {
		Location locParticle = loc.clone().add(0, 0.2, 0);
		locParticle.setPitch(0);

		new PartialParticle(Particle.FLAME, locParticle)
			.count(45)
			.extraRange(0.05, 0.2)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.SMOKE_NORMAL, locParticle)
			.count(25)
			.extraRange(0.05, 0.2)
			.spawnAsPlayerActive(player);

		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 65, 0.24f,
			true, 0, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 45, 0.24f,
			true, 0, Particle.FLAME);
		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 15, 0.24f,
			true, 0, Particle.LAVA);
		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 65, 3f,
			true, 0, Particle.CRIT);

		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 2, 0, 0, 0)
			.minimumCount(1)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 1.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 1.5f);
		world.playSound(loc, Sound.BLOCK_DECORATED_POT_SHATTER, SoundCategory.PLAYERS, 2f, 0.4f);
	}

	public void bombEnhancementExplode(World world, Player player, Location loc, double radius) {
		Location locParticle = loc.clone().add(0, 0.2, 0);
		locParticle.setPitch(0);

		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 35, 0.24f,
			true, 0, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 45, 0.24f,
			true, 0, Particle.SMALL_FLAME);
		ParticleUtils.drawParticleCircleExplosion(player, locParticle, 0, radius, 0, 0, 10, 0.24f,
			true, 0, Particle.LAVA);

		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0)
			.minimumCount(1)
			.spawnAsPlayerActive(player);
	}

	public void shrapnelExplode(World world, Player player, Location loc, Vector dir) {
		double radius = 3;
		double angle = 65;

		double[] rot = VectorUtils.vectorToRotation(dir);

		Location locParticle = loc.clone().add(0, 0.2, 0);
		locParticle.setPitch(0);

		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.45f, 2f);

		for (int i = 0; i < angle; i++) {
			Vector newDir = VectorUtils.rotationToVector(rot[0] + FastUtils.randomDoubleInRange(-angle / 2, angle / 2), rot[1] + FastUtils.randomDoubleInRange(-angle / 2, angle / 2));

			new PartialParticle(Particle.CRIT, locParticle, 1, newDir.getX(), FastUtils.randomDoubleInRange(-0.5, 0.5), newDir.getZ())
				.directionalMode(true)
				.extra(radius)
				.spawnAsPlayerActive(player);
		}

	}

	public Material getShrapnelItem() {
		return Material.RAW_IRON_BLOCK;
	}

	public void shrapnelTick(World world, Player player, Location loc) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 5, 0.075, 0.075, 0.075).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.05, 0.05, 0.05)
			.data(GRAY)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 8, 0.075, 0.075, 0.075)
			.data(BLACK)
			.spawnAsPlayerActive(player);
	}

	public void shrapnelHit(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
	}

	public String getBase64Head() {
		return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmYzNzkyMTJmNDIwNjBhZTA1NjNjNzA3MzlhN2VjNDJhZDQ4ZTcwZjc0MjEwYjI5MGQyMzA3YTQ3ODQ1ZWMyYyJ9fX0=";
	}

	public void firstStrike(World world, Player player, LivingEntity target) {
		Location loc = target.getLocation();

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 2f);
		world.playSound(loc, Sound.BLOCK_DECORATED_POT_SHATTER, SoundCategory.PLAYERS, 2f, 0.8f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.4f);
	}

	// Shouldn't need to override this
	public @Nullable ItemDisplay modify(Projectile bomb, Player player, Plugin plugin) {
		ItemDisplay bombHead = DisplayEntityUtils.spawnItemDisplayWithBase64Head(bomb.getLocation(), getBase64Head());
		if (bombHead == null) {
			return null;
		}
		bombHead.setRotation(player.getYaw(), 0);
		Transformation transformation = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf());
		bombHead.setTransformation(transformation);
		bomb.addPassenger(bombHead);
		EntityUtils.setRemoveEntityOnUnload(bombHead);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (bombHead.isDead() || !bombHead.isValid()) {
					this.cancel();
					return;
				}
				if (!bomb.isValid()) {
					bombHead.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return bombHead;
	}
}
