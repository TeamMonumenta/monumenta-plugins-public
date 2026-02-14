package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
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
	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 110), 0.75f);

	public void lightningTotemSpawn(Location standLocation, Player player, ArmorStand stand, double radius) {

	}

	public void lightningTotemShock(Player player, LivingEntity mob) {
		double widthDelta = PartialParticle.getWidthDelta(mob);
		double widerWidthDelta = widthDelta * 1.5;
		double doubleWidthDelta = widthDelta * 2;
		double heightDelta = PartialParticle.getHeightDelta(mob);
		new PartialParticle(Particle.FALLING_DUST, LocationUtils.getHalfHeightLocation(mob), 15,
			widerWidthDelta, heightDelta, widerWidthDelta, 1, Material.YELLOW_CONCRETE_POWDER.createBlockData()).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHeightLocation(mob, 0.25), 10,
			doubleWidthDelta, heightDelta / 2, doubleWidthDelta, 1, COLOR_YELLOW).spawnAsPlayerActive(player);

		Location mobLoc = mob.getEyeLocation();
		mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 0.3f, 2f);
	}

	public void lightningTotemPulse(Player player, Location standLocation, double radius) {
		PPCircle lightningArea = new PPCircle(Particle.CRIT, standLocation, radius).ringMode(false).count(40).delta(0.01).extra(0.05);
		lightningArea.spawnAsPlayerActive(player);

		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.5f, 2f);
		standLocation.getWorld().playSound(standLocation, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.5f, 2f);
	}

	public void lightningTotemTick(Player player, double radius, Location standLocation, ArmorStand stand) {
		new PPCircle(Particle.CRIT, standLocation.clone().add(0, 0.3, 0), radius).countPerMeter(0.4).spawnAsPlayerActive(player);
	}

	public void lightningTotemStrike(Player player, Location standLocation, LivingEntity target, boolean meleeActivated) {
		PPLightning lightning = new PPLightning(Particle.END_ROD, target.getLocation()).count(8).duration(3);
		lightning.init(4, 2.5, 0.3, 0.3);
		lightning.spawnAsPlayerActive(player);

		World world = player.getWorld();
		Location loc = target.getLocation();

		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 2f, 3);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.3f, 2f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.7f, 1.2f);
		if (meleeActivated) {
			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.7f);
		} else {
			world.playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1f, 0.7f);
			world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1f, 2f);
		}
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
