package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.Acumen;
import com.playmonumenta.plugins.itemstats.infusions.Focus;
import com.playmonumenta.plugins.itemstats.infusions.Perspicacity;
import com.playmonumenta.plugins.itemstats.infusions.Tenacity;
import com.playmonumenta.plugins.itemstats.infusions.Vigor;
import com.playmonumenta.plugins.itemstats.infusions.Vitality;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils.InfusionSelection;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;


public class InfusionCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final int ROW = 6;
	private static final int COLUMNS = 9;
	private static final int MAX_LORE_LENGTH = 30;
	private static final Material JUNK_ITEM = Material.GRAY_STAINED_GLASS_PANE;

	private static final ImmutableList<EquipmentSlot> SLOT_ORDER = ImmutableList.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
	private static final Map<InfusionSelection, List<ItemStack>> mInfusionPanelsMap = new HashMap<>();
	private static final Map<InfusionSelection, ItemStack> mPanelList = new HashMap<>();

	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mMaxLevelReachedItem = new ItemStack(Material.CAKE);

	private final Map<Integer, ItemClicked> mMapFunction;


	static {

		// Add items to mPanelList and mInfusionPanelsMap

		addItems(InfusionSelection.VITALITY, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Vitality.HEALTH_MOD_PER_LEVEL * i) + "% max health" + perLevel + ".");
		addItems(InfusionSelection.TENACITY, (i, perLevel) -> "Take " + StringUtils.multiplierToPercentage(Tenacity.DAMAGE_REDUCTION_PER_LEVEL * i) + "% less damage" + perLevel + ".");
		addItems(InfusionSelection.VIGOR, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Vigor.DAMAGE_MOD_PER_LEVEL * i) + "% more melee damage" + perLevel + ".");
		addItems(InfusionSelection.FOCUS, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Focus.DAMAGE_MOD_PER_LEVEL * i) + "% more projectile damage" + perLevel + ".");
		addItems(InfusionSelection.PERSPICACITY, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Perspicacity.DAMAGE_MOD_PER_LEVEL * i) + "% more magic damage" + perLevel + ".");
		addItems(InfusionSelection.ACUMEN, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Acumen.ACUMEN_MULTIPLIER * i) + "% more experience" + perLevel + ".");

		//INVALID ITEM.
		//placeholder when an item can't be infused.

		ItemStack invalidItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta meta = invalidItem.getItemMeta();
		meta.displayName(Component.text("Invalid item", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));

		List<Component> itemLore = new ArrayList<Component>();
		itemLore.add(Component.text("Your helmet can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);

		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your chestplate can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your leggings can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your boots can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your main hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your off hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());

		//Refund item
		ItemMeta refundMeta = mRefundItem.getItemMeta();
		refundMeta.displayName(Component.text("Click to refund this item's infusions", NamedTextColor.DARK_GRAY)
							.decoration(TextDecoration.ITALIC, false)
							.decoration(TextDecoration.BOLD, true));
		GUIUtils.splitLoreLine(refundMeta, "You will receive " + (InfusionUtils.FULL_REFUND ? "100" : (int) (InfusionUtils.REFUND_PERCENT * 100)) + "% of the experience, but all of the materials back.", NamedTextColor.GRAY, MAX_LORE_LENGTH, true);
		mRefundItem.setItemMeta(refundMeta);

		//Cake for max level reached
		ItemMeta maxMeta = mMaxLevelReachedItem.getItemMeta();
		maxMeta.displayName(Component.text("Congratulations!", NamedTextColor.DARK_AQUA)
						.decoration(TextDecoration.BOLD, true)
						.decoration(TextDecoration.ITALIC, false));
		GUIUtils.splitLoreLine(maxMeta, "You've reached the max Infusion level on this item.", NamedTextColor.DARK_AQUA, MAX_LORE_LENGTH, true);
		mMaxLevelReachedItem.setItemMeta(maxMeta);
	}

	public InfusionCustomInventory(Player owner) {
		super(owner, ROW * COLUMNS, "Infusions");
		mMapFunction = new HashMap<>();
		loadInv(owner);
	}

	public void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();
		loadInfusionPage(player);
		fillWithJunk();
	}

	private void loadInfusionPage(Player player) {
		int row = 0;
		for (EquipmentSlot equipmentSlot : SLOT_ORDER) {
			ItemStack is = player.getEquipment().getItem(equipmentSlot);
			if (InfusionUtils.isInfusionable(is)) {
				loadRowNormalInfusionItem(player, equipmentSlot, row);
				final int mRow = row;
				//we need to delay the item change so the skins are loaded
				new BukkitRunnable() {
					@Override
					public void run() {
						ItemStack itemStack = new ItemStack(is.getType());
						ItemMeta originalMeta = is.getItemMeta();
						ItemMeta meta = itemStack.getItemMeta();
						if (originalMeta instanceof LeatherArmorMeta oldLeather && meta instanceof LeatherArmorMeta newLeather) {
							newLeather.setColor(oldLeather.getColor());
						}
						meta.displayName(originalMeta.displayName()
							                 .decoration(TextDecoration.BOLD, true)
							                 .decoration(TextDecoration.ITALIC, false));
						meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						itemStack.setItemMeta(meta);
						ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(is));
						mInventory.setItem((mRow * 9) + 1, itemStack);
					}
				}.runTaskLater(Plugin.getInstance(), 2);

			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				mInventory.setItem((row * 9) + 1, invalidItem);
			}
			row++;
		}
	}



	private void fillWithJunk() {
		ItemStack junk = new ItemStack(JUNK_ITEM, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);

		for (int i = 0; i < (ROW * COLUMNS); i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, junk);
			}
		}
	}

	private static void addItems(InfusionSelection infusion, BiFunction<Integer, String, String> function) {
		addSelectionItem(infusion, function.apply(1, " per level"));
		addInfoItems(infusion, i -> function.apply(i, ""));
	}

	private static void addSelectionItem(InfusionSelection infusion, String desc) {
		ItemStack item = GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel(), infusion.getColor(), true, desc, NamedTextColor.GRAY, MAX_LORE_LENGTH);
		mPanelList.put(infusion, item);
	}

	private static void addInfoItems(InfusionSelection infusion, IntFunction<String> function) {
		List<ItemStack> items = IntStream.range(1, 5)
			.mapToObj(i -> GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel() + " level " + i, infusion.getColor(), true, function.apply(i), NamedTextColor.GRAY, MAX_LORE_LENGTH))
			.toList();
		mInfusionPanelsMap.put(infusion, items);
	}

	private void loadRowNormalInfusionItem(Player player, EquipmentSlot equipmentSlot, int row) {
		Plugin plugin = Plugin.getInstance();
		ItemStack item = player.getEquipment().getItem(equipmentSlot);
		InfusionSelection infusion = InfusionUtils.getCurrentInfusion(item);
		int infusionLvl = InfusionUtils.getInfuseLevel(item);

		List<ItemStack> panelsInfusions = mInfusionPanelsMap.get(infusion);
		//notes: if panelsInfusions == null mean that this item has no infusion.

		//check if the item has an infusion or not
		if (infusionLvl > 0) {
			//set the refund item
			mInventory.setItem((row * 9), mRefundItem);
			mMapFunction.put((row * 9), (p, inventory, slot) -> {
				try {
					InfusionUtils.refundInfusion(player.getEquipment().getItem(equipmentSlot), p);
				} catch (WrapperCommandSyntaxException e) {
					p.sendMessage(Component.text("Error refunding infusion. Please contact a mod: " + e.getMessage()));
				}
			});

			//set the panels to show the current infusion and level
			if (panelsInfusions != null) {
				for (int index = 0; index < infusionLvl; index++) {
					mInventory.setItem((row * 9) + 2 + index, panelsInfusions.get(index));
				}
			}

			if (infusionLvl < 4) {
				int slot = (row * 9) + 2 + infusionLvl;
				//creating item and setting the meta
				ItemStack infuseItem = new ItemStack(Material.ENCHANTED_BOOK, 1);
				ItemMeta infuseMeta = infuseItem.getItemMeta();
				infuseMeta.displayName(Component.text("Click to infuse to level " + (infusionLvl + 1), NamedTextColor.DARK_AQUA)
						                       .decoration(TextDecoration.ITALIC, false)
						                       .decoration(TextDecoration.BOLD, true));
				List<Component> itemLore = new ArrayList<>();
				itemLore.add(Component.text("You need " + InfusionUtils.getExpLvlInfuseCost(plugin, player, item) + " experience levels", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false));
				int currency = -1;

				try {
					currency = InfusionUtils.calcInfuseCost(item);
				} catch (WrapperCommandSyntaxException e) {
					currency = -1;
				}

				if (currency > 0) {
					Region region = ItemStatUtils.getRegion(item);
					if (region == Region.RING) {
						itemLore.add(Component.text("and " + currency + " Pulsating Diamonds", NamedTextColor.GRAY)
							.decoration(TextDecoration.ITALIC, false));
					}

					if (region == Region.ISLES) {
						itemLore.add(Component.text("and " + currency + " Pulsating Emeralds", NamedTextColor.GRAY)
							.decoration(TextDecoration.ITALIC, false));
					}

					if (region == Region.VALLEY) {
						itemLore.add(Component.text("and " + currency + " Pulsating Gold", NamedTextColor.GRAY)
							.decoration(TextDecoration.ITALIC, false));
					}
				}
				infuseMeta.lore(itemLore);
				infuseItem.setItemMeta(infuseMeta);
				mInventory.setItem(slot, infuseItem);

				mMapFunction.put(slot, (p, inventory, itemSlot) -> {
					attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion);
				});
			} else {
				int slot = (row * 9) + 2 + infusionLvl;
				mInventory.setItem(slot, mMaxLevelReachedItem);
			}
		} else {
			ItemStack mInfuseStack = new ItemStack(Material.ENCHANTED_BOOK, 1);
			ItemMeta meta = mInfuseStack.getItemMeta();
			meta.displayName(Component.text("Click to select an Infusion to infuse this item with", NamedTextColor.DARK_GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("The first infusion costs only " + InfusionUtils.getExpLvlInfuseCost(plugin, player, item) + " experience levels", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			mInfuseStack.setItemMeta(meta);
			mInventory.setItem((row * 9), mInfuseStack);

			//set the function when the item is clicked

			mPanelList.forEach((inf, it) -> {
				int loc = (row * 9) + 2 + inf.ordinal();
				mInventory.setItem(loc, it);
				mMapFunction.put(loc, (p, inventory, slot) -> {
					attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), inf);
				});
			});
		}
	}

	private void attemptInfusion(Player p, ItemStack item, InfusionSelection infusion) {
		if (item.getAmount() > 1) {
			p.sendMessage(Component.text("You cannot infuse stacked items.", NamedTextColor.RED));
			return;
		}
		if (!InfusionUtils.isInfusionable(item)) {
			p.sendMessage(Component.text("This item cannot be infused.", NamedTextColor.RED));
			return;
		}

		if (InfusionUtils.canPayInfusion(p, item)) {
			if (InfusionUtils.payInfusion(p, item)) {
				EntityUtils.fireworkAnimation(p);
				InfusionUtils.infuseItem(p, item, infusion);
			} else {
				p.sendMessage(Component.text("If you see this message please contact a mod! (Error in paying infusion cost)", NamedTextColor.RED));
			}
		} else {
			p.sendMessage(Component.text("You don't have enough currency and/or experience for this infusion.", NamedTextColor.RED));
		}
	}

	@Override
	public void inventoryClick(InventoryClickEvent event) {
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
		if (event.getPlayer() instanceof Player player) {
			PlayerTracking playerTracking = PlayerTracking.getInstance();
			if (playerTracking != null) {
				playerTracking.updateItemSlotProperties(player, player.getInventory().getHeldItemSlot());
				playerTracking.updateItemSlotProperties(player, 36);
				playerTracking.updateItemSlotProperties(player, 37);
				playerTracking.updateItemSlotProperties(player, 38);
				playerTracking.updateItemSlotProperties(player, 39);
				playerTracking.updateItemSlotProperties(player, 40);
			}
		}
	}
}
