package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.TotemicProjectionCS;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.TotemicConsecrationCS;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public abstract class TotemAbility extends Ability implements AbilityWithDuration {
	public static final double VELOCITY = 1.25;
	public static final double TIME_TO_DROP = 75;
	public static final double XZ_DISTANCE_TO_DROP = 14;
	public static final String PROJECTION_ON_RECAST_DISABLED_OBJECTIVE = "ProjectionOnRecastDisabled";

	private final Map<ThrowableProjectile, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();
	protected @Nullable LivingEntity mTotem = null;
	protected double mRadius;
	private final String mProjectileName;
	private final String mTotemName;
	public final String mDisplayName;
	public double mWhirlwindBuffPercent = 0;
	public boolean mDecayedBuffed = false;
	public int mDuration;
	public boolean mIsInFlight = false;
	private final TotemicProjectionCS mCosmetic;

	public double mSpiritualismMultiplier = 1;
	public boolean mIsBlessed = false;
	public double mTotemRadiusMultiplier = 1;
	private @Nullable TotemicConsecrationCS consecrationCosmetic;

	private @Nullable BukkitRunnable mTotemTickingRunnable;
	private int mLastCastTicks = 0;

	public TotemAbility(Plugin plugin, Player player, AbilityInfo<?> info, String projectileName, String totemName, String displayName) {
		super(plugin, player, info);
		mProjectileName = projectileName;
		mTotemName = totemName;
		mDisplayName = displayName;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TotemicProjectionCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 5) {
			return false;
		}
		mLastCastTicks = ticks;

		if (isOnCooldown()) {
			return attemptProjectionRecast();
		}
		if (mTotemTickingRunnable != null) {
			mTotemTickingRunnable.cancel();
		}

		World world = mPlayer.getWorld();
		ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, mProjectileName, null, LocationUtils.isLocationInWater(mPlayer.getLocation()));
		mCosmetic.totemCast(mPlayer, proj, mTotemName);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		int cd = getModifiedCooldown();
		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid() || p.getTicksLived() >= 100);
		mIsInFlight = true;
		new BukkitRunnable() {
			int mT = 0;
			final Location mPlayerLocation = mPlayer.getLocation();

			@Override
			public void run() {
				if (mProjectiles.get(proj) != playerItemStats) {
					mIsInFlight = false;
					this.cancel();
				}

				Location projLoc = proj.getLocation();
				projLoc.setY(mPlayer.getLocation().getY());
				if (mT >= TIME_TO_DROP
					|| projLoc.distance(mPlayerLocation) >= XZ_DISTANCE_TO_DROP) {
					proj.setVelocity(new Vector(0, -2, 0));
				}


				if (mT > cd) {
					mIsInFlight = false;
					proj.remove();
					this.cancel();
				}

				if (proj.isDead()) {
					if (mProjectiles.remove(proj) != null) {
						placeTotem(proj.getLocation(), playerItemStats);
					}
					mIsInFlight = false;
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private boolean attemptProjectionRecast() {
		if (ScoreboardUtils.checkTag(mPlayer, PROJECTION_ON_RECAST_DISABLED_OBJECTIVE)) {
			return false;
		}
		TotemicProjection projection = mPlugin.mAbilityManager.getPlayerAbility(mPlayer, TotemicProjection.class);
		if (projection != null && projection.getCharges() > 0 && !mIsInFlight && this.isOnCooldown() && !ShamanPassiveManager.getTotemList(mPlayer).isEmpty()) {
			return projection.cast();
		}
		return false;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if ((proj instanceof Snowball || proj instanceof Trident)
			&& proj.getTicksLived() <= 160 && mProjectiles.containsKey(proj)) {
			ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);
			if (!mPlayer.getWorld().equals(proj.getWorld()) || mPlayer.getLocation().distance(proj.getLocation()) >= 50) {
				return;
			}
			if (stats != null) {
				placeTotem(proj.getLocation(), stats);
			}
			proj.remove();
		}
	}

	private int mCurrDuration = -1;

	private void placeTotem(Location bLoc, ItemStatManager.PlayerItemStats stats) {
		World world = mPlayer.getWorld();
		Vector v = mPlayer.getLocation().toVector().subtract(bLoc.toVector());
		float f = (float) -Math.toDegrees(Math.atan2(v.getX(), v.getZ()));
		ArmorStand stand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, mTotemName);
		if (stand == null || stand.isDead() || !stand.isValid()) {
			return;
		}
		stand.setMarker(false);
		stand.setGravity(true);
		stand.setRotation(f, 0);
		stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		stand.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);

		placeTotem(bLoc, mPlayer, stand);

		ShamanPassiveManager.addTotem(mPlayer, stand, this);

		ArmorStand durationStand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, "TotemDurationStand");
		if (durationStand != null) {
			durationStand.setMarker(true);
			durationStand.customName(Component.text(""));
			durationStand.setCustomNameVisible(true);
			durationStand.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		}

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
				if (mIsBlessed) {
					if (consecrationCosmetic == null) {
						consecrationCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new TotemicConsecrationCS());
					}
					consecrationCosmetic.blessedTotemTick(mPlayer, standLocation, getTotemRadius());
				}

				if (durationStand != null) {
					AbilityUtils.produceDurationString(stand, durationStand, mDuration, mT, mWhirlwindBuffPercent, mDecayedBuffed);
				}

				if (((mWhirlwindBuffPercent != 0 && mT >= mDuration * mWhirlwindBuffPercent) || (mWhirlwindBuffPercent == 0 && mT >= mDuration))
					|| mPlayer.isDead() || !mPlayer.isOnline()
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
				if (durationStand != null) {
					durationStand.remove();
				}
				mDecayedBuffed = false;
				mWhirlwindBuffPercent = 0;
				ShamanPassiveManager.removeTotem(mPlayer, stand);
				mTotem = null;
				mCurrDuration = -1;
				mIsBlessed = false;
				setTotemRadiusMultiplier(1);
				ClientModHandler.updateAbility(mPlayer, TotemAbility.this);
				super.cancel();
			}
		};
		mTotemTickingRunnable.runTaskTimer(mPlugin, 0, 1);
		mTotem = stand;
	}

	public abstract void placeTotem(Location loc, Player player, ArmorStand stand);

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() + (int) (mWhirlwindBuffPercent * mDuration)  - this.mCurrDuration : 0;
	}

	public abstract void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats);

	public abstract void onTotemExpire(World world, Location standLocation);

	public abstract void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning);

	@Override
	public void invalidate() {
		if (mTotemTickingRunnable != null) {
			mTotemTickingRunnable.cancel();
		}
		super.invalidate();
	}

	public void setRadius(double radius) {
		mRadius = radius;
	}

	public double getTotemRadius() {
		return mRadius * mTotemRadiusMultiplier;
	}

	public void setTotemRadiusMultiplier(double multiplier) {
		mTotemRadiusMultiplier = multiplier;
	}

	public @Nullable Location getTotemLocation() {
		if (mTotem == null) {
			return null;
		}
		return mTotem.getLocation();
	}

	public List<Player> getPlayersInRange() {
		if (mTotem == null) {
			return new ArrayList<>();
		}
		return new Hitbox.SphereHitbox(mTotem.getLocation(), getTotemRadius()).getHitPlayers(true);
	}
}
