package com.playmonumenta.plugins.graves;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

public final class Grave {
	private static final String KEY_TIME = "time";
	private static final String KEY_LOCATION = "location";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_SHARD = "shard";
	private static final String KEY_POSE = "pose";
	private static final String KEY_EQUIPMENT = "equipment";
	private static final String KEY_ITEMS = "items";
	private static final String KEY_GHOST = "ghost";
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

	private @Nullable BukkitRunnable mRunnable = null;
	GraveManager mManager;
	Player mPlayer;
	public final UUID mUuid;
	private final Instant mDeathTime;
	private final boolean mSmall;
	String mShardName;
	Location mLocation;
	private final HashMap<String, EulerAngle> mPose;
	private final HashMap<String, ItemStack> mEquipment;
	HashSet<GraveItem> mItems;
	private @Nullable ArmorStand mEntity;
	private final HashSet<UUID> mGraveMessageCooldown = new HashSet<>();
	private boolean mLoggedOut = false;
	boolean mAlertedSpawned = false;
	/**
	 * Whether this is a grave that holds no items and instead gives one free unshatter to equipped items if collected.
	 */
	final boolean mGhostGrave;

	// For deserializing a grave from data
	private Grave(GraveManager manager, Player player, UUID uuid, String shard,
	              Instant time, boolean small, Location location, boolean isGhost, HashMap<String, EulerAngle> pose,
	              HashMap<String, ItemStack> equipment, JsonArray items) {
		mManager = manager;
		mPlayer = player;
		mUuid = uuid;
		mDeathTime = time;
		mSmall = small;
		mShardName = shard;
		mLocation = location;
		mPose = pose;
		mGhostGrave = isGhost;
		mEquipment = equipment;
		mItems = new HashSet<>();
		for (JsonElement itemData : items) {
			JsonObject data = itemData.getAsJsonObject();
			GraveItem item = GraveItem.deserialize(data);
			mItems.add(item);
		}
		if (isOnThisShard() && !isEmpty()) {
			mManager.addUnloadedGrave(Chunk.getChunkKey(mLocation), this);
		}
	}

	// For spawning a new grave on death
	public Grave(GraveManager manager, Player player, HashMap<EquipmentSlot, ItemStack> equipment) {
		mManager = manager;
		mPlayer = player;
		mUuid = UUID.randomUUID();
		mDeathTime = Instant.now();
		mSmall = false;
		mShardName = ServerProperties.getShardName();
		mLocation = mPlayer.getLocation().clone();
		if (mLocation.getY() < 1) {
			mLocation.setY(1);
		}
		mEquipment = new HashMap<>();
		mEquipment.put(KEY_EQUIPMENT_HEAD, equipment.get(EquipmentSlot.HEAD));
		mEquipment.put(KEY_EQUIPMENT_BODY, equipment.get(EquipmentSlot.CHEST));
		mEquipment.put(KEY_EQUIPMENT_LEGS, equipment.get(EquipmentSlot.LEGS));
		mEquipment.put(KEY_EQUIPMENT_FEET, equipment.get(EquipmentSlot.FEET));
		mEquipment.put(KEY_EQUIPMENT_HAND, equipment.get(EquipmentSlot.HAND));
		mEquipment.put(KEY_EQUIPMENT_OFF_HAND, equipment.get(EquipmentSlot.OFF_HAND));
		mPose = new HashMap<>();
		generateNewPose();
		mItems = new HashSet<>();
		mGhostGrave = true;
		mAlertedSpawned = true;
		spawn();
	}

	// For spawning a single-item grave from a dropped item.
	public Grave(ThrownItem item) {
		mManager = item.mManager;
		mPlayer = item.mPlayer;
		mUuid = UUID.randomUUID();
		mDeathTime = Instant.now();
		mShardName = item.mShardName;
		mLocation = item.mLocation.clone();
		if (mLocation.getY() < 1) {
			mLocation.setY(1);
		}
		mEquipment = new HashMap<>();
		mEquipment.put(KEY_EQUIPMENT_HAND, item.mItem);
		mPose = new HashMap<>();
		generateNewPose();
		setPoseDegrees(KEY_POSE_LEFT_ARM, 260d, 45d, 0d);
		setPoseDegrees(KEY_POSE_RIGHT_ARM, 270d, 330d, 0d);
		mSmall = true;
		mItems = new HashSet<>();
		mItems.add(new GraveItem(item));
		item.delete();
		mGhostGrave = false;
		mAlertedSpawned = true;
		spawn();
	}

	Location getLocation() {
		if (mEntity != null && mEntity.isValid()) {
			return mEntity.getLocation().clone();
		}
		return mLocation.clone();
	}

