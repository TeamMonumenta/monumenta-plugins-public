package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class CalculateReforge extends GenericCommand {

	public static ItemStack mCXP = null;
	public static ItemStack mHXP = null;
	public static ItemStack mCCS = null;
	public static ItemStack mHCS = null;

	public static void register() {
		registerPlayerCommand("calculatereforge", "monumenta.command.calculatereforge", (sender, player) -> {
			run(player);
		});
	}

	@SuppressWarnings("deprecation")
	private static void run(Player player) throws WrapperCommandSyntaxException {
		// Grab currency items from the loot tables so we can scan for and remove them from players' inventories.
		if (mCXP == null) {
			mCXP = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r1/items/currency/concentrated_experience"));
		}
		if (mHXP == null) {
			mHXP = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r1/items/currency/hyper_experience"));
		}
		if (mCCS == null) {
			mCCS = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/compressed_crystalline_shard"));
		}
		if (mHCS == null) {
			mHCS = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard"));
		}

		List<ItemStack> shatteredItems = new ArrayList<>();
		for (ItemStack item : player.getInventory()) {
			if (ItemStatUtils.isShattered(item)) {
				shatteredItems.add(item);
			}
		}
		if (!shatteredItems.isEmpty()) {
			Map<Region, Integer> fullInventoryCost = ItemUtils.getReforgeCosts(shatteredItems);
			TextComponent message1 = null; // First line, Cost of reforging hand item
			TextComponent message2 = null; // Second line, Cost of reforging entire inventory
			TextComponent message3 = new TextComponent("[Reforge Held Item] "); // First clicable area, reforge hand item
			TextComponent message4 = new TextComponent("[Reforge all items]"); // Second clickable area, reforge entire inventory

			// Make clickable areas actually clickable
			message3.setColor(ChatColor.DARK_PURPLE);
			message3.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/reforgehelditem"));
			message4.setColor(ChatColor.DARK_PURPLE);
			message4.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/reforgeinventory"));

			// Check how much it costs to reforge the mainhand item
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null && ItemStatUtils.isShattered(hand)) {
				Region handRegion = ItemStatUtils.getRegion(hand);
				String handCurrency = null;
				if (handRegion == Region.VALLEY) {
					handCurrency = "XP";
				} else if (handRegion == Region.ISLES) {
					handCurrency = "CS";
				} else if (handRegion == Region.SHULKER_BOX) {
					// Use the current region to get the currency for the reforge.
					if (player.getWorld().getName().equals("Project_Epic-valley")) {
						// King's Valley: Use XP
						handCurrency = "XP";
					} else if (player.getWorld().getName().equals("Project_Epic-isles")) {
						// Celsian Isles: Use CS
						handCurrency = "CS";
					} else {
						// This shouldn't happen, but if it does, default to XP
						handCurrency = "XP";
					}
				}
				if (handCurrency != null) {
					// If the player is holding a shattered item, let them know how much it costs to reforge
					int handCost = ItemUtils.getReforgeCost(hand);
					message1 = new TextComponent(String.format(
					                                 "To reforge the item in your main hand, it will cost you %s%dH%s%s/%s%dC%s%s.\n",
					                                 ChatColor.YELLOW, handCost / 64, handCurrency, ChatColor.RESET, ChatColor.YELLOW,
					                                 handCost % 64, handCurrency, ChatColor.RESET));
				}
			}
			int cxp = fullInventoryCost.getOrDefault(Region.VALLEY, 0);
			int ccs = fullInventoryCost.getOrDefault(Region.ISLES, 0);
			int csb = fullInventoryCost.getOrDefault(Region.SHULKER_BOX, 0);
			if (csb != 0) {
				// The "Shulker Box" tag does not exist. It is internally used to identify shulker boxes.
				// Shulker boxes can be reforged with R1 or R2 currency, based on the region you are in.
				if (player.getWorld().getName().equals("Project_Epic-valley")) {
					// King's Valley: Use XP
					cxp += csb;
				} else if (player.getWorld().getName().equals("Project_Epic-isles")) {
					// Celsian Isles: Use CS
					ccs += csb;
				} else {
					// This shouldn't happen, but if it does, default to XP
					cxp += csb;
				}

			}

			// Tell the player the cost to reforge all items in their inventory
			if (cxp > 0 && ccs > 0) {
				message2 = new TextComponent(String.format(
						"To reforge all items in your inventory, it will cost you %s%dHXP%s/%s%dCXP%s and %s%dHCS%s/%s%dCCS%s.\n",
						ChatColor.YELLOW, cxp / 64, ChatColor.RESET, ChatColor.YELLOW, cxp % 64, ChatColor.RESET,
						ChatColor.YELLOW, ccs / 64, ChatColor.RESET, ChatColor.YELLOW, ccs % 64, ChatColor.RESET));
			} else if (cxp > 0) {
				message2 = new TextComponent(String.format(
						"To reforge all items in your inventory, it will cost you %s%dHXP%s/%s%dCXP%s\n",
						ChatColor.YELLOW, cxp / 64, ChatColor.RESET, ChatColor.YELLOW, cxp % 64, ChatColor.RESET));
			} else if (ccs > 0) {
				message2 = new TextComponent(String.format(
						"To reforge all items in your inventory, it will cost you %s%dHCS%s/%s%dCCS%s\n",
						ChatColor.YELLOW, ccs / 64, ChatColor.RESET, ChatColor.YELLOW, ccs % 64, ChatColor.RESET));
			} else {
				player.sendMessage("You don't have any shattered items");
				CommandAPI.fail("Player must have a Shattered item in their inventory!");
				return;
			}
			if (message1 != null && message2 != null) {
				player.spigot().sendMessage(message1, message2, message3, message4);
			} else if (message1 != null) {
				player.spigot().sendMessage(message1, message3);
			} else if (message2 != null) {
				player.spigot().sendMessage(message2, message4);
			}

			// Add the tag that allows the player to run the command to reforge their items, and remove it 30 seconds later.
			player.setMetadata("PlayerCanReforge", new FixedMetadataValue(Plugin.getInstance(), true));
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.hasMetadata("PlayerCanReforge")) {
						player.removeMetadata("PlayerCanReforge", Plugin.getInstance());
					}
				}
			}.runTaskLater(Plugin.getInstance(), 30 * 20);
		} else {
			player.sendMessage("You don't have any shattered items");
			CommandAPI.fail("Player must have a shattered item in their inventory");
		}
	}
}
