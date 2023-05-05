package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public abstract class TotemAbility extends Ability {

	private static final double VELOCITY = 2;

	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();
	private final String mProjectileName;
	private final String mTotemName;
	public @Nullable LivingEntity mAttachedMob = null;
	public @Nullable LivingEntity mMobStuckWithEffect = null;

	public TotemAbility(Plugin plugin, Player player, AbilityInfo<?> info, String projectileName, String totemName) {
		super(plugin, player, info);
		mProjectileName = projectileName;
		mTotemName = totemName;
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, mProjectileName, Particle.CLOUD);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		int cd = getModifiedCooldown();
		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid() || p.getTicksLived() >= 100);
		new BukkitRunnable() {
			int mT = 0;
			Location mLastLoc = proj.getLocation();
			@Override
			public void run() {
				if (mProjectiles.get(proj) != playerItemStats) {
					this.cancel();
				}

				if (mT > cd) {
					proj.remove();
					this.cancel();
				}

				if (!proj.isDead()) {
					mLastLoc = proj.getLocation();
				} else {
					if (mProjectiles.remove(proj) != null) {
						placeTotem(proj.getLocation(), playerItemStats);
					}
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && proj.getTicksLived() <= 160) {
			ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);
			if (!mPlayer.getWorld().equals(proj.getWorld())) {
				return;
			}
			if (stats != null) {
				Entity hitMob = event.getHitEntity();
				Ability stickyTotems = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, AdhesiveTotems.class);
				if (stickyTotems != null) {
					if (hitMob instanceof LivingEntity &&
							!EntityUtils.isBoss(hitMob) &&
							!(hitMob instanceof Player || hitMob instanceof ArmorStand)) {
						mAttachedMob = (LivingEntity) hitMob;

						if (stickyTotems.isLevelTwo()) {
							mMobStuckWithEffect = (LivingEntity) hitMob;
							if (getInfo().getLinkedSpell() != null) {
								AdhesiveTotems.onTotemHitMob(mPlugin, mPlayer, (LivingEntity) hitMob, getInfo().getLinkedSpell());
							}
						}
					}
				}

				placeTotem(proj.getLocation(), stats);
			}
		}
	}

	private void placeTotem(Location bLoc, ItemStatManager.PlayerItemStats stats) {
		World world = mPlayer.getWorld();
		bLoc.setDirection(mPlayer.getLocation().toVector().subtract(bLoc.toVector()).normalize());
		ArmorStand stand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, mTotemName);
		if (stand == null || stand.isDead() || !stand.isValid()) {
			return;
		}
		stand.setMarker(false);
		stand.setGravity(mAttachedMob == null);
		stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		stand.addScoreboardTag("REMOVE_ON_UNLOAD");

		TotemicEmpowerment.addTotem(mPlayer, stand);

		ArmorStand durationStand = (ArmorStand) world.spawnEntity(bLoc, EntityType.ARMOR_STAND);
		durationStand.setMarker(true);
		durationStand.setInvisible(true);
		durationStand.setInvulnerable(true);
		durationStand.setCustomNameVisible(true);
		durationStand.setGravity(false);
		durationStand.setSmall(true);
		durationStand.setBasePlate(false);
		durationStand.setCollidable(false);

		int duration = getTotemDuration();
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				Location standLocation = stand.getLocation();
				if (stand.isDead() || !stand.isValid()) {
					durationStand.remove();
					TotemicEmpowerment.removeTotem(mPlayer, stand);
					this.cancel();
					return;
				}

				onTotemTick(mT, stand, world, standLocation, stats);

				if (mAttachedMob != null) {
					if (mAttachedMob.isDead() || !mAttachedMob.isValid()) {
						stand.setGravity(true);
						mAttachedMob = null;
					} else {
						stand.teleport(mAttachedMob.getEyeLocation().clone().add(0, 0.5, 0));
					}
				} else {
					stand.setGravity(true);
				}

				AbilityUtils.produceDurationString(stand, durationStand, duration, mT);

				if (mT >= duration || mPlayer.isDead() || !mPlayer.isValid() || !mPlayer.getWorld().equals(standLocation.getWorld())) {
					durationStand.remove();
					TotemicEmpowerment.removeTotem(mPlayer, stand);
					onTotemExpire(world, standLocation);
					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	public abstract int getTotemDuration();

	public abstract void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats);

	public abstract void onTotemExpire(World world, Location standLocation);
}
