package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class MainhandRegeneration implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.LIGHT_PURPLE + "Mainhand Regeneration";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 1000049, 0, true, false));
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.REGENERATION);
	}
}
