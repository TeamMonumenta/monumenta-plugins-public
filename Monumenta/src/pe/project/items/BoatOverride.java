package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;

public class BoatOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return player.getGameMode() != GameMode.ADVENTURE;	//	Prevent placing boats in adventure mode.
	}
}
