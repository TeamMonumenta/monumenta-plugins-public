package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class EnderChestOverride extends BaseOverride {
	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && ZoneUtils.isInPlot(player)) {
			return true;
		}

		return false;
	}

	/* Chests placed on barriers can not be broken */
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		if ((player.getGameMode() == GameMode.CREATIVE) || ChestOverride.breakable(block)) {
			ItemStack inHand = player.getInventory().getItemInMainHand();
			if (!ZoneUtils.isInPlot(player) && inHand != null && inHand.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 0) {
				event.setDropItems(false);
				Collection<ItemStack> drops = block.getDrops(new ItemStack(inHand.getType()));
				drops.forEach(drop -> player.getWorld().dropItemNaturally(block.getLocation(), drop));
			}
			return true;
		}
		return false;
	}
}
