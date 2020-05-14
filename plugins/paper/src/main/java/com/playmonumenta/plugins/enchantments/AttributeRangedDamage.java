package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class AttributeRangedDamage implements BaseEnchantment {
	private static final String PROPERTY_NAME = " Ranged Damage";
	public static final String DAMAGE_METAKEY = "AttributeRangedDamageMetakey";
	// Bow velocity comes out at around 2.95 to 3.05
	private static final double ARROW_VELOCITY_SCALE = 3;

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
		// This should never be called with level = 0, but it doesn't hurt anything
		if (level != 0) {
			proj.setMetadata(DAMAGE_METAKEY, new FixedMetadataValue(plugin, InventoryUtils.getAttributeValue(level)));
		}
	}

	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(DAMAGE_METAKEY)) {
			double damage = proj.getMetadata(DAMAGE_METAKEY).get(0).asDouble();
			// Only scale damage if not fully charged arrow and if it is an arrow being launched
			if (proj instanceof Arrow && !((Arrow) proj).isCritical()) {
				// Arrow speed will be different if arrow speed attribute is active, so scale properly
				damage *= Math.min(1, proj.getVelocity().length() / ARROW_VELOCITY_SCALE / AttributeArrowSpeed.getArrowSpeedModifier(proj));
			}

			//Regardless of projectile type, set damage here
			event.setDamage(damage);
		}
	}
}
