package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public final class ZenithCharmPowerGUI extends CustomInventory {
	public static final NamespacedKey GEODE_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/indigo_blightdust");
	private static final Material FILLER = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
	private static final Material UNPURCHASED_MAT = Material.ORANGE_STAINED_GLASS_PANE;
	private static final Material PURCHASED_MAT = Material.LIME_STAINED_GLASS_PANE;
	private static final String CHARM_SCOREBOARD = "DepthsCharmPower";
	private static final List<Triple<Integer, Integer, Integer>> SLOT_POWER_COST = List.of(
		Triple.of(10, 9, 0),
		Triple.of(11, 10, 32),
		Triple.of(12, 11, 64),
		Triple.of(13, 12, 128),
		Triple.of(14, 13, 256),
		Triple.of(15, 14, 512),
		Triple.of(16, 15, 1024)
	);

	public ZenithCharmPowerGUI(Player player) {
		super(player, 27, "Zenith Charm Power");
		if (getCharmPowerLevel(player) < 9) {
			ScoreboardUtils.setScoreboardValue(player, CHARM_SCOREBOARD, 9);
		}
		setLayout(player);
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
		for (Triple<Integer, Integer, Integer> slotInfo : SLOT_POWER_COST) {
			if (event.getSlot() == slotInfo.getLeft()) {
				if (getCharmPowerLevel(player) == slotInfo.getMiddle() - 1) {
					if (attemptUpgrade(player, slotInfo)) {
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
						EntityUtils.fireworkAnimation(player.getLocation(), List.of(Color.GRAY, Color.WHITE, Color.RED), FireworkEffect.Type.BURST, 5);
						if (slotInfo.getMiddle() == 15) {
							MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + player.getName() + "\",\"color\":\"gold\",\"bold\":true,\"italic\":true},{\"text\":\" has purchased the final charm power upgrade for Celestial Zenith! Congratulations!\",\"color\":\"gold\",\"italic\":true,\"bold\":false}]");
						}
						ScoreboardUtils.setScoreboardValue(player, CHARM_SCOREBOARD, slotInfo.getMiddle());
					} else {
						player.sendMessage("Need more Indigo Blightdust to purchase this!");
					}
				} else {
					player.sendMessage("You need to purchase the earlier levels of charm power first, you currently have " + getCharmPowerLevel(player) + " charm power!");
				}
			}
		}
		setLayout(player);
	}

	void setLayout(Player player) {
		for (Triple<Integer, Integer, Integer> slotInfo : SLOT_POWER_COST) {
			ItemStack builtItem;
			ItemMeta builtMeta;
			//left slot, middle power level, right cost
			if (getCharmPowerLevel(player) < slotInfo.getMiddle()) {
				builtItem = new ItemStack(UNPURCHASED_MAT, 1);
				builtMeta = builtItem.getItemMeta();
				builtMeta.displayName(Component.text("Purchase upgrade to " + slotInfo.getMiddle() + " charm power",
						NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false));
				GUIUtils.splitLoreLine(builtMeta, "Costs " + slotInfo.getRight() + " Indigo Blightdust",
					NamedTextColor.GRAY, 30, true);
			} else {
				builtItem = new ItemStack(PURCHASED_MAT, 1);
				builtMeta = builtItem.getItemMeta();
				builtMeta.displayName(Component.text("Already have " + slotInfo.getMiddle() + " power",
						NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false));
			}
			builtItem.setItemMeta(builtMeta);
			mInventory.setItem(slotInfo.getLeft(), builtItem);
		}

		mInventory.setItem(4, GUIUtils.createBasicItem(Material.NETHER_STAR, "Hover below for levels and costs.", NamedTextColor.WHITE));
	}

	Boolean attemptUpgrade(Player player, Triple<Integer, Integer, Integer> slotInfo) {
		int totalToRemove = slotInfo.getRight();
		ItemStack currencyItemReq = InventoryUtils.getItemFromLootTable(player, GEODE_KEY);
		if (currencyItemReq == null) {
			return false;
		}
		currencyItemReq.setAmount(totalToRemove);
		return WalletUtils.tryToPayFromInventoryAndWallet(player, currencyItemReq);
	}

	int getCharmPowerLevel(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, CHARM_SCOREBOARD).orElse(0);
	}
}
