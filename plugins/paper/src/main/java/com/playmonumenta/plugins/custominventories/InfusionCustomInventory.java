package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.Region;
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
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class InfusionCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final int ROW = 6;
	private static final int COLUMNS = 9;

	private static final ImmutableList<EquipmentSlot> SLOT_ORDER = ImmutableList.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
	private static final Map<InfusionSelection, List<ItemStack>> mInfusionPanelsMap = new HashMap<>();
	private static final Map<InfusionSelection, ItemStack> mPanelList = new HashMap<>();

	private static final List<ItemStack> mInvalidItems;
	private static final ItemStack mRefundItem;
	private static final ItemStack mMaxLevelReachedItem;

	private final Map<Integer, ItemClicked> mMapFunction;


	static {
		// Add items to mPanelList and mInfusionPanelsMap

		addItems(InfusionSelection.VITALITY, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Vitality.HEALTH_MOD_PER_LEVEL * i) + "% max health" + perLevel + ".");
		addItems(InfusionSelection.TENACITY, (i, perLevel) -> "Take " + StringUtils.multiplierToPercentage(Tenacity.DAMAGE_REDUCTION_PER_LEVEL * i) + "% less damage" + perLevel + ".");
		addItems(InfusionSelection.VIGOR, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Vigor.DAMAGE_MOD_PER_LEVEL * i) + "% more melee damage" + perLevel + ".");
		addItems(InfusionSelection.FOCUS, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Focus.DAMAGE_MOD_PER_LEVEL * i) + "% more projectile damage" + perLevel + ".");
		addItems(InfusionSelection.PERSPICACITY, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Perspicacity.DAMAGE_MOD_PER_LEVEL * i) + "% more magic damage" + perLevel + ".");
		addItems(InfusionSelection.ACUMEN, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Acumen.ACUMEN_MULTIPLIER * i) + "% more experience" + perLevel + ".");

		mInvalidItems = Stream.of("helmet", "chestplate", "leggings", "boots", "main hand", "off hand")
			.map(s -> GUIUtils.createBasicItem(Material.ARMOR_STAND, "Invalid Item", NamedTextColor.GRAY, true, "Your " + s + " can't be infused.", NamedTextColor.DARK_GRAY)).toList();

		//Refund item
		mRefundItem = GUIUtils.createBasicItem(Material.GRINDSTONE, "Click to refund this item's infusions", NamedTextColor.DARK_GRAY, true, "You will receive " + (InfusionUtils.FULL_REFUND ? "100" : (int) (InfusionUtils.REFUND_PERCENT * 100)) + "% of the experience, but all of the materials back.", NamedTextColor.GRAY);

		//Cake for max level reached
		mMaxLevelReachedItem = GUIUtils.createBasicItem(Material.CAKE, "Congratulations!", NamedTextColor.DARK_AQUA, true, "You've reached the max Infusion level on this item.", NamedTextColor.DARK_AQUA);
	}

	public InfusionCustomInventory(Player owner) {
		super(owner, ROW * COLUMNS, "Infusions");
		mMapFunction = new HashMap<>();
		loadInv(owner);
	}

	public void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> loadInfusionPage(player), 1);
		GUIUtils.fillWithFiller(mInventory);
	}

	private void loadInfusionPage(Player player) {
		int row = 0;
		for (EquipmentSlot equipmentSlot : SLOT_ORDER) {
			ItemStack is = player.getEquipment().getItem(equipmentSlot);
			if (InfusionUtils.isInfusionable(is)) {
				loadRowNormalInfusionItem(player, equipmentSlot, row);
				//we need to delay the item change so the skins are loaded
				mInventory.setItem((row * 9) + 1, is);

			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				mInventory.setItem((row * 9) + 1, invalidItem);
			}
			row++;
		}
	}

	private static void addItems(InfusionSelection infusion, BiFunction<Integer, String, String> function) {
		addSelectionItem(infusion, function.apply(1, " per level"));
		addInfoItems(infusion, i -> function.apply(i, ""));
	}

	private static void addSelectionItem(InfusionSelection infusion, String desc) {
		ItemStack item = GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel(), infusion.getColor(), true, desc, NamedTextColor.GRAY);
		mPanelList.put(infusion, item);
	}

	private static void addInfoItems(InfusionSelection infusion, IntFunction<String> function) {
		List<ItemStack> items = IntStream.range(1, 5)
			.mapToObj(i -> GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel() + " level " + i, infusion.getColor(), true, function.apply(i), NamedTextColor.GRAY))
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
				Component lore = Component.text("You need " + InfusionUtils.getExpLvlInfuseCost(plugin, player, item) + " experience levels", NamedTextColor.GRAY);

				int currency = -1;
				try {
					currency = InfusionUtils.calcInfuseCost(item);
				} catch (WrapperCommandSyntaxException e) {
					currency = -1;
				}

				if (currency > 0) {
					Region region = ItemStatUtils.getRegion(item);
					if (region == Region.RING) {
						lore = lore.append(Component.text(" and " + currency + " Pulsating Diamonds", NamedTextColor.GRAY));
					}

					if (region == Region.ISLES) {
						lore = lore.append(Component.text(" and " + currency + " Pulsating Emeralds", NamedTextColor.GRAY));
					}

					if (region == Region.VALLEY) {
						lore = lore.append(Component.text(" and " + currency + " Pulsating Gold", NamedTextColor.GRAY));
					}
				}

				ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, 1, "Click to infuse to level " + (infusionLvl + 1), NamedTextColor.DARK_AQUA, true, lore, 30, true);
				mInventory.setItem(slot, infuseItem);


				mMapFunction.put(slot, (p, inventory, itemSlot) -> {
					attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion);
				});
			} else {
				int slot = (row * 9) + 2 + infusionLvl;
				mInventory.setItem(slot, mMaxLevelReachedItem);
			}
		} else {
			ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Click to select an Infusion to infuse this item with", NamedTextColor.DARK_GRAY, true, "The first infusion costs only " + InfusionUtils.getExpLvlInfuseCost(plugin, player, item) + " experience levels", NamedTextColor.GRAY);
			mInventory.setItem((row * 9), infuseItem);

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

		if (InfusionUtils.tryToPayInfusion(p, item)) {
			EntityUtils.fireworkAnimation(p);
			InfusionUtils.infuseItem(p, item, infusion);
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
