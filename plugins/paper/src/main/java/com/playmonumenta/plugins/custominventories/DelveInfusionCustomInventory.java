package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.Ardor;
import com.playmonumenta.plugins.itemstats.infusions.Aura;
import com.playmonumenta.plugins.itemstats.infusions.Carapace;
import com.playmonumenta.plugins.itemstats.infusions.Choler;
import com.playmonumenta.plugins.itemstats.infusions.Decapitation;
import com.playmonumenta.plugins.itemstats.infusions.Empowered;
import com.playmonumenta.plugins.itemstats.infusions.Epoch;
import com.playmonumenta.plugins.itemstats.infusions.Execution;
import com.playmonumenta.plugins.itemstats.infusions.Expedite;
import com.playmonumenta.plugins.itemstats.infusions.Fueled;
import com.playmonumenta.plugins.itemstats.infusions.Galvanic;
import com.playmonumenta.plugins.itemstats.infusions.Grace;
import com.playmonumenta.plugins.itemstats.infusions.Mitosis;
import com.playmonumenta.plugins.itemstats.infusions.Natant;
import com.playmonumenta.plugins.itemstats.infusions.Nutriment;
import com.playmonumenta.plugins.itemstats.infusions.Pennate;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
import com.playmonumenta.plugins.itemstats.infusions.Reflection;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.itemstats.infusions.Soothing;
import com.playmonumenta.plugins.itemstats.infusions.Understanding;
import com.playmonumenta.plugins.itemstats.infusions.Unyielding;
import com.playmonumenta.plugins.itemstats.infusions.Usurper;
import com.playmonumenta.plugins.itemstats.infusions.Vengeful;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionSelection;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
import org.jetbrains.annotations.Nullable;