	public Instant getDeathTime() {
		return mDeathTime;
	}

	public Collection<GraveItem> getItems() {
		return mItems;
	}

	boolean isEmpty() {
		return mItems.isEmpty() && !mGhostGrave;
	}

	private boolean isOnThisShard() {
		return ServerProperties.getShardName().equals(mShardName);
	}

	private boolean canSpawn() {
		World world = mPlayer.getWorld();
		if (!mLoggedOut && (mEntity == null || !mEntity.isValid()) && isOnThisShard()) {
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
			mEntity.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
			mEntity.setGravity(false);
			mEntity.setCanMove(false);
			mEntity.setSilent(true);
			mEntity.customName(Component.text(mPlayer.getName() + (mPlayer.getName().endsWith("s") ? "' Grave" : "'s Grave")));
			mEntity.setCustomNameVisible(!mSmall);
			mEntity.addScoreboardTag("Grave");
			mManager.addGrave(mEntity, this);
			mManager.removeUnloadedGrave(Chunk.getChunkKey(mLocation), this);
			startTracking();
			if (!mAlertedSpawned) {
				mAlertedSpawned = true;
				Component message = Component.text("You have a grave at ", NamedTextColor.AQUA)
					                    .append(Component.text(mLocation.getBlockX() + "," + mLocation.getBlockY() + "," + mLocation.getBlockZ()));
				if (!mGhostGrave) {
					message = message.append(Component.text(" with "))
						          .append(Component.text(mItems.size() == 1 ? "1 item." : mItems.size() + " items.")
							                  .hoverEvent(HoverEvent.showText(getItemList(false))));
				}
				message = message.append(Component.text(" (/help death for more info)")
					                         .clickEvent(ClickEvent.runCommand("/help death")))
					          .append(Component.text(" "))
					          .append(Component.text("Click to delete this grave permanently.", NamedTextColor.RED)
						                  .hoverEvent(HoverEvent.showText(Component.text("Delete grave", NamedTextColor.RED)))
						                  .clickEvent(ClickEvent.runCommand("/grave delete " + mUuid)));
				mPlayer.sendMessage(message);
			}
		}
	}

