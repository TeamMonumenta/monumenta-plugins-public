package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class AttributeArrowSpeed implements BaseEnchantment {
	private static final String SPEED_METAKEY = "AttributeArrowSpeedMetakey";
	private static final String PROPERTY_NAME = " Arrow Speed";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean negativeLevelsAllowed() {
		return true;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player, ItemSlot slot) {
		return InventoryUtils.getCustomAttribute(item, getProperty(), player, slot);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		// If level is 0, that means we have no modifiers
		if (level != 0 && proj instanceof Arrow) {
			// This only ever gets percent modifiers, so we add the implicit base value of "1"
			double arrowSpeedModifier = InventoryUtils.getAttributeValue(level + 1);
			proj.setMetadata(SPEED_METAKEY, new FixedMetadataValue(plugin, arrowSpeedModifier));
			proj.setVelocity(proj.getVelocity().multiply(arrowSpeedModifier));
		}
	}

	public static double getArrowSpeedModifier(Projectile proj) {
		if (proj.hasMetadata(SPEED_METAKEY)) {
			return proj.getMetadata(AttributeArrowSpeed.SPEED_METAKEY).get(0).asDouble();
		}
		return 1;
	}
}
