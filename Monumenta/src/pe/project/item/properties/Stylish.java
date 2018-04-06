package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import pe.project.Plugin;
import pe.project.utils.ParticleUtils;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class Stylish implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.LIGHT_PURPLE + "Stylish";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR);
	}

	@Override
	public boolean hasTickingEffect() {
		return true;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
	}
}