public final class DelveInfusionCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final int MAX_LORE_LENGTH = 30;
	private static final Material JUNK_ITEM = Material.GRAY_STAINED_GLASS_PANE;

	private static final Map<DelveInfusionSelection, List<ItemStack>> mDelveInfusionPanelsMap = new HashMap<>();
	private static final HashMap<DelveInfusionSelection, ItemStack> mDelvePanelList = new HashMap<>();

	private static final ImmutableList<EquipmentSlot> SLOT_ORDER = ImmutableList.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mMaxLevelReachedItem = new ItemStack(Material.CAKE);

	private final Map<Integer, ItemClicked> mMapFunction;

	private @Nullable EquipmentSlot mSlotSelected;

	static {
		// Add items into mDelvePanelList and mDelveInfusionPanelsMap

		//R1
		addItems(DelveInfusionSelection.PENNATE, (i, perLevel) -> "Fall damage is reduced by " + StringUtils.multiplierToPercentage(Pennate.REDUCT_PCT_PER_LEVEL * i) + "%" + perLevel + ".");
		addItems(DelveInfusionSelection.CARAPACE, (i, perLevel) -> "After being hit by a mob, you gain " + StringUtils.multiplierToPercentage(Carapace.DAMAGE_REDUCTION_PER_LEVEL * i) + "% damage reduction" + perLevel + " for 5s. Being hit again while active refreshes the duration.");
		addItems(DelveInfusionSelection.AURA, (i, perLevel) -> "Mobs in a 3 block radius from you are slowed by " + StringUtils.multiplierToPercentage(Aura.SLOW_PER_LEVEL * i) + "%" + perLevel + " for 0.5 seconds. This is refreshed as long as they are in range.");
		addItems(DelveInfusionSelection.EXPEDITE, (i, perLevel) -> "Damaging an enemy with an ability increases your movement speed by " + StringUtils.multiplierToPercentage(Expedite.PERCENT_SPEED_PER_LEVEL * i) + "%" + perLevel + " for 5 seconds, stacking up to 3 times.");
		addItems(DelveInfusionSelection.CHOLER, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Choler.DAMAGE_MLT_PER_LVL * i) + "% more damage" + perLevel + " to any mob that is on fire, slowed, or stunned.");
		addItems(DelveInfusionSelection.UNYIELDING, (i, perLevel) -> "Gain " + StringUtils.to2DP(Unyielding.KB_PER_LEVEL * i * 10) + " Knockback Resistance" + perLevel + ".");
		addItems(DelveInfusionSelection.USURPER, (i, perLevel) -> "Heal " + StringUtils.multiplierToPercentage(Usurper.HEAL_PCT_PER_LVL * i) + "% of your max health" + perLevel + " whenever you slay an elite or boss enemy.");
		addItems(DelveInfusionSelection.VENGEFUL, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Vengeful.DAMAGE_MLT_PER_LVL * i) + "% more damage" + perLevel + " against the last enemy that damaged you.");
		//R2
		addItems(DelveInfusionSelection.EMPOWERED, (i, perLevel) -> "When you gain XP, you have a " + StringUtils.multiplierToPercentage(Empowered.PERCENT_CHANCE * i) + "% chance per XP" + perLevel + " point to repair all currently equipped items by 1% of their max durability.");
		addItems(DelveInfusionSelection.NUTRIMENT, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Nutriment.HEALING_PERCENT_PER_LEVEL * i) + "% extra healing" + perLevel + ".");
		addItems(DelveInfusionSelection.EXECUTION, (i, perLevel) -> "After killing an enemy, you deal " + StringUtils.multiplierToPercentage(Execution.PERCENT_DAMAGE_PER_LEVEL * i) + "% more damage" + perLevel + " for 4 seconds.");
		addItems(DelveInfusionSelection.REFLECTION, (i, perLevel) -> "1 second after taking ability damage, deal " + StringUtils.multiplierToPercentage(Reflection.REFLECT_PCT_PER_LEVEL * i) + "%" + perLevel + " of the spell's damage to all mobs in a 4 block radius.");
		addItems(DelveInfusionSelection.MITOSIS, (i, perLevel) -> "Mining a spawner debuffs all mobs in a 5 block radius with " + StringUtils.multiplierToPercentage(Mitosis.PERCENT_WEAKEN_PER_LEVEL * i) + "%" + perLevel + " Weakness for 3 seconds.");
		addItems(DelveInfusionSelection.ARDOR, (i, perLevel) -> "Mining a spawner outside of water grants you " + StringUtils.multiplierToPercentage(Ardor.PERCENT_SPEED_PER_LEVEL * i) + "% speed" + perLevel + " for 4s. Mining a spawner underwater refreshes " + 0.5 * i + " breath" + perLevel + ".");
		addItems(DelveInfusionSelection.EPOCH, (i, perLevel) -> "Class abilities cooldowns are reduced by " + StringUtils.multiplierToPercentage(Epoch.COOLDOWN_REDUCTION_PER_LEVEL * i) + "%" + perLevel + ".");
		addItems(DelveInfusionSelection.NATANT, (i, perLevel) -> "You move " + StringUtils.multiplierToPercentage(Natant.PERCENT_SPEED_PER_LEVEL * i) + "%" + perLevel + " faster when in water.");
		addItems(DelveInfusionSelection.UNDERSTANDING, (i, perLevel) -> {
			double levels = Understanding.POINTS_PER_LEVEL * i;
			String s = levels == 1 ? "" : "s";
			if (!perLevel.isEmpty()) {
				perLevel += " of this Infusion";
			}
			return "All other Delve Infusions you are currently benefiting from gain " + levels + " level" + s + perLevel + ".";
		});
		//R3
		addItems(DelveInfusionSelection.SOOTHING, (i, perLevel) -> "Regenerate " + Soothing.HEAL_PER_LEVEL * i + " health" + perLevel + " each second.");
		addItems(DelveInfusionSelection.FUELED, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Fueled.DR_PER_MOB * i) + "% Damage Reduction" + perLevel + " for every enemy on fire, slowed, or stunned within 8 blocks, capped at 4 mobs.");
		addItems(DelveInfusionSelection.REFRESH, (i, perLevel) -> "Reduces the cooldown of infinite consumable foods by " + StringUtils.multiplierToPercentage(Refresh.REDUCTION_PER_LEVEL * i) + "%" + perLevel + ".");
		addItems(DelveInfusionSelection.QUENCH, (i, perLevel) -> "Increase duration of consumables by " + StringUtils.multiplierToPercentage(Quench.DURATION_BONUS_PER_LVL * i) + "%.");
		addItems(DelveInfusionSelection.GRACE, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Grace.ATKS_BONUS * i) + "% attack speed" + perLevel + ".");
		addItems(DelveInfusionSelection.GALVANIC, (i, perLevel) -> "Gain a " + StringUtils.multiplierToPercentage(Galvanic.STUN_CHANCE_PER_LVL * i) + "% chance" + perLevel + " to stun a mob for 2 seconds (0.5 seconds for elites) when dealing or taking non-ability melee or projectile damage.");
		addItems(DelveInfusionSelection.DECAPITATION, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Decapitation.DAMAGE_MLT_PER_LVL * i) + "% additional damage" + perLevel + " on a critical melee strike.");

		//INVALIDS ITEM.
		//placeholder when an item can't be infused.

		ItemStack invalidItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta meta = invalidItem.getItemMeta();
		meta.displayName(Component.text("Invalid item", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true));

		List<Component> itemLore = new ArrayList<>();
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
		refundMeta.displayName(Component.text("Click to refund this item's infusions.", NamedTextColor.DARK_GRAY)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true));
		GUIUtils.splitLoreLine(refundMeta, "You will receive " + (DelveInfusionUtils.FULL_REFUND ? "100" : (int) (DelveInfusionUtils.REFUND_PERCENT * 100)) + "% of the experience, but all of the materials back.", NamedTextColor.GRAY, MAX_LORE_LENGTH, true);
		mRefundItem.setItemMeta(refundMeta);

		//Cake for max level reached
		ItemMeta maxMeta = mMaxLevelReachedItem.getItemMeta();
		maxMeta.displayName(Component.text("Congratulations!", NamedTextColor.DARK_AQUA)
			.decoration(TextDecoration.BOLD, true)
			.decoration(TextDecoration.ITALIC, false));
		GUIUtils.splitLoreLine(maxMeta, "You've reached the max Delve Infusion level on this item.", NamedTextColor.DARK_AQUA, MAX_LORE_LENGTH, true);
		mMaxLevelReachedItem.setItemMeta(maxMeta);
	}

	public DelveInfusionCustomInventory(Player owner) {
		super(owner, 54, "Delve Infusions");
		mMapFunction = new HashMap<>();
		mSlotSelected = null;
		loadInv(owner);
	}

	private void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();

		if (mSlotSelected == null) {
			loadDelveInfusionPage(player);
		} else {
			loadDelveInfusionSelection(mSlotSelected, player);
		}

		fillWithJunk();
	}


	private void loadDelveInfusionSelection(EquipmentSlot equipmentSlot, Player player) {
		ItemStack infusedItem = player.getEquipment().getItem(equipmentSlot);
		//we need to delay this loading to make the item skin applied
		new BukkitRunnable() {
			@Override
			public void run() {
				ItemStack itemStack = new ItemStack(infusedItem.getType());
				ItemMeta originalMeta = infusedItem.getItemMeta();
				ItemMeta meta = itemStack.getItemMeta();
				if (originalMeta instanceof LeatherArmorMeta oldLeather && meta instanceof LeatherArmorMeta newLeather) {
					newLeather.setColor(oldLeather.getColor());
				}
				meta.displayName(Component.text("Placeholder", TextColor.fromCSSHexString("000000"))
					                 .decoration(TextDecoration.BOLD, true)
					                 .decoration(TextDecoration.ITALIC, false));
				itemStack.setItemMeta(meta);
				ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(infusedItem));
				mInventory.setItem(4, itemStack);
			}
		}.runTaskLater(Plugin.getInstance(), 2);

		HashMap<DelveInfusionSelection, Integer> itemPlacements = new HashMap<>();

		itemPlacements.put(DelveInfusionSelection.PENNATE, 9);
		itemPlacements.put(DelveInfusionSelection.CARAPACE, 10);
		itemPlacements.put(DelveInfusionSelection.AURA, 11);
		itemPlacements.put(DelveInfusionSelection.EXPEDITE, 12);
		itemPlacements.put(DelveInfusionSelection.CHOLER, 14);
		itemPlacements.put(DelveInfusionSelection.UNYIELDING, 15);
		itemPlacements.put(DelveInfusionSelection.USURPER, 16);
		itemPlacements.put(DelveInfusionSelection.VENGEFUL, 17);

		itemPlacements.put(DelveInfusionSelection.EMPOWERED, 18);
		itemPlacements.put(DelveInfusionSelection.NUTRIMENT, 19);
		itemPlacements.put(DelveInfusionSelection.EXECUTION, 20);
		itemPlacements.put(DelveInfusionSelection.REFLECTION, 21);
		itemPlacements.put(DelveInfusionSelection.MITOSIS, 22);
		itemPlacements.put(DelveInfusionSelection.ARDOR, 23);
		itemPlacements.put(DelveInfusionSelection.EPOCH, 24);
		itemPlacements.put(DelveInfusionSelection.NATANT, 25);
		itemPlacements.put(DelveInfusionSelection.UNDERSTANDING, 26);

		itemPlacements.put(DelveInfusionSelection.SOOTHING, 27);
		itemPlacements.put(DelveInfusionSelection.FUELED, 28);
		itemPlacements.put(DelveInfusionSelection.REFRESH, 29);

		itemPlacements.put(DelveInfusionSelection.QUENCH, 36);
		itemPlacements.put(DelveInfusionSelection.GRACE, 37);
		itemPlacements.put(DelveInfusionSelection.GALVANIC, 38);
		itemPlacements.put(DelveInfusionSelection.DECAPITATION, 39);

		itemPlacements.forEach((infusion, place) -> {
			if (infusion.isUnlocked(player)) {
				mInventory.setItem(place, mDelvePanelList.get(infusion));
				mMapFunction.put(place, (p, inventory, slot) -> {
					attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion);
					mSlotSelected = null;
				});
			}
		});

		ItemStack swapPage = new ItemStack(Material.PAPER);
		ItemMeta meta = swapPage.getItemMeta();
		meta.displayName(Component.text("Back!")
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false)
				.color(TextColor.fromCSSHexString("ffa000")));
		swapPage.setItemMeta(meta);
		mInventory.setItem(53, swapPage);

		mMapFunction.put(53, (p, clickedInventory, slot) -> {
			mSlotSelected = null;
		});

	}

	private void attemptInfusion(Player p, ItemStack item, DelveInfusionSelection infusion) {
		if (item.getAmount() > 1) {
			p.sendMessage(Component.text("You cannot infuse stacked items.", NamedTextColor.RED));
			return;
		}
		if (!InfusionUtils.isInfusionable(item)) {
			p.sendMessage(Component.text("This item cannot be infused.", NamedTextColor.RED));
			return;
		}

		try {
			if (DelveInfusionUtils.canPayInfusion(item, infusion, p)) {
				if (DelveInfusionUtils.payInfusion(item, infusion, p)) {
					DelveInfusionUtils.infuseItem(p, item, infusion);
				} else {
					p.sendMessage(Component.text("If you see this message please contact a mod! (Error in paying infusion cost)", NamedTextColor.RED));
				}
			} else {
				p.sendMessage(Component.text("You don't have enough experience and/or currency for this infusion.", NamedTextColor.RED));
			}
		} catch (Exception e) {
			p.sendMessage(Component.text("If you see this message please contact a mod! (Error in infusing)", NamedTextColor.RED));
			e.printStackTrace();
		}
	}

	private void loadDelveInfusionPage(Player player) {
		//load panels for each item with the corresponding infusions.
		int row = 0;
		for (EquipmentSlot equipmentSlot : SLOT_ORDER) {
			ItemStack item = player.getEquipment().getItem(equipmentSlot);
			//check valid item
			if (InfusionUtils.isInfusionable(item)) {
				//same tier needed.
				DelveInfusionSelection infusion = DelveInfusionUtils.getCurrentInfusion(item);
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
						mInventory.setItem((rowF * 9) + 1, itemStack);
					}
				}.runTaskLater(Plugin.getInstance(), 2);

				if (infusion != null) {
					mInventory.setItem((row * 9), mRefundItem);
					mMapFunction.put((row * 9), (p, inventory, slot) -> {
						DelveInfusionUtils.refundInfusion(player.getEquipment().getItem(equipmentSlot), p);
					});

					//load the infusion.
					int level = DelveInfusionUtils.getInfusionLevel(item, infusion);
					List<ItemStack> panelsList = mDelveInfusionPanelsMap.get(infusion);
					if (panelsList != null) {
						for (int i = 0; i < level; i++) {
							if (panelsList.get(i) != null) {
								mInventory.setItem((row * 9) + 2 + i, panelsList.get(i));
							}
						}
					}

					int slot = (row * 9) + 2 + level;
					if (level < DelveInfusionUtils.MAX_LEVEL) {
						//if we didn't reach max level then load item to infuse
						ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Click to infuse to level " + (level + 1), NamedTextColor.DARK_AQUA, true,
							"You will need " + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[level] + " Voidstained Geodes, " + DelveInfusionUtils.MAT_COST_PER_INFUSION[level] + " " + infusion.getDelveMatPlural() + ", and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY);
						mInventory.setItem(slot, infuseItem);
						mMapFunction.put(slot, (p, inventory, slotClicked) -> {
							attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion);
						});
					} else {
						//Max level reached
						mInventory.setItem(slot, mMaxLevelReachedItem);
					}
				} else {
					//Item with no infusion -> load item to swap page
					//if we didn't reach max level then load item to infuse
					ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Click to select a Delve Infusion.", NamedTextColor.DARK_AQUA, true,
						"The first Delve Infusion level costs " + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[0] + " Voidstained Geodes, " + DelveInfusionUtils.MAT_COST_PER_INFUSION[0] + " corresponding Delve Materials, and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY);
					mInventory.setItem((rowF * 9) + 2 + 4, infuseItem);
					mMapFunction.put((rowF * 9) + 2 + 4, (p, inventory, slot) -> {
						mSlotSelected = equipmentSlot;
					});
				}
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

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, junk);
			}
		}
	}

	private static void addItems(DelveInfusionSelection infusion, BiFunction<Integer, String, String> function) {
		addSelectionItem(infusion, function.apply(1, " per level"));
		addInfoItems(infusion, i -> function.apply(i, ""));
	}

	private static void addSelectionItem(DelveInfusionSelection infusion, String desc) {
		ItemStack item = GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel(), infusion.getColor(), true, desc, NamedTextColor.GRAY, MAX_LORE_LENGTH);
		GUIUtils.splitLoreLine(item, "Requires " + infusion.getDelveMatPlural(), TextColor.fromHexString("#555555"), MAX_LORE_LENGTH, false);
		mDelvePanelList.put(infusion, item);
	}

	private static void addInfoItems(DelveInfusionSelection infusion, IntFunction<String> function) {
		List<ItemStack> items = IntStream.range(1, 5)
			.mapToObj(i -> GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel() + " level " + i, infusion.getColor(), true, function.apply(i), NamedTextColor.GRAY, MAX_LORE_LENGTH))
			.toList();
		mDelveInfusionPanelsMap.put(infusion, items);
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
