package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public final class DepthsWeaponAspectGUI extends CustomInventory {
	private static final Material FILLER = Material.RED_STAINED_GLASS_PANE;
	private static final Material PURCHASED_MAT = Material.LIME_STAINED_GLASS_PANE;
	private static final Material BUY_ITEM = Material.GOLD_NUGGET;
	private static final int CCS_AMOUNT = 12;
	private static final String PAID_SCOREBOARD_TAG = "DepthsWeaponAspectUpgradeBought";

	public DepthsWeaponAspectGUI(Player player) {
		super(player, 27, "Select an Aspect");


		setLayout(player, mInventory, player.getScoreboardTags().contains(PAID_SCOREBOARD_TAG));
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory ||
			    clickedItem == null ||
			    clickedItem.getType() == FILLER ||
			    clickedItem.getType() == PURCHASED_MAT ||
			    event.isShiftClick()) {
			return;
		}
		Player player = (Player) event.getWhoClicked();

		if (clickedItem.getType() == BUY_ITEM) {
			Boolean didUpgrade = attemptUpgrade(player);
			if (didUpgrade) {
				setLayout(player, mInventory, true);
				player.addScoreboardTag(PAID_SCOREBOARD_TAG);
			}
			return;
		}

		int slot;
		if (player.getScoreboardTags().contains(PAID_SCOREBOARD_TAG)) {
			switch (event.getSlot()) {
				case 10 -> slot = 0;
				case 11 -> slot = 1;
				case 12 -> slot = 2;
				case 14 -> slot = 3;
				case 15 -> slot = 4;
				case 16 -> slot = 5;
				default -> {
					player.closeInventory();
					return;
				}
			}
		} else {
			switch (event.getSlot()) {
				case 10 -> slot = 0;
				case 13 -> slot = 1;
				case 16 -> slot = 2;
				default -> {
					player.closeInventory();
					return;
				}
			}
		}

		DepthsManager.getInstance().playerChoseWeaponAspect((Player) event.getWhoClicked(), slot);
		player.removeScoreboardTag(PAID_SCOREBOARD_TAG);
		player.closeInventory();
	}

	void setLayout(Player player, Inventory inventory, Boolean paid) {
		int[] paidLocations = {10, 11, 12, 14, 15, 16};
		int[] unpaidLocations = {10, 13, 16};
		int[] chosenArray;

		DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
		if (depthsPlayer == null) {
			close();
			return;
		}
		List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> weapons = depthsPlayer.mWeaponOfferings;
		List<DepthsAbilityItem> items = new ArrayList<>();

		if (weapons == null || weapons.size() == 0) {
			close();
			return;
		}

		for (DepthsAbilityInfo<? extends WeaponAspectDepthsAbility> weapon : weapons) {
			items.add(weapon.getAbilityItem(1));
		}

		Material payButton = paid ? PURCHASED_MAT : BUY_ITEM;
		ItemStack paidFor = new ItemStack(payButton, 1);

		for (int i = 0; i < 27; i++) {
			inventory.setItem(i, new ItemStack(Material.RED_STAINED_GLASS_PANE, 1));
		}

		if (paid) {
			chosenArray = paidLocations;
			ItemMeta meta = paidFor.getItemMeta();
			meta.displayName(Component.text("Upgrade successfully purchased!", NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false));
			paidFor.setItemMeta(meta);
			inventory.setItem(18, paidFor);
		} else {
			chosenArray = unpaidLocations;
			ItemMeta meta = paidFor.getItemMeta();
			meta.displayName(Component.text("Purchase Upgrade", NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false));
			ArrayList<Component> loreLines = new ArrayList<>();
			loreLines.add(Component.text("Choose from all aspects", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
			loreLines.add(Component.text("for ", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(CCS_AMOUNT + " CCS", NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false)));
			meta.lore(loreLines);
			paidFor.setItemMeta(meta);
			inventory.setItem(18, paidFor);
		}

		int itemIndex = 0;
		for (int location : chosenArray) {
			inventory.setItem(location, items.get(itemIndex).mItem);
			itemIndex++;
		}
	}

	Boolean attemptUpgrade(Player player) {
		int totalToRemove = CCS_AMOUNT;
		ItemStack currencyItem = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/compressed_crystalline_shard"));
		if (currencyItem == null) {
			return false;
		}
		currencyItem.setAmount(1);
		String currencyName = "Compressed Crystalline Shard";

		if (player.getInventory().containsAtLeast(currencyItem, totalToRemove)) {

			HashMap<Integer, ? extends ItemStack> findItems = player.getInventory().all(currencyItem.getType());
			if (!findItems.isEmpty()) {
				for (Entry<Integer, ? extends ItemStack> set : findItems.entrySet()) {
					ItemStack testStack = set.getValue().clone();
					testStack.setAmount(1);
					if (ItemUtils.getPlainName(testStack).equals(currencyName)) {
						ItemStack foundItem = set.getValue();
						if (foundItem.getAmount() > totalToRemove) {
							foundItem.setAmount(foundItem.getAmount() - totalToRemove);
							player.getInventory().setItem(set.getKey(), foundItem);
							return true;
						} else if (foundItem.getAmount() == totalToRemove) {
							foundItem.setAmount(0);
							player.getInventory().setItem(set.getKey(), null);
							return true;
						} else {
							totalToRemove -= foundItem.getAmount();
							foundItem.setAmount(0);
						}
					}
				}
			}
		}
		return false;
	}
}
