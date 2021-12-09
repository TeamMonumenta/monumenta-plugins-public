package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;


public class Choler implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Choler";
	private static final double DAMAGE_MLT_PER_LVL = 0.01;
	public static final String LEVEL_METAKEY = "CholerLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity enemy, EntityDamageByEntityEvent entityDamageByEntityEvent) {
		if (EntityUtils.isStunned(enemy) || EntityUtils.isSlowed(plugin, enemy) || enemy.getFireTicks() > 0) {
			entityDamageByEntityEvent.setDamage(entityDamageByEntityEvent.getDamage() * (1 + (DAMAGE_MLT_PER_LVL * DelveInfusionUtils.getModifiedLevel(plugin, player, level))));
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, DelveInfusionUtils.getModifiedLevel(plugin, player, level)));
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
			double level = proj.getMetadata(LEVEL_METAKEY).get(0).asDouble();
			if (EntityUtils.isStunned(target) || EntityUtils.isSlowed(plugin, target) || target.getFireTicks() > 0) {
				event.setDamage(event.getDamage() * (1 + (DAMAGE_MLT_PER_LVL * level)));
			}
		}
	}

}
