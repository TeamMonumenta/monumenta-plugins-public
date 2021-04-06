package com.playmonumenta.plugins.graves;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GraveManager {
	private static final String KEY_PLUGIN_DATA = "MonumentaGravesV2";
	private static final String KEY_GRAVES = "graves";
	private static final String KEY_THROWN_ITEMS = "thrown_items";
	private static final HashMap<UUID,GraveManager> INSTANCES = new HashMap<>();
	private static final HashMap<UUID,Grave> GRAVES = new HashMap<>();
	private static final HashMap<UUID,GraveItem> GRAVE_ITEMS = new HashMap<>();
	private static final HashMap<UUID,ThrownItem> THROWN_ITEMS = new HashMap<>();
	private static final HashMap<Long,HashSet<Grave>> UNLOADED_GRAVES = new HashMap<>();
	private static final HashMap<Long,HashSet<GraveItem>> UNLOADED_GRAVE_ITEMS = new HashMap<>();
	private static final HashMap<Long,HashSet<ThrownItem>> UNLOADED_THROWN_ITEMS = new HashMap<>();
	private final ArrayList<ThrownItem> mThrownItems = new ArrayList<>();
	private final ArrayList<Grave> mGraves = new ArrayList<>();
	private final HashSet<UUID> mAllowed = new HashSet<>();
	private final Player mPlayer;
	private boolean mLoggedOut = false;

	private GraveManager(Player player) {
		mPlayer = player;
		JsonObject graveManagerData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (graveManagerData != null) {
			if (graveManagerData.has(KEY_GRAVES) && graveManagerData.get(KEY_GRAVES).isJsonArray()) {
				JsonArray gravesData = graveManagerData.getAsJsonArray(KEY_GRAVES);
				for (JsonElement graveData : gravesData) {
					JsonObject data = graveData.getAsJsonObject();
					mGraves.add(Grave.deserialize(this, mPlayer, data));
				}
			}
			if (graveManagerData.has(KEY_THROWN_ITEMS) && graveManagerData.get(KEY_THROWN_ITEMS).isJsonArray()) {
				JsonArray thrownItemsData = graveManagerData.getAsJsonArray(KEY_THROWN_ITEMS);
				for (JsonElement thrownItemData : thrownItemsData) {
					JsonObject data = thrownItemData.getAsJsonObject();
					mThrownItems.add(ThrownItem.deserialize(this, player, data));
				}
			}
		}
	}

	public static GraveManager getInstance(Player player) {
		return INSTANCES.get(player.getUniqueId());
	}

	public static void onLogin(Player player) {
		GraveManager manager = new GraveManager(player);
		INSTANCES.put(player.getUniqueId(),manager);
		for (Grave grave : manager.mGraves) {
			grave.onLogin();
		}
		for (ThrownItem item : manager.mThrownItems) {
			item.onLogin();
		}
	}

	public static void onLogout(Player player) {
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		if (manager != null) {
			for (Grave grave : manager.mGraves) {
				grave.onLogout();
			}
			for (ThrownItem item : manager.mThrownItems) {
				item.onLogout();
			}

			manager.mLoggedOut = true;
		}
	}

	public static void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		if (manager != null) {
			JsonObject data = new JsonObject();
			JsonArray graves = new JsonArray();
			JsonArray thrownItems = new JsonArray();
			data.add(KEY_GRAVES, graves);
			data.add(KEY_THROWN_ITEMS, thrownItems);
			Iterator<Grave> iterGraves = manager.mGraves.iterator();
			while (iterGraves.hasNext()) {
				Grave grave = iterGraves.next();
				if (grave.isEmpty()) {
					grave.delete();
					iterGraves.remove();
				} else {
					grave.onSave();
					JsonObject graveData = grave.serialize();
					if (graveData != null) {
						graves.add(graveData);
					}
				}
			}
			Iterator<ThrownItem> iterThrownItems = manager.mThrownItems.iterator();
			while (iterThrownItems.hasNext()) {
				ThrownItem item = iterThrownItems.next();
				if (item.isValid()) {
					item.onSave();
					JsonObject itemData = item.serialize();
					if (itemData != null) {
						thrownItems.add(itemData);
					}
				} else {
					item.delete();
					iterThrownItems.remove();
				}
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
			if (manager.mLoggedOut) {
				INSTANCES.remove(player.getUniqueId());
			}
		}
	}

	public static void onChunkLoad(ChunkLoadEvent event) {
		HashSet<Grave> graves = new HashSet<>(getUnloadedGraves(event.getChunk().getChunkKey()));
		for (Grave grave : graves) {
			grave.onChunkLoad();
		}
		HashSet<GraveItem> graveItems = new HashSet<>(getUnloadedGraveItems(event.getChunk().getChunkKey()));
		for (GraveItem item : graveItems) {
			item.onChunkLoad();
		}
		HashSet<ThrownItem> thrownItems = new HashSet<>(getUnloadedThrownItems(event.getChunk().getChunkKey()));
		for (ThrownItem item : thrownItems) {
			item.onChunkLoad();
		}
	}

	public static void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (GRAVES.containsKey(entity.getUniqueId())) {
				GRAVES.get(entity.getUniqueId()).onChunkUnload();
			} else if (GRAVE_ITEMS.containsKey(entity.getUniqueId())) {
				GRAVE_ITEMS.get(entity.getUniqueId()).onChunkUnload();
			} else if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
				THROWN_ITEMS.get(entity.getUniqueId()).onChunkUnload();
			}
		}
	}

	// Called on any death, used for shattering items currently in limbo
	public static void onDeath(Player player) {
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		for (Grave grave : manager.mGraves) {
			grave.onDeath();
		}
	}

	// Called only on a death that would result in a grave
	public static void onDeath(Player player, ArrayList<ItemStack> droppedItems, HashMap<EquipmentSlot, ItemStack> equipment) {
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		manager.mGraves.add(new Grave(manager, player, droppedItems, equipment));
	}

	// Called only when an item is dropped that, when destroyed, would result in a grave
	public static void onDropItem(Player player, Item entity) {
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		manager.mThrownItems.add(new ThrownItem(manager, player, entity));
	}

	// Called when a player left or right clicks a grave
	public static void onInteract(Player player, Entity entity) {
		if (GRAVES.containsKey(entity.getUniqueId())) {
			GRAVES.get(entity.getUniqueId()).onInteract(player);
		}
	}

	// Called when any player is within pickup range of any item
	public static void onAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		Item entity = event.getItem();
		if (GRAVE_ITEMS.containsKey(entity.getUniqueId())) {
			GRAVE_ITEMS.get(entity.getUniqueId()).onAttemptPickupItem(event);
		} else if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
			ThrownItem item = THROWN_ITEMS.get(entity.getUniqueId());
			item.onAttemptPickupItem(event);
			if (!item.isValid()) {
				item.mManager.mThrownItems.remove(item);
			}
		}
	}

	// Called when an item entity is killed by any method
	public static void onDestroyItem(Item entity) {
		if (GRAVE_ITEMS.containsKey(entity.getUniqueId())) {
			GRAVE_ITEMS.get(entity.getUniqueId()).onDestroyItem();
		} else if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
			ThrownItem item = THROWN_ITEMS.remove(entity.getUniqueId());
			if (item.isValid()) {
				item.onDestroyItem();
				item.mManager.mGraves.add(new Grave(item));
			}
			item.mManager.mThrownItems.remove(item);
		}
	}

	public static boolean isGrave(Entity entity) {
		return entity.getScoreboardTags().contains("Grave") || GRAVES.containsKey(entity.getUniqueId());
	}

	public static boolean isGraveItem(Entity entity) {
		return entity.getScoreboardTags().contains("GraveItem") || GRAVE_ITEMS.containsKey(entity.getUniqueId());
	}

	public static boolean isThrownItem(Entity entity) {
		return entity.getScoreboardTags().contains("ThrownItem") || THROWN_ITEMS.containsKey(entity.getUniqueId());
	}

	private static HashSet<Grave> getUnloadedGraves(Long key) {
		if (!UNLOADED_GRAVES.containsKey(key)) {
			UNLOADED_GRAVES.put(key, new HashSet<>());
		}
		return UNLOADED_GRAVES.get(key);
	}

	private static HashSet<GraveItem> getUnloadedGraveItems(Long key) {
		if (!UNLOADED_GRAVE_ITEMS.containsKey(key)) {
			UNLOADED_GRAVE_ITEMS.put(key, new HashSet<>());
		}
		return UNLOADED_GRAVE_ITEMS.get(key);
	}

	private static HashSet<ThrownItem> getUnloadedThrownItems(Long key) {
		if (!UNLOADED_THROWN_ITEMS.containsKey(key)) {
			UNLOADED_THROWN_ITEMS.put(key, new HashSet<>());
		}
		return UNLOADED_THROWN_ITEMS.get(key);
	}

	void addGrave(UUID uuid, Grave grave) {
		GRAVES.put(uuid, grave);
	}

	void removeGrave(UUID uuid) {
		GRAVES.remove(uuid);
	}

	void addItem(UUID uuid, GraveItem item) {
		GRAVE_ITEMS.put(uuid, item);
	}

	void addItem(UUID uuid, ThrownItem item) {
		THROWN_ITEMS.put(uuid, item);
	}

	void removeItem(UUID uuid) {
		GRAVE_ITEMS.remove(uuid);
		THROWN_ITEMS.remove(uuid);
	}

	void addUnloadedGrave(Long key, Grave grave) {
		getUnloadedGraves(key).add(grave);
	}

	void removeUnloadedGrave(Long key, Grave grave) {
		getUnloadedGraves(key).remove(grave);
	}

	void addUnloadedItem(Long key, GraveItem item) {
		getUnloadedGraveItems(key).add(item);
	}

	void addUnloadedItem(Long key, ThrownItem item) {
		getUnloadedThrownItems(key).add(item);
	}

	void removeUnloadedItem(Long key, GraveItem item) {
		getUnloadedGraveItems(key).remove(item);
	}

	void removeUnloadedItem(Long key, ThrownItem item) {
		getUnloadedThrownItems(key).remove(item);
	}

	boolean isOwner(Player player) {
		return player.getUniqueId() == mPlayer.getUniqueId();
	}

	boolean hasPermission(Player player) {
		return isOwner(player) || mAllowed.contains(player.getUniqueId());
	}

	public boolean grantPermission(Player player) {
		if (!hasPermission(player)) {
			mAllowed.add(player.getUniqueId());
			return true;
		}
		return false;
	}

	public boolean revokePermission(Player player) {
		if (mAllowed.contains(player.getUniqueId())) {
			mAllowed.remove(player.getUniqueId());
			return true;
		}
		return false;
	}

	public int getGravesCount() {
		removeEmptyGraves();
		return mGraves.size();
	}

	public int getGravesPageCount() {
		removeEmptyGraves();
		if (mGraves.size() % 5 == 0) {
			return (mGraves.size() / 5);
		}
		return (mGraves.size() / 5) + 1;
	}

	public ArrayList<Component> getGravesList(int page) {
		removeEmptyGraves();
		ArrayList<Component> list = new ArrayList<>();
		int first = 5 * (page - 1);
		int last = first + 4;
		if (mGraves.size() > first) {
			for (int i = first; i <= last; i++) {
				if (mGraves.size() > i) {
					list.add(getGraveInfo(i));
				}
			}
		}
		return list;
	}

	public Component getGraveInfo(int index) {
		if (mGraves.size() > index) {
			Grave grave = mGraves.get(index);
			return Component.text("World: (", NamedTextColor.GRAY)
				.append(Component.text(grave.mWorldName.replace("Project_Epic-", ""), NamedTextColor.WHITE))
				.append(Component.text(") Loc: (", NamedTextColor.GRAY))
				.append(Component.text(grave.mLocation.getBlockX() + "," + grave.mLocation.getBlockY() + "," + grave.mLocation.getBlockZ(), NamedTextColor.WHITE))
				.append(Component.text(") Items: (", NamedTextColor.GRAY))
				.append(Component.text(grave.mItems.size(), NamedTextColor.WHITE))
				.append(Component.text(")", NamedTextColor.GRAY));
		}
		return null;
	}

	public boolean summonGrave(int index, Location location) {
		if (mGraves.size() > index) {
			Grave grave = mGraves.get(index);
			grave.summon(location);
			return true;
		}
		return false;
	}

	private void removeEmptyGraves() {
		mGraves.removeIf(Grave::isEmpty);
	}
}
