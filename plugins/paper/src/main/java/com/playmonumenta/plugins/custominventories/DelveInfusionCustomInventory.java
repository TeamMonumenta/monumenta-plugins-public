package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.Ardor;
import com.playmonumenta.plugins.itemstats.infusions.Aura;
import com.playmonumenta.plugins.itemstats.infusions.Carapace;
import com.playmonumenta.plugins.itemstats.infusions.Celestial;
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
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionSelection;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class DelveInfusionCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final Map<DelveInfusionSelection, List<ItemStack>> mDelveInfusionPanelsMap = new HashMap<>();
	private static final HashMap<DelveInfusionSelection, ItemStack> mDelvePanelList = new HashMap<>();

	private static final ImmutableList<EquipmentSlot> SLOT_ORDER = ImmutableList.of(
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
	private static final List<ItemStack> mInvalidItems;
	private static final ItemStack mRefundItem;
	@SuppressWarnings("unused") // We'll want to turn this back on at some point later.
	private static final ItemStack mFullRefundItem;
	private static final ItemStack mMaxLevelReachedItem;
	private static final ItemStack mMaxLevelReachedRevelationItem;

	private final Map<Integer, ItemClicked> mMapFunction;

	private @Nullable EquipmentSlot mSlotSelected;
	private final boolean mZenithCompleted;
	private final boolean mDepthsCompleted;
	private DelveInfusionUtils.DelveInfusionMaterial mDelveInfusionMaterial;

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
		addItems(DelveInfusionSelection.REFLECTION, (i, perLevel) -> "1 second after taking magic or blast damage, deal " + StringUtils.multiplierToPercentage(Reflection.REFLECT_PCT_PER_LEVEL * i) + "%" + perLevel + " of the damage to all mobs in a 4 block radius.");
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
			return "All non-Delve Infusions you are currently benefiting from gain " + StringUtils.to2DP(levels) + " level" + s + " per item" + perLevel + ".";
		});
		//R3
		addItems(DelveInfusionSelection.SOOTHING, (i, perLevel) -> "Regenerate " + Soothing.HEAL_PER_LEVEL * i + " health" + perLevel + " each second.");
		addItems(DelveInfusionSelection.FUELED, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Fueled.DR_PER_MOB * i) + "% Damage Reduction" + perLevel + " for every enemy on fire, slowed, or stunned within 8 blocks, capped at 4 mobs.");
		addItems(DelveInfusionSelection.REFRESH, (i, perLevel) -> "Reduces the cooldown of infinite consumable foods by " + StringUtils.multiplierToPercentage(Refresh.REDUCTION_PER_LEVEL * i) + "%" + perLevel + ".");
		addItems(DelveInfusionSelection.QUENCH, (i, perLevel) -> "Increase duration of consumables by " + StringUtils.multiplierToPercentage(Quench.DURATION_BONUS_PER_LVL * i) + "%" + perLevel + ".");
		addItems(DelveInfusionSelection.GRACE, (i, perLevel) -> "Gain " + StringUtils.multiplierToPercentage(Grace.ATKS_BONUS * i) + "% attack speed" + perLevel + ".");
		addItems(DelveInfusionSelection.GALVANIC, (i, perLevel) -> "Gain a " + StringUtils.multiplierToPercentage(Galvanic.STUN_CHANCE_PER_LVL * i) + "% chance" + perLevel + " to stun a mob for 2 seconds (0.5 seconds for elites) when dealing or taking non-ability melee or projectile damage.");
		addItems(DelveInfusionSelection.DECAPITATION, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Decapitation.DAMAGE_MLT_PER_LVL * i) + "% additional damage" + perLevel + " on a critical melee strike.");
		addItems(DelveInfusionSelection.CELESTIAL, (i, perLevel) -> "Deal " + StringUtils.multiplierToPercentage(Celestial.DAMAGE_BONUS_PER_LEVEL * i) + "% additional damage" + perLevel + " to mobs that are at a higher elevation than you.");

		mInvalidItems = Stream.of("helmet", "chestplate", "leggings", "boots", "main hand", "off hand")
			.map(s -> GUIUtils.createBasicItem(Material.ARMOR_STAND, "Invalid Item", NamedTextColor.GRAY, true, "Your " + s + " can't be infused.", NamedTextColor.DARK_GRAY)).toList();

		//Refund item
		mRefundItem = GUIUtils.createBasicItem(Material.GRINDSTONE, "Click to refund this item's infusions.", NamedTextColor.DARK_GRAY, true, "You will receive " + (DelveInfusionUtils.FULL_REFUND ? "100" : (int) (DelveInfusionUtils.REFUND_PERCENT * 100)) + "% of the experience, but all of the materials back.", NamedTextColor.GRAY);

		//Refund item
		mFullRefundItem = GUIUtils.createBasicItem(Material.GRINDSTONE, "Click to refund this item's infusions.", NamedTextColor.DARK_GRAY, true, "You will receive 100% of the experience, and all of the materials back.", NamedTextColor.GRAY);

		//Cake for max level reached
		mMaxLevelReachedItem = GUIUtils.createBasicItem(Material.CAKE, "Congratulations!", NamedTextColor.DARK_AQUA, true, "You've reached the max Delve Infusion level on this item.", NamedTextColor.DARK_AQUA);

		// Echo shard for item with Revelation & max level
		mMaxLevelReachedRevelationItem = GUIUtils.createBasicItem(Material.ECHO_SHARD, "Revelation!", NamedTextColor.DARK_AQUA, true, "You've reached the max Delve Infusion level on this item, and Invoked it to its true potential.", NamedTextColor.DARK_AQUA);
	}

	public DelveInfusionCustomInventory(Player owner) {
		super(owner, 54, "Delve Infusions");
		mMapFunction = new HashMap<>();
		mSlotSelected = null;
		mDepthsCompleted = ScoreboardUtils.getScoreboardValue(owner, "Depths").orElse(0) > 0;
		mZenithCompleted = ScoreboardUtils.getScoreboardValue(owner, "Zenith").orElse(0) > 0;
		mDelveInfusionMaterial = mZenithCompleted ?
			                         (mDepthsCompleted ?
				                          (ServerProperties.getAbilityEnhancementsEnabled(owner) ? DelveInfusionUtils.DelveInfusionMaterial.INDIGO_BLIGHTDUST : DelveInfusionUtils.DelveInfusionMaterial.VOIDSTAINED_GEODE)
				                          : DelveInfusionUtils.DelveInfusionMaterial.INDIGO_BLIGHTDUST)
			                         : DelveInfusionUtils.DelveInfusionMaterial.VOIDSTAINED_GEODE;
		if (mDepthsCompleted || mZenithCompleted) {
			loadInv(owner);
		} else {
			close();
			owner.sendMessage(Component.text("You have not completed the content required to access Delve Infusions.", NamedTextColor.RED));
		}
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
		mInventory.setItem(4, infusedItem);

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
		itemPlacements.put(DelveInfusionSelection.CELESTIAL, 38);
		itemPlacements.put(DelveInfusionSelection.GALVANIC, 39);
		itemPlacements.put(DelveInfusionSelection.DECAPITATION, 40);

		itemPlacements.forEach((infusion, place) -> {
			if (infusion.isUnlocked(player)) {
				mInventory.setItem(place, mDelvePanelList.get(infusion));
				mMapFunction.put(place, (p, inventory, slot) -> {
					attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion, mDelveInfusionMaterial);
					mSlotSelected = null;
				});
			}
		});

		ItemStack swapPage = GUIUtils.createBasicItem(Material.PAPER, "Back!", TextColor.fromCSSHexString("ffa000"), true);
		mInventory.setItem(53, swapPage);
		mMapFunction.put(53, (p, clickedInventory, slot) -> {
			mSlotSelected = null;
		});

		if (mDepthsCompleted && mZenithCompleted) {
			ItemStack currencyItem = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(player, mDelveInfusionMaterial.mLootTable));
			GUIUtils.splitLoreLine(currencyItem, "Currently using " + mDelveInfusionMaterial.mItemNamePlural + ". Click to switch to " + mDelveInfusionMaterial.getNext().mItemNamePlural + ".", NamedTextColor.DARK_GRAY, 30, true);
			mInventory.setItem(49, currencyItem);
			mMapFunction.put(49, (p, clickedInventory, slot) -> {
				mDelveInfusionMaterial = mDelveInfusionMaterial.getNext();
			});
		}

	}

	private void attemptInfusion(Player p, ItemStack item, DelveInfusionSelection infusion, DelveInfusionUtils.DelveInfusionMaterial delveInfusionMaterial) {
		if (item.getAmount() > 1) {
			p.sendMessage(Component.text("You cannot infuse stacked items.", NamedTextColor.RED));
			return;
		}
		if (!InfusionUtils.isInfusionable(item)) {
			p.sendMessage(Component.text("This item cannot be infused.", NamedTextColor.RED));
			return;
		}

		try {
			if (DelveInfusionUtils.tryToPayInfusion(item, infusion, p, delveInfusionMaterial)) {
				DelveInfusionUtils.infuseItem(p, item, infusion, delveInfusionMaterial);
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
				mInventory.setItem((row * 9) + 1, item);

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
						DelveInfusionUtils.DelveInfusionMaterial delveInfusionMaterial = DelveInfusionUtils.getDelveInfusionMaterial(item);
						ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Click to infuse to level " + (level + 1), NamedTextColor.DARK_AQUA, true,
							"You will need " + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[level] + " " + delveInfusionMaterial.mItemNamePlural + ", " + DelveInfusionUtils.MAT_COST_PER_INFUSION[level] + " " + infusion.getDelveMatPlural() + ", and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY);
						mInventory.setItem(slot, infuseItem);
						mMapFunction.put(slot, (p, inventory, slotClicked) -> {
							attemptInfusion(p, player.getEquipment().getItem(equipmentSlot), infusion, delveInfusionMaterial);
						});
					} else {
						//Max level reached

						if (ItemStatUtils.hasInfusion(item, InfusionType.REVELATION)) {
							if (panelsList != null && panelsList.get(DelveInfusionUtils.MAX_LEVEL) != null) {
								mInventory.setItem(slot, panelsList.get(DelveInfusionUtils.MAX_LEVEL));
								slot++;
								mInventory.setItem(slot, mMaxLevelReachedRevelationItem);
							}
						} else {
							mInventory.setItem(slot, mMaxLevelReachedItem);
						}
					}
				} else {
					//Item with no infusion -> load item to swap page
					//if we didn't reach max level then load item to infuse
					String materials = "";
					if (mDepthsCompleted) {
						materials += "Voidstained Geodes";
						if (mZenithCompleted) {
							materials += " or ";
						}
					}
					if (mZenithCompleted) {
						materials += "Indigo Blightdust";
					}
					ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Click to select a Delve Infusion.", NamedTextColor.DARK_AQUA, true,
						"The first Delve Infusion level costs " + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[0] + " " + materials + ", " + DelveInfusionUtils.MAT_COST_PER_INFUSION[0] + " corresponding Delve Materials, and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY);
					mInventory.setItem((row * 9) + 2 + 4, infuseItem);
					mMapFunction.put((row * 9) + 2 + 4, (p, inventory, slot) -> {
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
		GUIUtils.fillWithFiller(mInventory);
	}

	private static void addItems(DelveInfusionSelection infusion, BiFunction<Integer, String, String> function) {
		addSelectionItem(infusion, function.apply(1, " per level"));
		addInfoItems(infusion, i -> function.apply(i, ""));
	}

	private static void addSelectionItem(DelveInfusionSelection infusion, String desc) {
		ItemStack item = GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel(), infusion.getColor(), true, desc, NamedTextColor.GRAY);
		GUIUtils.splitLoreLine(item, "Requires " + infusion.getDelveMatPlural(), TextColor.fromHexString("#555555"), false);
		mDelvePanelList.put(infusion, item);
	}

	private static void addInfoItems(DelveInfusionSelection infusion, IntFunction<String> function) {
		List<ItemStack> items = IntStream.range(1, DelveInfusionUtils.MAX_LEVEL + 2)
			.mapToObj(i -> GUIUtils.createBasicItem(infusion.getMaterial(), infusion.getCapitalizedLabel() + " level " + i, infusion.getColor(), true, function.apply(i), NamedTextColor.GRAY))
			.toList();
		mDelveInfusionPanelsMap.put(infusion, items);
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
