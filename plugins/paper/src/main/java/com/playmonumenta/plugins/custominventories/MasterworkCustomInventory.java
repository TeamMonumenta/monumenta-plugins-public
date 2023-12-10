package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

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

	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mUpgradeItem;
	private static final ItemStack mUpgradeItemSeven;
	private static final ItemStack mNoPossibleUpgradeItem;
	private static final ItemStack mFullUpgradeA;
	private static final ItemStack mFullUpgradeB;
	private static final ItemStack mFullUpgradeC;
	private static final ItemStack mRefundItem;
	private static final ItemStack mBalanceRefundItem;
	private static final ItemStack mBackItem;
	private static final ItemStack mPreviewItem = new ItemStack(Material.CHORUS_FLOWER);

	private final Map<Integer, ItemClicked> mMapFunction;
	private int mRowSelected = 99;
	private boolean mIsPreview = false;
	private static HashMap<Integer, int[]> mPatternMap = new HashMap<>();
	private int mMagicRow = Integer.MAX_VALUE;


	static {
		ItemStack invalidItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta meta = invalidItem.getItemMeta();
		meta.displayName(Component.text("Invalid item", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));

		List<Component> itemLore = new ArrayList<Component>();
		itemLore.add(Component.text("Your helmet can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);

		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your chestplate can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your leggings can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your boots can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your main hand can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your off hand can't be upgraded.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());

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

		mPatternMap.put(0, new int[] {});
		mPatternMap.put(1, new int[] {22});
		mPatternMap.put(2, new int[] {22, 31});
		mPatternMap.put(3, new int[] {30, 31, 32});
		mPatternMap.put(4, new int[] {22, 30, 31, 32});
		mPatternMap.put(5, new int[] {22, 30, 31, 32, 40});

		// Back item
		mBackItem = GUIUtils.createBasicItem(Material.STRING, "Back", NamedTextColor.RED, true);
	}

	public MasterworkCustomInventory(Player owner) {
		super(owner, 54, "Masterwork");
		mMapFunction = new HashMap<>();
		mRowSelected = 99;
		loadInv(owner);
	}

	private void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();
		PlayerInventory pi = player.getInventory();
		List<ItemStack> items = new ArrayList<>();
		items.addAll(Arrays.asList(pi.getArmorContents()));
		Collections.reverse(items);
		items.add(pi.getItemInMainHand());
		items.add(pi.getItemInOffHand());

		if (mIsPreview) {
			setUpPreview(items.get(mMagicRow < mRowSelected ? mMagicRow : mRowSelected), player);
		} else if (mRowSelected == 99) {
			loadMasterworkPage(items, player);
		} else {
			loadMasterworkPath(items.get(mRowSelected), player);
		}

		GUIUtils.fillWithFiller(mInventory);
	}


	private void loadMasterworkPath(ItemStack item, Player p) {
		MasterworkUtils.MasterworkCost masterworkCost = MasterworkUtils.getMasterworkCost(item);
		Masterwork current = ItemStatUtils.getMasterwork(item);
		if (current == Masterwork.VI) {
			// Generate base item
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
			mInventory.setItem(39, newItemA);
			mInventory.setItem(40, newItemB);
			mInventory.setItem(41, newItemC);
			// Fill in mid material + cost
			ItemStack upgradeIconA = new ItemStack(Material.RED_DYE);
			ItemMeta standardMetaA = upgradeIconA.getItemMeta();
			standardMetaA.displayName(Component.text("Enhance Item (Fortitude)", TextColor.fromHexString("#D02E28"))
					.decoration(TextDecoration.BOLD, true)
					.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLoreA = new ArrayList<>();
			itemLoreA.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			for (String str : costA.getCostStringList(p)) {
				itemLoreA.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaA.lore(itemLoreA);
			upgradeIconA.setItemMeta(standardMetaA);
			mInventory.setItem(21, upgradeIconA);
			mMapFunction.put(21, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemA, costA);
			});

			ItemStack upgradeIconB = new ItemStack(Material.LIGHT_BLUE_DYE);
			ItemMeta standardMetaB = upgradeIconB.getItemMeta();
			standardMetaB.displayName(Component.text("Enhance Item (Potency)", TextColor.fromHexString("#4AC2E5"))
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLoreB = new ArrayList<>();
			itemLoreB.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			for (String str : costB.getCostStringList(p)) {
				itemLoreB.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaB.lore(itemLoreB);
			upgradeIconB.setItemMeta(standardMetaB);
			mInventory.setItem(22, upgradeIconB);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemB, costB);
			});

			ItemStack upgradeIconC = new ItemStack(Material.YELLOW_DYE);
			ItemMeta standardMetaC = upgradeIconC.getItemMeta();
			standardMetaC.displayName(Component.text("Enhance Item (Alacrity)", TextColor.fromHexString("#FFFA75"))
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLoreC = new ArrayList<>();
			itemLoreC.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			for (String str : costC.getCostStringList(p)) {
				itemLoreC.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaC.lore(itemLoreC);
			upgradeIconC.setItemMeta(standardMetaC);
			mInventory.setItem(23, upgradeIconC);
			mMapFunction.put(23, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemC, costC);
			});

			ItemMeta previewMeta = mPreviewItem.getItemMeta();
			previewMeta.displayName(Component.text("Preview Masterwork Levels", TextColor.fromHexString("#FFAA00"))
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));
			mPreviewItem.setItemMeta(previewMeta);
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
			mInventory.setItem(40, newItem);
			// Fill in mid material + cost
			ItemStack upgradeIcon = new ItemStack(Material.RAW_IRON);
			ItemMeta standardMeta = upgradeIcon.getItemMeta();
			standardMeta.displayName(Component.text("Enhance Item", TextColor.fromHexString("#FFAA00"))
					.decoration(TextDecoration.BOLD, true)
					.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLore = new ArrayList<>();
			itemLore.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
					.decoration(TextDecoration.ITALIC, false));
			for (String str : cost.getCostStringList(p)) {
				itemLore.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMeta.lore(itemLore);
			upgradeIcon.setItemMeta(standardMeta);
			mInventory.setItem(22, upgradeIcon);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItem, cost);
			});

			ItemMeta previewMeta = mPreviewItem.getItemMeta();
			previewMeta.displayName(Component.text("Preview Masterwork Levels", TextColor.fromHexString("#FFAA00"))
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));
			mPreviewItem.setItemMeta(previewMeta);
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

	private void setUpPreview(ItemStack item, Player p) {
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

		int j = 0;
		for (int i : mPatternMap.getOrDefault(tiers, new int[0])) {
			mInventory.setItem(i, allMasterworks.get(j));
			j++;
		}
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

	private void loadMasterworkPage(List<ItemStack> items, Player ply) {
		int row = 0;
		for (ItemStack item : items) {
			if (item != null) {
				//check valid item
				if (MasterworkUtils.isMasterwork(item)) {
					final int rowF = row;

					//we need to delay this loading to make the item skin applied
					new BukkitRunnable() {
						@Override
						public void run() {
							ItemStack itemStack = new ItemStack(item.getType());
							ItemMeta meta = itemStack.getItemMeta();
							meta.displayName(item.getItemMeta().displayName()
								.decoration(TextDecoration.BOLD, true)
								.decoration(TextDecoration.ITALIC, false));
							meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
							itemStack.setItemMeta(meta);
							ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(item));
							mInventory.setItem(rowF * 9, itemStack);
						}
					}.runTaskLater(Plugin.getInstance(), 2);

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
								ItemStack newItem = MasterworkUtils.preserveModified(item,
										InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
												NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(item))));
								attemptUpgrade(player, item, newItem, costA);
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
								ItemStack newItem = MasterworkUtils.preserveModified(item,
										InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
												NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(item))));
								attemptUpgrade(player, item, newItem, costB);
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
								ItemStack newItem = MasterworkUtils.preserveModified(item,
									InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
										NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(item))));
								attemptUpgrade(player, item, newItem, costC);
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
								ItemStack newItem = MasterworkUtils.preserveModified(item,
									InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
										NamespacedKeyUtils.fromString(MasterworkUtils.getPrevItemPath(item))));
								attemptUpgrade(player, item, newItem, MasterworkUtils.getMasterworkCost(item).get(ItemStatUtils.getMasterwork(item)));
							});
						}
					}

				} else {
					ItemStack invalidItem = mInvalidItems.get(row);
					mInventory.setItem((row * 9), invalidItem);
				}
			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				mInventory.setItem((row * 9), invalidItem);
			}
			row++;
		}
	}


	private void fillWithColoredJunk(int slot, Material filler) {
		ItemStack junk = new ItemStack(filler, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);
		mInventory.setItem(slot, junk);
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
		itemClicked.run(player, clickedInventory, slot);

		loadInv(player);

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
