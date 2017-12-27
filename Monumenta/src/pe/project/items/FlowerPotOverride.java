package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;

public class FlowerPotOverride extends OverrideItem {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		// Don't allow non-creative players to put saplings with lore text in flower pots
		return (player == null || player.getGameMode() == GameMode.CREATIVE || !item.hasItemMeta() || !item.getItemMeta().hasLore());
	}
}
