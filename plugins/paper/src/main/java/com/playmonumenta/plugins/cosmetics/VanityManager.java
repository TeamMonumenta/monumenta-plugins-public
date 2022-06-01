package com.playmonumenta.plugins.cosmetics;

import com.destroystokyo.paper.event.player.PlayerDataLoadEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Nullable;

public class VanityManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "MonumentaVanity";
	public static final String INVISIBLE_NBT_KEY = "Invisible";

	public static class VanityData {
		private final Map<EquipmentSlot, ItemStack> mEquipped = new HashMap<>();
		public boolean mSelfVanityEnabled = true;
		public boolean mOtherVanityEnabled = true;
		public boolean mLockboxSwapEnabled = true;

		public void equip(EquipmentSlot slot, @Nullable ItemStack item) {
			if (item == null || item.getType() == Material.AIR) {
				mEquipped.remove(slot);
			} else {
				cleanForDisplay(item);
				item.setAmount(1);
				mEquipped.put(slot, item);
			}
		}

		public @Nullable ItemStack getEquipped(EquipmentSlot slot) {
			return mEquipped.get(slot);
		}
	}

	private final Map<UUID, VanityData> mData = new HashMap<>();

	public VanityData getData(Player player) {
		return mData.computeIfAbsent(player.getUniqueId(), key -> new VanityData());
	}

	public void toggleSelfVanity(Player player) {
		VanityData data = getData(player);
		data.mSelfVanityEnabled = !data.mSelfVanityEnabled;
	}

	public void toggleOtherVanity(Player player) {
		VanityData data = getData(player);
		data.mOtherVanityEnabled = !data.mOtherVanityEnabled;
	}

	public void toggleLockboxSwap(Player player) {
		VanityData data = getData(player);
		data.mLockboxSwapEnabled = !data.mLockboxSwapEnabled;
	}

	// not an event listener - called by the cosmetics manager so that cosmetics are loaded before this is called
	public void playerDataLoadEvent(PlayerDataLoadEvent event) {
		Player player = event.getPlayer();
		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (data == null) {
			return;
		}
		VanityData vanityData = new VanityData();
		JsonObject items = data.getAsJsonObject("equipped");
		for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
			ItemStack item = NBTItem.convertNBTtoItem(new NBTContainer(entry.getValue().getAsString()));
			EquipmentSlot slot = EquipmentSlot.valueOf(entry.getKey());
			if (hasVanityUnlocked(player, item, slot)
				    && (slot != EquipmentSlot.OFF_HAND || isValidOffhandVanityItem(item))) {
				if (!ItemStatUtils.isClean(item)) {
					ItemUtils.setPlainTag(item);
					ItemStatUtils.generateItemStats(item);
					ItemStatUtils.markClean(item);
				}
				vanityData.equip(slot, item);
			}
		}
		vanityData.mSelfVanityEnabled = data.getAsJsonPrimitive("selfVanityEnabled").getAsBoolean();
		vanityData.mLockboxSwapEnabled = data.getAsJsonPrimitive("lockboxSwapEnabled").getAsBoolean();
		mData.put(player.getUniqueId(), vanityData);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		VanityData vanityData = mData.get(event.getPlayer().getUniqueId());
		if (vanityData == null) {
			return;
		}
		JsonObject data = new JsonObject();
		JsonObject equipped = new JsonObject();
		for (Map.Entry<EquipmentSlot, ItemStack> entry : vanityData.mEquipped.entrySet()) {
			equipped.addProperty(entry.getKey().name(), NBTItem.convertItemtoNBT(entry.getValue()).toString());
		}
		data.add("equipped", equipped);
		data.addProperty("selfVanityEnabled", vanityData.mSelfVanityEnabled);
		data.addProperty("lockboxSwapEnabled", vanityData.mLockboxSwapEnabled);
		event.setPluginData(KEY_PLUGIN_DATA, data);
	}

	// Discard cosmetic data a few ticks after player leaves shard
	// (give time for save event to register)
	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mData.remove(player.getUniqueId());
			}
		}, 10);
	}

	// Update the offhand vanity item after right-clicking, as it may have changed state on the client
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
			    && !ItemUtils.isNullOrAir(offHand)
			    && offHand.getMaxItemUseDuration() == 0) { // don't update items that have a use time (and the vanity is the same type anyway)
			VanityData data = getData(player);
			if (data.mSelfVanityEnabled && !ItemUtils.isNullOrAir(data.getEquipped(EquipmentSlot.OFF_HAND))) {
				player.getInventory().setItemInOffHand(null);
				player.getInventory().setItemInOffHand(offHand);
			}
		}
	}

	/**
	 * Removes data from an item that is not needed to properly display it, for example shulker box contents.
	 */
	public static void cleanForDisplay(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return;
		}
		NBTItem nbtItem = new NBTItem(item, true);
		if (item.getType() != Material.SHIELD && !ItemUtils.isBanner(item)) {
			nbtItem.removeKey("BlockEntityTag"); // shulker contents, and also other invisible block entity data
		}
		if (nbtItem.getType("display") == NBTType.NBTTagCompound) {
			// display name and lore (plain.display.xyz is kept for RP purposes)
			NBTCompound display = nbtItem.getCompound("display");
			display.removeKey("Lore");
			display.removeKey("name");
		}
		nbtItem.removeKey("pages"); // book contents
		NBTCompound monumenta = nbtItem.getCompound(ItemStatUtils.MONUMENTA_KEY);
		if (monumenta != null) {
			NBTCompound playerModified = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (playerModified != null) {
				playerModified.removeKey(ItemStatUtils.ITEMS_KEY); // custom items storage
			}
			monumenta.removeKey(ItemStatUtils.STOCK_KEY);
			monumenta.removeKey(ItemStatUtils.LORE_KEY);
		}
		nbtItem.removeKey("AttributeModifiers");
	}

	public static boolean isInvisibleVanityItem(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return false;
		}
		NBTCompound monumenta = new NBTItem(itemStack).getCompound(ItemStatUtils.MONUMENTA_KEY);
		return monumenta != null && Boolean.TRUE.equals(monumenta.getBoolean(INVISIBLE_NBT_KEY));
	}

	public static ItemStack getInvisibleVanityItem(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> makeInvisibleVanityItem(Material.LEATHER_HELMET, "Helmet");
			case CHEST -> makeInvisibleVanityItem(Material.LEATHER_CHESTPLATE, "Chestplate");
			case LEGS -> makeInvisibleVanityItem(Material.LEATHER_LEGGINGS, "Leggings");
			case FEET -> makeInvisibleVanityItem(Material.LEATHER_BOOTS, "Boots");
			default -> makeInvisibleVanityItem(Material.GHAST_TEAR, "Offhand");
		};
	}

	private static ItemStack makeInvisibleVanityItem(Material type, String name) {
		ItemStack item = new ItemStack(type);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Invisible " + name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
			leatherArmorMeta.setColor(Color.WHITE);
		}
		meta.addItemFlags(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);
		new NBTItem(item, true).addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(INVISIBLE_NBT_KEY, true);
		return item;
	}

	private static String getCosmeticsName(ItemStack item) {
		return item.getType().name().toLowerCase(Locale.ROOT) + ":" + ItemUtils.getPlainNameIfExists(item);
	}

	public boolean hasVanityUnlocked(Player player, ItemStack item, EquipmentSlot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return true;
		}
		return hasFreeAccess(player)
			       || (CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.VANITY, getCosmeticsName(item))
				           && (slot != EquipmentSlot.HEAD || ItemUtils.getEquipmentSlot(item) == EquipmentSlot.HEAD));
	}

	public void unlockVanity(Player player, ItemStack item) {
		CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.VANITY, getCosmeticsName(item));
	}

	public static boolean hasFreeAccess(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_4;
	}

	public static boolean isValidOffhandVanityItem(ItemStack item) {
		if (item == null) {
			return false;
		}
		Material type = item.getType();
		return switch (type) {
			case AIR, WRITABLE_BOOK, BUCKET -> false;
			default -> true;
		};
	}

}
