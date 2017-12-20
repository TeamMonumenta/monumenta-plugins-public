package pe.project.item.properties;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import pe.project.Plugin;
import pe.project.utils.ParticleUtils;

public class Stylish extends ItemProperty {
	private static String PROPERTY_NAME = "* Stylish *";

	@Override
	public boolean hasTickingEffect() {
		return true;
	}

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean validSlot(EquipmentSlot slot) {
		// Stylish *shirt*
		return EquipmentSlot.CHEST.equals(slot);
	}

	@Override
	public void tick(Plugin plugin, World world, Player player) {
		ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
	}
}
