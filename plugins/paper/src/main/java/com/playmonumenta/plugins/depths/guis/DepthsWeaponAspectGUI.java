package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
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
	private static final int AR_AMOUNT = 16;
	private static final String PAID_SCOREBOARD_TAG = "DepthsWeaponAspectUpgradeBought";

	public DepthsWeaponAspectGUI(Player player) {
		super(player, 27, "Select an Aspect");


		setLayout(player, mInventory, player.getScoreboardTags().contains(PAID_SCOREBOARD_TAG));
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
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
			boolean didUpgrade = attemptUpgrade(player);
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

		DepthsManager.getInstance().playerChoseWeaponAspect(player, slot);
		player.removeScoreboardTag(PAID_SCOREBOARD_TAG);
		player.closeInventory();
	}

	void setLayout(Player player, Inventory inventory, boolean paid) {
		int[] paidLocations = {10, 11, 12, 14, 15, 16};
		int[] unpaidLocations = {10, 13, 16};
		int[] chosenArray;

		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null) {
			close();
			return;
		}

		List<String> items = depthsPlayer.mWeaponOfferings;
		if (items == null || items.isEmpty()) {
			close();
			return;
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
			List<Component> loreLines = new ArrayList<>();
			loreLines.add(Component.text("Choose from all aspects", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
			if (DepthsUtils.getDepthsContent().equals(DepthsContent.DARKEST_DEPTHS)) {
				loreLines.add(Component.text("for ", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(CCS_AMOUNT + " CCS", NamedTextColor.YELLOW)
						.decoration(TextDecoration.ITALIC, false)));
			} else {
				loreLines.add(Component.text("for ", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.text(AR_AMOUNT + " AR", NamedTextColor.YELLOW)
						.decoration(TextDecoration.ITALIC, false)));
			}

			meta.lore(loreLines);
			paidFor.setItemMeta(meta);
			inventory.setItem(18, paidFor);
		}

		int itemIndex = 0;
		for (int location : chosenArray) {
			String name = items.get(itemIndex);
			DepthsAbilityInfo<?> info = DepthsManager.getInstance().getAbility(name);
			if (info == null) {
				continue;
			}
			DepthsAbilityItem item = info.getAbilityItem(1, null);
			if (item == null) {
				continue;
			}
			inventory.setItem(location, item.getItem(player));
			itemIndex++;
		}
	}

	boolean attemptUpgrade(Player player) {
		boolean r2 = DepthsUtils.getDepthsContent() == DepthsContent.DARKEST_DEPTHS;
		int totalToRemove = r2 ? CCS_AMOUNT : AR_AMOUNT;
		ItemStack currencyItemReq = InventoryUtils.getItemFromLootTable(player,
			NamespacedKeyUtils.fromString(r2 ? "epic:r2/items/currency/compressed_crystalline_shard" : "epic:r3/items/currency/archos_ring"));
		if (currencyItemReq == null) {
			return false;
		}
		currencyItemReq.setAmount(totalToRemove);
		return WalletUtils.tryToPayFromInventoryAndWallet(player, currencyItemReq);
	}
}
