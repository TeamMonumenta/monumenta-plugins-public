package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class Radiant implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Radiant";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		// Radiant is different from the others - it applies effects only for a short duration
		// and doesn't remove them when you switch off
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.GLOWING, 600, 0, true, false));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, true, false));
	}

	@Override
	public boolean hasTickingEffect() {
		return true;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		applyProperty(plugin, player, level);
	}
}
