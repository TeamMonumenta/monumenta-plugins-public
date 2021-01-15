package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;

public class RegionScalingDamageDealt implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.DARK_GRAY + "Celsian Isles : ";

	private static final String APPLY_MULTIPLIER_METAKEY = "ApplyRegionScalingMultiplier";
	private static final double DAMAGE_DEALT_MULTIPLIER = 0.5;

	private static final String ATTRIBUTE_CANCELLATION_NAME = "RegionScalingAttributeCancellation";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public int getLevelFromItem(ItemStack item) {
		return ServerProperties.getClassSpecializationsEnabled() ? 0 : BaseEnchantment.super.getLevelFromItem(item);
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int value, Projectile proj, ProjectileLaunchEvent event) {
		proj.setMetadata(APPLY_MULTIPLIER_METAKEY, new FixedMetadataValue(plugin, null));
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
	}

	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(APPLY_MULTIPLIER_METAKEY)) {
			event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM,
				new PotionEffect(PotionEffectType.SLOW_DIGGING, 20, 0, false, true));
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_ARMOR, ATTRIBUTE_CANCELLATION_NAME);

		ItemStack mainhand = player.getInventory().getItemInMainHand();
		if (mainhand != null) {
			ItemMeta meta = mainhand.getItemMeta();
			if (meta != null) {
				for (AttributeModifier modifier : meta.getAttributeModifiers(Attribute.GENERIC_ARMOR)) {
					if (modifier.getSlot() == EquipmentSlot.HAND && modifier.getAmount() > 0) {
						EntityUtils.addAttribute(player, Attribute.GENERIC_ARMOR,
								new AttributeModifier(ATTRIBUTE_CANCELLATION_NAME, -modifier.getAmount(), modifier.getOperation()));
					}
				}
			}
		}
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_ARMOR, ATTRIBUTE_CANCELLATION_NAME);

		// Potion effects need to be applied for at least 1 second, so we remove it manually so that there's no "spillover" effect
		PotionEffect effect = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
		if (effect != null && effect.getDuration() <= 20) {
			player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
		}
	}

}
