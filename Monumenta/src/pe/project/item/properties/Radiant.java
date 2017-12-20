package pe.project.item.properties;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class Radiant extends ItemProperty {
	private static String PROPERTY_NAME = "* Radiant *";

	@Override
	public boolean hasTickingEffect() {
		return true;
	}

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean requireSoulbound() {
		return true;
	}

	@Override
	public void applyProperty(Plugin plugin, Player player) {
		// Radiant is different from the others - it applies effects only for a short duration
		// and doesn't remove them when you switch off
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.GLOWING, 600, 0, true, false));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, true, false));
	}

	@Override
	public void tick(Plugin plugin, World world, Player player) {
		applyProperty(plugin, player);
	}
}
