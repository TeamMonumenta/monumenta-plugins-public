package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import java.util.Collection;

public class ReworkRefundInfusions extends GenericCommand {
	public static void register() {
		registerPlayerCommand("refundinfusions", "monumenta.command.refundinfusions", ReworkRefundInfusions::run);
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item == null || item.getType() == Material.AIR) {
			sender.sendMessage("Must be holding an item!");
			return;
		}
		// Infusions
		InfusionUtils.refundInfusion(item, player, Plugin.getInstance());
		DelveInfusionUtils.refundInfusion(item, player);
		// Boss Enchants
		if (ItemStatUtils.getInfusionLevel(item, InfusionType.HOPE) > 0) {
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r1/items/currency/hyper_experience"), 3 * item.getAmount());
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r1/kaul/crownshard"), 9 * item.getAmount());
			ItemStatUtils.removeInfusion(item, InfusionType.HOPE, false);
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.PHYLACTERY) > 0) {
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard"), 2 * item.getAmount());
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r2/lich/materials/ancestral_effigy"), 6 * item.getAmount());
			ItemStatUtils.removeInfusion(item, InfusionType.PHYLACTERY, false);
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.COLOSSAL) > 0) {
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard"), 3 * item.getAmount());
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r2/eldrask/materials/epic_material"), 9 * item.getAmount());
			ItemStatUtils.removeInfusion(item, InfusionType.COLOSSAL, false);
		}
		// Locked
		if (ItemStatUtils.getInfusionLevel(item, InfusionType.LOCKED) > 0) {
			giveMaterials(player, NamespacedKeyUtils.fromString("epic:r1/items/currency/hyper_experience"), 3 * item.getAmount());
			ItemStatUtils.removeInfusion(item, InfusionType.LOCKED, false);
		}
		ItemStatUtils.generateItemStats(player.getInventory().getItemInMainHand());
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
		if (playerItemStats != null) {
			playerItemStats.updateStats(player, true);
		}
	}

	private static void giveMaterials(Player player, NamespacedKey key, int refundMaterials) throws WrapperCommandSyntaxException {
		LootTable lt = Bukkit.getLootTable(key);
		if (lt != null) {
			LootContext.Builder builder = new LootContext.Builder(player.getLocation());
			LootContext context = builder.build();
			Collection<ItemStack> items = lt.populateLoot(FastUtils.RANDOM, context);
			if (items.size() > 0) {
				ItemStack materials = items.iterator().next();
				materials.setAmount(refundMaterials);
				InventoryUtils.giveItem(player, materials);
				return;
			}
		}
		CommandAPI.fail("ERROR while refunding infusion (failed to get loot table). Please contact a moderator if you see this message!");
	}
}
