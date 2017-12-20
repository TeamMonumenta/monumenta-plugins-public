package pe.project.item.properties;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import pe.project.Plugin;

public class ItemProperty {
	public String getProperty() { return ""; }

	// By default item properties are valid in all slots
	public boolean validSlot(EquipmentSlot slot) { return true; }
	public boolean hasTickingEffect() { return false; }

	public void applyProperty(Plugin plugin, Player player) { }
	public void removeProperty(Plugin plugin, Player player) { }

	public void tick(Plugin plugin, World world, Player player) { }
}
