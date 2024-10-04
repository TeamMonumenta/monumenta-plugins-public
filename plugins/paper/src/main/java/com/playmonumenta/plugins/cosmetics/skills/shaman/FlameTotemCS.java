package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

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

	public void flameTotemBombEnhanced(Player player, List<LivingEntity> targets, Location standLocation, Plugin plugin, double mBombRadius) {
		Location standEyeLocation = standLocation.clone().add(0, 1.5, 0);
		for (LivingEntity target : targets) {
			Location targetLocation = target.getLocation();
			new PPLine(Particle.SOUL_FIRE_FLAME, standEyeLocation, targetLocation)
				.countPerMeter(8).delta(0).spawnAsPlayerActive(player);
			ParticleUtils.explodingRingEffect(plugin, targetLocation.clone().add(0, 0.1, 0),
				mBombRadius, 1.2, 5, 0.2,
				loc -> new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0.1, 0, 0)
					.spawnAsPlayerActive(player));
		}
		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
			SoundCategory.PLAYERS, 0.3f, 0.5f);
	}

	public void flameTotemBomb(Player player, List<LivingEntity> targets, Location standLocation, Plugin plugin, double mBombRadius) {
		Location standEyeLocation = standLocation.clone().add(0, 1.5, 0);
		for (LivingEntity target : targets) {
			Location targetLocation = target.getLocation();
			new PPLine(Particle.FLAME, standEyeLocation, targetLocation)
				.countPerMeter(8).delta(0).spawnAsPlayerActive(player);
			ParticleUtils.explodingRingEffect(plugin, targetLocation.clone().add(0, 0.1, 0),
				mBombRadius, 1.2, 5, 0.2,
				loc -> new PartialParticle(Particle.FLAME, loc, 1, 0, 0.1, 0, 0)
					.spawnAsPlayerActive(player));
		}
		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
			SoundCategory.PLAYERS, 0.3f, 0.5f);
	}

	public void flameTotemExpire(World world, Player player, Location standLocation) {
		new PartialParticle(Particle.REDSTONE, standLocation, 45, 0.2, 1.1, 0.2, 0.1, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, standLocation, 40, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.7f, 0.5f);
	}
}
