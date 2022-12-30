package com.playmonumenta.plugins.graves;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GraveManager {
	private static final String KEY_PLUGIN_DATA = "MonumentaGravesV2";
	private static final String KEY_GRAVES = "graves";
	private static final String KEY_THROWN_ITEMS = "thrown_items";
	private static final HashMap<UUID, GraveManager> INSTANCES = new HashMap<>();
	// graves by armor stand UUID, not grave UUID
	private static final HashMap<UUID, Grave> GRAVES = new HashMap<>();
	private static final HashMap<UUID, ThrownItem> THROWN_ITEMS = new HashMap<>();
	private static final HashMap<Long, HashSet<Grave>> UNLOADED_GRAVES = new HashMap<>();
	private static final HashMap<Long, HashSet<ThrownItem>> UNLOADED_THROWN_ITEMS = new HashMap<>();
	private final ArrayList<ThrownItem> mThrownItems = new ArrayList<>();
	private final ArrayList<Grave> mGraves = new ArrayList<>();
	private final Player mPlayer;
	private boolean mLoggedOut = false;
	private @Nullable UUID mDeleteAttemptUUID = null;
	private int mDeleteAttemptTicksLived = -1;

	private GraveManager(Player player) {
		mPlayer = player;
		JsonObject graveManagerData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (graveManagerData != null) {
			if (graveManagerData.has(KEY_GRAVES) && graveManagerData.get(KEY_GRAVES).isJsonArray()) {
				JsonArray gravesData = graveManagerData.getAsJsonArray(KEY_GRAVES);
				for (JsonElement graveData : gravesData) {
					JsonObject data = graveData.getAsJsonObject();
					Grave grave = Grave.deserialize(this, mPlayer, data);
					if (grave != null && !grave.isEmpty()) {
						mGraves.add(grave);
					}
				}
			}
			if (graveManagerData.has(KEY_THROWN_ITEMS) && graveManagerData.get(KEY_THROWN_ITEMS).isJsonArray()) {
				JsonArray thrownItemsData = graveManagerData.getAsJsonArray(KEY_THROWN_ITEMS);
				for (JsonElement thrownItemData : thrownItemsData) {
					JsonObject data = thrownItemData.getAsJsonObject();
					ThrownItem thrownItem = ThrownItem.deserialize(this, player, data);
					if (thrownItem != null) {
						mThrownItems.add(thrownItem);
					}
				}
			}
		}
	}

	public static @Nullable GraveManager getInstance(Player player) {
		return INSTANCES.get(player.getUniqueId());
	}

	public static Collection<GraveManager> getAllInstances() {
		return Collections.unmodifiableCollection(INSTANCES.values());
	}

	public static void onLogin(Player player) {
		GraveManager manager = new GraveManager(player);
		INSTANCES.put(player.getUniqueId(), manager);
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

			/* If the player's INSTANCE map hasn't been cleared in 1s and they're still logged out, clear it
			 * There's some memory leak here where sometimes players don't get removed from this map
			 */
			UUID playerUUID = player.getUniqueId();
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				if (Bukkit.getPlayer(playerUUID) == null) {
					INSTANCES.remove(playerUUID);
				}
			}, 20);
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
			for (Grave grave : manager.mGraves) {
				grave.onSave();
				graves.add(grave.serialize());
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

	public static void onEntityAddToWorld(EntityAddToWorldEvent event) {
		/*
		 * Graves are not saved in the chunks they are in - if one chunk loads in, it's because the server crashed.
		 * These graves should be removed, as they're also saved on the player and will be respawned.
		 * Note that while this event is called for newly created graves as well, those graves don't have the Grave tag when they spawn so this won't remove them.
		 */
		if (event.getEntity() instanceof ArmorStand armorStand && armorStand.getScoreboardTags().contains("Grave") && !GRAVES.containsKey(armorStand.getUniqueId())) {
			// delayed to not run in the EntityAddToWorldEvent which is finicky
			Bukkit.getScheduler().runTask(Plugin.getInstance(), armorStand::remove);
		}
	}

	public static void onChunkLoad(ChunkLoadEvent event) {
		HashSet<Grave> graves = new HashSet<>(getUnloadedGraves(event.getChunk().getChunkKey()));
		for (Grave grave : graves) {
			grave.onChunkLoad();
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
			} else if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
				THROWN_ITEMS.get(entity.getUniqueId()).onChunkUnload();
			}
		}
	}

	// Called only on a death that would result in a grave
	public static void onDeath(Player player, HashMap<EquipmentSlot, ItemStack> equipment) {
		GraveManager manager = INSTANCES.computeIfAbsent(player.getUniqueId(), key -> new GraveManager(player));
		String shard = ServerProperties.getShardName();

		if (equipment.entrySet().stream().filter(e -> e.getKey() != EquipmentSlot.HAND)
			    .map(Map.Entry::getValue)
			    .allMatch(item -> ItemUtils.isNullOrAir(item)
				                      || ItemStatUtils.getTier(item) == ItemStatUtils.Tier.NONE
				                      || ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED) >= Shattered.MAX_LEVEL)) {
			// Check Lich infusion
			if (Plugin.getInstance().mItemStatManager.getInfusionLevel(player, ItemStatUtils.InfusionType.PHYLACTERY) == 0
				    || ScoreboardUtils.getScoreboardValue(player, Phylactery.GRAVE_XP_SCOREBOARD).orElse(0) == 0) {
				player.sendMessage(Component.text("You died but had nothing equipped that could shatter, so no grave was created nor were items shattered further. ", NamedTextColor.GRAY)
					                   .append(Component.text("(/help death for more info)", NamedTextColor.GRAY).clickEvent(ClickEvent.runCommand("/help death"))));
			} else if (manager.mGraves.stream().noneMatch(grave -> grave.mGhostGrave && shard.equals(grave.mShardName))) {
				manager.mGraves.add(new Grave(manager, player, equipment));
				player.sendMessage(Component.text("You died but had nothing equipped that could shatter, nevertheless you left a grave to store your experience! ", NamedTextColor.GRAY)
					                   .append(Component.text("(/help death for more info)", NamedTextColor.GRAY).clickEvent(ClickEvent.runCommand("/help death"))));
			}
		} else if (manager.mGraves.stream().noneMatch(grave -> grave.mGhostGrave && shard.equals(grave.mShardName))) {
			manager.mGraves.add(new Grave(manager, player, equipment));
			player.sendMessage(Component.text("You died and left a grave! Return to it to repair your items! ", NamedTextColor.RED)
				.append(Component.text("(/help death for more info)", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/help death"))));
		} else {
			player.sendMessage(Component.text("You died but already had a grave, so no new grave was created. Your items have shattered further! ", NamedTextColor.RED)
				.append(Component.text("(/help death for more info)", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/help death"))));
		}
	}

	// Called only when an item is dropped that, when destroyed, would result in a grave
	public static void onDropItem(Player player, Item entity) {
		GraveManager manager = INSTANCES.get(player.getUniqueId());
		if (entity != null && manager != null) {
			manager.mThrownItems.add(new ThrownItem(manager, player, entity));
		}
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
		if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
			ThrownItem item = THROWN_ITEMS.get(entity.getUniqueId());
			item.onAttemptPickupItem(event);
			if (!item.isValid()) {
				item.mManager.mThrownItems.remove(item);
			}
		}
	}

	// Called when an item entity is killed by any method
	public static void onDestroyItem(Item entity) {
		if (THROWN_ITEMS.containsKey(entity.getUniqueId())) {
			ThrownItem item = THROWN_ITEMS.remove(entity.getUniqueId());
			if (item.isValid()) {
				item.onDestroyItem();
				if (ItemStatUtils.getInfusionLevel(item.mItem, ItemStatUtils.InfusionType.HOPE) <= 0) {
					Shattered.shatter(item.mItem, Shattered.DROPPED_ITEM_DESTROYED);
				}
				item.mManager.mGraves.add(new Grave(item));
				item.mManager.mPlayer.sendMessage(Component.text("An item you dropped at ", NamedTextColor.RED)
					.append(Component.text(item.mLocation.getBlockX() + "," + item.mLocation.getBlockY() + "," + item.mLocation.getBlockZ()))
					.append(Component.text(" was destroyed. A grave will keep it safe for you. "))
					.append(Component.text("(/help death for more info)")
						.clickEvent(ClickEvent.runCommand("/help death")))
				);
			}
			item.mManager.mThrownItems.remove(item);
		}
	}

	public static boolean isGrave(Entity entity) {
		return entity instanceof ArmorStand && (entity.getScoreboardTags().contains("Grave") || GRAVES.containsKey(entity.getUniqueId()));
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

	private static HashSet<ThrownItem> getUnloadedThrownItems(Long key) {
		if (!UNLOADED_THROWN_ITEMS.containsKey(key)) {
			UNLOADED_THROWN_ITEMS.put(key, new HashSet<>());
		}
		return UNLOADED_THROWN_ITEMS.get(key);
	}

	void addGrave(ArmorStand armorStand, Grave grave) {
		GRAVES.put(armorStand.getUniqueId(), grave);
	}

	void removeGrave(ArmorStand armorStand) {
		GRAVES.remove(armorStand.getUniqueId());
	}

	void removeGrave(Grave grave) {
		removeUnloadedGrave(Chunk.getChunkKey(grave.mLocation), grave);
		mGraves.remove(grave);
	}

	void addItem(Item entity, ThrownItem item) {
		THROWN_ITEMS.put(entity.getUniqueId(), item);
	}

	void removeItem(Item entity) {
		THROWN_ITEMS.remove(entity.getUniqueId());
	}

	void addUnloadedGrave(Long key, Grave grave) {
		getUnloadedGraves(key).add(grave);
	}

	void removeUnloadedGrave(Long key, Grave grave) {
		getUnloadedGraves(key).remove(grave);
	}

	void addUnloadedItem(Long key, ThrownItem item) {
		getUnloadedThrownItems(key).add(item);
	}

	void removeUnloadedItem(Long key, ThrownItem item) {
		getUnloadedThrownItems(key).remove(item);
	}

	boolean isOwner(Player player) {
		return player.getUniqueId().equals(mPlayer.getUniqueId());
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
		return getGravesList(page, grave -> null);
	}

	public ArrayList<Component> getGravesList(int page, Function<Grave, Component> extraText) {
		removeEmptyGraves();
		ArrayList<Component> list = new ArrayList<>();
		int first = 5 * (page - 1);
		int last = first + 4;
		if (mGraves.size() > first) {
			for (int i = first; i <= last; i++) {
				if (mGraves.size() > i) {
					Grave grave = mGraves.get(i);
					Component graveInfo = getGraveInfo(grave);
					Component extra = extraText.apply(grave);
					if (extra != null) {
						graveInfo = graveInfo.append(extra);
					}
					list.add(graveInfo);
				}
			}
		}
		return list;
	}

	public Component getGraveInfo(Grave grave) {
		Component itemList = grave.getItemList(false);
		return Component.text("World: (", NamedTextColor.GRAY)
			.append(Component.text(grave.mShardName, NamedTextColor.WHITE))
			.append(Component.text(") Loc: (", NamedTextColor.GRAY))
			.append(Component.text(grave.mLocation.getBlockX() + "," + grave.mLocation.getBlockY() + "," + grave.mLocation.getBlockZ(), NamedTextColor.WHITE))
				.append(Component.text(") ", NamedTextColor.GRAY))
				.append(Component.text("Items: (", NamedTextColor.GRAY)
						.hoverEvent(HoverEvent.showText(itemList)))
				.append(Component.text(grave.mItems.size(), NamedTextColor.WHITE)
						.hoverEvent(HoverEvent.showText(itemList)))
				.append(Component.text(")", NamedTextColor.GRAY)
						.hoverEvent(HoverEvent.showText(itemList)))
				.append(Component.text(" "))
				.append(Component.text("[X]", NamedTextColor.DARK_RED)
						.hoverEvent(HoverEvent.showText(Component.text("Click to delete", NamedTextColor.RED)))
						.clickEvent(ClickEvent.runCommand("/grave delete " + grave.mUuid)));
	}

	public List<Grave> getGraves() {
		return mGraves;
	}

	public @Nullable Grave getGrave(UUID uuid) {
		return mGraves.stream().filter(grave -> grave.mUuid.equals(uuid)).findFirst().orElse(null);
	}

	public static @Nullable Grave getGraveFromAnyPlayer(UUID uuid) {
		return INSTANCES.values().stream().map(m -> m.getGrave(uuid)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private void removeEmptyGraves() {
		mGraves.removeIf(Grave::isEmpty);
	}

	public boolean isDeleteConfirmation(UUID uuid, int ticksLived) {
		if (uuid.equals(mDeleteAttemptUUID) && ticksLived <= mDeleteAttemptTicksLived + 20 * 20) {
			return true;
		} else {
			mDeleteAttemptUUID = uuid;
			mDeleteAttemptTicksLived = ticksLived;
			return false;
		}
	}

	public boolean cancelDeletion() {
		mDeleteAttemptTicksLived = -1;
		if (mDeleteAttemptUUID == null) {
			return false;
		}
		mDeleteAttemptUUID = null;
		return true;
	}
}
