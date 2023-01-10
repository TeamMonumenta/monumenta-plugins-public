package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Masterwork;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils.MasterworkCost;
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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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

	//TODO: Replace with next max level
	private static final int MAX_MASTERWORK_LEVEL = 3;

	private static final int MAX_LORE_LENGTH = 30;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material ORANGE_FILLER = Material.ORANGE_STAINED_GLASS_PANE;
	private static final Material PURPLE_FILLER = Material.PURPLE_STAINED_GLASS_PANE;
	private static final Material A_FILLER = Material.RED_STAINED_GLASS_PANE;
	private static final Material B_FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
	private static final Material C_FILLER = Material.YELLOW_STAINED_GLASS_PANE;

	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mUpgradeItem = new ItemStack(Material.RAW_IRON);
	private static final ItemStack mUpgradeItemSeven = new ItemStack(Material.RAW_GOLD);
	private static final ItemStack mNoPossibleUpgradeItem = new ItemStack(Material.NETHERITE_INGOT);
	private static final ItemStack mFullUpgradeA = new ItemStack(Material.RED_DYE);
	private static final ItemStack mFullUpgradeB = new ItemStack(Material.LIGHT_BLUE_DYE);
	private static final ItemStack mFullUpgradeC = new ItemStack(Material.YELLOW_DYE);
	private static final ItemStack mRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mBalanceRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mBackItem = new ItemStack(Material.STRING);
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
		ItemMeta standardMeta = mUpgradeItem.getItemMeta();
		standardMeta.displayName(Component.text("Enhance Item", TextColor.fromHexString("#FFAA00"))
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(standardMeta, "Click to view next upgrade and associated costs.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mUpgradeItem.setItemMeta(standardMeta);

		//Legendary Level
		ItemMeta legendMeta = mUpgradeItemSeven.getItemMeta();
		legendMeta.displayName(Component.text("Enhance Item", TextColor.fromHexString("#FFAA00"))
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(legendMeta, "Click to view next upgrade and associated costs.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mUpgradeItemSeven.setItemMeta(legendMeta);

		//Limit Level
		ItemMeta limitMeta = mNoPossibleUpgradeItem.getItemMeta();
		limitMeta.displayName(Component.text("Masterwork Limit Reached", TextColor.fromHexString("#5D2D87"))
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(limitMeta, "This item cannot be upgraded at the moment. Click to view previous masterwork levels.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mNoPossibleUpgradeItem.setItemMeta(limitMeta);

		//Max Level Reached A
		ItemMeta maxMetaA = mFullUpgradeA.getItemMeta();
		maxMetaA.displayName(Component.text("Congratulations!", TextColor.fromHexString("#D02E28"))
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(maxMetaA, "You've reached the max Masterwork level on this item.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mFullUpgradeA.setItemMeta(maxMetaA);

		//Max Level Reached B
		ItemMeta maxMetaB = mFullUpgradeB.getItemMeta();
		maxMetaB.displayName(Component.text("Congratulations!", TextColor.fromHexString("#4AC2E5"))
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(maxMetaB, "You've reached the max Masterwork level on this item.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mFullUpgradeB.setItemMeta(maxMetaB);

		//Max Level Reached C
		ItemMeta maxMetaC = mFullUpgradeC.getItemMeta();
		maxMetaC.displayName(Component.text("Congratulations!", TextColor.fromHexString("#FFFA75"))
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(maxMetaC, "You've reached the max Masterwork level on this item.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mFullUpgradeC.setItemMeta(maxMetaC);

		//Refund
		ItemMeta refund = mRefundItem.getItemMeta();
		refund.displayName(Component.text("Refund Legendary Upgrade", NamedTextColor.GRAY)
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(refund, "Click to refund Legendary upgrade to Epic level. This refunds 100% of the Location Materials and 75% of the Augments.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mRefundItem.setItemMeta(refund);

		//Nerf Refund
		ItemMeta balancerefund = mBalanceRefundItem.getItemMeta();
		balancerefund.displayName(Component.text("Refund Upgrade", NamedTextColor.GRAY)
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(balancerefund, "Click to refund this item's Masterwork costs. You will get 100% of the materials back.", MAX_LORE_LENGTH, ChatColor.DARK_GRAY);
		mBalanceRefundItem.setItemMeta(balancerefund);

		mPatternMap.put(0, new int[] {});
		mPatternMap.put(1, new int[] {22});
		mPatternMap.put(2, new int[] {22, 31});
		mPatternMap.put(3, new int[] {30, 31, 32});
		mPatternMap.put(4, new int[] {22, 30, 31, 32});

		// Back item
		ItemMeta backMeta = mBackItem.getItemMeta();
		backMeta.displayName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true));
		mBackItem.setItemMeta(backMeta);
	}

	private static void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (!currentString.equals(defaultColor + "")) {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
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

		fillWithJunk();
	}


	private void loadMasterworkPath(ItemStack item, Player p) {
		if (ItemStatUtils.getMasterwork(item) == Masterwork.VI) {
			// Generate base item
			mInventory.setItem(4, item);

			String costStringA = ItemStatUtils.getLocation(item).getName() + "_7a";
			String costStringB = ItemStatUtils.getLocation(item).getName() + "_7b";
			String costStringC = ItemStatUtils.getLocation(item).getName() + "_7c";

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
			for (String str : MasterworkUtils.getCostStringList(MasterworkCost.getMasterworkCost(costStringA), p)) {
				itemLoreA.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaA.lore(itemLoreA);
			upgradeIconA.setItemMeta(standardMetaA);
			mInventory.setItem(21, upgradeIconA);
			mMapFunction.put(21, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemA, MasterworkCost.getMasterworkCost(costStringB));
			});

			ItemStack upgradeIconB = new ItemStack(Material.LIGHT_BLUE_DYE);
			ItemMeta standardMetaB = upgradeIconB.getItemMeta();
			standardMetaB.displayName(Component.text("Enhance Item (Potency)", TextColor.fromHexString("#4AC2E5"))
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLoreB = new ArrayList<>();
			itemLoreB.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			for (String str : MasterworkUtils.getCostStringList(MasterworkCost.getMasterworkCost(costStringB), p)) {
				itemLoreB.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaB.lore(itemLoreB);
			upgradeIconB.setItemMeta(standardMetaB);
			mInventory.setItem(22, upgradeIconB);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemB, MasterworkCost.getMasterworkCost(costStringB));
			});

			ItemStack upgradeIconC = new ItemStack(Material.YELLOW_DYE);
			ItemMeta standardMetaC = upgradeIconC.getItemMeta();
			standardMetaC.displayName(Component.text("Enhance Item (Alacrity)", TextColor.fromHexString("#FFFA75"))
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false));
			List<Component> itemLoreC = new ArrayList<>();
			itemLoreC.add(Component.text("To enhance the selected item, you will need", NamedTextColor.DARK_GRAY)
				.decoration(TextDecoration.ITALIC, false));
			for (String str : MasterworkUtils.getCostStringList(MasterworkCost.getMasterworkCost(costStringC), p)) {
				itemLoreC.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMetaC.lore(itemLoreC);
			upgradeIconC.setItemMeta(standardMetaC);
			mInventory.setItem(23, upgradeIconC);
			mMapFunction.put(23, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItemC, MasterworkCost.getMasterworkCost(costStringC));
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
			String costString = ItemStatUtils.getLocation(item).getName() + "_"
					+ (MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) + 1);

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
			for (String str : MasterworkUtils.getCostStringList(MasterworkCost.getMasterworkCost(costString), p)) {
				itemLore.add(Component.text(str, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
			standardMeta.lore(itemLore);
			upgradeIcon.setItemMeta(standardMeta);
			mInventory.setItem(22, upgradeIcon);
			mMapFunction.put(22, (player, inventory, slot) -> {
				attemptUpgrade(p, item, newItem, MasterworkCost.getMasterworkCost(costString));
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

		fillWithJunk();
		mInventory.setItem(0, mBackItem);

		int j = 0;
		for (int i : mPatternMap.getOrDefault(tiers, new int[0])) {
			mInventory.setItem(i, allMasterworks.get(j));
			j++;
		}
	}

	private void attemptUpgrade(Player p, ItemStack item, ItemStack nextItem, MasterworkCost cost) {
		if (item.getAmount() > 1) {
			p.sendMessage(ChatColor.RED + "You cannot upgrade stacked items.");
			return;
		}

		if (MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) >
			MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(nextItem))) {
			MasterworkUtils.payCost(cost, p, true);
			item.setItemMeta(nextItem.getItemMeta());
			ItemStatUtils.generateItemStats(item);
			p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1.f, 1.f);
			return;
		}

		try {
			if (MasterworkUtils.canPayCost(cost, p, false)) {
				MasterworkUtils.payCost(cost, p, false);
				item.setItemMeta(nextItem.getItemMeta());
				ItemStatUtils.generateItemStats(item);
				MasterworkUtils.animate(p, ItemStatUtils.getMasterwork(nextItem));
			} else {
				p.sendMessage(ChatColor.RED + "You don't have enough currency for this upgrade.");
			}
			mRowSelected = 99;
		} catch (Exception e) {
			p.sendMessage(ChatColor.RED + "If you see this message please contact a mod! (Error in upgrade)");
			e.printStackTrace();
		}
	}

	private void loadMasterworkPage(List<ItemStack> items, Player ply) {
		//load pannels for each item with the corresponding infusions.
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
					if (currMasterwork >= 0 && currMasterwork < MAX_MASTERWORK_LEVEL && currMasterwork != 6) {
						mInventory.setItem((row * 9) + currMasterwork + 2, mUpgradeItem);
						mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
							mRowSelected = rowF;
						});
						for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
							fillWithColoredJunk(i, ORANGE_FILLER);
						}
						// Case where upgrade locked
					} else if (currMasterwork >= MAX_MASTERWORK_LEVEL && currMasterwork != 7) {
						mInventory.setItem((row * 9) + currMasterwork + 2, mNoPossibleUpgradeItem);
						mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
							p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_FALL, 1.f, 1.f);
							mIsPreview = true;
							mMagicRow = rowF;
						});
						for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
							fillWithColoredJunk(i, PURPLE_FILLER);
						}
						// Case where at M6
					} else if (currMasterwork == 6 && MAX_MASTERWORK_LEVEL == 7) {
						mInventory.setItem((row * 9) + currMasterwork + 2, mUpgradeItemSeven);
						mMapFunction.put((row * 9) + currMasterwork + 2, (p, inventory, slot) -> {
							mRowSelected = rowF;
						});
						for (int i = (row * 9) + 1; i < (row * 9) + currMasterwork + 2; i++) {
							fillWithColoredJunk(i, ORANGE_FILLER);
						}
					// Case where complete
					} else if (currMasterwork == 7) {
						String costStringA = ItemStatUtils.getLocation(item).getName() + "_7a";
						String costStringB = ItemStatUtils.getLocation(item).getName() + "_7b";
						String costStringC = ItemStatUtils.getLocation(item).getName() + "_7c";

						if (m == Masterwork.VIIA) {
							mInventory.setItem((row * 9) + 8, mFullUpgradeA);
							if (!ServerProperties.getMasterworkRefundEnabled()) {
								mInventory.setItem((row * 9) + 1, mRefundItem);
							}
							mMapFunction.put((row * 9) + 1, (player, inventory, slot) -> {
								ItemStack newItem = MasterworkUtils.preserveModified(item,
										InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
												NamespacedKeyUtils.fromString(MasterworkUtils.getSixItemPath(item))));
								attemptUpgrade(player, item, newItem, MasterworkCost.getMasterworkCost(costStringA));
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
								attemptUpgrade(player, item, newItem, MasterworkCost.getMasterworkCost(costStringB));
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
								attemptUpgrade(player, item, newItem, MasterworkCost.getMasterworkCost(costStringC));
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
								String refundString = ItemStatUtils.getLocation(item).getName() + "_"
									                      + MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item));
								ItemStack newItem = MasterworkUtils.preserveModified(item,
									InventoryUtils.getItemFromLootTableOrThrow(player.getLocation(),
										NamespacedKeyUtils.fromString(MasterworkUtils.getPrevItemPath(item))));
								attemptUpgrade(player, item, newItem, MasterworkCost.getMasterworkCost(refundString));
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

	private void fillWithJunk() {
		ItemStack junk = new ItemStack(FILLER, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, junk);
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

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		Inventory clickedInventory = event.getClickedInventory();
		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		event.setCancelled(true);

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
		if (event.getPlayer() instanceof Player) {
			Player player = (Player) event.getPlayer();
			PlayerTracking.getInstance().updateItemSlotProperties(player, player.getInventory().getHeldItemSlot());
			PlayerTracking.getInstance().updateItemSlotProperties(player, 36);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 37);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 38);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 39);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 40);
		}
	}
}
