package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class IgnitionDriveCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.IGNITION_DRIVE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	public void ignitionDriveDamageEffect(Player player, Location location, double radius, boolean startLaunch) {
		// Fire particles for the ignition effect
		new PartialParticle(Particle.FLAME, location, 25, radius / 2, 0.5, radius / 2, 0.1).spawnAsPlayerActive(player);

		// Lava particles for additional fire effect
		new PartialParticle(Particle.LAVA, location, 15, radius / 2, 0.3, radius / 2, 0.05).spawnAsPlayerActive(player);

		// Smoke particles for the explosion effect
		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 20, radius / 2, 0.4, radius / 2, 0.08).spawnAsPlayerActive(player);

		// Orange dust particles for fiery glow
		new PartialParticle(Particle.REDSTONE, location, 30, radius / 2, 0.3, radius / 2, 0.1,
			new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.5f)).spawnAsPlayerActive(player);

		// Only play damage sound effects at the end
		if (startLaunch) {
			return;
		}

		World world = player.getWorld();
		Location loc = player.getLocation();

		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1f, 0.8f, 1);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 2f, 3);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1f, 0.6f, 1);
	}

	public void ignitionDriveLaunchEffect(Player player) {
		Location loc = player.getLocation();

		// Launch trail particles
		new PartialParticle(Particle.FLAME, loc, 15, 0.5, 0.3, 0.5, 0.05).spawnAsPlayerActive(player);

		// Launch sound
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.5f, 1);
		world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.PLAYERS, 1.0f, 1.4f, 1);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 2f, 3);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.2f);
	}

	public void ignitionDriveStunMobSFX(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 1.4f, 1);
	}
}
