package pe.project.item.properties;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class Radiant extends ItemProperty {
	private static String PROPERTY_NAME = "* Radiant *";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void applyProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.GLOWING, 1000000, 0, true, false));
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.GLOWING);
	}
}
