package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSetSlotHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutWindowItemsHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
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
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.IntoxicatingWarmth;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LiquidCourage;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.TemporalBender;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.managers.LoadoutManager;
import com.playmonumenta.plugins.overrides.WorldshaperOverride;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.PotionMeta;

public class VirtualItemsReplacer extends PacketAdapter {

	public static final String IS_VIRTUAL_ITEM_NBT_KEY = "IsVirtualItem";

	// Map of Infinity Food Enchantments to their virtual cooldown items
	private static final ImmutableMap<EnchantmentType, Material> FOOD_COOLDOWN_ITEMS = ImmutableMap.<EnchantmentType, Material>builder()
		.put(EnchantmentType.JUNGLES_NOURISHMENT, JunglesNourishment.COOLDOWN_ITEM)
		.put(EnchantmentType.RAGE_OF_THE_KETER, RageOfTheKeter.COOLDOWN_ITEM)
		.put(EnchantmentType.TEMPORAL_BENDER, TemporalBender.COOLDOWN_ITEM)
		.put(EnchantmentType.INTOXICATING_WARMTH, IntoxicatingWarmth.COOLDOWN_ITEM)
		.put(EnchantmentType.LIQUID_COURAGE, LiquidCourage.COOLDOWN_ITEM)
		.put(EnchantmentType.GRAPPLING, Grappling.COOLDOWN_ITEM)
		.build();

	private final Plugin mPlugin;

