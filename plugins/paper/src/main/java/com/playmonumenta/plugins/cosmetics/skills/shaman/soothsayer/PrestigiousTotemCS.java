package com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

public class PrestigiousTotemCS extends WhirlwindTotemCS implements PrestigeCS {

	public static final String NAME = "Prestigious Totem";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A knightly statue sparks",
			"a courageous will."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CANDLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	private final BlockData[] mBlockData = {Material.WHITE_WOOL.createBlockData(), Material.BIRCH_WOOD.createBlockData(), Material.OAK_WOOD.createBlockData(), Material.DIRT.createBlockData()};

	@Override
	public void whirlwindTotemSpawn(World world, Player player, Location standLocation, ArmorStand stand) {
		EntityEquipment standArmor = stand.getEquipment();
		standArmor.clear();
		ItemStack eraecus = DisplayEntityUtils.generateRPItem(Material.GOLDEN_HELMET, "Eraecus");
		ItemStack goldenSunChestplate = DisplayEntityUtils.generateRPItem(Material.GOLDEN_CHESTPLATE, "Golden Sun Chestplate");
		ItemStack celestialSiege = DisplayEntityUtils.generateRPItem(Material.IRON_LEGGINGS, "Pelias' Greaves");
		ItemStack goldenSunBoots = DisplayEntityUtils.generateRPItem(Material.GOLDEN_BOOTS, "Golden Sun Boots");
		ItemStack[] armor = {goldenSunBoots, celestialSiege, goldenSunChestplate, eraecus};
		standArmor.setArmorContents(armor);

		ItemStack tournamentLongsword = DisplayEntityUtils.generateRPItem(Material.IRON_SWORD, "Tournament Longsword");
		standArmor.setItemInMainHand(tournamentLongsword);
		standArmor.setItemInOffHand(tournamentLongsword);
		stand.setLeftArmPose(new EulerAngle(0, Math.toRadians(80), Math.toRadians(-40)));
		stand.setRightArmPose(new EulerAngle(0, Math.toRadians(-75), Math.toRadians(40)));

		world.playSound(standLocation, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.0f, 0.6f);
	}

	@Override
	public void whirlwindTotemPulse(Player player, Location standLocation, double radius) {
		standLocation.getWorld().playSound(standLocation, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.7f, 1.1f);
		standLocation.getWorld().playSound(standLocation, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.7f, 0.9f);
		standLocation.getWorld().playSound(standLocation, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.8f, 1.25f);
		new PartialParticle(Particle.CLOUD, standLocation, 10, 1.5, 1.5, 1.5, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WAX_OFF, standLocation, 10, 1.5, 1.5, 1.5, 5).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PPCircle(Particle.CLOUD, standLocation.clone().add(0, 0.2, 0), radius / 4).countPerMeter(0.8).delta(1, 0, -4).extra(0.01 * radius).directionalMode(true).rotateDelta(true).spawnAsPlayerActive(player);
				new PPCircle(Particle.FALLING_DUST, standLocation.clone().add(0, 0.2, 0), radius / 4 * (1 + mTicks)).countPerMeter(4).delta(0.1).data(mBlockData[mTicks]).spawnAsPlayerActive(player);
				mTicks++;
				if (mTicks >= 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	@Override
	public void whirlwindTotemExpire(Player player, World world, Location standLocation, double radius) {
		new PartialParticle(Particle.CLOUD, standLocation, 25, 0.8, 0.8, 0.8, 0.05).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.ENTITY_IRON_GOLEM_DEATH,
			SoundCategory.PLAYERS, 1.0f, 1.5f);
	}
}
