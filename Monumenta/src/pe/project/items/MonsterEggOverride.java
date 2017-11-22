package pe.project.items;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;

public class MonsterEggOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		Bukkit.broadcastMessage("broken");

		//	Are we clicking on a monster spawner?
		return (block.getType() != Material.MOB_SPAWNER);
	}
}
