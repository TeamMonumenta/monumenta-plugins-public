package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
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

public class LightningTotemCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.LIGHTNING_TOTEM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.YELLOW_WOOL;
	}

	public static final Particle.DustOptions DUST_GRAY_LARGE = new Particle.DustOptions(Color.fromRGB(51, 51, 51), 2);

	public void lightningTotemSpawn(Location standLocation, Player player, ArmorStand stand, double radius) {

	}

	public void lightningTotemTick(Player player, double radius, Location standLocation, ArmorStand stand) {
		new PPCircle(Particle.CRIT, standLocation.clone().add(0, 0.3, 0), radius).countPerMeter(0.4).spawnAsPlayerActive(player);
	}

	public void lightningTotemStrike(Player player, Location standLocation, LivingEntity target) {
		PPLightning lightning = new PPLightning(Particle.END_ROD, target.getLocation())
			.count(8).duration(3);
		player.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST,
			SoundCategory.PLAYERS, 1, 1.25f);
		lightning.init(4, 2.5, 0.3, 0.3);
		lightning.spawnAsPlayerActive(player);
	}

	public void lightningTotemEnhancementStorm(Player player, Location loc, double stormRadius) {
		new PartialParticle(
			Particle.REDSTONE, loc.clone().add(0, 4, 0), 15, 1.5, 0.25, 1.5, 0, DUST_GRAY_LARGE).spawnAsPlayerActive(player);
	}

	public void lightningTotemEnhancementStrike(Player player, Location loc, double stormRadius) {
		PPLightning lightning = new PPLightning(Particle.END_ROD, loc).count(8).duration(3).height(4);
		player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.25f);
		lightning.init(3, 2.5, 0.3, 0.3);
		lightning.spawnAsPlayerActive(player);
	}

	public void lightningTotemExpire(Player player, Location standLocation, World world) {
		new PartialParticle(Particle.FLASH, standLocation, 3, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.ENTITY_BLAZE_DEATH,
			SoundCategory.PLAYERS, 0.7f, 0.5f);
	}
}
