package com.playmonumenta.plugins.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.protocollib.VirtualItemsReplacer;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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
		public boolean mGuiVanityEnabled = true;

		public void equip(EquipmentSlot slot, @Nullable ItemStack item) {
			if (item == null || item.getType() == Material.AIR) {
				mEquipped.remove(slot);
			} else {
				item = cleanCopyForDisplay(item);
				item.setAmount(1);
				mEquipped.put(slot, item);
			}
		}

		public void setEquipped(Map<EquipmentSlot, ItemStack> vanity) {
			mEquipped.clear();
			for (Map.Entry<EquipmentSlot, ItemStack> entry : vanity.entrySet()) {
				equip(entry.getKey(), entry.getValue());
			}
		}

		public @Nullable ItemStack getEquipped(EquipmentSlot slot) {
			return mEquipped.get(slot);
		}

		public Map<EquipmentSlot, ItemStack> getEquipped() {
			return new HashMap<>(mEquipped);
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

	public void toggleGuiVanity(Player player) {
		VanityData data = getData(player);
		data.mGuiVanityEnabled = !data.mGuiVanityEnabled;
	}

	public void toggleLockboxSwap(Player player) {
		VanityData data = getData(player);
		data.mLockboxSwapEnabled = !data.mLockboxSwapEnabled;
	}

	// not an event listener - called by the cosmetics manager so that cosmetics are loaded before this is called
	public void playerJoinEvent(PlayerJoinEvent event) {
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
			if (isValidVanityItem(player, item, slot)) {
				if (!ItemStatUtils.isClean(item)) {
					ItemUtils.setPlainTag(item);
					ItemStatUtils.generateItemStats(item);
					ItemStatUtils.markClean(item);
				}
				vanityData.equip(slot, item);
			}
		}
		vanityData.mSelfVanityEnabled = data.getAsJsonPrimitive("selfVanityEnabled").getAsBoolean();
		vanityData.mOtherVanityEnabled = Optional.ofNullable(data.getAsJsonPrimitive("otherVanityEnabled")).map(JsonPrimitive::getAsBoolean).orElse(true);
		vanityData.mGuiVanityEnabled = Optional.ofNullable(data.getAsJsonPrimitive("guiVanityEnabled")).map(JsonPrimitive::getAsBoolean).orElse(true);
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
		data.addProperty("otherVanityEnabled", vanityData.mOtherVanityEnabled);
		data.addProperty("guiVanityEnabled", vanityData.mGuiVanityEnabled);
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
			    && event.getHand() == EquipmentSlot.OFF_HAND
			    && !ItemUtils.isNullOrAir(offHand)
			    && offHand.getMaxItemUseDuration() == 0) { // don't update items that have a use time (and the vanity is the same type anyway)
			VanityData data = getData(player);
			ItemStack offhandVanity = data.getEquipped(EquipmentSlot.OFF_HAND);
			if (data.mSelfVanityEnabled && !ItemUtils.isNullOrAir(offhandVanity)) {
				player.updateInventory();

				// For 2-block tall blocks, send a block update for the block 2 above the clicked block to prevent the top half from visually staying
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
					switch (offhandVanity.getType()) {
						case PEONY, ROSE_BUSH, TALL_GRASS, TALL_SEAGRASS, LARGE_FERN, SUNFLOWER, LILAC,
							     DARK_OAK_DOOR, ACACIA_DOOR, BIRCH_DOOR, CRIMSON_DOOR, IRON_DOOR, JUNGLE_DOOR, OAK_DOOR, SPRUCE_DOOR, WARPED_DOOR -> {
							Block block = event.getClickedBlock().getRelative(BlockFace.UP, 2);
							player.sendBlockChange(block.getLocation(), block.getBlockData());
						}
						default -> {
						}
					}
				}
			}
		}
	}

	/**
	 * Removes data from an item that is not needed to properly display it, for example shulker box contents.
	 *
	 * @return A copy of the passed item stack with some data removed
	 */
	public static ItemStack cleanCopyForDisplay(ItemStack item) {
		if (item == null) {
			return null;
		}
		if (!item.hasItemMeta()) {
			return ItemUtils.clone(item);
		}
		NBTItem nbtItem = new NBTItem(item);
		if (item.getType() != Material.SHIELD && !ItemUtils.isBanner(item)) {
			nbtItem.removeKey("BlockEntityTag"); // shulker contents, and also other invisible block entity data
		}
		if (nbtItem.getType("display") == NBTType.NBTTagCompound) {
			// display lore (plain.display.Lore is kept for RP purposes)
			NBTCompound display = nbtItem.getCompound("display");
			display.removeKey("Lore");
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
		return nbtItem.getItem();
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

	/**
	 * Checks if the player may equip the given item into the given vanity slot (is unlocked or is patron, and checks if the slot is valid for the item's type)
	 */
	public static boolean isValidVanityItem(Player player, ItemStack item, EquipmentSlot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return true;
		}
		return ((slot == EquipmentSlot.OFF_HAND && isValidOffhandVanityItem(item)) || (hasFreeAccess(player) && slot == EquipmentSlot.HEAD) || ItemUtils.getEquipmentSlot(item) == slot)
			       && hasVanityUnlocked(player, item);
	}

	public static boolean hasVanityUnlocked(Player player, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return true;
		}
		return hasFreeAccess(player) || CosmeticsManager.getInstance().playerHasCosmetic(player, CosmeticType.VANITY, getCosmeticsName(item));
	}

	public void unlockVanity(Player player, ItemStack item) {
		CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.VANITY, getCosmeticsName(item));
	}

	public static boolean hasFreeAccess(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_3;
	}

	public static boolean isValidOffhandVanityItem(ItemStack item) {
		if (item == null) {
			return false;
		}
		Material type = item.getType();
		return switch (type) {
			case AIR, WRITABLE_BOOK, BUCKET, WATER_BUCKET, COD_BUCKET, PUFFERFISH_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET, LAVA_BUCKET -> false;
			default -> true;
		};
	}

	/**
	 * Applies vanity to the given item, Modifies the passed-in item stack!
	 *
	 * @param itemStack     Real item stack to apply vanity to (will be modified!)
	 * @param vanityData    Vanity data for the player
	 * @param equipmentSlot The item's slot
	 * @param self          Whether this is for the player's own equipment or not. Affects offhands.
	 */
	public static void applyVanity(ItemStack itemStack, VanityData vanityData, EquipmentSlot equipmentSlot, boolean self) {
		ItemStack vanityItem = vanityData.getEquipped(equipmentSlot);
		if (vanityItem != null && vanityItem.getType() != Material.AIR) {
			if (self
				    && equipmentSlot == EquipmentSlot.OFF_HAND
				    && (itemStack.getMaxItemUseDuration() > 0 || vanityItem.getMaxItemUseDuration() > 0)
				    && itemStack.getType() != vanityItem.getType()) {
				// don't allow changing item type of useable items (e.g. food, shields) to prevent not being slowed down while using them or just messing with their use in general
				return;
			}
			boolean invisible = VanityManager.isInvisibleVanityItem(vanityItem);
			if (invisible && equipmentSlot != EquipmentSlot.OFF_HAND) { // invisible armor: only add tag for RP and add lore line
				NBTItem nbt = new NBTItem(itemStack, true);
				nbt.addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(VanityManager.INVISIBLE_NBT_KEY, true);
				ItemMeta meta = itemStack.getItemMeta();
				if (meta != null) {
					List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
					lore.add(0, Component.text("Invisibility vanity skin applied", NamedTextColor.GOLD));
					meta.lore(lore);
					itemStack.setItemMeta(meta);
				}
				VirtualItemsReplacer.markVirtual(itemStack);
				return;
			}
			ItemMeta vanityMeta = vanityItem.getItemMeta();
			if (vanityMeta == null) {
				return;
			}
			ItemMeta originalMeta = itemStack.getItemMeta();

			if (vanityMeta instanceof Damageable vanityDamage
				    && vanityItem.getType().getMaxDurability() > 0) {
				// Copy over durability, adjusted for potentially changed max durability,
				// or remove any damage present if the original item is unbreakable
				if (originalMeta instanceof Damageable originalDamage
					    && itemStack.getType().getMaxDurability() > 0
					    && !originalMeta.isUnbreakable()) {
					vanityMeta.setUnbreakable(false);
					vanityDamage.setDamage((int) Math.round(1.0 * vanityItem.getType().getMaxDurability() * originalDamage.getDamage() / itemStack.getType().getMaxDurability()));
				} else if (!vanityMeta.isUnbreakable()) {
					vanityDamage.setDamage(0);
				}
			}

			// copy display name and lore, but not plain ones
			Component vanityDisplayName = vanityMeta.displayName();
			if (originalMeta != null) {
				vanityMeta.displayName(Objects.requireNonNullElseGet(originalMeta.displayName(),
					() -> Component.translatable(itemStack.getType().getTranslationKey()).decoration(TextDecoration.ITALIC, false)));
			}
			List<Component> lore = originalMeta == null || originalMeta.lore() == null ? new ArrayList<>() : new ArrayList<>(originalMeta.lore());
			if (invisible) {
				lore.add(0, Component.text("Invisibility vanity skin applied", NamedTextColor.GOLD));
			} else {
				// This does not use the plain name as that cuts off some characters
				if (vanityDisplayName != null) {
					vanityDisplayName = Component.text(MessagingUtils.plainText(vanityDisplayName));
				} else {
					vanityDisplayName = Component.translatable(vanityItem.getType().getTranslationKey());
				}
				lore.add(0, Component.text("Vanity skin: ", NamedTextColor.GOLD).append(vanityDisplayName));
			}
			// add durability lore line if vanity item is unbreakable
			if (originalMeta instanceof Damageable originalDamage
				    && itemStack.getType().getMaxDurability() > 0
				    && !originalMeta.isUnbreakable()
				    && (!(vanityMeta instanceof Damageable) || vanityItem.getType().getMaxDurability() <= 0)) {
				lore.add(Component.translatable("item.durability", NamedTextColor.WHITE,
					Component.text(itemStack.getType().getMaxDurability() - originalDamage.getDamage()),
					Component.text(itemStack.getType().getMaxDurability())
				).decoration(TextDecoration.ITALIC, false));
			}
			vanityMeta.lore(lore);

			// copy attributes
			for (EquipmentSlot s : EquipmentSlot.values()) {
				vanityMeta.removeAttributeModifier(s);
			}
			if (originalMeta != null) {
				for (Map.Entry<Attribute, AttributeModifier> entry : originalMeta.getAttributeModifiers(equipmentSlot).entries()) {
					vanityMeta.addAttributeModifier(entry.getKey(), entry.getValue());
				}
			}

			// copy enchantments
			vanityMeta.getEnchants().keySet().forEach(vanityMeta::removeEnchant);
			if (originalMeta != null) {
				originalMeta.getEnchants().forEach((ench, level) -> vanityMeta.addEnchant(ench, level, true));
			}

			// merge flags
			if (originalMeta != null) {
				vanityMeta.addItemFlags(originalMeta.getItemFlags().toArray(ItemFlag[]::new));
			}
			vanityMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // hide vanilla values like banner patterns

			itemStack.setType(vanityItem.getType());
			itemStack.setItemMeta(vanityMeta);
			VirtualItemsReplacer.markVirtual(itemStack);

			if (invisible) {
				NBTItem nbt = new NBTItem(itemStack, true);
				nbt.addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(VanityManager.INVISIBLE_NBT_KEY, true);
			}
		}
	}

}
