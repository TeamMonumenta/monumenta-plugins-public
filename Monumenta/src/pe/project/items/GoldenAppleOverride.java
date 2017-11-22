package pe.project.items;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;

public class GoldenAppleOverride extends OverrideItem {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		return !(clickedEntity instanceof ZombieVillager);
	}
}
