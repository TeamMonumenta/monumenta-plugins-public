package com.playmonumenta.plugins.enchantments.infusions;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.server.properties.ServerProperties;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class Focus implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Focus";
	private static final String LEVEL_METAKEY = "FocusLevelMetakey";
	private static final double DAMAGE_PCT_PER_LEVEL = 0.01;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		if (ServerProperties.getInfusionsEnabled()) {
			return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
		} else {
			return EnumSet.noneOf(ItemSlot.class);
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
	}


	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY)) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			event.setDamage(event.getDamage() * (1.0 + level * DAMAGE_PCT_PER_LEVEL));
		}
	}
}
