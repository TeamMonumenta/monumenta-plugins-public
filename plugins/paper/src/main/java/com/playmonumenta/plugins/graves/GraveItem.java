package com.playmonumenta.plugins.graves;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
import org.checkerframework.checker.nullness.qual.Nullable;

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

		@Override
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
	private static final String KEY_NBT = "nbt";
	private static final String KEY_AGE = "age";
	private static final String KEY_SLOT = "slot";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_Z = "z";
	private @Nullable BukkitRunnable mRunnable;
	private final GraveManager mManager;
	private final Grave mGrave;
	private final Player mPlayer;
	ItemStack mItem;
	private @Nullable Item mEntity;
	private @Nullable Location mLocation;
	private @Nullable Vector mVelocity;
	private @Nullable Short mAge;
	public @Nullable Integer mSlot;
	@Nullable Status mStatus;
	private int mTickLastStatusChange;
	private boolean mLoggedOut = false;

	private GraveItem(GraveManager manager, Grave grave, Player player, ItemStack item) {
		mManager = manager;
		mGrave = grave;
		mPlayer = player;
		mItem = item;
		mEntity = null;
		mLocation = null;
		mVelocity = null;
		mAge = null;
		mSlot = null;
		mStatus = null;
		mRunnable = null;
		mTickLastStatusChange = Bukkit.getCurrentTick();
	}

	// Full GraveItem from deserialized data
	public GraveItem(GraveManager manager, Grave grave, Player player, ItemStack item, Status status, Location location, Vector velocity, Short age, Integer slot) {
		this(manager, grave, player, item);
		mStatus = status;
		mLocation = location != null ? location : mGrave.getLocation();
		mVelocity = velocity;
		mAge = age;
		mSlot = slot;
		if (mStatus == Status.DROPPED && isOnThisShard()) {
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
		}
	}

	// New GraveItem from player death
	public GraveItem(Grave grave, ItemStack item, int slot) {
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
		mLocation = grave.getLocation();
		mVelocity = null;
		mSlot = slot;
		if (mStatus == Status.DROPPED && isOnThisShard()) {
			mManager.addUnloadedItem(Chunk.getChunkKey(mLocation), this);
			spawn();
		}
	}

	// New GraveItem from ThrownItem being destroyed
	public GraveItem(Grave grave, ThrownItem item, boolean destroyedByVoid) {
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
				if (!destroyedByVoid && ItemStatUtils.getInfusionLevel(mItem, InfusionType.HOPE) > 0) {
					mStatus = Status.SAFE;
				} else {
					mStatus = Status.LIMBO;
				}
		}
		mLocation = grave.getLocation();
	}

	public @Nullable UUID getUniqueId() {
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

	private boolean isOnThisShard() {
		return ServerProperties.getShardName().equals(mGrave.mShardName);
	}

	private boolean canSpawn() {
		if (!mLoggedOut && isOnThisShard() && mStatus == Status.DROPPED && (mEntity == null || !mEntity.isValid())) {
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
		mTickLastStatusChange = Bukkit.getCurrentTick();
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
			ItemStatUtils.shatter(mItem);
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
		// This event should always be cancelled, items are not picked up directly
		// They are either placed in the grave directly, or the pickup is simulated via DeathItemListener
		event.setCancelled(true);
		Player player = event.getPlayer();
		if (player == mPlayer) {
			// Owner picking up items
			if (mGrave.collectItem(player, this) == 0) {
				// Owner picking up full stack
				mGrave.removeItem(this);
				mManager.removeItem(event.getItem().getUniqueId());
			} else {
				// Owner couldn't pick up full stack, put in grave safe
				remove(Status.SAFE);
				player.spawnParticle(Particle.VILLAGER_HAPPY, mLocation, 1);
				player.playSound(mLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.5f);
				//TODO alert player the item wouldn't fit in their inventory so it was put in their grave
			}
		} else {
			// Non-owner within pickup range, put in grave safe
			remove(Status.SAFE);
			player.spawnParticle(Particle.VILLAGER_HAPPY, mLocation, 1);
			player.playSound(mLocation, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.5f);
			//TODO alert player the item belonged to another player and was placed in their grave
			//TODO alert owner their item was picked up and safely put in their grave
		}
	}

	void onDestroyItem(boolean destroyedByVoid) {
		if (mStatus == Status.DROPPED) {
			if (!destroyedByVoid && ItemStatUtils.getInfusionLevel(mItem, InfusionType.HOPE) > 0) {
				remove(Status.SAFE);
			} else {
				remove(Status.LIMBO);
				if (!mGrave.mAlertedLimbo) {
					mGrave.mAlertedLimbo = true;
					mPlayer.sendMessage(Component.text("Some of the items you died with were destroyed, but don't worry! If you can get back without dying again, you can get them back! ", NamedTextColor.RED)
						.append(Component.text("(/help death for more info)", NamedTextColor.RED)
							.clickEvent(ClickEvent.runCommand("/help death"))));
				}
			}
		}
	}

	void onDeath() {
		if (isOnThisShard() && mStatus == Status.LIMBO) {
			if (Math.abs(mTickLastStatusChange - Bukkit.getCurrentTick()) < 40) {
				Plugin.getInstance().getLogger().warning("Tried to shatter an item due to death that was just placed in limbo <2s ago, ignoring");
				return;
			}

			ItemUtils.ItemDeathResult result = ItemUtils.getItemDeathResult(mItem);
			if (result == ItemUtils.ItemDeathResult.SHATTER) {
				remove(Status.SHATTERED);
				if (!mGrave.mAlertedShatter) {
					mGrave.mAlertedShatter = true;
					mPlayer.sendMessage(Component.text("Some of the items in your grave were shattered! You can still get them back from the grave, but won't be able to equip them until repaired. ", NamedTextColor.RED)
						.append(Component.text("(/help death for more info)", NamedTextColor.RED)
							.clickEvent(ClickEvent.runCommand("/help death"))));
				}
			} else {
				remove(Status.LOST);
				if (!mGrave.mAlertedLost) {
					mGrave.mAlertedLost = true;
					mPlayer.sendMessage(Component.text("Some of the items in your grave were lost! This only happens to common items like torches and food; be sure to restock! ", NamedTextColor.RED)
						.append(Component.text("(/help death for more info)", NamedTextColor.RED)
							.clickEvent(ClickEvent.runCommand("/help death"))));
				}
			}
		}
	}

	void onLogin() {
		spawn();
	}

	void onLogout() {
		mLoggedOut = true;
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

	static @Nullable GraveItem deserialize(GraveManager manager, Grave grave, Player player, JsonObject data) {
		ItemStack item = null;
		Status status = null;
		Short age = null;
		Integer slot = null;
		Location location = null;
		Vector velocity = null;
		if (data.has(KEY_NBT) && data.get(KEY_NBT).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_NBT).isString()) {
			item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_NBT).getAsString()));
		}
		if (ItemUtils.getItemDeathResult(item) == ItemUtils.ItemDeathResult.KEEP_NOGRAVE) {
			// Item should not exist, delete it
			return null;
		}
		if (data.has(KEY_STATUS) && data.get(KEY_STATUS).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_STATUS).isString()) {
			status = GraveItem.Status.fromString(data.getAsJsonPrimitive(KEY_STATUS).getAsString());
		}
		if (data.has(KEY_AGE) && data.get(KEY_AGE).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_AGE).isNumber()) {
			age = data.getAsJsonPrimitive(KEY_AGE).getAsShort();
		}
		if (data.has(KEY_SLOT) && data.get(KEY_SLOT).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_SLOT).isNumber()) {
			slot = data.getAsJsonPrimitive(KEY_SLOT).getAsInt();
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

		return new GraveItem(manager, grave, player, item, status, location, velocity, age, slot);
	}

	@Nullable JsonObject serialize() {
		update();
		if (mStatus == Status.COLLECTED) {
			return null;
		}
		JsonObject data = new JsonObject();
		data.addProperty(KEY_NBT, NBTItem.convertItemtoNBT(mItem).toString());
		data.addProperty(KEY_STATUS, mStatus == null ? null : mStatus.toString());
		data.addProperty(KEY_AGE, mAge);
		data.addProperty(KEY_SLOT, mSlot);

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
