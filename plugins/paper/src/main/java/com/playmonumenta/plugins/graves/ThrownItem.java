package com.playmonumenta.plugins.graves;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import javax.annotation.Nullable;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ThrownItem {
	private static final String KEY_LOCATION = "location";
	private static final String KEY_VELOCITY = "velocity";
	private static final String KEY_INSTANCE = "instance";
	private static final String KEY_WORLD = "world";
	private static final String KEY_NBT = "nbt";
	private static final String KEY_AGE = "age";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_Z = "z";

	private @Nullable BukkitRunnable mRunnable = null;
	GraveManager mManager;
	Player mPlayer;
	ItemStack mItem;
	private @Nullable Item mEntity;
	String mWorldName;
	Location mLocation;
	private Vector mVelocity;
	Integer mDungeonInstance;
	private Short mAge;
	private boolean mValid;
	private boolean mLoggedOut = false;

	// Full ThrownItem from deserialization
	public ThrownItem(GraveManager manager, Player player, ItemStack item, String world, Location location, Vector velocity, Integer instance, Short age) {
		mManager = manager;
		mPlayer = player;
		mItem = item;
		mWorldName = world;
		mLocation = location;
		mVelocity = velocity;
		mDungeonInstance = instance;
		mAge = age;
		mValid = true;
		if (isInThisWorld()) {
			updateInstance();
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
		}
	}

	// New ThrownItem from drop event
	public ThrownItem(GraveManager manager, Player player, Item entity) {
		mManager = manager;
		mPlayer = player;
		mItem = entity.getItemStack();
		mEntity = entity;
		mWorldName = entity.getWorld().getName();
		mLocation = entity.getLocation().clone();
		mVelocity = entity.getVelocity().clone();
		mAge = new NBTEntity(entity).getShort("Age");
		mValid = true;
		mDungeonInstance = null;
		if (ScoreboardUtils.getScoreboardValue("$Shard", "const").orElse(0) > 0) {
			mDungeonInstance = ScoreboardUtils.getScoreboardValue(mPlayer, "DAccess").orElse(0);
		}
		// Item dropping for first time, make immune to explosions for 5 seconds.
		// This prevents shattering due to double-creepers and TNT traps.
		mEntity.addScoreboardTag("ExplosionImmune");
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mEntity != null && mEntity.isValid()) {
					mEntity.removeScoreboardTag("ExplosionImmune");
				}
			}
		}.runTaskLater(Plugin.getInstance(), 5 * 20);
		mEntity.addScoreboardTag("ThrownItem");
		mManager.addItem(mEntity.getUniqueId(), this);
		startTracking();
	}

	private boolean isInThisWorld() {
		return mPlayer.getWorld().getName().equals(mWorldName);
	}

	private boolean canSpawn() {
		if (!mLoggedOut && isInThisWorld() && (mEntity == null || !mEntity.isValid())) {
			mLocation.setWorld(mPlayer.getWorld());
			return mLocation.isChunkLoaded();
		}
		return false;
	}

	private void spawn() {
		if (canSpawn()) {
			mEntity = mPlayer.getWorld().dropItem(mLocation, mItem);
			mEntity.setVelocity(mVelocity);
			mEntity.setCanMobPickup(false);
			mEntity.setPickupDelay(0);
			mEntity.setThrower(mPlayer.getUniqueId());
			NBTEntity nbt = new NBTEntity(mEntity);
			nbt.setShort("Age", mAge);
			mManager.addItem(mEntity.getUniqueId(), this);
			mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
			startTracking();
		}
	}

	private void remove() {
		if (mEntity != null) {
			mManager.removeItem(mEntity.getUniqueId());
			mEntity.remove();
			mEntity = null;
			stopTracking();
		}
	}

	private void startTracking() {
		if (mRunnable == null) {
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mEntity != null && mEntity.isValid()) {
						// Check the item's location and the next block down for water, just in case it bobs out of the water.
						// Should fix the weird bug with hoped items no longer floating?
						Location loc = mEntity.getLocation();
						if (!(loc.getBlock().isLiquid() || loc.getBlock().getRelative(BlockFace.DOWN).isLiquid())
							&& loc.getY() > mLocation.getY() + 2) {
							mEntity.teleport(mLocation);
						}
					} else {
						delete();
					}
				}
			};
			mRunnable.runTaskTimer(Plugin.getInstance(), 20, 20);
		}
	}

	private void stopTracking() {
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}

	void delete() {
		mValid = false;
		remove();
		mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
	}

	boolean isValid() {
		return mValid;
	}

	void onLogin() {
		spawn();
	}

	void onLogout() {
		mLoggedOut = true;
		remove();
		mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
	}

	void onSave() {
		update();
	}

	void onChunkLoad() {
		spawn();
	}

	void onChunkUnload() {
		remove();
		mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
	}

	void onDestroyItem() {
		mValid = false;
		update();
		//TODO alert player that an item is now in limbo
	}

	public void onAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		// Any player is allowed to pick up a thrown item
		if (event.getRemaining() == 0) {
			// Stack was fully picked up
			mValid = false;
		} else {
			mItem.setAmount(event.getRemaining());
		}
	}

	private void update() {
		if (mEntity != null) {
			if (mEntity.isValid()) {
				mLocation = mEntity.getLocation();
				mVelocity = mEntity.getVelocity();
				mAge = new NBTEntity(mEntity).getShort(KEY_AGE);
			} else {
				delete();
			}
		}
	}

	private void updateInstance() {
		if (isInThisWorld() && mDungeonInstance != null) {
			int instance = ScoreboardUtils.getScoreboardValue(mPlayer, "DAccess").orElse(0);
			if (instance != 0 && instance != mDungeonInstance) {
				int x = 512 * ((instance / Constants.DUNGEON_INSTANCE_MODULUS) - (mDungeonInstance / Constants.DUNGEON_INSTANCE_MODULUS));
				int z = 512 * ((instance % Constants.DUNGEON_INSTANCE_MODULUS) - (mDungeonInstance % Constants.DUNGEON_INSTANCE_MODULUS));
				mLocation.add(x, 0, z);
				mDungeonInstance = instance;
			}
		}
	}

	static ThrownItem deserialize(GraveManager manager, Player player, JsonObject data) {
		ItemStack item = null;
		String world = null;
		Integer instance = null;
		Short age = null;
		Location location = null;
		Vector velocity = null;
		if (data.has(KEY_NBT) && data.get(KEY_NBT).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_NBT).isString()) {
			item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_NBT).getAsString()));
		}
		if (data.has(KEY_WORLD) && data.get(KEY_WORLD).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_WORLD).isString()) {
			world = data.getAsJsonPrimitive(KEY_WORLD).getAsString();
		}
		if (data.has(KEY_INSTANCE) && data.get(KEY_INSTANCE).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_INSTANCE).isNumber()) {
			instance = data.getAsJsonPrimitive(KEY_INSTANCE).getAsInt();
		}
		if (data.has(KEY_AGE) && data.get(KEY_AGE).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_AGE).isNumber()) {
			age = data.getAsJsonPrimitive(KEY_AGE).getAsShort();
		}
		if (data.has(KEY_LOCATION) && data.get(KEY_LOCATION).isJsonObject()) {
			JsonObject loc = data.getAsJsonObject(KEY_LOCATION);
			double x = loc.get(KEY_X).getAsDouble();
			double y = loc.get(KEY_Y).getAsDouble();
			double z = loc.get(KEY_Z).getAsDouble();
			location = new Location(null, x, y, z);
		}
		if (data.has(KEY_VELOCITY) && data.get(KEY_VELOCITY).isJsonObject()) {
			JsonObject loc = data.getAsJsonObject(KEY_VELOCITY);
			double x = loc.get(KEY_X).getAsDouble();
			double y = loc.get(KEY_Y).getAsDouble();
			double z = loc.get(KEY_Z).getAsDouble();
			velocity = new Vector(x, y, z);
		}

		return new ThrownItem(manager, player, item, world, location, velocity, instance, age);
	}

	@Nullable JsonObject serialize() {
		update();
		if (!mValid) {
			return null;
		}
		JsonObject data = new JsonObject();
		data.addProperty(KEY_NBT, NBTItem.convertItemtoNBT(mItem).toString());
		data.addProperty(KEY_WORLD, mWorldName);
		data.addProperty(KEY_INSTANCE, mDungeonInstance);
		data.addProperty(KEY_AGE, mAge);

		if (mLocation != null) {
			JsonObject location = new JsonObject();
			location.addProperty(KEY_X, mLocation.getX());
			location.addProperty(KEY_Y, mLocation.getY());
			location.addProperty(KEY_Z, mLocation.getZ());
			data.add(KEY_LOCATION, location);
		}

		if (mVelocity != null) {
			JsonObject velocity = new JsonObject();
			velocity.addProperty(KEY_X, mVelocity.getX());
			velocity.addProperty(KEY_Y, mVelocity.getY());
			velocity.addProperty(KEY_Z, mVelocity.getZ());
			data.add(KEY_VELOCITY, velocity);
		}

		return data;
	}
}
