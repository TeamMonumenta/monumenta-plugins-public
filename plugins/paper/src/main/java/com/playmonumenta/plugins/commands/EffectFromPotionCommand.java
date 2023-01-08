package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scoreboard.Scoreboard;

/**
 * EffectFromPotionCommand (Last written by PikaLegend_ 31.03.2022)
 * /effectfrompotion is a command for consuming potions from inventory (mainly for use of PotionInjector).
 * <p>
 * Syntax: /effectfrompotion <player> <slotObjective> [subSlotObjective]
 * <p>
 * Slot corresponds to the item in player's inventory.
 * If subSlot is empty, it is assumed that slot is a potion and therefore will consume it.
 * If subSlot is specified, it is assumed the item is a shulkerbox and therefore looks into the shulker's
 * inventory to obtain the potion, and consume it.
 */
public class EffectFromPotionCommand {

	private static final String COMMAND = "effectfrompotion";
	private static final String PERMISSION = "monumenta.command.effectfrompotion";

	public static void register(Plugin plugin) {
		new CommandAPICommand(COMMAND)
			// Syntax:
			// /effectfrompotion <Selector> <slotObjective>
			// Consume potion in slot based on scoreboard value.
			.withPermission(CommandPermission.fromString(PERMISSION))
			.withArguments(
				new EntitySelectorArgument.OnePlayer("players"),
				new ObjectiveArgument("slotScoreboard")
			)
			.executes((sender, args) -> {
				// Obtain Player, SlotScore, and SubSlot Score
				Player player = (Player) args[0];
				String slotScore = (String) args[1];

				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				int slot = scoreboard.getObjective(slotScore).getScore(player.getName()).getScore();

				potionLookUp(plugin, player, slot);
			})
			.register();

		new CommandAPICommand(COMMAND)
			// Syntax:
			// /effectfrompotion <Player> <slotObjective> <subSlotObjective>
			// Consume potion in slot based on scoreboard value.
			.withPermission(CommandPermission.fromString(PERMISSION))
			.withArguments(
				new EntitySelectorArgument.OnePlayer("players"),
				new ObjectiveArgument("slotScoreboard"),
				new ObjectiveArgument("subSlotScoreboard")
			)
			.executes((sender, args) -> {
				// Obtain Player, SlotScore, and SubSlot Score
				Player player = (Player) args[0];
				String slotScore = (String) args[1];
				String subSlotScore = (String) args[2];

				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				int slot = scoreboard.getObjective(slotScore).getScore(player.getName()).getScore();
				int subSlot = scoreboard.getObjective(subSlotScore).getScore(player.getName()).getScore();

				potionLookUp(plugin, player, slot, subSlot);
			})
			.register();
	}

	private static void potionLookUp(Plugin plugin, Player player, int slot) {
		potionLookUp(plugin, player, slot, -1);
	}

	private static void potionLookUp(Plugin plugin, Player player, int slot, int subSlot) {
		// Check if potion is in inventory;
		if (subSlot < 0) {
			// Potion is in inventory.
			// slot = Slot of Potion
			ItemStack potion = player.getInventory().getItem(slot);

			// If item is a potion, apply effects
			if (potion.getItemMeta() instanceof PotionMeta potionMeta || ItemStatUtils.isConsumable(potion)) {
				ItemStack updatedPotion = consumePotion(plugin, player, potion);

				player.getInventory().setItem(slot, updatedPotion);
			}
		} else {
			// Potion is in Shulker Box
			// slot = Slot of Shulker Box (PI), subSlot = Slot in Shulker Box.
			ItemStack shulkerBox = player.getInventory().getItem(slot);

			// If item is indeed a shulkerbox
			if (shulkerBox.getItemMeta() instanceof BlockStateMeta shulkerBoxMeta
				&& shulkerBoxMeta.getBlockState() instanceof ShulkerBox shulkerBoxMetaBlockState) {
				ItemStack potion = shulkerBoxMetaBlockState.getInventory().getItem(subSlot);

				// If item is indeed a potion, apply effects.
				if (potion.getItemMeta() instanceof PotionMeta || ItemStatUtils.isConsumable(potion)) {
					ItemStack updatedPotion = consumePotion(plugin, player, potion);

					// Set potion into the shulkerbox
					shulkerBoxMetaBlockState.getInventory().setItem(subSlot, updatedPotion);
					shulkerBoxMeta.setBlockState(shulkerBoxMetaBlockState);
					shulkerBox.setItemMeta(shulkerBoxMeta);
					player.getInventory().setItem(slot, shulkerBox);
				}
			}
		}
	}

	/**
	 * Consume potion and apply potion effects. Return ItemStack potion.
	 *
	 * @param plugin = Plugin
	 * @param player = Player
	 * @param potion = ItemStack
	 * @return updatedPotion = ItemStack
	 */
	private static ItemStack consumePotion(Plugin plugin, Player player, ItemStack potion) {
		if (ItemStatUtils.isConsumable(potion)) {
			ItemStatUtils.applyCustomEffects(plugin, player, potion);
		}

		ItemStack updatedPotion;

		// Test for Starvation.
		int starvation = ItemStatUtils.getEnchantmentLevel(potion, ItemStatUtils.EnchantmentType.STARVATION);
		if (starvation > 0) {
			Starvation.apply(player, starvation);
		}

		// Check for Cleric Sacred Provisions
		if (NonClericProvisionsPassive.testRandomChance(player)) {
			// Cleric Sacred Provisions triggers, make nice sounds.
			NonClericProvisionsPassive.sacredProvisionsSound(player);
			updatedPotion = potion;
		} else {
			// If there is more than one potion, decrement potion by one
			if (potion.getAmount() > 1) {
				potion.setAmount(potion.getAmount() - 1);
				updatedPotion = potion;
			} else {
				// Set potion to air.
				updatedPotion = new ItemStack(Material.AIR);
			}
		}

		return updatedPotion;
	}
}
