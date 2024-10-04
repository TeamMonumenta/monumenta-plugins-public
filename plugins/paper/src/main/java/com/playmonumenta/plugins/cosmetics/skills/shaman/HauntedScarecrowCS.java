package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

public class HauntedScarecrowCS extends FlameTotemCS {

	public static final String NAME = "Haunted Scarecrow";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"An otherworldly spirit possesses the stand,",
			"relentlessly spreading its sickly blight."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.DEAD_BUSH;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void flameTotemSpawn(Location standLocation, Player player, ArmorStand stand, double radius) {
		EntityEquipment standArmor = stand.getEquipment();
		standArmor.clear();

		ItemStack fleshWithoutNames = DisplayEntityUtils.generateRPItem(Material.GOLDEN_HELMET, "Flesh Without Names");

		ItemStack plaguebringer = DisplayEntityUtils.generateRPItem(Material.LEATHER_CHESTPLATE, "Plaguebringer");
		LeatherArmorMeta plaguebringerMeta = ((LeatherArmorMeta) plaguebringer.getItemMeta());
		plaguebringerMeta.setColor(Color.fromRGB(64, 73, 69));
		plaguebringer.setItemMeta(plaguebringerMeta);

		ItemStack vilepriestGarments = DisplayEntityUtils.generateRPItem(Material.LEATHER_LEGGINGS, "Vilepriest Garments");
		LeatherArmorMeta vilepriestGarmentsMeta = ((LeatherArmorMeta) vilepriestGarments.getItemMeta());
		vilepriestGarmentsMeta.setColor(Color.fromRGB(39, 46, 48));
		vilepriestGarments.setItemMeta(vilepriestGarmentsMeta);

		ItemStack grovewalkerSandals = DisplayEntityUtils.generateRPItem(Material.LEATHER_BOOTS, "Grovewalker Sandals");
		LeatherArmorMeta grovewalkerSandalsMeta = ((LeatherArmorMeta) grovewalkerSandals.getItemMeta());
		grovewalkerSandalsMeta.setColor(Color.fromRGB(15, 17, 19));
		grovewalkerSandals.setItemMeta(grovewalkerSandalsMeta);

		ItemStack[] armor = {grovewalkerSandals, vilepriestGarments, plaguebringer, fleshWithoutNames};
		standArmor.setArmorContents(armor);

		ItemStack harvestmansScythe = DisplayEntityUtils.generateRPItem(Material.STONE_HOE, "Harvestman's Scythe");
		standArmor.setItemInMainHand(harvestmansScythe);
		standArmor.setItemInOffHand(harvestmansScythe);
		stand.setLeftArmPose(new EulerAngle(Math.toRadians(190), Math.toRadians(-90), Math.toRadians(-90)));
		stand.setRightArmPose(new EulerAngle(Math.toRadians(190), Math.toRadians(90), Math.toRadians(90)));
		stand.setInvisible(false);

		player.getWorld().playSound(standLocation, Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.PLAYERS, 0.3f, 0.7f);
		player.getWorld().playSound(standLocation, Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		player.getWorld().playSound(standLocation, Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, 0.5f, 1.6f);
	}

	@Override
	public void flameTotemTickEnhanced(Player player, Location standLocation, double radius) {
		flameTotemTick(player, standLocation, radius);
	}

	@Override
	public void flameTotemTick(Player player, Location standLocation, double radius) {
		new PPCircle(Particle.SMOKE_NORMAL, standLocation, radius).ringMode(true).countPerMeter(0.1).delta(0.05).spawnAsPlayerActive(player);
		new PPCircle(Particle.SOUL, standLocation, radius).ringMode(true).countPerMeter(0.05).delta(0.05).spawnAsPlayerActive(player);
		new PPCircle(Particle.SMOKE_NORMAL, standLocation, 1).ringMode(false).countPerMeter(0.05).delta(0.15).spawnAsPlayerActive(player);
		new PPCircle(Particle.SOUL, standLocation, 1).ringMode(false).countPerMeter(0.025).delta(0.15).spawnAsPlayerActive(player);
	}

	@Override
	public void flameTotemBombEnhanced(Player player, List<LivingEntity> targets, Location standLocation, Plugin plugin, double mBombRadius) {
		flameTotemBomb(player, targets, standLocation, plugin, mBombRadius);
	}

	@Override
	public void flameTotemBomb(Player player, List<LivingEntity> targets, Location standLocation, Plugin plugin, double mBombRadius) {
		World world = standLocation.getWorld();
		Location standEyeLocation = standLocation.add(0, 0.8, 0);
		world.playSound(standLocation, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(standLocation, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.PLAYERS, 1.7f, 0.8f);
		world.playSound(standLocation, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.65f, 1.7f);
		world.playSound(standLocation, Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 2.0f, 1.1f);
		for (LivingEntity target : targets) {
			Location targetLoc = LocationUtils.getHalfHeightLocation(target);
			new PPLine(Particle.CRIT, standEyeLocation, targetLoc).countPerMeter(4).delta(0.05).extra(0.1).spawnAsPlayerActive(player);
			new PPLine(Particle.SMOKE_NORMAL, standEyeLocation, targetLoc).countPerMeter(4).delta(0.12).spawnAsPlayerActive(player);
			new PPLine(Particle.CRIT_MAGIC, standEyeLocation, targetLoc).countPerMeter(4).delta(0.05).extra(0.1).spawnAsPlayerActive(player);
			new PPLine(Particle.SOUL_FIRE_FLAME, standEyeLocation, targetLoc).countPerMeter(1.5).delta(0.08).extra(0.015).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SMOKE_NORMAL, targetLoc, 10).delta(1).spawnAsPlayerActive(player);
			world.playSound(targetLoc, Sound.ITEM_AXE_STRIP, SoundCategory.PLAYERS, 2.2f, 0.6f);
			world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.5f, 0.9f);
		}
	}

	@Override
	public void flameTotemExpire(World world, Player player, Location standLocation) {
		new PartialParticle(Particle.FLASH, standLocation).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, standLocation, 25, 0.5, 1, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, standLocation, 40, 0.5, 1, 0.5, 0.15).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(standLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 0.7f, 1.1f);
		world.playSound(standLocation, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.8f, 1.2f);
	}
}
