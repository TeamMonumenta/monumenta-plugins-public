package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.MonumentaTrigger;
import com.playmonumenta.plugins.commands.PickLevelAfterAnvils;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LimeTesseractOverride extends BaseOverride {

	public static int CONCENTRATED_CURRENCY_PER_ANVIL = 8;

	// left click in hand: take out anvils (only upgraded tess)
	// (putting in anvils is handled in AnvilFixInInventory)
	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (!ItemStatUtils.isUpgradedLimeTesseract(item)) {
			return true;
		}
		if (!checkCanUse(player, item)) {
			return false;
		}
		int charges = ItemStatUtils.getCharges(item);
		if (charges == 0) {
			player.sendMessage(Component.text("There are no anvils in this tesseract.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			return false;
		}
		int given = Math.min(charges, 64);
		InventoryUtils.giveItemFromLootTable(player, PickLevelAfterAnvils.ANVIL_TABLE, given);
		ItemStatUtils.setCharges(item, charges - given);
		ItemStatUtils.generateItemStats(item);
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 2);
		player.sendMessage(Component.text("The tesseract pulses and " + (given == 1 ? "an anvil appears" : given + " anvils appear") + " in your inventory.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
		return false;
	}

	// right click in hand or inventory: make one anvil
	// shift + right click in hand: make all anvils (with confirmation), or make anvils while keeping a configurable amount of levels
	@Override
	public boolean inventoryClickInteraction(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() != ClickType.RIGHT || !isAnyLimeTesseract(item)) {
			return true;
		}
		if (!checkCanUse(player, item)) {
			return false;
		}
		makeOneAnvil(player, item);
		return false;
	}

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (!isAnyLimeTesseract(item)) {
			return true;
		}
		if (!checkCanUse(player, item)) {
			return false;
		}
		if (player.isSneaking()) {
			if (player.getLevel() < PickLevelAfterAnvils.LEVELS_PER_ANVIL) {
				player.sendMessage(Component.text("You need " + PickLevelAfterAnvils.LEVELS_PER_ANVIL + " or more levels to operate the tesseract.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				return false;
			}
			player.sendMessage(Component.text("Convert all available experience into anvils?", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
			player.sendMessage(Component.text("[Convert All]", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
				.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, MonumentaTrigger.makeTrigger(player, LimeTesseractOverride::makeAllAnvils)))
				.append(Component.text("  "))
				.append(Component.text("[Select levels to keep]", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
					.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, MonumentaTrigger.makeTrigger(player, PickLevelAfterAnvils::run)))));
		} else {
			makeOneAnvil(player, item);
		}
		return false;
	}

	private static boolean checkCanUse(Player player, ItemStack item) {
		if (item.getAmount() > 1) {
			player.sendMessage(Component.text("Cannot use stacked tesseracts!", NamedTextColor.RED));
			return false;
		}
		if (ScoreboardUtils.getScoreboardValue(player, "Quest114").orElse(0) < 20) {
			player.sendMessage(Component.text("You must finish Primeval Creations IV before you can use this tesseract.", NamedTextColor.RED));
			return false;
		}
		return true;
	}

	private void makeOneAnvil(Player player, ItemStack item) {
		if (player.getLevel() >= PickLevelAfterAnvils.LEVELS_PER_ANVIL) {
			ExperienceUtils.addTotalExperience(player, -PickLevelAfterAnvils.XP_PER_ANVIL);
			if (ItemStatUtils.isUpgradedLimeTesseract(item)) {
				player.sendMessage(Component.text("The tesseract pulls from your intellect and gains an anvil charge.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				ItemStatUtils.setCharges(item, ItemStatUtils.getCharges(item) + 1);
				ItemStatUtils.generateItemStats(item);
			} else {
				player.sendMessage(Component.text("The tesseract pulls from your intellect, and you feel your inventory get slightly heavier.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				InventoryUtils.giveItemFromLootTable(player, PickLevelAfterAnvils.ANVIL_TABLE, 1);
			}
		} else {
			player.sendMessage(Component.text("You need " + PickLevelAfterAnvils.LEVELS_PER_ANVIL + " or more levels to operate the tesseract.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
	}

	public static void makeAllAnvils(Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (!isAnyLimeTesseract(item)) {
			player.sendMessage(Component.text("You must be holding the tesseract when using this!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			return;
		}
		if (!checkCanUse(player, item)) {
			return;
		}
		int totalExperience = ExperienceUtils.getTotalExperience(player);
		int anvilsCreated = totalExperience / PickLevelAfterAnvils.XP_PER_ANVIL;
		ExperienceUtils.setTotalExperience(player, totalExperience - anvilsCreated * PickLevelAfterAnvils.XP_PER_ANVIL);
		if (ItemStatUtils.isUpgradedLimeTesseract(item)) {
			player.sendMessage(Component.text("The tesseract pulls from your intellect and gains " + anvilsCreated + " anvil charges.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
			ItemStatUtils.setCharges(item, ItemStatUtils.getCharges(item) + anvilsCreated);
			ItemStatUtils.generateItemStats(item);
		} else {
			player.sendMessage(Component.text("The tesseract pulls from your intellect, and gives you " + anvilsCreated + " anvils.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
			InventoryUtils.giveItemFromLootTable(player, PickLevelAfterAnvils.ANVIL_TABLE, anvilsCreated);
		}
	}

	public static boolean isAnyLimeTesseract(ItemStack item) {
		if (item == null || item.getType() != Material.LIME_STAINED_GLASS) {
			return false;
		}
		String name = ItemUtils.getPlainNameIfExists(item);
		return "Tesseract of Knowledge".equals(name) || "Tesseract of Knowledge (u)".equals(name);
	}

}
