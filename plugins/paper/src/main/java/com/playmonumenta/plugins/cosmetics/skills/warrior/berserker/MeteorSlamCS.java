package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MeteorSlamCS implements CosmeticSkill {

	private static final Color COLO_COLOR_TIP = Color.fromRGB(160, 26, 26);
	private static final Color COLO_COLOR_BASE = Color.fromRGB(195, 180, 180);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.METEOR_SLAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CORAL_FAN;
	}


	public void onUpwardSlash(World world, Location location, Player player, double radius, int coneAngle) {
		world.playSound(location, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 2f, 2f);
		world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(location, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.4f, 0.7f);
		world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.3f, 1f);

		Location pLoc = location.clone().add(0, 0, 0);
		Location hitLoc = location.clone().add(location.getDirection().setY(0).normalize().multiply(2.2));

		Vector dir = LocationUtils.getDirectionTo(hitLoc, pLoc).multiply(2);
		hitLoc.setDirection(dir);
		hitLoc.add(0, 3, 0);

		ParticleUtils.drawHalfArc(hitLoc.clone().subtract(dir), 2.2, 100 - coneAngle / 2.0, 0, 140, 12, 0.2, false, 40,
			(Location l, int ring, double angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
					new Particle.DustOptions(
						ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, ring / 16D),
						(float) (0.6 + (angleProgress * ring * 0.125))
					))
					.spawnAsPlayerActive(player);
			});

		ParticleUtils.drawHalfArc(hitLoc.clone().subtract(dir), 2.2, 100 + coneAngle / 2.0, 0, 140, 12, 0.2, false, 40,
			(Location l, int ring, double angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
					new Particle.DustOptions(
						ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, ring / 16D),
						(float) (0.6 + (angleProgress * ring * 0.125))
					))
					.spawnAsPlayerActive(player);
			});

	}

	public void onSlam(World world, Location location, Player player, double radius, double fallDistance) {
		float volumeScale = (float) Math.min(0.1 + fallDistance / 8 * 0.9, 1);

		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 1.5f, 0.4f);
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, volumeScale * 1.4f, 0.4f);
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, volumeScale, 0.4f);
		world.playSound(location, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, volumeScale * 0.7f, 0.4f);
		world.playSound(location, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, volumeScale * 0.3f, 2f);
		world.playSound(location, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, volumeScale * 1.2f, 0.4f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, volumeScale * 1.2f, 0.4f);
		world.playSound(location, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, volumeScale * 0.7f, 0.4f);
		world.playSound(location, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, volumeScale * 1.4f, 1.0f);

		new PartialParticle(Particle.FLAME, location, 30, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(player);
		new PPCircle(Particle.LAVA, location, radius - 0.5)
			.delta(0.5, 0.5, 0.5)
			.countPerMeter(4)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.SMOKE_NORMAL, location.clone().add(0, 0.25, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extra(radius)
			.count(20)
			.spawnAsPlayerActive(player);
	}

	public void onSlamCritical(Plugin plugin, World world, Location location, Player player) {

		world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.2f, 0.5f);
		world.playSound(location, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);

		Location groundedLoc = LocationUtils.fallToGround(location, location.getY() - 5);
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				new PartialParticle(Particle.FLAME, groundedLoc, 7, 0F, 0F, 0F, 0.1F).spawnAsPlayerActive(player);
				new PartialParticle(Particle.EXPLOSION_NORMAL, groundedLoc, 5, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(player);
				new PPCircle(Particle.LAVA, groundedLoc, 0.75)
					.delta(0.2, 0.2, 0.2)
					.countPerMeter(1)
					.spawnAsPlayerActive(player);

				new PPCircle(Particle.SMOKE_NORMAL, groundedLoc.clone().add(0, 0.25, 0), 0.5)
					.rotateDelta(true)
					.directionalMode(true)
					.delta(0.04, 0, 0)
					.extra(0.2)
					.count(5)
					.spawnAsPlayerActive(player);

			}
		};
		runnable.runTaskLater(plugin, 4);
	}

	public void onGroundPoundCast(Plugin plugin, World world, Location location, Player player) {
		world.playSound(location, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 2f, 1.5f);
		world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(location, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.4f, 0.5f);
		world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.3f, 0.85f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.5f);
	}

	public void onGroundPoundTick(Plugin plugin, World world, Location location, Player player) {
		new PartialParticle(Particle.GUST, location, 1, 0F, 0F, 0F, 0.1F).spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, location, 0.5)
			.count(15)
			.delta(0.07, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(1.5).extraVariance(0.5)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, location, 1.5)
			.count(16)
			.ringMode(true)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.SMOKE_NORMAL, location, 1.5)
			.count(16)
			.ringMode(true)
			.spawnAsPlayerActive(player);
	}

	public void onGroundPoundSlam(Plugin plugin, World world, Location location, Player player, double radius) {
		new PartialParticle(Particle.FLASH, location, 1, 0F, 0F, 0F, 0.1F).spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, location, 0.5)
			.count(80)
			.delta(0.2, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(1.5).extraVariance(0.5)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.SMOKE_NORMAL, location, 0.5)
			.count(80)
			.delta(0.2, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(1.5).extraVariance(0.5)
			.spawnAsPlayerActive(player);

		world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 1.2f);
	}
}
