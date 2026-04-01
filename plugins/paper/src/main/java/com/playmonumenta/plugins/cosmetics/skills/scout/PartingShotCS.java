package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

public class PartingShotCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PARTING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	public void dodge(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, "minecraft:item.armor.unequip_wolf", SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.PLAYERS, 2f, 1.8f);

		new PartialParticle(Particle.CLOUD, LocationUtils.getHalfHeightLocation(player))
			.count(25)
			.delta(0.2, 0.5, 0.2)
			.extra(0.3f)
			.spawnAsPlayerActive(player);

		loc.setPitch(0);
		loc.add(0, 0.25, 0);

		for (int i = 0; i < 3; i++) {
			ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 1 + i, 1, 0, 45, 0.3f + 0.2f * i, false, 0, Particle.CLOUD);
		}
	}

	public void tickEffect(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.CLOUD, LocationUtils.getHalfHeightLocation(player))
			.count(5)
			.delta(0.2, 0.5, 0.2)
			.extra(0.01f)
			.spawnAsPlayerBuff(player);
	}

	public void shoot(World world, Player player, Location loc, Projectile proj) {
		world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2f, 0.4f);
		Vector dir = loc.getDirection();

		for (int i = 0; i < 30; i++) {
			new PartialParticle(Particle.CLOUD, loc)
				.count(1)
				.directionalMode(true)
				.delta(dir.getX() + FastUtils.randomDoubleInRange(-0.15, 0.15),
					dir.getY() + FastUtils.randomDoubleInRange(-0.15, 0.15),
					dir.getZ() + FastUtils.randomDoubleInRange(-0.15, 0.15))
				.extra(1.125f)
				.spawnAsPlayerActive(player);
		}
		Location forward = loc.clone().add(dir);
		forward.setPitch(forward.getPitch() + 90);

		ParticleUtils.drawParticleCircleExplosion(player, forward,
			0, 5, 1, 0, 45, 0.3f, false, 0, Particle.CLOUD);
	}

	public void hitMob(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_CAT_HISS, SoundCategory.PLAYERS, 0.4f, 1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.4f, 1f);

		new PartialParticle(Particle.SWEEP_ATTACK, loc)
			.minimumCount(1)
			.count(5)
			.delta(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.SMOKE_NORMAL, loc)
			.count(25)
			.delta(0.1)
			.extra(0.25f)
			.spawnAsPlayerActive(player);
	}

	public void land(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1f, 1.6f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.6f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2f, 0.4f);

		Location pLoc = player.getLocation();
		pLoc.setPitch(0);

		ParticleUtils.drawParticleCircleExplosion(player, pLoc,
			0, radius, 1, 0, 45, 0.6f, false, 0, Particle.SMOKE_LARGE);

		new PartialParticle(Particle.SMOKE_NORMAL, pLoc)
			.count(100)
			.extra(0.2f)
			.spawnAsPlayerActive(player);
	}

	public void expire(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1f, 2f);
	}

	public String getDummyName() {
		return "Dummy";
	}

	public void revealStart(World world, Player player, Location loc, double radius) {
		new PartialParticle(Particle.FLASH, loc)
			.count(1)
			.minimumCount(1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.GUST, loc)
			.count(1)
			.minimumCount(1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.FIREWORKS_SPARK, loc)
			.count(50)
			.extraRange(0.2, 0.4)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.WAX_OFF, loc)
			.count(50)
			.extraRange(0.6, 1)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 1.2f);

		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.4f, 0.1f);
	}

	public void revealOnTarget(World world, Player player, LivingEntity mob) {
		world.playSound(mob.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, SoundCategory.PLAYERS, 0.4f, 0.7f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
	}

}
