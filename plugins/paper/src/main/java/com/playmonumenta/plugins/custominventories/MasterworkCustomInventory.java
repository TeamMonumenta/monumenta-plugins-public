package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class MasterworkCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final Material ORANGE_FILLER = Material.ORANGE_STAINED_GLASS_PANE;
	private static final Material PURPLE_FILLER = Material.PURPLE_STAINED_GLASS_PANE;
	private static final Material A_FILLER = Material.RED_STAINED_GLASS_PANE;
	private static final Material B_FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
	private static final Material C_FILLER = Material.YELLOW_STAINED_GLASS_PANE;

	private static final List<ItemStack> mInvalidItems;
	private static final ItemStack mUpgradeItem;
	private static final ItemStack mUpgradeItemSeven;
	private static final ItemStack mNoPossibleUpgradeItem;
	private static final ItemStack mFullUpgradeA;
	private static final ItemStack mFullUpgradeB;
	private static final ItemStack mFullUpgradeC;
	private static final ItemStack mRefundItem;
	private static final ItemStack mBalanceRefundItem;
	private static final ItemStack mBackItem;
	private static final ItemStack mPreviewItem;

	private static final ImmutableList<EquipmentSlot> SLOT_ORDER = ImmutableList.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);

	private static final String PERMISSSION = "monumenta.masterwork";

	private final Map<Integer, ItemClicked> mMapFunction;
	private int mRowSelected;
	private boolean mIsPreview = false;
	private static final HashMap<Integer, int[]> mPatternMap = new HashMap<>();
	private int mMagicRow = Integer.MAX_VALUE;


	static {
		mInvalidItems = Stream.of("helmet", "chestplate", "leggings", "boots", "main hand", "off hand")
			.map(s -> GUIUtils.createBasicItem(Material.ARMOR_STAND, "Invalid Item", NamedTextColor.GRAY, true, "Your " + s + " can't be upgraded.", NamedTextColor.DARK_GRAY)).toList();

		//Standard Level
		mUpgradeItem = GUIUtils.createBasicItem(Material.RAW_IRON, "Enhance Item", TextColor.fromHexString("#FFAA00"), true, "Click to view next upgrade and associated costs.");

		//Legendary Level
		mUpgradeItemSeven = GUIUtils.createBasicItem(Material.RAW_GOLD, "Enhance Item", TextColor.fromHexString("#FFAA00"), true, "Click to view next upgrade and associated costs.");

		//Limit Level
		mNoPossibleUpgradeItem = GUIUtils.createBasicItem(Material.NETHERITE_INGOT, "Masterwork Limit Reached", TextColor.fromHexString("#5D2D87"), true, "This item cannot be upgraded at the moment. Click to view previous masterwork levels.");

		//Max Level Reached A
		mFullUpgradeA = GUIUtils.createBasicItem(Material.RED_DYE, "Congratulations!", TextColor.fromHexString("#D02E28"), true, "You've reached the max Masterwork level on this item.");

		//Max Level Reached B
		mFullUpgradeB = GUIUtils.createBasicItem(Material.LIGHT_BLUE_DYE, "Congratulations!", TextColor.fromHexString("#4AC2E5"), "You've reached the max Masterwork level on this item.");

		//Max Level Reached C
		mFullUpgradeC = GUIUtils.createBasicItem(Material.YELLOW_DYE, "Congratulations!", TextColor.fromHexString("#FFFA75"), true, "You've reached the max Masterwork level on this item.");

		//Refund
		mRefundItem = GUIUtils.createBasicItem(Material.GRINDSTONE, "Refund Legendary Upgrade", NamedTextColor.GRAY, true, "Click to refund Legendary upgrade to Epic level. This refunds 100% of the Location Materials and 75% of the Augments.");

		//Nerf Refund
		mBalanceRefundItem = GUIUtils.createBasicItem(Material.GRINDSTONE, "Refund Upgrade", NamedTextColor.GRAY, true, "Click to refund this item's Masterwork costs. You will get 100% of the materials back.");

		mPreviewItem = GUIUtils.createBasicItem(Material.CHORUS_FLOWER, "Preview Masterwork Levels", TextColor.fromHexString("#FFAA00"), true);

		mPatternMap.put(0, new int[] {});
		mPatternMap.put(1, new int[] {22});
		mPatternMap.put(2, new int[] {31, 22});
		mPatternMap.put(3, new int[] {32, 31, 30});
		mPatternMap.put(4, new int[] {32, 31, 30, 22});
		mPatternMap.put(5, new int[] {40, 32, 31, 30, 22});

		// Back item
		mBackItem = GUIUtils.createBasicItem(Material.STRING, "Back", NamedTextColor.RED, true);
	}

	public MasterworkCustomInventory(Player owner) {
		super(owner, 54, "Masterwork");
		mMapFunction = new HashMap<>();
		mRowSelected = 99;
		if (!owner.hasPermission(PERMISSSION)) {
			owner.sendMessage(Component.text("Masterworking is currently unavailable.", NamedTextColor.RED));
			close();
			return;
		}
		loadInv(owner);
	}

	private void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();

		if (mIsPreview) {
			setUpPreview(player);
		} else if (mRowSelected == 99) {
			loadMasterworkPage(player);
		} else {
			loadMasterworkPath(player);
		}

		GUIUtils.fillWithFiller(mInventory);
	}


	private void loadMasterworkPath(Player p) {
		EquipmentSlot equipmentSlot = SLOT_ORDER.get(mRowSelected);
		ItemStack item = p.getInventory().getItem(equipmentSlot);
		MasterworkUtils.MasterworkCost masterworkCost = MasterworkUtils.getMasterworkCost(item);
		Masterwork current = ItemStatUtils.getMasterwork(item);
		if (current == Masterwork.VI) {
			mInventory.setItem(4, item);

			MasterworkUtils.MasterworkCostLevel costA = masterworkCost.get(Masterwork.VIIA);
			MasterworkUtils.MasterworkCostLevel costB = masterworkCost.get(Masterwork.VIIB);
			MasterworkUtils.MasterworkCostLevel costC = masterworkCost.get(Masterwork.VIIC);

			// Generate new item
			ItemStack newItemA = MasterworkUtils.preserveModified(item, InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(),
					NamespacedKeyUtils.fromString(MasterworkUtils.getSevenItemPath(item, Masterwork.VIIA))));
			ItemStack newItemB = MasterworkUtils.preserveModified(item, InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(),
					NamespacedKeyUtils.fromString(MasterworkUtils.getSevenItemPath(item, Masterwork.VIIB))));
			ItemStack newItemC = MasterworkUtils.preserveModified(item, InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(),
					NamespacedKeyUtils.fromString(MasterworkUtils.getSevenItemPath(item, Masterwork.VIIC))));

			ItemStack newItemAButBetter = newItemA.clone();
			ItemStack newItemBButBetter = newItemB.clone();
			ItemStack newItemCButBetter = newItemC.clone();
			newItemAButBetter.lore(getEditedLoreDiff(newItemA, item));
			newItemBButBetter.lore(getEditedLoreDiff(newItemB, item));
			newItemCButBetter.lore(getEditedLoreDiff(newItemC, item));

			mInventory.setItem(39, newItemAButBetter);
			mInventory.setItem(40, newItemBButBetter);
			mInventory.setItem(41, newItemCButBetter);
			// Fill in mid material + cost
			ItemStack upgradeIconA = createCostItem(p, Material.RED_DYE, "Enhance Item (Fortitude)", TextColor.fromHexString("#D02E28"), costA);
			mInventory.setItem(21, upgradeIconA);
			mMapFunction.put(21, (player, inventory, slot) -> {
				attemptUpgrade(p, equipmentSlot, newItemA, costA);
			});

			ItemStack upgradeIconB = createCostItem(p, Material.LIGHT_BLUE_DYE, "Enhance Item (Potency)", TextColor.fromHexString("#4AC2E5"), costB);
			mInventory.setItem(22, upgradeIconB);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, equipmentSlot, newItemB, costB);
			});

			ItemStack upgradeIconC = createCostItem(p, Material.YELLOW_DYE, "Enhance Item (Alacrity)", TextColor.fromHexString("#FFFA75"), costC);
			mInventory.setItem(23, upgradeIconC);
			mMapFunction.put(23, (player, inventory, slot) -> {
				attemptUpgrade(p, equipmentSlot, newItemC, costC);
			});

			mInventory.setItem(49, mPreviewItem);
			mMapFunction.put(49, (player, inventory, slot) -> {
				mIsPreview = true;
			});

			mInventory.setItem(0, mBackItem);
			mMapFunction.put(0, (player, inventory, slot) -> {
				mRowSelected = 99;
			});

			// Fill in aesthetics
			fillWithColoredJunk(13, ORANGE_FILLER);
			fillWithColoredJunk(30, A_FILLER);
			fillWithColoredJunk(31, B_FILLER);
			fillWithColoredJunk(32, C_FILLER);
			fillWithColoredJunk(10, ORANGE_FILLER);
			fillWithColoredJunk(19, ORANGE_FILLER);
			fillWithColoredJunk(37, ORANGE_FILLER);
			fillWithColoredJunk(16, ORANGE_FILLER);
			fillWithColoredJunk(34, ORANGE_FILLER);
			fillWithColoredJunk(43, ORANGE_FILLER);
		} else {
			// Generate base item
			mInventory.setItem(4, item);
			MasterworkUtils.MasterworkCostLevel cost = masterworkCost.get(current.next());

			// Generate new item
			ItemStack newItem = MasterworkUtils.preserveModified(item, InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(),
					NamespacedKeyUtils.fromString(MasterworkUtils.getNextItemPath(item))));
			ItemStack newItemButBetter = newItem.clone();
			List<Component> updatedLore = getEditedLoreDiff(newItemButBetter, item);
			newItemButBetter.lore(updatedLore);
			mInventory.setItem(40, newItemButBetter);
			// Fill in mid material + cost
			ItemStack upgradeIcon = createCostItem(p, Material.RAW_IRON, "Enhance Item", TextColor.fromHexString("#FFAA00"), cost);
			mInventory.setItem(22, upgradeIcon);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, equipmentSlot, newItem, cost);
			});

			mInventory.setItem(49, mPreviewItem);
			mMapFunction.put(49, (player, inventory, slot) -> {
				mIsPreview = true;
			});

			mInventory.setItem(0, mBackItem);
			mMapFunction.put(0, (player, inventory, slot) -> {
				mRowSelected = 99;
			});
			// Fill in aesthetics
			fillWithColoredJunk(13, ORANGE_FILLER);
			fillWithColoredJunk(31, ORANGE_FILLER);
			fillWithColoredJunk(10, ORANGE_FILLER);
			fillWithColoredJunk(19, ORANGE_FILLER);
			fillWithColoredJunk(37, ORANGE_FILLER);
			fillWithColoredJunk(16, ORANGE_FILLER);
			fillWithColoredJunk(34, ORANGE_FILLER);
			fillWithColoredJunk(43, ORANGE_FILLER);
		}
	}

	private void setUpPreview(Player p) {
		ItemStack item = p.getInventory().getItem(SLOT_ORDER.get(Math.min(mMagicRow, mRowSelected)));
		mMapFunction.put(0, (player, inventory, slot) -> {
			mIsPreview = false;
			mMagicRow = Integer.MAX_VALUE;
		});
		List<ItemStack> allMasterworks = MasterworkUtils.getAllMasterworks(item, p);

		int tiers = allMasterworks.size();

		//Only true when it is an epic, by design (or only has tiers above the max)
		if (tiers == 0) {
			mMapFunction.get(0).run(p, mInventory, 0);
			loadInv(p);
			return;
		}

		GUIUtils.fillWithFiller(mInventory);
		mInventory.setItem(0, mBackItem);


		int j = allMasterworks.size() - 1;
		for (int i : mPatternMap.getOrDefault(tiers, new int[0])) {
			ItemStack currentItem = allMasterworks.get(j);
			if (j != 0) {
				ItemStack previousItem = allMasterworks.get(j - 1);
				List<Component> currentLore = getEditedLoreDiff(currentItem, previousItem);
				currentItem.lore(currentLore);
			}

			mInventory.setItem(i, currentItem);
			j--;
		}
	}

	private List<Component> getEditedLoreDiff(ItemStack currentItem, ItemStack previousItem) {
		DecimalFormat decimalFormat = new DecimalFormat("0.#");
		List<Component> currentLore = new ArrayList<>(currentItem.lore());
		List<Component> currentLoreDiff = new ArrayList<>(currentItem.lore());
		currentLoreDiff.removeAll(previousItem.lore());
		TextColor color = TextColor.color(255, 215, 0);

		//Go through each diffed string. Should only be Enchants, Attributes, Tier and masterwork stars (which will get ignored).
		for (Component component : currentLoreDiff) {
			String contentString = MessagingUtils.plainText(component);
			EnchantmentType enchantmentType = EnchantmentType.getEnchantmentType(contentString);
			if (enchantmentType != null) {
				//Enchantment is one level
				if (ItemStatUtils.hasEnchantment(previousItem, enchantmentType)) {
					//Previous had it
					continue;
				} else {
					//Not one level -> one level == 1
					Component appended = Component.text(" (+");
					appended = appended.append(Component.text("I)"));
					appended = appended.color(color);
					appended = component.append(appended);
					currentLore.set(currentLore.indexOf(component), appended);
					continue;
				}
			} else {
				//Enchantment is not one level
				int k = contentString.lastIndexOf(" ");
				if (k == -1) {
					//Not an enchant string at all.
					continue;
				}
				String enchant = contentString.substring(0, k);
				enchantmentType = EnchantmentType.getEnchantmentType(enchant);
				if (enchantmentType != null) {
					int beforeLevel = ItemStatUtils.getEnchantmentLevel(previousItem, enchantmentType);
					int afterLevel = ItemStatUtils.getEnchantmentLevel(currentItem, enchantmentType);
					int diff = afterLevel - beforeLevel;
					if (diff != 0) {
						Component appended = Component.text(" (+");
						String romanNumeral = StringUtils.toRoman(diff);
						appended = appended.append(Component.text(romanNumeral + ")"));
						appended = appended.color(TextColor.color(color));
						appended = component.append(appended);
						currentLore.set(currentLore.indexOf(component), appended);
						continue;
					}
				}
			}

			//All attribute lines have digits in them.
			if (contentString.replaceAll("[0-9]", "").equals(contentString)) {
				continue;
			}

			//Remove everything but the name
			String attributeString = contentString.replaceAll("[0-9%-+.]", "");
			Operation operation;

			//Edge cases. This system was not built with this stuff in mind. Attribute names != what is actually on the item
			if (contentString.contains("%")) {
				operation = Operation.MULTIPLY;
				if ((AttributeType.getAttributeType(attributeString) == null)) {
					attributeString = attributeString.concat("Multiply");
				}
			} else {
				operation = Operation.ADD;
				if ((AttributeType.getAttributeType(attributeString) == null)) {
					attributeString = attributeString.concat("Add");
				}
			}

			//Check if it is an attribute.
			AttributeType attributeType = AttributeType.getAttributeType(attributeString);
			if (attributeType != null) {
				//I don't know a good way of checking if something has slots or not, so I have to loop over every single one.
				for (Slot slot : Slot.values()) {
					double afterValue = ItemStatUtils.getAttributeAmount(currentItem, attributeType, operation, slot);
					if (afterValue == 0) {
						continue;
					}
					//If its in beforeValue but not afterValue well too bad where am I going to put it anyway??? figure it out
					double beforeValue = ItemStatUtils.getAttributeAmount(previousItem, attributeType, operation, slot);
					//Even if it is a new attribute, it not be 0. Essentially it will only be false if they are the same value.
					if (afterValue - beforeValue != 0) {
						double diff = afterValue - beforeValue;
						diff = Math.round(diff * 100) / (operation.equals(Operation.MULTIPLY) ? 1.0 : 100.0);
						Component appended = Component.text(" (+");
						appended = appended.append(Component.text(decimalFormat.format(diff) + (operation.equals(Operation.MULTIPLY) ? "%" : "") + ")"));
						appended = appended.color(color);
						appended = component.append(appended);
						currentLore.set(currentLore.indexOf(component), appended);
					}
				}
			}
		}

		return currentLore;
	}

	private void attemptUpgrade(Player p, EquipmentSlot equipmentSlot, ItemStack nextItem, MasterworkUtils.MasterworkCostLevel cost) {
		attemptUpgrade(p, p.getInventory().getItem(equipmentSlot), nextItem, cost);
	}

	private void attemptUpgrade(Player p, ItemStack item, ItemStack nextItem, MasterworkUtils.MasterworkCostLevel cost) {
		if (item.getAmount() > 1) {
			p.sendMessage(Component.text("You cannot upgrade stacked items.", NamedTextColor.RED));
			return;
		}

		Masterwork next = ItemStatUtils.getMasterwork(nextItem);
		if (MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) >
			MasterworkUtils.getMasterworkAsInt(next)) {
			cost.tryPayCost(p, item, true, next);
			item.setType(nextItem.getType());
			item.setItemMeta(nextItem.getItemMeta());
			ItemUpdateHelper.generateItemStats(item);
			p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.f, 1.f);
			return;
		}

		try {
			if (cost.tryPayCost(p, item, false, next)) {
				item.setType(nextItem.getType());
				item.setItemMeta(nextItem.getItemMeta());
				ItemUpdateHelper.generateItemStats(item);
				MasterworkUtils.animate(p, ItemStatUtils.getMasterwork(nextItem));
			} else {
				p.sendMessage(Component.text("You don't have enough currency for this upgrade.", NamedTextColor.RED));
			}
			mRowSelected = 99;
		} catch (Exception e) {
			p.sendMessage(Component.text("If you see this message please contact a mod! (Error in upgrade)", NamedTextColor.RED));
			e.printStackTrace();
		}
	}

	private void loadMasterworkPage(Player ply) {
		for (int row = 0; row < 6; row++) {
			EquipmentSlot equipmentSlot = SLOT_ORDER.get(row);
			ItemStack item = ply.getInventory().getItem(equipmentSlot);
			if (MasterworkUtils.isMasterwork(item)) {
				final int rowF = row;

				ItemStack clone = item.clone();
				GUIUtils.setPlaceholder(clone);
				mInventory.setItem(rowF * 9, clone);

				Masterwork m = ItemStatUtils.getMasterwork(item);
				int currMasterwork = MasterworkUtils.getMasterworkAsInt(m);

				// Case where upgrade possible
				if (currMasterwork >= 0 && currMasterwork < Masterwork.CURRENT_MAX_MASTERWORK && currMasterwork != 6) {
					mInventory.setItem((row * 9) + currMasterwork + 2, mUpgradeItem);
					mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
						mRowSelected = rowF;
					});
					for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
						fillWithColoredJunk(i, ORANGE_FILLER);
					}
					// Case where upgrade locked
				} else if (currMasterwork >= Masterwork.CURRENT_MAX_MASTERWORK && currMasterwork != 7) {
					mInventory.setItem((row * 9) + currMasterwork + 2, mNoPossibleUpgradeItem);
					mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
						p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 1.f, 1.f);
						mIsPreview = true;
						mMagicRow = rowF;
					});
					for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
						fillWithColoredJunk(i, PURPLE_FILLER);
					}
					// Case where at M6
				} else if (currMasterwork == 6 && Masterwork.CURRENT_MAX_MASTERWORK == 7) {
					mInventory.setItem((row * 9) + currMasterwork + 2, mUpgradeItemSeven);
					mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
						mRowSelected = rowF;
					});
					for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
						fillWithColoredJunk(i, ORANGE_FILLER);
					}
					// Case where complete
				} else if (currMasterwork == 7) {
					MasterworkUtils.MasterworkCost masterworkCost = MasterworkUtils.getMasterworkCost(item);
					MasterworkUtils.MasterworkCostLevel costA = masterworkCost.get(Masterwork.VIIA);
					MasterworkUtils.MasterworkCostLevel costB = masterworkCost.get(Masterwork.VIIB);
					MasterworkUtils.MasterworkCostLevel costC = masterworkCost.get(Masterwork.VIIC);

					if (m == Masterwork.VIIA) {
						mInventory.setItem((row * 9) + 8, mFullUpgradeA);
						if (!ServerProperties.getMasterworkRefundEnabled()) {
							mInventory.setItem((row * 9) + 1, mRefundItem);
						}
						mMapFunction.put((row * 9) + 1, (player, inventory, slot) -> {
							ItemStack equippedItem = ply.getInventory().getItem(equipmentSlot);
							ItemStack newItem = MasterworkUtils.preserveModified(equippedItem,
								InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
									NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(equippedItem))));
							attemptUpgrade(player, equippedItem, newItem, costA);
						});
						for (int i = (row * 9) + 2; i < (row * 9) + 8; i++) {
							fillWithColoredJunk(i, A_FILLER);
						}
					} else if (m == Masterwork.VIIB) {
						mInventory.setItem((row * 9) + 8, mFullUpgradeB);
						if (!ServerProperties.getMasterworkRefundEnabled()) {
							mInventory.setItem((row * 9) + 1, mRefundItem);
						}
						mMapFunction.put((row * 9) + 1, (player, inventory, slot) -> {
							ItemStack equippedItem = ply.getInventory().getItem(equipmentSlot);
							ItemStack newItem = MasterworkUtils.preserveModified(equippedItem,
								InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
									NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(equippedItem))));
							attemptUpgrade(player, equippedItem, newItem, costB);
						});
						for (int i = (row * 9) + 2; i < (row * 9) + 8; i++) {
							fillWithColoredJunk(i, B_FILLER);
						}
					} else {
						mInventory.setItem((row * 9) + 8, mFullUpgradeC);
						if (!ServerProperties.getMasterworkRefundEnabled()) {
							mInventory.setItem((row * 9) + 1, mRefundItem);
						}
						mMapFunction.put((row * 9) + 1, (player, inventory, slot) -> {
							ItemStack equippedItem = ply.getInventory().getItem(equipmentSlot);
							ItemStack newItem = MasterworkUtils.preserveModified(equippedItem,
								InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
									NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(equippedItem))));
							attemptUpgrade(player, equippedItem, newItem, costC);
						});
						for (int i = (row * 9) + 2; i < (row * 9) + 8; i++) {
							fillWithColoredJunk(i, C_FILLER);
						}
					}
				}

				if (ServerProperties.getMasterworkRefundEnabled()) {
					ItemStack baseMasterwork = MasterworkUtils.getBaseMasterwork(item, ply);
					if (baseMasterwork != null && ItemStatUtils.getMasterwork(item) != ItemStatUtils.getMasterwork(baseMasterwork)) {
						mInventory.setItem((row * 9) + 1, mBalanceRefundItem);
						mMapFunction.put((row * 9) + 1, (player, inventory, slot) -> {
							ItemStack equippedItem = ply.getInventory().getItem(equipmentSlot);
							ItemStack newItem = MasterworkUtils.preserveModified(equippedItem,
								InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
									NamespacedKeyUtils.fromString(MasterworkUtils.getPrevItemPath(equippedItem))));
							attemptUpgrade(player, equippedItem, newItem, MasterworkUtils.getMasterworkCost(equippedItem).get(ItemStatUtils.getMasterwork(equippedItem)));
						});
					}
				}

			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				mInventory.setItem((row * 9), invalidItem);
			}
		}
	}

	private void fillWithColoredJunk(int slot, Material filler) {
		ItemStack junk = new ItemStack(filler, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);
		mInventory.setItem(slot, junk);
	}

	private ItemStack createCostItem(Player player, Material mat, String name, TextColor color, MasterworkUtils.MasterworkCostLevel cost) {
		Component lore = Component.text("To enhance the selected item, you will need ", NamedTextColor.DARK_GRAY);
		for (String str : cost.getCostStringList(player)) {
			lore = lore.append(Component.text(str + " ", NamedTextColor.DARK_GRAY));
		}
		return GUIUtils.createBasicItem(mat, 1, name, color, true, lore, 30, true);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		Inventory clickedInventory = event.getClickedInventory();
		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);

		if (event.isShiftClick()) {
			return;
		}

		if (!mInventory.equals(clickedInventory)) {
			return;
		}

		ItemClicked itemClicked = mMapFunction.get(slot);
		if (itemClicked == null) {
			return;
		}

		if (!mIsPreview) {
			if (mRowSelected == 99) {
				int row = slot / 9;
				ItemStack slotItem = mInventory.getItem(row * 9);
				if (!GUIUtils.isPlaceholder(slotItem)) {
					return;
				}

				// Compare the equipped item to the one in the GUI
				if (slotItem == null || !checkSimilar(player, slotItem, row)) {
					loadInv(player);
					return;
				}
			} else if (mRowSelected < 6) {
				ItemStack guiItem = mInventory.getItem(4);
				if (guiItem == null || !checkSimilar(player, guiItem, mRowSelected)) {
					return;
				}
			}
		}

		itemClicked.run(player, clickedInventory, slot);

		loadInv(player);

	}

	private boolean checkSimilar(Player player, ItemStack guiItem, int equipmentSlot) {
		ItemStack clone1 = guiItem.clone();
		GUIUtils.setPlaceholder(clone1);

		ItemStack clone2 = player.getInventory().getItem(SLOT_ORDER.get(equipmentSlot)).clone();
		GUIUtils.setPlaceholder(clone2);

		return clone1.isSimilar(clone2);
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			PlayerTracking.getInstance().updateItemSlotProperties(player, player.getInventory().getHeldItemSlot());
			PlayerTracking.getInstance().updateItemSlotProperties(player, 36);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 37);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 38);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 39);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 40);
		}
	}
}
