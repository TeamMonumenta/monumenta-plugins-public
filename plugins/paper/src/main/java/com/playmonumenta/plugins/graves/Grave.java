package com.playmonumenta.plugins.graves;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.Phylactery;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Grave {
	private static final String KEY_TIME = "time";
	private static final String KEY_LOCATION = "location";
	private static final String KEY_WORLD = "world";
	private static final String KEY_INSTANCE = "instance";
	private static final String KEY_POSE = "pose";
	private static final String KEY_EQUIPMENT = "equipment";
	private static final String KEY_ITEMS = "items";
	private static final String KEY_SMALL = "small";
	private static final String KEY_EQUIPMENT_HEAD = "head";
	private static final String KEY_EQUIPMENT_BODY = "body";
	private static final String KEY_EQUIPMENT_LEGS = "legs";
	private static final String KEY_EQUIPMENT_FEET = "feet";
	private static final String KEY_EQUIPMENT_HAND = "hand";
	private static final String KEY_EQUIPMENT_OFF_HAND = "off_hand";
	private static final String KEY_POSE_HEAD = "head";
	private static final String KEY_POSE_BODY = "body";
	private static final String KEY_POSE_LEFT_ARM = "left_arm";
	private static final String KEY_POSE_RIGHT_ARM = "right_arm";
	private static final String KEY_POSE_LEFT_LEG = "left_leg";
	private static final String KEY_POSE_RIGHT_LEG = "right_leg";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_Z = "z";
	private static final String KEY_YAW = "yaw";
	private static final String KEY_PITCH = "pitch";
	private static final List<String> KEYS_POSE_PARTS = Arrays.asList(KEY_POSE_HEAD, KEY_POSE_BODY, KEY_POSE_LEFT_ARM, KEY_POSE_RIGHT_ARM, KEY_POSE_LEFT_LEG, KEY_POSE_RIGHT_LEG);
	private static final List<String> KEYS_EQUIPMENT_PARTS = Arrays.asList(KEY_EQUIPMENT_HEAD, KEY_EQUIPMENT_BODY, KEY_EQUIPMENT_LEGS, KEY_EQUIPMENT_FEET, KEY_EQUIPMENT_HAND, KEY_EQUIPMENT_OFF_HAND);

	private BukkitRunnable mRunnable = null;
	GraveManager mManager;
	Player mPlayer;
	private final Instant mDeathTime;
	private final boolean mSmall;
	String mWorldName;
	Integer mDungeonInstance;
	Location mLocation;
	private @Nullable HashMap<String, EulerAngle> mPose;
	private final HashMap<String, @Nullable ItemStack> mEquipment;
	HashSet<GraveItem> mItems;
	private @Nullable ArmorStand mEntity;

	boolean mAlertedSpawned = false;
	boolean mAlertedLimbo = false;
	boolean mAlertedShatter = false;
	boolean mAlertedLost = false;

	// For deserializing a grave from data
	private Grave(GraveManager manager, Player player, String world, Integer instance,
	              Instant time, boolean small, Location location, @Nullable HashMap<String, EulerAngle> pose,
	              HashMap<String, @Nullable ItemStack> equipment, JsonArray items) {
		mManager = manager;
		mPlayer = player;
		mDeathTime = time;
		mSmall = small;
		mWorldName = world;
		mDungeonInstance = instance;
		mLocation = location;
		mPose = pose;
		mEquipment = equipment;
		mItems = new HashSet<>();
		for (JsonElement itemData : items) {
			JsonObject data = itemData.getAsJsonObject();
			GraveItem item = GraveItem.deserialize(mManager, this, mPlayer, data);
			if (item != null) {
				mItems.add(item);
			}
		}
		if (isInThisWorld() && !isEmpty()) {
			updateInstance();
			mManager.addUnloadedGrave(Chunk.getChunkKey(mLocation), this);
		}
	}

	// For spawning a new grave on death
	public Grave(GraveManager manager, Player player, ArrayList<ItemStack> droppedItems, HashMap<EquipmentSlot, @Nullable ItemStack> equipment) {
		mManager = manager;
		mPlayer = player;
		mDeathTime = Instant.now();
		mSmall = false;
		mWorldName = mPlayer.getWorld().getName();
		mDungeonInstance = null;
		if (ScoreboardUtils.getScoreboardValue("$Shard", "const").orElse(0) > 0) {
			mDungeonInstance = ScoreboardUtils.getScoreboardValue(mPlayer, "DAccess").orElse(0);
		}
		mLocation = mPlayer.getLocation().clone();
		if (mLocation.getY() < 0) {
			mLocation.setY(0);
		}
		mEquipment = new HashMap<>();
		mEquipment.put(KEY_EQUIPMENT_HEAD, equipment.get(EquipmentSlot.HEAD));
		mEquipment.put(KEY_EQUIPMENT_BODY, equipment.get(EquipmentSlot.CHEST));
		mEquipment.put(KEY_EQUIPMENT_LEGS, equipment.get(EquipmentSlot.LEGS));
		mEquipment.put(KEY_EQUIPMENT_FEET, equipment.get(EquipmentSlot.FEET));
		mEquipment.put(KEY_EQUIPMENT_HAND, equipment.get(EquipmentSlot.HAND));
		mEquipment.put(KEY_EQUIPMENT_OFF_HAND, equipment.get(EquipmentSlot.OFF_HAND));
		generateNewPose();
		mItems = new HashSet<>();
		for (ItemStack item : droppedItems) {
			mItems.add(new GraveItem(this, item));
		}
		if (!mAlertedSpawned) {
			mAlertedSpawned = true;
			mPlayer.sendMessage(Component.text("You died and dropped items! Your grave will keep them safe; be careful on the way back! (/deathhelp for more info)", NamedTextColor.RED));
		}
		spawn();
	}

	// For spawning a single-item grave from a dropped item.
	public Grave(ThrownItem item) {
		mManager = item.mManager;
		mPlayer = item.mPlayer;
		mDeathTime = Instant.now();
		mWorldName = item.mWorldName;
		mDungeonInstance = item.mDungeonInstance;
		mLocation = item.mLocation.clone();
		if (mLocation.getY() < 0) {
			mLocation.setY(0);
		}
		mEquipment = new HashMap<>();
		mEquipment.put(KEY_EQUIPMENT_HAND, item.mItem);
		generateNewPose();
		setPoseDegrees(KEY_POSE_LEFT_ARM, 260d, 45d, 0d);
		setPoseDegrees(KEY_POSE_RIGHT_ARM, 270d, 330d, 0d);
		mSmall = true;
		mItems = new HashSet<>();
		mItems.add(new GraveItem(this, item));
		item.delete();
		if (!mAlertedSpawned) {
			mAlertedSpawned = true;
			mPlayer.sendMessage(Component.text("An item you dropped at ", NamedTextColor.RED)
				.append(Component.text(mLocation.getBlockX() + "," + mLocation.getBlockY() + "," + mLocation.getBlockZ()))
				.append(Component.text(" was destroyed. A grave will keep it safe for you. (/deathhelp for more info)"))
			);
		}
		spawn();
	}

	@Nullable UUID getUniqueId() {
		if (mEntity == null || !mEntity.isValid()) {
			//TODO Handle error
			return null;
		}
		return mEntity.getUniqueId();
	}

	Location getLocation() {
		if (mEntity != null && mEntity.isValid()) {
			return mEntity.getLocation().clone();
		}
		return mLocation.clone();
	}

	void removeItem(GraveItem item) {
		mItems.remove(item);
		if (isEmpty()) {
			delete();
			Phylactery.giveStoredXP(mPlayer);
		}
	}

	boolean isEmpty() {
		return mItems.isEmpty();
	}

	private boolean isInThisWorld() {
		return mPlayer.getWorld().getName().equals(mWorldName);
	}

	private boolean canSpawn() {
		World world = mPlayer.getWorld();
		if ((mEntity == null || !mEntity.isValid()) && isInThisWorld()) {
			mLocation.setWorld(world);
			return mLocation.isChunkLoaded();
		}
		return false;
	}

	private void spawn() {
		if (canSpawn()) {
			mEntity = (ArmorStand) mPlayer.getWorld().spawnEntity(mLocation, EntityType.ARMOR_STAND);
			mEntity.setSmall(mSmall);
			mEntity.setArms(true);
			mEntity.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
			mEntity.setHeadPose(mPose.get(KEY_POSE_HEAD));
			mEntity.setBodyPose(mPose.get(KEY_POSE_BODY));
			mEntity.setLeftArmPose(mPose.get(KEY_POSE_LEFT_ARM));
			mEntity.setRightArmPose(mPose.get(KEY_POSE_RIGHT_ARM));
			mEntity.setLeftLegPose(mPose.get(KEY_POSE_LEFT_LEG));
			mEntity.setRightLegPose(mPose.get(KEY_POSE_RIGHT_LEG));
			mEntity.setItem(EquipmentSlot.HEAD, mEquipment.get(KEY_EQUIPMENT_HEAD));
			mEntity.setItem(EquipmentSlot.CHEST, mEquipment.get(KEY_EQUIPMENT_BODY));
			mEntity.setItem(EquipmentSlot.LEGS, mEquipment.get(KEY_EQUIPMENT_LEGS));
			mEntity.setItem(EquipmentSlot.FEET, mEquipment.get(KEY_EQUIPMENT_FEET));
			mEntity.setItem(EquipmentSlot.HAND, mEquipment.get(KEY_EQUIPMENT_HAND));
			mEntity.setItem(EquipmentSlot.OFF_HAND, mEquipment.get(KEY_EQUIPMENT_OFF_HAND));
			mEntity.setGravity(false);
			mEntity.setCanMove(false);
			mEntity.setSilent(true);
			mEntity.setCustomName(mPlayer.getName() + (mPlayer.getName().endsWith("s") ? "' Grave" : "'s Grave"));
			mEntity.setCustomNameVisible(!mSmall);
			mEntity.addScoreboardTag("Grave");
			mManager.addGrave(mEntity.getUniqueId(), this);
			mManager.removeUnloadedGrave(Chunk.getChunkKey(mLocation), this);
			startTracking();
			if (!mAlertedSpawned) {
				mAlertedSpawned = true;
				mPlayer.sendMessage(Component.text("You have a grave at ", NamedTextColor.RED)
					.append(Component.text(mLocation.getBlockX() + "," + mLocation.getBlockY() + "," + mLocation.getBlockZ()))
					.append(Component.text(mItems.size() == 1 ? " with 1 item." : " with " + mItems.size() + " items."))
					.append(Component.text(" (/deathhelp for more info)"))
				);
			}
		}
	}

	private void remove() {
		if (mEntity != null) {
			update();
			stopTracking();
			mManager.removeGrave(mEntity.getUniqueId());
			mEntity.remove();
			mEntity = null;
		}
	}

	private void startTracking() {
		if (mRunnable == null) {
			mRunnable = new BukkitRunnable() {
				int mGlowingSeconds = 0;
				@Override
				public void run() {
					if (mEntity != null && mEntity.isValid()) {
						if (mPlayer.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
							mEntity.setGlowing(true);
							mGlowingSeconds = 5;
						} else if (mGlowingSeconds > 0) {
							mGlowingSeconds--;
						} else {
							mEntity.setGlowing(false);
						}
						if (mEntity.getScoreboardTags().contains("RespawnItems")) {
							mEntity.removeScoreboardTag("RespawnItems");
							for (GraveItem item : mItems) {
								if (item.mStatus == GraveItem.Status.DROPPED) {
									item.respawn();
								}
							}
						}
						if (mEntity.getScoreboardTags().contains("Delete")) {
							delete();
						}
					} else {
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

	private int collectItem(Player player, GraveItem item) {
		int slot = player.getInventory().firstEmpty();
		if (slot != -1) {
			Item entity = player.getWorld().dropItem(getLocation(), item.getItem());
			int remaining = Plugin.getInstance().mDeathItemListener.pickupItem(player, entity);
			item.collect(remaining);
			return remaining;
		}
		return item.mItem.getAmount();
	}

	void summon(Location location) {
		remove();
		mLocation = location;
		mWorldName = location.getWorld().getName();
		spawn();
		for (GraveItem item : mItems) {
			if (item.mStatus == GraveItem.Status.DROPPED) {
				item.respawn();
			}
		}
	}

	void delete() {
		Iterator<GraveItem> items = mItems.iterator();
		while (items.hasNext()) {
			GraveItem item = items.next();
			item.delete();
			items.remove();
		}
		remove();
		mManager.removeUnloadedGrave(Chunk.getChunkKey(mLocation), this);
	}

	void onLogin() {
		spawn();
		for (GraveItem item : mItems) {
			item.onLogin();
		}
	}

	void onLogout() {
		remove();
		mManager.removeUnloadedGrave(Chunk.getChunkKey(mLocation), this);
		for (GraveItem item : mItems) {
			item.onLogout();
		}
	}

	void onSave() {
		for (GraveItem item : mItems) {
			item.onSave();
		}
		update();
	}

	void onChunkLoad() {
		spawn();
	}

	void onChunkUnload() {
		remove();
		mManager.addUnloadedGrave(Chunk.getChunkKey(mLocation), this);
	}

	void onDeath() {
		for (GraveItem item : mItems) {
			item.onDeath();
		}
	}

	void onInteract(Player player) {
		int lost = 0;
		int dropped = 0;
		int saved = 0;
		int collected = 0;
		int collectedShattered = 0;
		int remaining = 0;
		int remainingShattered = 0;
		if (mManager.hasPermission(player)) {
			// Clicked by owner or trusted player
			if (!mManager.isOwner(player)) {
				player.sendMessage(Component.text("This grave belongs to ", NamedTextColor.AQUA)
					.append(mPlayer.displayName().hoverEvent(mPlayer))
					.append(Component.text("; they have given you permission to collect their items."))
				);
			}
			Iterator<GraveItem> items = mItems.iterator();
			while (items.hasNext()) {
				GraveItem item = items.next();
				if (item.mStatus == GraveItem.Status.COLLECTED) {
					// Item was previously collected, remove it from the list
					items.remove();
				}
				if (item.mStatus == GraveItem.Status.LOST) {
					// Item that can't shatter was destroyed
					lost++;
					items.remove();
				}
				if (item.mStatus == GraveItem.Status.DROPPED) {
					dropped++;
					item.glow();
				}
				if (item.mStatus == GraveItem.Status.LIMBO) {
					item.save();
				}
				if (item.mStatus == GraveItem.Status.SAFE) {
					int amountBefore = item.mItem.getAmount();
					int amountRemaining = collectItem(player, item);
					if (amountRemaining == 0) {
						items.remove();
						collected++;
					} else {
						if (amountRemaining != amountBefore) {
							collected++;
						}
						remaining++;
					}
				}
				if (item.mStatus == GraveItem.Status.SHATTERED) {
					int amountBefore = item.mItem.getAmount();
					int amountRemaining = collectItem(player, item);
					if (amountRemaining == 0) {
						items.remove();
						collected++;
						collectedShattered++;
					} else {
						if (amountRemaining != amountBefore) {
							collected++;
							collectedShattered++;
						}
						remaining++;
						remainingShattered++;
					}
				}
			}
			if (collected > 0) {
				player.sendMessage(Component.text("You collected ", NamedTextColor.AQUA)
					.append(Component.text(collected == 1 ? "1 item from the grave" : collected + " items from the grave"))
					.append(Component.text(collectedShattered == 0 ? "." : "; "))
					.append(Component.text(collectedShattered == 1 ? (collected == 1 ? "it was shattered." : "1 of them was shattered.") : ""))
					.append(Component.text(collectedShattered > 1 ? (collectedShattered == collected ? "they were all shattered." : collectedShattered + " of them were shattered.") : ""))
				);
			}
			if (remaining > 0) {
				player.sendMessage(Component.text("There ", NamedTextColor.AQUA)
					.append(Component.text(remaining == 1 ? "is 1 item remaining in the grave" : "are " + remaining + " items remaining in the grave"))
					.append(Component.text(remainingShattered == 0 ? "." : "; "))
					.append(Component.text(remainingShattered == 1 ? (remaining == 1 ? "it is shattered." : "1 of them is shattered.") : ""))
					.append(Component.text(remainingShattered > 1 ? (remainingShattered == remaining ? "they were all shattered." : remainingShattered + " of them were shattered.") : ""))
				);
			}
			if (dropped > 0) {
				player.sendMessage(Component.text("There ", NamedTextColor.AQUA)
					.append(Component.text(dropped == 1 ? "is 1 item left on the ground; it will now glow." : "are " + dropped + " items left on the ground; they will now glow"))
				);
			}
			if (lost > 0) {
				player.sendMessage(Component.text("There ", NamedTextColor.AQUA)
					.append(Component.text(lost == 1 ? "was 1 item lost due to dying in limbo." : "were " + lost + " items lost due to dying in limbo."))
				);
			}
		} else {
			// Clicked by non-owner
			for (GraveItem item : mItems) {
				if (item.mStatus == GraveItem.Status.DROPPED) {
					dropped++;
					item.glow();
				}
				if (item.mStatus == GraveItem.Status.LIMBO) {
					saved++;
					item.save();
				}
			}
			player.sendMessage(Component.text("This grave belongs to ", NamedTextColor.AQUA)
				.append(mPlayer.displayName().hoverEvent(mPlayer))
				.append(Component.text("."))
			);
			mPlayer.sendMessage(Component.text("Your grave at ", NamedTextColor.AQUA)
				.append(Component.text((int) mLocation.getX() + ", " + (int) mLocation.getY() + ", " + (int) mLocation.getZ(), NamedTextColor.AQUA))
				.append(Component.text(" has been located by another player!", NamedTextColor.AQUA))
			);
			if (dropped > 0) {
				player.sendMessage(Component.text("There ", NamedTextColor.AQUA)
					.append(Component.text(dropped == 1 ? "is 1 item" : "are " + dropped + " items"))
					.append(Component.text(" left on the ground; "))
					.append(Component.text(dropped == 1 ? "it will now glow." : "they will now glow."))
				);
			}
			if (saved > 0) {
				player.sendMessage(Component.text("Thank you for saving ", NamedTextColor.AQUA)
					.append(Component.text(saved == 1 ? "an item that was in limbo!" : saved + " items that were in limbo!"))
				);
				mPlayer.sendMessage(Component.text().color(NamedTextColor.AQUA)
					.append(player.displayName().hoverEvent(player))
					.append(Component.text(" saved the "))
					.append(Component.text(saved == 1 ? "limbo item in your grave!" : saved + " limbo items in your grave!"))
				);
			}
		}
		if (isEmpty()) {
			player.sendMessage(Component.text("Goodbye!", NamedTextColor.AQUA));
			delete();
			Phylactery.giveStoredXP(mPlayer);
		}
	}

	private void generateNewPose() {
		mPose = new HashMap<String, EulerAngle>() {{
			put(KEY_POSE_HEAD, new EulerAngle(0, 0, 0));
			put(KEY_POSE_BODY, new EulerAngle(0, 0, 0));
			put(KEY_POSE_LEFT_ARM, new EulerAngle(0, 0, 0));
			put(KEY_POSE_RIGHT_ARM, new EulerAngle(0, 0, 0));
			put(KEY_POSE_LEFT_LEG, new EulerAngle(0, 0, 0));
			put(KEY_POSE_RIGHT_LEG, new EulerAngle(0, 0, 0));
		}};
		//TODO Actually generate a randomized pose
	}

	private void setPoseRadians(String key, Double x, Double y, Double z) {
		EulerAngle old = mPose.getOrDefault(key, EulerAngle.ZERO);
		mPose.put(key, new EulerAngle(x == null ? old.getX() : x, y == null ? old.getY() : y, z == null ? old.getZ() : z));
	}

	private void setPoseDegrees(String key, Double x, Double y, Double z) {
		EulerAngle old = mPose.getOrDefault(key, EulerAngle.ZERO);
		x = x == null ? old.getX() : Math.toRadians(x);
		y = y == null ? old.getY() : Math.toRadians(y);
		z = z == null ? old.getZ() : Math.toRadians(z);
		setPoseRadians(key, x, y, z);
	}

	private void update() {
		if (mEntity != null && mEntity.isValid()) {
			mPose.put(KEY_POSE_HEAD, mEntity.getHeadPose());
			mPose.put(KEY_POSE_BODY, mEntity.getBodyPose());
			mPose.put(KEY_POSE_LEFT_ARM, mEntity.getLeftArmPose());
			mPose.put(KEY_POSE_RIGHT_ARM, mEntity.getRightArmPose());
			mPose.put(KEY_POSE_LEFT_LEG, mEntity.getLeftLegPose());
			mPose.put(KEY_POSE_RIGHT_LEG, mEntity.getRightLegPose());
			mLocation = mEntity.getLocation().clone();
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

	static Grave deserialize(GraveManager manager, Player player, JsonObject data) {
		String world = null;
		Integer instance = null;
		Instant time = null;
		boolean small = false;
		Location location = null;
		JsonArray items = new JsonArray();
		HashMap<String, EulerAngle> pose = new HashMap<String, EulerAngle>() {{
			for (String key : KEYS_POSE_PARTS) {
				put(key, new EulerAngle(0, 0, 0));
			}
		}};
		HashMap<String, ItemStack> equipment = new HashMap<String, ItemStack>() {{
			for (String key : KEYS_EQUIPMENT_PARTS) {
				put(key, null);
			}
		}};
		if (data.has(KEY_WORLD) && data.get(KEY_WORLD).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_WORLD).isString()) {
			world = data.getAsJsonPrimitive(KEY_WORLD).getAsString();
		}
		if (data.has(KEY_INSTANCE) && data.getAsJsonPrimitive(KEY_INSTANCE).isNumber()) {
			instance = data.getAsJsonPrimitive(KEY_INSTANCE).getAsInt();
		}
		if (data.has(KEY_TIME) && data.getAsJsonPrimitive(KEY_TIME).isNumber()) {
			time = Instant.ofEpochMilli(data.getAsJsonPrimitive(KEY_TIME).getAsInt());
		}
		if (data.has(KEY_SMALL) && data.get(KEY_SMALL).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_SMALL).isBoolean()) {
			small = data.getAsJsonPrimitive(KEY_SMALL).getAsBoolean();
		}
		if (data.has(KEY_LOCATION) && data.get(KEY_LOCATION).isJsonObject()) {
			JsonObject loc = data.getAsJsonObject(KEY_LOCATION);
			double x = loc.get(KEY_X).getAsDouble();
			double y = loc.get(KEY_Y).getAsDouble();
			double z = loc.get(KEY_Z).getAsDouble();
			float yaw = loc.get(KEY_YAW).getAsFloat();
			float pitch = loc.get(KEY_PITCH).getAsFloat();
			location = new Location(null, x, y, z, yaw, pitch);
		}
		if (data.has(KEY_POSE) && data.get(KEY_POSE).isJsonObject()) {
			JsonObject poseData = data.getAsJsonObject(KEY_POSE);
			for (String key : KEYS_POSE_PARTS) {
				if (poseData.has(key) && poseData.get(key).isJsonObject()) {
					JsonObject obj = poseData.getAsJsonObject(key);
					if (obj.has(KEY_X) && obj.get(KEY_X).isJsonPrimitive() && obj.getAsJsonPrimitive(KEY_X).isNumber()) {
						pose.get(key).setX(obj.getAsJsonPrimitive(KEY_X).getAsDouble());
					}
					if (obj.has(KEY_Y) && obj.get(KEY_Y).isJsonPrimitive() && obj.getAsJsonPrimitive(KEY_Y).isNumber()) {
						pose.get(key).setY(obj.getAsJsonPrimitive(KEY_Y).getAsDouble());
					}
					if (obj.has(KEY_Z) && obj.get(KEY_Z).isJsonPrimitive() && obj.getAsJsonPrimitive(KEY_Z).isNumber()) {
						pose.get(key).setZ(obj.getAsJsonPrimitive(KEY_Z).getAsDouble());
					}
				}
			}
		}
		if (data.has(KEY_EQUIPMENT) && data.get(KEY_EQUIPMENT).isJsonObject()) {
			JsonObject equipmentData = data.getAsJsonObject(KEY_EQUIPMENT);
			for (String key : KEYS_EQUIPMENT_PARTS) {
				if (equipmentData.has(key) && equipmentData.get(key).isJsonPrimitive() && equipmentData.getAsJsonPrimitive(key).isString()) {
					equipment.put(key, NBTItem.convertNBTtoItem(new NBTContainer(equipmentData.getAsJsonPrimitive(key).getAsString())));
				}
			}
		}
		if (data.has(KEY_ITEMS) && data.get(KEY_ITEMS).isJsonArray()) {
			items.addAll(data.getAsJsonArray(KEY_ITEMS));
		}
		return new Grave(manager, player, world, instance, time, small, location, pose, equipment, items);
	}

	public @Nullable JsonObject serialize() {
		JsonObject data = new JsonObject();

		data.addProperty(KEY_WORLD, mWorldName);
		data.addProperty(KEY_INSTANCE, mDungeonInstance);
		data.addProperty(KEY_SMALL, mSmall);

		if (mDeathTime != null) {
			data.addProperty(KEY_TIME, mDeathTime.toEpochMilli());
		}

		if (mLocation != null) {
			JsonObject location = new JsonObject();
			location.addProperty(KEY_X, mLocation.getX());
			location.addProperty(KEY_Y, mLocation.getY());
			location.addProperty(KEY_Z, mLocation.getZ());
			location.addProperty(KEY_YAW, mLocation.getYaw());
			location.addProperty(KEY_PITCH, mLocation.getPitch());
			data.add(KEY_LOCATION, location);
		}

		if (mPose != null && !mPose.isEmpty()) {
			JsonObject pose = new JsonObject();
			for (String key : KEYS_POSE_PARTS) {
				JsonObject part = new JsonObject();
				pose.add(key, part);
				EulerAngle angle = mPose.getOrDefault(key, EulerAngle.ZERO);
				part.addProperty(KEY_X, angle.getX());
				part.addProperty(KEY_Y, angle.getY());
				part.addProperty(KEY_Z, angle.getZ());
			}
			data.add(KEY_POSE, pose);
		}

		if (mEquipment != null && !mEquipment.isEmpty()) {
			JsonObject equipment = new JsonObject();
			for (String key : KEYS_EQUIPMENT_PARTS) {
				ItemStack item = mEquipment.get(key);
				equipment.addProperty(key, item == null || item.getType() == Material.AIR ? null : NBTItem.convertItemtoNBT(item).toString());
			}
			data.add(KEY_EQUIPMENT, equipment);
		}

		if (mItems != null && !mItems.isEmpty()) {
			JsonArray items = new JsonArray();
			for (GraveItem item : mItems) {
				JsonObject itemData = item.serialize();
				if (itemData != null) {
					items.add(itemData);
				}
			}
			if (items.size() == 0) {
				return null;
			}
			data.add(KEY_ITEMS, items);
		}

		return data;
	}

}
