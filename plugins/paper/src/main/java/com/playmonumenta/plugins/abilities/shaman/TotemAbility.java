package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.Sanctuary;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public abstract class TotemAbility extends Ability implements AbilityWithDuration {

	private static final double VELOCITY = 2;

	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();
	private final String mProjectileName;
	private final String mTotemName;
	public final String mDisplayName;
	public @Nullable LivingEntity mAttachedMob = null;
	public @Nullable LivingEntity mMobStuckWithEffect = null;
	public double mWhirlwindBuffPercent = 0;
	public boolean mDecayedBuffed = false;

	private @Nullable BukkitRunnable mTotemTickingRunnable;

	public TotemAbility(Plugin plugin, Player player, AbilityInfo<?> info, String projectileName, String totemName, String displayName) {
		super(plugin, player, info);
		mProjectileName = projectileName;
		mTotemName = totemName;
		mDisplayName = displayName;
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		if (mTotemTickingRunnable != null) {
			mTotemTickingRunnable.cancel();
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
			if (!mPlayer.getWorld().equals(proj.getWorld()) || mPlayer.getLocation().distance(proj.getLocation()) >= 50) {
				return;
			}
			if (stats != null) {
				Entity hitMob = event.getHitEntity();
				Ability stickyTotems = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, AdhesiveTotems.class);
				if (stickyTotems != null) {
					if (hitMob instanceof LivingEntity mob &&
							!EntityUtils.isBoss(hitMob) &&
							!(hitMob instanceof Player || hitMob instanceof ArmorStand || hitMob instanceof Villager)) {
						mAttachedMob = mob;
						mAttachedMob.getWorld().playSound(mAttachedMob, Sound.BLOCK_HONEY_BLOCK_PLACE, 2.0f, 0.7f);

						if (stickyTotems.isLevelTwo()) {
							mMobStuckWithEffect = mob;
							onAdhereToMob(mMobStuckWithEffect);
						}
					}
				}

				placeTotem(proj.getLocation(), stats);
			}
		}
	}

	private int mCurrDuration = -1;

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
		stand.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);

		TotemicEmpowerment.addTotem(mPlayer, stand);

		ArmorStand durationStand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, "TotemDurationStand");
		if (durationStand != null) {
			durationStand.setMarker(true);
			durationStand.customName(Component.text(""));
			durationStand.setCustomNameVisible(true);
		}

		int duration = getInitialAbilityDuration();
		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);
		mTotemTickingRunnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				Location standLocation = stand.getLocation();
				if (stand.isDead() || !stand.isValid()) {
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
				if (durationStand != null) {
					AbilityUtils.produceDurationString(stand, durationStand, duration, mT, mWhirlwindBuffPercent, mDecayedBuffed);
				}

				if (((mWhirlwindBuffPercent != 0 && mT >= duration * mWhirlwindBuffPercent) || (mWhirlwindBuffPercent == 0 && mT >= duration))
					|| mPlayer.isDead() || !mPlayer.isValid()
					|| !mPlayer.getWorld().equals(standLocation.getWorld())) {
					mWhirlwindBuffPercent = 0;
					mDecayedBuffed = false;
					onTotemExpire(world, standLocation);
					this.cancel();
				}
				mT++;

				if (mCurrDuration >= 0) {
					mCurrDuration++;
				}
			}

			@Override
			public synchronized void cancel() {
				mAttachedMob = null;
				mMobStuckWithEffect = null;
				if (durationStand != null) {
					durationStand.remove();
				}
				TotemicEmpowerment.removeTotem(mPlayer, stand);
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, TotemAbility.this);
				super.cancel();
			}
		};
		mTotemTickingRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public abstract int getInitialAbilityDuration();

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	public abstract void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats);

	public abstract void onTotemExpire(World world, Location standLocation);

	public void onAdhereToMob(LivingEntity hitMob) {

	}

	public void dealSanctuaryImpacts(List<LivingEntity> targets, int ticks) {
		Sanctuary sanctuary = AbilityManager.getManager().getPlayerAbility(mPlayer, Sanctuary.class);
		if (sanctuary != null) {
			sanctuary.dealSanctuaryDebuffs(targets, ticks);
		}
	}

	@Override
	public void invalidate() {
		if (mTotemTickingRunnable != null) {
			mTotemTickingRunnable.cancel();
		}
		super.invalidate();
	}
}