	private void remove() {
		if (mEntity != null) {
			update();
			stopTracking();
			mManager.removeGrave(mEntity);
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

						if (ScoreboardUtils.getScoreboardValue(mPlayer, Phylactery.GRAVE_XP_SCOREBOARD).orElse(0) > 0) {
							doPhylacteryParticles();
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

	private void doPhylacteryParticles() {
		if (mEntity == null) {
			return;
		}
		Location loc = mEntity.getLocation();
		new BukkitRunnable() {
			int mTicks = 0;
			double mY = 0;
			double mTheta = 0;
			@Override
			public void run() {
				mPlayer.spawnParticle(Particle.TOTEM, loc.clone().add(FastUtils.cos(mTheta), mY, FastUtils.sin(mTheta)), 1, 0, 0, 0, 0);
				mPlayer.spawnParticle(Particle.TOTEM, loc.clone().add(-FastUtils.cos(mTheta), mY, -FastUtils.sin(mTheta)), 1, 0, 0, 0, 0);

				mTicks += 2;
				mY += 0.2;
				mTheta += Math.PI / 5;

				if (mTicks >= 20) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	private void stopTracking() {
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}

	int collectItem(Player player, GraveItem item) {
		HashMap<Integer, ItemStack> remainingMap = player.getInventory().addItem(item.mItem);
		int remaining = 0;
		if (remainingMap.containsKey(0)) {
			remaining = remainingMap.get(0).getAmount();
		}
		item.collect(remaining);
		return remaining;
	}

	public void summon(Location location) {
		remove();
		mLocation = location;
		mShardName = ServerProperties.getShardName();
		spawn();
	}

	public void delete() {
		mItems.clear();
		mEquipment.clear();
		remove();
		mManager.removeGrave(this);
	}

	void onLogin() {
		spawn();
	}

	void onLogout() {
		mLoggedOut = true;
		remove();
		mManager.removeUnloadedGrave(Chunk.getChunkKey(mLocation), this);
	}

	void onSave() {
		update();
	}

	void onChunkLoad() {
		spawn();
	}

	void onChunkUnload() {
		remove();
		mManager.addUnloadedGrave(Chunk.getChunkKey(mLocation), this);
	}

	void onInteract(Player player) {
		int collected = 0;
		int remaining = 0;
		if (mManager.isOwner(player)) {
			if (mEntity != null && mEntity.getTicksLived() < 5) {
				// don't allow interacting with a grave just after it has spawned (riptide for example does this)
				return;
			}
			// Clicked by owner
			Iterator<GraveItem> items = mItems.iterator();
			while (items.hasNext()) {
				GraveItem item = items.next();
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

			Map<UUID, ItemStatManager.PlayerItemStats> itemStatsMap = Plugin.getInstance().mItemStatManager.getPlayerItemStatsMappings();
			if (itemStatsMap.containsKey(player.getUniqueId())) {
				itemStatsMap.get(player.getUniqueId()).updateStats(player, true, false);
			}

			if (collected > 0) {
				player.sendMessage(Component.text("You collected ", NamedTextColor.AQUA)
					.append(Component.text(collected == 1 ? "1 item from the grave" : collected + " items from the grave."))
				);
			}
			if (remaining > 0) {
				player.sendMessage(Component.text("There ", NamedTextColor.AQUA)
					.append(Component.text(remaining == 1 ? "is 1 item remaining in the grave" : "are " + remaining + " items remaining in the grave."))
				);
			}
			if (mItems.isEmpty()) {
				if (mGhostGrave) {
					// Remove one level of shattering from the player's items
					int unshattered = 0;
					boolean hasEnderChest = Arrays.stream(player.getInventory().getContents()).anyMatch(
						item -> item != null && ItemUtils.isShulkerBox(item.getType()) && "Remnant of the Rose".equals(ItemUtils.getPlainNameIfExists(item)));
					equipmentLoop:
					for (ItemStack graveEquipment : mEquipment.values()) {
						if (ItemStatUtils.getInfusionLevel(graveEquipment, ItemStatUtils.InfusionType.SHATTERED) >= Shattered.MAX_LEVEL) {
							continue;
						}
						ItemUtils.ItemIdentifier identifier = ItemUtils.getIdentifier(graveEquipment, true);
						for (Inventory inv : hasEnderChest ? new Inventory[] {player.getInventory(), player.getEnderChest()} : new Inventory[] {player.getInventory()}) {
							ItemStack[] contents = inv.getContents();
							for (int i = contents.length - 1; i >= 0; i--) { // loop from high to low to check equipment first
								ItemStack playerItem = contents[i];
								if (identifier.isIdentifierFor(playerItem, true)
									    && Shattered.unshatterOneLevel(playerItem)) {
									unshattered++;
									continue equipmentLoop;
								}
							}
							// if the item is not in the player's inventory, try Shulker boxes in the inventory (e.g. Loadout Lockboxes)
							for (ItemStack item : contents) {
								if (item != null
									    && ItemUtils.isShulkerBox(item.getType())
									    && item.getItemMeta() instanceof BlockStateMeta meta
									    && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
									for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
										if (identifier.isIdentifierFor(shulkerItem, true)
											    && Shattered.unshatterOneLevel(shulkerItem)) {
											unshattered++;
											meta.setBlockState(shulkerBox);
											item.setItemMeta(meta);
											continue equipmentLoop;
										}
									}
								}
							}
						}
					}
					if (unshattered == 0) {
						player.sendMessage(Component.text("You do not have any items on you that this grave could unshatter. ", NamedTextColor.AQUA)
							                   .append(Component.text("Click here to delete this grave.", NamedTextColor.RED)
								                           .hoverEvent(HoverEvent.showText(Component.text("Delete grave", NamedTextColor.RED)))
								                           .clickEvent(ClickEvent.runCommand("/grave delete " + mUuid))));
						Phylactery.giveStoredXP(mPlayer);
						return; // don't delete
					}
					player.sendMessage(Component.text("Grave collected! Repaired " + unshattered + " item" + (unshattered == 1 ? "" : "s") + ".", NamedTextColor.AQUA));
					Plugin.getInstance().mItemStatManager.updateStats(player);
					Phylactery.giveStoredXP(mPlayer);
				} else {
					player.sendMessage(Component.text("Goodbye!", NamedTextColor.AQUA));
				}
				delete();
			}
		} else {
			// Clicked by non-owner
			player.sendMessage(Component.text("This grave belongs to ", NamedTextColor.AQUA)
				                   .append(mPlayer.displayName().hoverEvent(mPlayer))
				                   .append(Component.text("."))
			);
			if (!mGraveMessageCooldown.contains(mUuid)) {
				Component message = Component.text("Your grave at ", NamedTextColor.AQUA)
					                    .append(Component.text(mLocation.getBlockX() + "," + mLocation.getBlockY() + "," + mLocation.getBlockZ(), NamedTextColor.AQUA));
				if (mItems.size() > 0) {
					message = message.append(Component.text(" with ", NamedTextColor.AQUA))
						          .append(Component.text(getItems().size() + " item" + (getItems().size() > 1 ? "s" : ""), NamedTextColor.AQUA)
							                  .hoverEvent(HoverEvent.showText(getItemList(false))));
				}
				message = message.append(Component.text(" has been located by another player! ", NamedTextColor.AQUA))
					          .append(Component.text("Click here to delete this grave permanently.", NamedTextColor.RED)
						                  .hoverEvent(HoverEvent.showText(Component.text("Delete grave", NamedTextColor.RED)))
						                  .clickEvent(ClickEvent.runCommand("/grave delete " + mUuid)));

				mPlayer.sendMessage(message);
				mGraveMessageCooldown.add(mUuid);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					mGraveMessageCooldown.remove(mUuid);
				}, 60);
			}
		}
	}

	private void generateNewPose() {
		mPose.put(KEY_POSE_HEAD, new EulerAngle(0, 0, 0));
		mPose.put(KEY_POSE_BODY, new EulerAngle(0, 0, 0));
		mPose.put(KEY_POSE_LEFT_ARM, new EulerAngle(0, 0, 0));
		mPose.put(KEY_POSE_RIGHT_ARM, new EulerAngle(0, 0, 0));
		mPose.put(KEY_POSE_LEFT_LEG, new EulerAngle(0, 0, 0));
		mPose.put(KEY_POSE_RIGHT_LEG, new EulerAngle(0, 0, 0));
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

	static @Nullable Grave deserialize(GraveManager manager, Player player, JsonObject data) {
		UUID uuid = null;
		String shard = null;
		Instant time = null;
		boolean small = false;
		Location location = null;
		JsonArray items = new JsonArray();
		HashMap<String, EulerAngle> pose = new HashMap<>();
		for (String key : KEYS_POSE_PARTS) {
			pose.put(key, new EulerAngle(0, 0, 0));
		}
		HashMap<String, ItemStack> equipment = new HashMap<>();
		for (String key : KEYS_EQUIPMENT_PARTS) {
			equipment.put(key, null);
		}
		if (data.has(KEY_UUID) && data.get(KEY_UUID).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_UUID).isString()) {
			uuid = UUID.fromString(data.getAsJsonPrimitive(KEY_UUID).getAsString());
		} else {
			uuid = UUID.randomUUID();
		}
		if (data.has(KEY_SHARD) && data.get(KEY_SHARD).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_SHARD).isString()) {
			shard = data.getAsJsonPrimitive(KEY_SHARD).getAsString();
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
						pose.computeIfAbsent(key, k -> new EulerAngle(0, 0, 0)).setX(obj.getAsJsonPrimitive(KEY_X).getAsDouble());
					}
					if (obj.has(KEY_Y) && obj.get(KEY_Y).isJsonPrimitive() && obj.getAsJsonPrimitive(KEY_Y).isNumber()) {
						pose.computeIfAbsent(key, k -> new EulerAngle(0, 0, 0)).setY(obj.getAsJsonPrimitive(KEY_Y).getAsDouble());
					}
					if (obj.has(KEY_Z) && obj.get(KEY_Z).isJsonPrimitive() && obj.getAsJsonPrimitive(KEY_Z).isNumber()) {
						pose.computeIfAbsent(key, k -> new EulerAngle(0, 0, 0)).setZ(obj.getAsJsonPrimitive(KEY_Z).getAsDouble());
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
		boolean isGhost = data.has(KEY_GHOST) && data.get(KEY_GHOST).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_GHOST).isBoolean() && data.getAsJsonPrimitive(KEY_GHOST).getAsBoolean();
		if (location == null || time == null || shard == null) {
			MMLog.severe("Unable to load grave for player " + player.getName() + "; JSON=" + data);
			return null;
		}
		return new Grave(manager, player, uuid, shard, time, small, location, isGhost, pose, equipment, items);
	}

	public JsonObject serialize() {
		JsonObject data = new JsonObject();

		data.addProperty(KEY_UUID, mUuid.toString());
		data.addProperty(KEY_SHARD, mShardName);
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
				items.add(itemData);
			}
			data.add(KEY_ITEMS, items);
		}

		data.addProperty(KEY_GHOST, mGhostGrave);

		return data;
	}

	public Component getItemList(boolean hovers) {
		Component output = Component.text("");
		for (GraveItem item : getItems()) {
			if (!output.equals(Component.text(""))) {
				output = output.append(Component.text(", ", NamedTextColor.WHITE));
			}

			if (hovers) {
				output = output.append(ItemUtils.getPlainNameComponentWithHover(item.getItem()));
			} else {
				output = output.append(ItemUtils.getPlainNameComponent(item.getItem()));
			}
		}
		return output;
	}

}
