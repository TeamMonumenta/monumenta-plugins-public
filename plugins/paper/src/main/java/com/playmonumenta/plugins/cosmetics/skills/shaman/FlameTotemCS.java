package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FlameTotemCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.FLAME_TOTEM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}

	public void flameTotemSpawn(Location standLocation, Player player, ArmorStand stand, double radius) {

	}

	public void flameTotemTickEnhanced(Player player, Location standLocation, double radius) {
		PPCircle fireRing = new PPCircle(Particle.SOUL_FIRE_FLAME, standLocation, radius).ringMode(true).countPerMeter(0.2).delta(0);
		fireRing.spawnAsPlayerActive(player);
	}

	public void flameTotemTick(Player player, Location standLocation, double radius) {
		PPCircle fireRing = new PPCircle(Particle.FLAME, standLocation, radius).ringMode(true).countPerMeter(0.6).delta(0);
		fireRing.spawnAsPlayerActive(player);
	}

	public void flameTotemPulse(Player player, Location standLocation, double radius) {
		PPCircle fireArea = new PPCircle(Particle.FLAME, standLocation, radius).ringMode(false).count(50).delta(0.01).extra(0.05);
		fireArea.spawnAsPlayerActive(player);

		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.3f, 0.5f);
	}

	public void flameTotemBomb(Player player, LivingEntity target, Location standLocation, Plugin plugin, double mBombRadius) {
		Location targetLocation = target.getLocation();
		new PPLine(Particle.FLAME, standLocation, targetLocation)
			.countPerMeter(8).delta(0).spawnAsPlayerActive(player);
		ParticleUtils.explodingRingEffect(plugin, targetLocation.clone().add(0, 0.1, 0),
			mBombRadius, 1.2, 5, 0.2,
			loc -> new PartialParticle(Particle.FLAME, loc, 1, 0, 0.1, 0, 0)
				.spawnAsPlayerActive(player));

		for (int i = 0; i < 3; i++) {
			new PartialParticle(Particle.LAVA, targetLocation.clone().add(0, 0.2 * i, 0), 8, mBombRadius * 0.4, 0.1 * i, mBombRadius * 0.4, 0)
				.spawnAsPlayerActive(player);
		}

		new PPCircle(Particle.FLAME, targetLocation.clone().add(0, 0.1, 0), mBombRadius)
			.ringMode(true)
			.countPerMeter(2.0)
			.delta(0.05)
			.spawnAsPlayerActive(player);

		ParticleUtils.explodingRingEffect(plugin, targetLocation.clone().add(0, 0.5, 0),
			mBombRadius, 1.5, 8, 0.3,
			loc -> new PartialParticle(Particle.FLAME, loc, 1, 0, 0.1, 0, 0.1)
				.spawnAsPlayerActive(player));

		new PartialParticle(Particle.SMOKE_LARGE, targetLocation.clone().add(0, 1.0, 0), 20, mBombRadius * 0.6, mBombRadius * 0.3, mBombRadius * 0.6, 0.05)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.SMALL_FLAME, targetLocation.clone().add(0, 0.5, 0), 15, mBombRadius * 0.7, mBombRadius * 0.5, mBombRadius * 0.7, 0.15)
			.spawnAsPlayerActive(player);
	}

	public void flameTotemExpire(World world, Player player, Location standLocation) {

	}
}
