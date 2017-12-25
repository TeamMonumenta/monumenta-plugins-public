package pe.project.item.properties;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class Regeneration extends ItemProperty {
	private static String PROPERTY_NAME = "* Regeneration *";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean validSlot(EquipmentSlot slot) {
		// Valid in all slots except main hand
		return !EquipmentSlot.HAND.equals(slot);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 1000049, 0, true, false));
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.REGENERATION);
	}
}
