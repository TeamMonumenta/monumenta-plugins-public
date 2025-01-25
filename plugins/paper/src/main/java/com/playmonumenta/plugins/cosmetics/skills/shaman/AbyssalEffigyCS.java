package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AbyssalEffigyCS extends LightningTotemCS {

	public static final String NAME = "Abyssal Effigy";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"An effigy of stone, drenched in mana.",
			"The deep blue eye makes a swift death for",
			"those who disturb its flowing magic."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	public static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(20, 200, 180), 1.1f);
	public static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(204, 153, 255), 1.1f);
	public static final Particle.DustOptions DARK_PURPLE = new Particle.DustOptions(Color.fromRGB(127, 0, 255), 1.35f);

	private @Nullable ItemDisplay mOrbOfHarrakfarDisplay = null;
	private @Nullable LivingEntity mTarget = null;

	@Override
	public void lightningTotemSpawn(Location standLocation, Player player, ArmorStand stand, double radius) {
		EntityEquipment standArmor = stand.getEquipment();
		standArmor.clear();
		ItemStack manashieldTunic = DisplayEntityUtils.generateRPItem(Material.CHAINMAIL_CHESTPLATE, "Manashield Tunic");
		ItemStack celestialSiege = DisplayEntityUtils.generateRPItem(Material.CHAINMAIL_LEGGINGS, "Celestial Siege");
		ItemStack mistsWake = DisplayEntityUtils.generateRPItem(Material.LEATHER_BOOTS, "Mist's Wake");
		ItemStack[] armor = {mistsWake, celestialSiege, manashieldTunic};
		standArmor.setArmorContents(armor);

		ItemStack hexbornShard = DisplayEntityUtils.generateRPItem(Material.AMETHYST_SHARD, "Hexborn Shard");
		standArmor.setItemInMainHand(hexbornShard);
		standArmor.setItemInOffHand(hexbornShard);
		stand.setLeftArmPose(new EulerAngle(Math.toRadians(-105), Math.toRadians(-90), 0));
		stand.setRightArmPose(new EulerAngle(Math.toRadians(-105), Math.toRadians(90), 0));

		if (mOrbOfHarrakfarDisplay != null) {
			mOrbOfHarrakfarDisplay.remove();
		}

		mOrbOfHarrakfarDisplay = standLocation.getWorld().spawn(standLocation.clone().add(0, 0.9, 0), ItemDisplay.class);
		mOrbOfHarrakfarDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
		mOrbOfHarrakfarDisplay.setItemStack(DisplayEntityUtils.generateRPItem(Material.SNOWBALL, "Orb of Harrakfar"));
		EntityUtils.setRemoveEntityOnUnload(mOrbOfHarrakfarDisplay);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), mOrbOfHarrakfarDisplay::remove, 400);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mOrbOfHarrakfarDisplay != null) {
					mOrbOfHarrakfarDisplay.teleport(stand.getLocation().add(0, 0.9, 0));
					mOrbOfHarrakfarDisplay.setRotation(stand.getLocation().getYaw(), 0);
				}
				mTicks++;
				if (mTicks > 20 || !stand.isValid()) {
					if (mOrbOfHarrakfarDisplay != null && !stand.isValid()) {
						mOrbOfHarrakfarDisplay.remove();
					}
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new PPSpiral(Particle.SOUL_FIRE_FLAME, standLocation.clone().subtract(0, LocationUtils.distanceToGround(standLocation, -64, 2) - 0.1, 0), radius).ticks(20).countPerBlockPerCurve(10).spawnAsPlayerActive(player);
		player.getWorld().playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 0.95f);
	}

	@Override
	public void lightningTotemTick(Player player, double radius, Location standLocation, ArmorStand stand) {
		new PPCircle(Particle.SCULK_SOUL, standLocation.clone().add(0, 0.2, 0), radius).countPerMeter(0.08).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, standLocation).delta(1.15).spawnAsPlayerActive(player);

		if (mTarget != null && !mTarget.isDead() && EntityUtils.getNearbyMobsInSphere(standLocation, radius, null).contains(mTarget)) {
			standLocation.setDirection(LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(mTarget), standLocation.clone().add(0, 1, 0)));
			new PPLine(Particle.ELECTRIC_SPARK, LocationUtils.getHalfHeightLocation(mTarget), standLocation.clone().add(0, 1, 0)).countPerMeter(0.6 + Math.random()).offset(Math.random()).spawnAsPlayerPassive(player);
		} else {
			standLocation.setPitch(0);
		}

		if (mOrbOfHarrakfarDisplay != null) {
			mOrbOfHarrakfarDisplay.teleport(standLocation.clone().add(0, 0.9, 0));
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				if (!stand.isValid() && mOrbOfHarrakfarDisplay != null) {
					mOrbOfHarrakfarDisplay.remove();
				}
			}, 1);
		}
	}

	@Override
	public void lightningTotemStrike(Player player, Location standLocation, LivingEntity target) {
		Location targetLocation = LocationUtils.getHalfHeightLocation(target);
		standLocation.add(0, 1, 0);

		new PartialParticle(Particle.CLOUD, targetLocation).count(5).delta(0.5).spawnAsPlayerActive(player);

		player.getWorld().playSound(targetLocation, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.2f, 0.8f);
		player.getWorld().playSound(targetLocation, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.2f, 1.2f);
		player.getWorld().playSound(standLocation, "block.vault.open_shutter", SoundCategory.PLAYERS, 1.8f, 1.2f);

		Location middleLoc = standLocation.add(targetLocation).multiply(0.5);
		Vector dir = LocationUtils.getDirectionTo(targetLocation, standLocation);
		double distance = targetLocation.distance(standLocation);
		ParticleUtils.drawParticleLineSlash(middleLoc, dir, 0, distance, 0.125, 3,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				new PartialParticle(Particle.GLOW, lineLoc, 2, 0.03, 0.03, 0.03, 0).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, lineLoc, 2, 0.05, 0.05, 0.05, 0).data(CYAN).spawnAsPlayerActive(player);
			});
		mTarget = target;
	}

	@Override
	public void lightningTotemEnhancementStorm(Player player, Location loc, double stormRadius) {
		new PPCircle(Particle.SQUID_INK, loc, stormRadius)
			.count(2).delta(0.1, 0.05, 0.1).spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, loc, stormRadius)
			.count(2).delta(0.1, 0.05, 0.1).data(DARK_PURPLE).spawnAsPlayerActive(player);
	}

	@Override
	public void lightningTotemEnhancementStrike(Player player, Location loc, double stormRadius) {
		player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.9f, 0.9f);
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, stormRadius, 0, 4,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.REDSTONE, location).data(PURPLE).spawnAsPlayerActive(player))
			)
		);
	}

	@Override
	public void lightningTotemExpire(Player player, Location standLocation, World world) {
		new PartialParticle(Particle.FLASH, standLocation, 3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.PORTAL, standLocation, 35, 0.15, 0.2, 0.15, 1).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.8f, 0.8f);
		world.playSound(standLocation, Sound.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, 0.8f);
		if (mOrbOfHarrakfarDisplay != null) {
			mOrbOfHarrakfarDisplay.remove();
		}
	}
}
