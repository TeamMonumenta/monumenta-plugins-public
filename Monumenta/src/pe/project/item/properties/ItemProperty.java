package pe.project.item.properties;

import org.bukkit.World;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class ItemProperty {
	public String getProperty() { return ""; }
	public boolean hasTickingEffect() { return false; }

	public void applyProperty(Plugin plugin, Player player) { }
	public void removeProperty(Plugin plugin, Player player) { }

	public void tick(Plugin plugin, World world, Player player) { }
}
