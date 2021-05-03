package com.playmonumenta.plugins.graves;

import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.Hope;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GraveItem {
	public enum Status {
		DROPPED, // Item dropped on death. Hasn't been destroyed or picked up yet.
		SAFE, // Item was picked up by another player. Owner can collect without risk of shattering.
		LIMBO, // Item was destroyed, will be shattered or lost if player dies before collecting it.
		SHATTERED, // Player died while item that can shatter was in limbo. Can be collected shattered.
		COLLECTED, // Player has already collected this item.
		LOST, // Player died while item that can't shatter was in limbo. Cannot be collected.
		;

		public static Status fromString(String string) {
			switch (string.toUpperCase()) {
				case "SAFE":
					return SAFE;
				case "LIMBO":
					return LIMBO;
				case "SHATTERED":
					return SHATTERED;
				case "COLLECTED":
					return COLLECTED;
				case "LOST":
					return LOST;
				default:
					return DROPPED;
			}
		}

		public String toString() {
			switch (this) {
				case SAFE:
					return "SAFE";
				case LIMBO:
					return "LIMBO";
				case SHATTERED:
					return "SHATTERED";
				case COLLECTED:
					return "COLLECTED";
				case LOST:
					return "LOST";
				default:
					return "DROPPED";
			}
		}
	}

	private static final String KEY_STATUS = "status";
	private static final String KEY_LOCATION = "location";
	private static final String KEY_VELOCITY = "velocity";
	private static final String KEY_INSTANCE = "instance";
	private static final String KEY_NBT = "nbt";
	private static final String KEY_AGE = "age";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_Z = "z";
	private BukkitRunnable mRunnable;
	GraveManager mManager;
	Grave mGrave;
	Player mPlayer;
	ItemStack mItem;
	Item mEntity;
	Location mLocation;
	Vector mVelocity;
	Integer mDungeonInstance;
	Short mAge;
	Status mStatus;

	private GraveItem(GraveManager manager, Grave grave, Player player, ItemStack item) {
		mManager = manager;
		mGrave = grave;
		mPlayer = player;
		mItem = item;
		mEntity = null;
		mLocation = null;
		mVelocity = null;
		mDungeonInstance = null;
		mAge = null;
		mStatus = null;
		mRunnable = null;
	}

	// Full GraveItem from deserialized data
	public GraveItem(GraveManager manager, Grave grave, Player player, ItemStack item, Status status, Integer instance, Location location, Vector velocity, Short age) {
		this(manager, grave, player, item);
		mStatus = status;
		mLocation = location != null ? location : mGrave.getLocation();
		mVelocity = velocity;
		mAge = age;
		mDungeonInstance = instance;
		if (mStatus == Status.DROPPED && isInThisWorld()) {
			updateInstance();
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
		}
	}

	// New GraveItem from player death
	public GraveItem(Grave grave, ItemStack item) {
		this(grave.mManager, grave, grave.mPlayer, item);
		switch (ItemUtils.getItemDeathResult(item)) {
			case SHATTER_NOW:
				mStatus = Status.SHATTERED;
				break;
			case SAFE:
				mStatus = Status.SAFE;
				break;
			default:
				mStatus = Status.DROPPED;
		}
		mDungeonInstance = grave.mDungeonInstance;
		mLocation = grave.getLocation();
		mVelocity = null;
		if (mStatus == Status.DROPPED && isInThisWorld()) {
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
			spawn();
		}
	}

	// New GraveItem from ThrownItem being destroyed
	public GraveItem(Grave grave, ThrownItem item) {
		this(grave.mManager, grave, grave.mPlayer, item.mItem);
		switch (ItemUtils.getItemDeathResult(mItem)) {
			case SHATTER_NOW:
				mStatus = Status.SHATTERED;
				break;
			case SAFE:
			case KEEP:
				mStatus = Status.SAFE;
				break;
			default:
				mStatus = Status.LIMBO;
		}
	}

	public UUID getUniqueId() {
		if (mEntity == null) {
			// Handle error
			return null;
		}
		return mEntity.getUniqueId();
	}

	private void startTracking() {
		if (mRunnable == null) {
			GraveItem item = this;
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mEntity != null && mEntity.isValid()) {
						// Check the item's location and the next block down for water, just in case it bobs out of the water.
						// Should fix the weird bug with hoped items no longer floating?
						Location loc = mEntity.getLocation();
						if (!(loc.getBlock().isLiquid() || loc.getBlock().getRelative(BlockFace.DOWN).isLiquid())
						    && loc.getY() > mGrave.mLocation.getY() + 2) {
							respawn();
						} else if (loc.getBlock().getType() == Material.LAVA) {
							//Force hoped items upwards if they're in lava.
							mEntity.setVelocity(new Vector(0, 0.4, 0));
						}
					} else {
						if (mStatus == Status.DROPPED) {
							delete();
							mGrave.removeItem(item);
						}
						stopTracking();
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

	private boolean isInThisWorld() {
		return mPlayer.getWorld().getName().equals(mGrave.mWorldName);
	}

	private boolean canSpawn() {
		if (isInThisWorld() && mStatus == Status.DROPPED && (mEntity == null || !mEntity.isValid())) {
			mLocation.setWorld(mPlayer.getWorld());
			return mLocation.isChunkLoaded();
		}
		return false;
	}

	private void spawn() {
		if (canSpawn()) {
			if (mLocation == null || mVelocity == null) {
				mEntity = mPlayer.getWorld().dropItemNaturally(mGrave.getLocation(), mItem);
			} else {
				mEntity = mPlayer.getWorld().dropItem(mLocation, mItem);
				mEntity.setVelocity(mVelocity);
			}
			mEntity.setCanMobPickup(false);
			mEntity.setPickupDelay(0);
			mEntity.setThrower(mGrave.getUniqueId());
			mEntity.setOwner(mPlayer.getUniqueId());
			if (InventoryUtils.getCustomEnchantLevel(mItem, Hope.PROPERTY_NAME, false) > 0) {
				mEntity.setInvulnerable(true);
			}
			if (mAge != null) {
				NBTEntity nbt = new NBTEntity(mEntity);
				nbt.setShort("Age", mAge);
			} else {
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
			}
			mEntity.addScoreboardTag("GraveItem");
			mManager.addItem(mEntity.getUniqueId(), this);
			mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
			startTracking();
		}
	}

	private void remove(Status newStatus) {
		mStatus = newStatus;
		update();
		stopTracking();
		if (mEntity != null) {
			mManager.removeItem(mEntity.getUniqueId());
			mEntity.remove();
			mEntity = null;
		}
	}

	private void update() {
		if (mEntity != null) {
			if (mEntity.isValid()) {
				mLocation = mEntity.getLocation();
				mVelocity = mEntity.getVelocity();
				NBTEntity nbt = new NBTEntity(mEntity);
				mAge = nbt.getShort(KEY_AGE);
			} else if (mStatus == Status.DROPPED) {
				delete();
			}
		}
	}

	private void updateInstance() {
		if (isInThisWorld() && mDungeonInstance != null) {
			int instance = ScoreboardUtils.getScoreboardValue(mPlayer, "DAccess");
			if (instance != 0 && instance != mDungeonInstance) {
				int x = 512 * ((instance / 1000) - (mDungeonInstance / 1000));
				int z = 512 * ((instance % 1000) - (mDungeonInstance % 1000));
				mLocation.add(x, 0, z);
				mDungeonInstance = instance;
			}
		}
	}

	void respawn() {
		remove(Status.DROPPED);
		mLocation = mGrave.getLocation();
		mVelocity = null;
		spawn();
	}

	void glow() {
		if (mEntity != null && mEntity.isValid()) {
			mEntity.setGlowing(true);
		}
	}

	void save() {
		remove(Status.SAFE);
	}

	ItemStack getItem() {
		if (mStatus == Status.SHATTERED) {
			ItemUtils.shatterItem(mItem);
		}
		return mItem;
	}

	void collect(int remaining) {
		if (remaining == 0) {
			remove(Status.COLLECTED);
		} else {
			mItem.setAmount(remaining);
			if (mEntity != null && mEntity.isValid()) {
				mEntity.setItemStack(mItem);
			}
		}
	}

	void delete() {
		remove(Status.COLLECTED);
		mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
	}

	void onAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		Player player = event.getPlayer();
		if (player == mPlayer) {
			// Owner picking up items
			if (event.getRemaining() == 0) {
				// Owner picking up full stack, will be marked collected in onPickupItem
				// Owner picked up full stack, mark as collected
				mStatus = Status.COLLECTED;
				mGrave.removeItem(this);
			} else {
				// Owner couldn't pick up full stack, put in grave safe
				remove(Status.SAFE);
				player.spawnParticle(Particle.VILLAGER_HAPPY, mLocation, 1);
				player.playSound(mLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.5f);
				event.setCancelled(true);
				//TODO alert player the item wouldn't fit in their inventory so it was put in their grave
			}
		} else {
			// Non-owner within pickup range, put in grave safe
			remove(Status.SAFE);
			player.spawnParticle(Particle.VILLAGER_HAPPY, mLocation, 1);
			player.playSound(mLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.5f);
			event.setCancelled(true);
			//TODO alert player the item belonged to another player and was placed in their grave
			//TODO alert owner their item was picked up and safely put in their grave
		}
	}

	void onDestroyItem() {
		if (mStatus == Status.DROPPED) {
			remove(Status.LIMBO);
			if (!mGrave.mAlertedLimbo) {
				mGrave.mAlertedLimbo = true;
				mPlayer.sendMessage(Component.text("Some of the items you died with were destroyed, but don't worry! If you can get back without dying again, you can get them back! (/deathhelp for more info)", NamedTextColor.RED));
			}
		}
	}

	void onDeath() {
		if (isInThisWorld() && mStatus == Status.LIMBO) {
			ItemUtils.ItemDeathResult result = ItemUtils.getItemDeathResult(mItem);
			if (result == ItemUtils.ItemDeathResult.SHATTER) {
				remove(Status.SHATTERED);
				if (!mGrave.mAlertedShatter) {
					mGrave.mAlertedShatter = true;
					mPlayer.sendMessage(Component.text("Some of the items in your grave were shattered! You can still get them back from the grave, but won't be able to equip them until repaired. (/deathhelp for more info)", NamedTextColor.RED));
				}
			} else {
				remove(Status.LOST);
				if (!mGrave.mAlertedLost) {
					mGrave.mAlertedLost = true;
					mPlayer.sendMessage(Component.text("Some of the items in your grave were lost! This only happens to common items like torches and food; be sure to restock! (/deathhelp for more info)", NamedTextColor.RED));
				}
				//TODO Let player know an item in limbo was lost on death
			}
		}
	}

	void onLogin() {
		spawn();
	}

	void onLogout() {
		remove(mStatus);
		if (mLocation != null) {
			mManager.removeUnloadedItem(Chunk.getChunkKey(mLocation), this);
		}
	}

	void onSave() {
		update();
	}

	void onChunkLoad() {
		spawn();
	}

	void onChunkUnload() {
		if (mStatus == Status.DROPPED) {
			remove(Status.DROPPED);
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
		}
	}

	static GraveItem deserialize(GraveManager manager, Grave grave, Player player, JsonObject data) {
		ItemStack item = null;
		Status status = null;
		Integer instance = null;
		Short age = null;
		Location location = null;
		Vector velocity = null;
		if (data.has(KEY_NBT) && data.get(KEY_NBT).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_NBT).isString()) {
			item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_NBT).getAsString()));
		}
		if (data.has(KEY_STATUS) && data.get(KEY_STATUS).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_STATUS).isString()) {
			status = GraveItem.Status.fromString(data.getAsJsonPrimitive(KEY_STATUS).getAsString());
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

		return new GraveItem(manager, grave, player, item, status, instance, location, velocity, age);
	}

	JsonObject serialize() {
		update();
		if (mStatus == Status.COLLECTED) {
			return null;
		}
		JsonObject data = new JsonObject();
		data.addProperty(KEY_NBT, NBTItem.convertItemtoNBT(mItem).toString());
		data.addProperty(KEY_STATUS, mStatus.toString());
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
