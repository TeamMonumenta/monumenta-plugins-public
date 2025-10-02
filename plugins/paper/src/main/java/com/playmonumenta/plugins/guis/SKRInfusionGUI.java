package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.infusions.Celerity;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionMaterial;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionSelection;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.custominventories.DelveInfusionCustomInventory.mMaxLevelReachedItem;
import static com.playmonumenta.plugins.custominventories.DelveInfusionCustomInventory.mMaxLevelReachedRevelationItem;

public class SKRInfusionGUI extends Gui {

	private static final NamespacedKey HAR_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/hyperchromatic_archos_ring");
	private static final NamespacedKey MEM_KEY = NamespacedKeyUtils.fromString("epic:r3/dungeons/skr/silver_memory_fragment");
	private static final int[] HAR_COSTS = {2, 4, 8, 16};
	private static final int[] MEM_COSTS = {2, 4, 6, 8};
	private static final int[] XP_COSTS = {ExperienceUtils.LEVEL_40, ExperienceUtils.LEVEL_50, ExperienceUtils.LEVEL_60, ExperienceUtils.LEVEL_70};

	private static final ItemStack REFUND_ITEM = GUIUtils.createBasicItem(Material.GRINDSTONE,
		"Click to refund this item's infusions.", NamedTextColor.DARK_GRAY, true,
		"You will receive " + (DelveInfusionUtils.FULL_REFUND ? "100" : (int) (DelveInfusionUtils.REFUND_PERCENT * 100)) + "% of the experience, and all of the materials back.", NamedTextColor.GRAY);
	private static final ItemStack CELERITY_INFO_ITEM = GUIUtils.createBasicItem(Material.FEATHER, 1,
		"Celerity", Location.SKR.getColor(), true,
		Component.text("Gain " + StringUtils.multiplierToPercentageWithSign(Celerity.SPEED_BONUS) + " Speed per level if there are no hostile mobs within an 18 block radius.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			.appendNewline()
			.append(Component.text("Requires Silver Memory Fragments and Hyperchromatic Archos Rings", NamedTextColor.DARK_GRAY)),
		30, true);


	public SKRInfusionGUI(Player player) {
		super(player, 3 * 9, Component.text("SKR Infusions"));
	}

	@Override
	protected void setup() {
		setItem(0, 4, CELERITY_INFO_ITEM);

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		DelveInfusionSelection infusion = DelveInfusionUtils.getCurrentInfusion(mainhand);

		// check valid item
		if (InfusionUtils.isInfusionable(mainhand)) {
			int level = DelveInfusionUtils.getInfuseLevel(mainhand);

			setItem(1, 1, mainhand);

			if (level > 0) {
				if (infusion != DelveInfusionSelection.CELERITY) {
					// item is already infused, but not with celerity
					setItem(1, 0, REFUND_ITEM).onClick((clickEvent) -> {
						DelveInfusionUtils.refundInfusion(mainhand, mPlayer);
						open();
					});
					setItem(1, 2, GUIUtils.createBasicItem(Material.BARRIER,
						"Cannot Infuse", NamedTextColor.RED, true,
						"This item already has a Delve Infusion on it. Refund it to infuse it with Celerity."));
					return;
				} else {
					// item is infused with freerunner, use sturdy refund method
					setItem(1, 0, REFUND_ITEM).onClick((clickEvent) -> {
						refundCelerity(mainhand, mPlayer);
						open();
					});
				}
			}

			// create level panes
			for (int i = 0; i < level; i++) {
				ItemStack levelItem = GUIUtils.createBasicItem(Material.FEATHER, 1,
					Component.text("Celerity", Location.SKR.getColor()).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)
						.append(Component.text(" [Lv. " + (i + 1) + "]", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)),
					Component.text("Gain " + StringUtils.multiplierToPercentageWithSign(Celerity.SPEED_BONUS * (i + 1)) + " Speed if there are no hostile mobs within an 18 block radius.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					30, true);

				setItem(1, 2 + i, levelItem);
			}


			if (level < DelveInfusionUtils.MAX_LEVEL) {
				// create "Click to upgrade" item
				ItemStack infuseItem = GUIUtils.createBasicItem(Material.ENCHANTED_BOOK,
					"Click to infuse to level " + (level + 1), NamedTextColor.DARK_AQUA, true,
					"You will need " + MEM_COSTS[level] + " Memory Fragments, " + HAR_COSTS[level] + " Hyperchromatic Archos Rings, and " + DelveInfusionUtils.getExpLvlInfuseCost(mainhand) + " experience levels", NamedTextColor.GRAY);
				setItem(1, 2 + level, infuseItem).onClick((clickEvent) ->
					attemptInfusion(mPlayer, mainhand, level + 1)
				);
			} else {
				if (!ItemStatUtils.hasInfusion(mainhand, InfusionType.REVELATION)) {
					setItem(1, 6, mMaxLevelReachedItem);
				} else {
					ItemStack level5item = GUIUtils.createBasicItem(Material.FEATHER, 1,
						Component.text("Celerity", Location.SKR.getColor()).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)
							.append(Component.text(" [Lv. 5]", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)),
						Component.text("Gain " + StringUtils.multiplierToPercentageWithSign(Celerity.SPEED_BONUS * 5) + " Speed if there are no hostile mobs within an 18 block radius.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
						30, true);
					setItem(1, 6, level5item);
					setItem(1, 7, mMaxLevelReachedRevelationItem);
				}
			}

		} else {
			setItem(1, 1, GUIUtils.createBasicItem(Material.ARMOR_STAND,
				"Invalid Item", NamedTextColor.RED, true,
				"You must be holding a valid item to infuse!", NamedTextColor.GRAY));
		}
	}

	private void attemptInfusion(Player p, ItemStack item, int level) {
		if (item.getAmount() > 1) {
			p.sendMessage(Component.text("You cannot infuse stacked items.", NamedTextColor.RED));
			return;
		}
		if (!InfusionUtils.isInfusionable(item)) {
			p.sendMessage(Component.text("This item cannot be infused.", NamedTextColor.RED));
			return;
		}

		try {
			if (payInfusion(item, level)) {
				DelveInfusionUtils.infuseItem(p, item, DelveInfusionSelection.CELERITY, DelveInfusionMaterial.MEMORY_FRAGMENTS);
				open();
			} else {
				p.sendMessage(Component.text("You don't have enough experience and/or currency for this infusion.", NamedTextColor.RED));
			}
		} catch (Exception e) {
			p.sendMessage(Component.text("If you see this message please contact a mod! (Error in infusing)", NamedTextColor.RED));
			e.printStackTrace();
		}
	}

	private boolean payInfusion(ItemStack item, int level) {
		if (DelveInfusionSelection.CELERITY.getLootTable() == null || level <= 0) {
			return false;
		}

		List<ItemStack> mats = getCurrencyCost(level, mPlayer);

		int playerXP = ExperienceUtils.getTotalExperience(mPlayer);
		int xpCost = XP_COSTS[level - 1];
		if (playerXP < xpCost) {
			return false;
		}

		if (!WalletUtils.tryToPayFromInventoryAndWallet(mPlayer, mats)) {
			return false;
		}

		ExperienceUtils.setTotalExperience(mPlayer, playerXP - xpCost);

		String matStr = mats.stream().filter(it -> it != null && it.getAmount() > 0)
			.map(it -> "'" + ItemUtils.getPlainName(it) + ":" + it.getAmount() + "'")
			.collect(Collectors.joining(","));
		AuditListener.logPlayer("[Delve Infusion] Item infused - player=" + mPlayer.getName() + ", item='" + ItemUtils.getPlainName(item) + "', infusion type=" + DelveInfusionSelection.CELERITY.getLabel()
			+ "', new level=" + level + ", stack size=" + item.getAmount() + ", material cost=" + matStr + ", XP cost=" + xpCost);
		if (!ItemStatUtils.checkOwnership(mPlayer, item)) {
			AuditListener.logPlayer("[Ownership Tracker] Player " + mPlayer.getName() + " changed an item ('" + ItemUtils.getPlainName(item) + "') that was not Owned by them.");
		}

		return true;
	}

	public static void refundCelerity(ItemStack item, Player player) {
		DelveInfusionSelection infusion = DelveInfusionUtils.getCurrentInfusion(item);
		if (infusion == null) {
			return;
		}
		InfusionType infusionType = infusion.getInfusionType();
		if (infusionType == null) {
			return;
		}

		int level = DelveInfusionUtils.getInfuseLevel(item);

		/* Audit */
		String matStr = "";
		int totalXP = 0;
		for (int i = 0; i < level; i++) {
			List<ItemStack> mats = getCurrencyCost(i + 1, player);

			for (ItemStack mat : mats) {
				InventoryUtils.giveItem(player, mat);
			}

			int xpRefund = (int) (XP_COSTS[i] * (DelveInfusionUtils.FULL_REFUND ? 1 : DelveInfusionUtils.REFUND_PERCENT) * item.getAmount());
			ExperienceUtils.setTotalExperience(player, ExperienceUtils.getTotalExperience(player) + xpRefund);
			totalXP += xpRefund;

			/* Audit */
			for (ItemStack it : mats) {
				if (it != null && it.getAmount() > 0) {
					if (!matStr.isEmpty()) {
						matStr += ",";
					}
					matStr += "'" + ItemUtils.getPlainName(it) + ":" + it.getAmount() + "'";
				}
			}
		}

		ItemStatUtils.removeInfusion(item, infusionType);
		DelveInfusionUtils.removeDelveInfusionMaterial(item);

		AuditListener.logPlayer("[Delve Infusion] Refund - player=" + player.getName() + ", item='" + ItemUtils.getPlainName(item) + "', infusion type=" + infusionType
			+ "', from level=" + level + ", stack size=" + item.getAmount() + ", refunded materials=" + matStr + ", refunded XP=" + totalXP);
		if (!ItemStatUtils.checkOwnership(player, item)) {
			AuditListener.logPlayer("[Ownership Tracker] Player " + player.getName() + " changed an item ('" + ItemUtils.getPlainName(item) + "') that was not Owned by them.");
		}

	}

	private static List<ItemStack> getCurrencyCost(int level, Player player) {
		List<ItemStack> costs = new ArrayList<>();

		ItemStack harCost = InventoryUtils.getItemFromLootTable(player, HAR_KEY);
		if (harCost != null) {
			harCost.setAmount(HAR_COSTS[level - 1]);
			costs.add(harCost);
		}
		ItemStack fragCost = InventoryUtils.getItemFromLootTable(player, MEM_KEY);
		if (fragCost != null) {
			fragCost.setAmount(MEM_COSTS[level - 1]);
			costs.add(fragCost);
		}

		return costs;
	}
}
