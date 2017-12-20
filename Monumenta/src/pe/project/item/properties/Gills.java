package pe.project.item.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class Gills extends ItemProperty {
	private static String PROPERTY_NAME = "* Gills *";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean validSlot(EquipmentSlot slot) {
		// Only valid in offhand slot
		return EquipmentSlot.OFF_HAND.equals(slot);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.WATER_BREATHING, 1000000, 0, true, false));
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.WATER_BREATHING);
	}
}
