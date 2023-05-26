package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSetSlotHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutWindowItemsHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.commands.VirtualFirmament;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
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
import com.playmonumenta.plugins.utils.PlayerUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;

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
			boolean isArmorOrOffhandSlot = (5 <= slot && slot <= 8) || slot == 45;
			boolean isHotbarOrOffhandSlot = 36 <= slot && slot < 46;

			// Virtual Firmament
			if (isHotbarOrOffhandSlot
				    && ItemUtils.isShulkerBox(itemStack.getType())
				    && VirtualFirmament.isEnabled(player)) {
				String plainName = ItemUtils.getPlainNameIfExists(itemStack);
				if ("Firmament".equals(plainName) || "Doorway from Eternity".equals(plainName)) {
					if (itemStack.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
						new NBTItem(itemStack, true).setString("FirmamentColor", shulkerBox.getColor() == null ? "undyed" : shulkerBox.getColor().name().toLowerCase(Locale.ROOT));
						int count = Arrays.stream(shulkerBox.getInventory().getContents())
							            .filter(Objects::nonNull)
							            .mapToInt(ItemStack::getAmount).sum();
						itemStack.setAmount(Math.max(1, Math.min(count, 64)));
					}
					itemStack.setType("Firmament".equals(plainName) ? Material.PRISMARINE : Material.BLACKSTONE);
					markVirtual(itemStack);
					return;
				}
			}

			// Virtual cooldown items for Infinity food
			for (Map.Entry<ItemStatUtils.EnchantmentType, Material> entry : FOOD_COOLDOWN_ITEMS.entrySet()) {
				if (ItemStatUtils.getEnchantmentLevel(itemStack, entry.getKey()) > 0
					    && mPlugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(entry.getKey()))) {
					itemStack.setType(entry.getValue());
					markVirtual(itemStack);
					return;
				}
			}

			// Worldshaper's Loom cooldown item
			if (WorldshaperOverride.isWorldshaperItem(itemStack)
				    && mPlugin.mEffectManager.hasEffect(player, WorldshaperOverride.COOLDOWN_SOURCE)) {
				itemStack.setType(WorldshaperOverride.COOLDOWN_ITEM);
				markVirtual(itemStack);
				return;
			}

			// Vanity
			if (isArmorOrOffhandSlot) {
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

			// Quivers: fake a larger stack size to not have them disappear from the inventory on the client when a bow is shot (until the next inventory update happens)
			if (ItemStatUtils.isQuiver(itemStack)) {
				if (itemStack.getAmount() == 1) {
					NBTCompoundList items = ItemStatUtils.addPlayerModified(new NBTItem(itemStack)).getCompoundList(ItemStatUtils.ITEMS_KEY);
					long amount = 0;
					for (NBTListCompound compound : items) {
						amount += ItemStatUtils.addPlayerModified(compound.addCompound("tag")).getLong(CustomContainerItemManager.AMOUNT_KEY);
						if (amount >= 64) {
							break;
						}
					}
					itemStack.setAmount(Math.max(1, (int) Math.min(amount, 64)));
					markVirtual(itemStack);
				}
				return;
			}

			// Alchemical Utensils
			if (isHotbarOrOffhandSlot && ItemUtils.isAlchemistItem(itemStack) && PlayerUtils.isAlchemist(player)) {
				AlchemistPotions potionsAbility = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);

				if (potionsAbility == null) {
					return;
				}

				int count = potionsAbility.getCharges();
				new NBTItem(itemStack, true).setInteger("CHARGES", count);
				itemStack.setAmount(Math.max(1, count));

				ItemUtils.modifyMeta(itemStack, meta -> {
					if (meta instanceof PotionMeta potionMeta) {
						double ratio = ((double) count) / potionsAbility.getMaxCharges();
						int color = (int) (ratio * 255);
						if (potionsAbility.isGruesomeMode()) {
							potionMeta.setColor(Color.fromRGB(color, 0, 0));
						} else {
							potionMeta.setColor(Color.fromRGB(0, color, 0));
						}
					}
					meta.displayName(ItemUtils.getDisplayName(itemStack).append(Component.text(" (" + count + ")")));
				});
				markVirtual(itemStack);
				return;
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

		// Purge nested "Items" key in "BlockEntityTag" to prevent NBT banning
		if (ItemUtils.isShulkerBox(itemStack.getType())) {
			nestedShulkerCheck(itemStack);
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

	/**
	 * Purge "Items" key in nested "BlockEntityTag" for shulker boxes
	 * Ensures mods such as Shulker Tooltip can still operate properly
	 */
	private static void nestedShulkerCheck(ItemStack itemStack) {
		// all shulkers should have a BlockEntityTag but check anyway
		NBTCompound blockEntityTag = new NBTItem(itemStack, true).getCompound("BlockEntityTag");
		if (blockEntityTag == null) {
			return;
		}
		NBTCompoundList items = blockEntityTag.getCompoundList("Items"); // this probably could be changed to ItemStatUtils.ITEMS_KEY but this is for vanilla
		if (items == null) {
			return;
		}
		Boolean foundNested = false;
		for (NBTCompound item : items) {
			// we don't know if this is a container with a loottable! so check it
			NBTCompound nestedBlockEntityTag = item.getCompound("BlockEntityTag");
			if (nestedBlockEntityTag == null) {
				continue;
			}
			// now check again if the Items tag exists
			NBTCompoundList nestedItems = nestedBlockEntityTag.getCompoundList("Items");
			if (nestedItems == null) {
				continue;
			}
			foundNested = true;
			nestedItems.clear();
		}
		if (foundNested) {
			markVirtual(itemStack);
		}
	}
}