	public VirtualItemsReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.OPEN_WINDOW_MERCHANT);
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
				processItemWindow(item, i, player, isPlayerInventory);
			}
		} else if (packet.getType().equals(PacketType.Play.Server.OPEN_WINDOW_MERCHANT)) {
			List<MerchantRecipe> recipeList = packet.getMerchantRecipeLists().read(0);
			for (int i = 0; i < recipeList.size(); i++) {
				MerchantRecipe recipe = recipeList.get(i);
				List<ItemStack> ingredients = recipe.getIngredients();
				for (ItemStack ingredient : ingredients) {
					processTradeMenu(ingredient, -1, player, false);
				}
				ItemStack result = recipe.getResult();
				processTradeMenu(result, -1, player, false);
				MerchantRecipe recipeCopy = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(),
					recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
				recipeCopy.setIngredients(ingredients);
				recipeList.set(i, recipeCopy);
			}
			packet.getMerchantRecipeLists().write(0, recipeList);
		} else { // PacketType.Play.Server.SET_SLOT
			PacketPlayOutSetSlotHandle handle = PacketPlayOutSetSlotHandle.createHandle(packet.getHandle());
			boolean isPlayerInventory = handle.getWindowId() == 0;
			processSetSlot(handle.getItem(), handle.getSlot(), player, isPlayerInventory);
		}
	}

	// these methods is to determine what packet lags the most/gets called more often in spark
	// this may get reworked in the future
	private void processSetSlot(ItemStack itemStack, int slot, Player player, boolean isPlayerInventory) {
		processItem(itemStack, slot, player, isPlayerInventory);
	}

	private void processItemWindow(ItemStack itemStack, int slot, Player player, boolean isPlayerInventory) {
		processItem(itemStack, slot, player, isPlayerInventory);
	}

	private void processTradeMenu(ItemStack itemStack, int slot, Player player, boolean isPlayerInventory) {
		processItem(itemStack, slot, player, isPlayerInventory);
	}

	// the actual processing event
	private void processItem(ItemStack itemStack, int slot, Player player, boolean isPlayerInventory) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return;
		}

		if (isPlayerInventory) {
			boolean isArmorOrOffhandSlot = (5 <= slot && slot <= 8) || slot == 45;
			boolean isHotbarOrOffhandSlot = 36 <= slot && slot < 46;

			if (isHotbarOrOffhandSlot) {
				// Virtual Firmament
				if (ItemUtils.isShulkerBox(itemStack.getType())
				    && VirtualFirmament.isEnabled(player)) {
					String plainName = ItemUtils.getPlainNameIfExists(itemStack);
					if ("Firmament".equals(plainName) || "Doorway from Eternity".equals(plainName)) {
						int blockCountInsideFirm = NBT.get(itemStack, nbt -> {
							int count = 0;
							ReadableNBTList<ReadWriteNBT> items = ItemUtils.getContainerItems(nbt);
							if (items != null) {
								for (ReadWriteNBT itemNBT : items) {
									if (itemNBT.hasTag("tag")) { // probably has lore
										continue;
									}
									String id = itemNBT.getString("id"); // minecraft:tag
									Byte amount = itemNBT.getByte("Count"); // count
									Material mat = Material.matchMaterial(id); // use bukkit's material system to check if it is a block
									// check copied from FirmanentOverride
									if (mat == null
										|| mat.isAir()
										|| ItemUtils.notAllowedTreeReplace.contains(mat)
										|| (!mat.isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(mat))) {
										continue;
									}
									if (mat.isBlock()) {
										count += amount;
									}
									if (count >= 64) { // if above 64, we know we have enough blocks
										break;
									}
								}
							}
							return count;
						});
						itemStack.setAmount(Math.max(1, Math.min(blockCountInsideFirm, 64)));
						// ? Add shulker box id here for resourcepack team when they need dyed support
						itemStack.setType("Firmament".equals(plainName) ? Material.PRISMARINE : Material.BLACKSTONE);
						markVirtual(itemStack);
						return;
					}
				}

				// Virtual cooldown items for Infinity food
				for (Map.Entry<EnchantmentType, Material> entry : FOOD_COOLDOWN_ITEMS.entrySet()) {
					if (mPlugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(entry.getKey())) && ItemStatUtils.getEnchantmentLevel(itemStack, entry.getKey()) > 0) {
						itemStack.setType(entry.getValue());
						markVirtual(itemStack);
						return;
					}
				}

				// Worldshaper's Loom cooldown item
				if (mPlugin.mEffectManager.hasEffect(player, WorldshaperOverride.COOLDOWN_SOURCE) && WorldshaperOverride.isWorldshaperItem(itemStack)) {
					itemStack.setType(WorldshaperOverride.COOLDOWN_ITEM);
					markVirtual(itemStack);
					return;
				}
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
					return;
				}
			}

			// Quivers: fake a larger stack size to not have them disappear from the inventory on the client when a bow is shot (until the next inventory update happens)
			if (ItemStatUtils.isQuiver(itemStack)) {
				if (itemStack.getAmount() == 1) {
					long amount = NBT.get(itemStack, nbt -> {
						long count = 0;
						ReadableNBTList<ReadWriteNBT> items = ItemStatUtils.getItemList(nbt);
						if (items == null) {
							return count;
						}
						for (ReadWriteNBT compound : items) {
							// this can be null
							ReadWriteNBT tag = compound.getCompound("tag");
							if (tag == null) {
								continue;
							}
							ReadableNBT playerTag = ItemStatUtils.getPlayerModified(tag);
							if (playerTag == null) {
								continue;
							}
							count += playerTag.getLong(CustomContainerItemManager.AMOUNT_KEY);
							if (count >= 64) {
								break;
							}
						}
						return count;
					});
					itemStack.setAmount(Math.max(1, (int) Math.min(amount, 64)));
					markVirtual(itemStack);
				}
				return;
			}

			// Alchemical Utensils
			if (isHotbarOrOffhandSlot && PlayerUtils.isAlchemist(player) && ItemUtils.isAlchemistItem(itemStack)) {
				AlchemistPotions potionsAbility = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);

				if (potionsAbility == null) {
					return;
				}

				int count = potionsAbility.getCharges();
				itemStack.setAmount(Math.max(1, count));

				NBT.modify(itemStack, nbt -> {
					nbt.modifyMeta((nbtr, meta) -> {
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
					nbt.setInteger("CHARGES", count);
					markVirtual(nbt);
				});
				return;
			}
		}

		// Custom shulker names
		if (ItemUtils.isShulkerBox(itemStack.getType())) {
			// add back if people are somehow getting themselves inventory banned with shulkers again
			// nestedContainerCheck(itemStack);
			String customName = ItemStatUtils.getPlayerCustomName(itemStack);
			if (customName != null) {
				String prefix;
				String suffix;
				if (ItemStatUtils.getRegion(itemStack) == Region.SHULKER_BOX) {
					prefix = "`";
					suffix = "´";
				} else {
					String lock = NBT.get(itemStack, ItemUtils::getContainerLock);
					if (lock != null && !lock.isBlank()) {
						if (lock.equals(ShulkerEquipmentListener.LOCK_STRING)) { // loadout lockboxes
							prefix = "Loadout: ";
							suffix = "";
						} else if (lock.equals(ShulkerEquipmentListener.CHARM_STRING)) { // charm boxes
							prefix = "C.H.A.R.M.: ";
							suffix = "";
						} else {
							prefix = null;
							suffix = null;
						}
					} else if (LoadoutManager.isEquipmentStorageBox(itemStack)) { // equipment cases
						prefix = "Case: ";
						suffix = "";
					} else {
						prefix = null;
						suffix = null;
					}
				}
				if (prefix != null && !customName.isEmpty()) {
					NBT.modify(itemStack, nbt -> {
						nbt.modifyMeta((nbtr, meta) -> {
							Component existingName = meta.displayName();
							meta.displayName(Component.text(prefix + customName + suffix,
								existingName == null ? Style.style(NamedTextColor.WHITE, TextDecoration.BOLD, TextDecoration.ITALIC) : existingName.style()));
						});
						markVirtual(nbt);
					});
					return;
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
		return NBT.get(itemStack, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			return monumenta != null && Boolean.TRUE.equals(monumenta.getBoolean(IS_VIRTUAL_ITEM_NBT_KEY));
		});
	}

	public static void markVirtual(ItemStack item) {
		NBT.modify(item, nbt -> {
			markVirtual(nbt);
		});
	}

	public static void markVirtual(ReadWriteNBT nbt) {
		nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(IS_VIRTUAL_ITEM_NBT_KEY, true);
	}

	public static void nestedContainerCheck(ItemStack itemStack) {
		NBT.modify(itemStack, nbt -> {
			// new chests/shulkers don't have a BlockEntityTag
			ReadWriteNBT blockEntityTag = nbt.getCompound("BlockEntityTag");
			if (blockEntityTag == null) {
				return;
			}
			ReadWriteNBTCompoundList items = blockEntityTag.getCompoundList("Items");
			if (items == null || items.isEmpty()) {
				return;
			}
			boolean found = false;
			for (ReadWriteNBT item : items) {
				ReadWriteNBT tag = item.getCompound("tag");
				if (tag == null) {
					continue;
				}
				// we can attempt to remove the tag without checking if it exists
				tag.removeKey("BlockEntityTag");
				found = true;
			}
			if (found) {
				markVirtual(nbt);
			}
		});
	}
}
