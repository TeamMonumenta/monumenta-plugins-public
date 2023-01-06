package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSetSlotHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutWindowItemsHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.VirtualFirmament;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.enchantments.IntoxicatingWarmth;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LiquidCourage;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.TemporalBender;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.overrides.WorldshaperOverride;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public class VirtualItemsReplacer extends PacketAdapter {

	public static final String IS_VIRTUAL_ITEM_NBT_KEY = "IsVirtualItem";

	// Map of Infinity Food Enchantments to their virtual cooldown items
	private static final ImmutableMap<ItemStatUtils.EnchantmentType, Material> FOOD_COOLDOWN_ITEMS = ImmutableMap.<ItemStatUtils.EnchantmentType, Material>builder()
		.put(ItemStatUtils.EnchantmentType.JUNGLES_NOURISHMENT, JunglesNourishment.COOLDOWN_ITEM)
		.put(ItemStatUtils.EnchantmentType.RAGE_OF_THE_KETER, RageOfTheKeter.COOLDOWN_ITEM)
		.put(ItemStatUtils.EnchantmentType.TEMPORAL_BENDER, TemporalBender.COOLDOWN_ITEM)
		.put(ItemStatUtils.EnchantmentType.INTOXICATING_WARMTH, IntoxicatingWarmth.COOLDOWN_ITEM)
		.put(ItemStatUtils.EnchantmentType.LIQUID_COURAGE, LiquidCourage.COOLDOWN_ITEM)
		.build();

	private final Plugin mPlugin;

	public VirtualItemsReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.OPEN_WINDOW_MERCHANT);
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			// Creative mode directly sends items to the server, so would break virtual items.
			// Spectators don't see items anyway so skip them.
			return;
		}
		PacketContainer packet = event.getPacket();
		if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
			PacketPlayOutWindowItemsHandle handle = PacketPlayOutWindowItemsHandle.createHandle(packet.getHandle());
			boolean isPlayerInventory = handle.getWindowId() == 0;
			List<ItemStack> items = handle.getItems();
			for (int i = 0; i < items.size(); i++) {
				ItemStack item = items.get(i);
				processItem(item, i, player, isPlayerInventory);
			}
		} else if (packet.getType().equals(PacketType.Play.Server.OPEN_WINDOW_MERCHANT)) {
			List<MerchantRecipe> recipeList = packet.getMerchantRecipeLists().read(0);
			for (int i = 0; i < recipeList.size(); i++) {
				MerchantRecipe recipe = recipeList.get(i);
				List<ItemStack> ingredients = recipe.getIngredients();
				for (ItemStack ingredient : ingredients) {
					processItem(ingredient, -1, player, false);
				}
				ItemStack result = recipe.getResult();
				processItem(result, -1, player, false);
				MerchantRecipe recipeCopy = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(),
					recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
				recipeCopy.setIngredients(ingredients);
				recipeList.set(i, recipeCopy);
			}
			packet.getMerchantRecipeLists().write(0, recipeList);
		} else { // PacketType.Play.Server.SET_SLOT
			PacketPlayOutSetSlotHandle handle = PacketPlayOutSetSlotHandle.createHandle(packet.getHandle());
			boolean isPlayerInventory = handle.getWindowId() == 0;
			processItem(handle.getItem(), handle.getSlot(), player, isPlayerInventory);
		}
	}

	private void processItem(ItemStack itemStack, int slot, Player player, boolean isPlayerInventory) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return;
		}

		if (isPlayerInventory) {
			// Virtual Firmament
			if (36 <= slot && slot < 46 // hotbar or offhand
				    && ItemUtils.isShulkerBox(itemStack.getType())
				    && VirtualFirmament.isEnabled(player)) {
				String plainName = ItemUtils.getPlainNameIfExists(itemStack);
				if ("Firmament".equals(plainName)) {
					itemStack.setType(Material.PRISMARINE);
					itemStack.setAmount(64);
					markVirtual(itemStack);
					return;
				} else if ("Doorway from Eternity".equals(plainName)) {
					itemStack.setType(Material.BLACKSTONE);
					itemStack.setAmount(64);
					markVirtual(itemStack);
					return;
				}
			}

			// Virtual cooldown items for Infinity food
			for (Map.Entry<ItemStatUtils.EnchantmentType, Material> entry : FOOD_COOLDOWN_ITEMS.entrySet()) {
				if (ItemStatUtils.getEnchantmentLevel(itemStack, entry.getKey()) > 0
					    && mPlugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(entry.getKey()))) {
					// Then we need to replace the item in the packet.
					itemStack.setType(entry.getValue());
					markVirtual(itemStack);
					return;
				}
			}

			if (WorldshaperOverride.isWorldshaperItem(itemStack)
				    && mPlugin.mEffectManager.hasEffect(player, WorldshaperOverride.COOLDOWN_SOURCE)) {
				// Then we need to replace the item in the packet.
				itemStack.setType(WorldshaperOverride.COOLDOWN_ITEM);
				markVirtual(itemStack);
				return;
			}

			// Vanity
			if ((5 <= slot && slot <= 8) || slot == 45) { // armor or offhand
				VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
				if (vanityData != null && vanityData.mSelfVanityEnabled) {
					EquipmentSlot equipmentSlot = switch (slot) {
						case 5 -> EquipmentSlot.HEAD;
						case 6 -> EquipmentSlot.CHEST;
						case 7 -> EquipmentSlot.LEGS;
						case 8 -> EquipmentSlot.FEET;
						default -> EquipmentSlot.OFF_HAND;
					};
					VanityManager.applyVanity(itemStack, vanityData, equipmentSlot, true);
				}
			}
		}

		// Lootboxes: don't send stored items to prevent NBT banning
		if (ChestUtils.isLootBox(itemStack)) {
			NBTCompound monumenta = new NBTItem(itemStack, true).getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta != null) {
				NBTCompound playerModified = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
				if (playerModified != null) {
					playerModified.removeKey(ItemStatUtils.ITEMS_KEY);
					markVirtual(itemStack);
				}
			}
		}

		// Custom shulker names
		if (ItemUtils.isShulkerBox(itemStack.getType())) {
			String prefix;
			String suffix;
			if (ItemStatUtils.getRegion(itemStack) == ItemStatUtils.Region.SHULKER_BOX) {
				prefix = "`";
				suffix = "´";
			} else if (ShulkerEquipmentListener.isOmnilockbox(itemStack)) {
				prefix = "Omnilockbox: ";
				suffix = "";
			} else if (ShulkerEquipmentListener.isEquipmentBox(itemStack)) {
				prefix = "Loadout: ";
				suffix = "";
			} else if (ShulkerEquipmentListener.isCharmBox(itemStack)) {
				prefix = "C.H.A.R.M.: ";
				suffix = "";
			} else {
				prefix = null;
				suffix = null;
			}
			if (prefix != null) {
				NBTCompound playerModified = ItemStatUtils.getPlayerModified(new NBTItem(itemStack));
				if (playerModified != null) {
					String customName = playerModified.getString(ItemStatUtils.PLAYER_CUSTOM_NAME_KEY);
					if (customName != null && !customName.isEmpty()) {
						ItemUtils.modifyMeta(itemStack, meta -> {
							Component existingName = meta.displayName();
							meta.displayName(Component.text(prefix + customName + suffix,
								existingName == null ? Style.style(NamedTextColor.WHITE, TextDecoration.BOLD, TextDecoration.ITALIC) : existingName.style()));
						});
						markVirtual(itemStack);
					}
				}
			}
		}
	}

	/**
	 * Checks if a given item is marked virtual. This is used to prevent making them real items in creative mode.
	 */
	public static boolean isVirtualItem(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return false;
		}
		NBTCompound monumenta = new NBTItem(itemStack).getCompound(ItemStatUtils.MONUMENTA_KEY);
		return monumenta != null && Boolean.TRUE.equals(monumenta.getBoolean(IS_VIRTUAL_ITEM_NBT_KEY));
	}

	public static void markVirtual(ItemStack item) {
		new NBTItem(item, true).addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(IS_VIRTUAL_ITEM_NBT_KEY, true);
	}

}
