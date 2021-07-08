package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Regicide implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Regicide";
	private static final double DAMAGE_BONUS_PER_LEVEL = 0.1;
	private static final double BOSS_BONUS_PER_LEVEL = 0.05;
	public static final String LEVEL_METAKEY = "RegicideLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	public static double calculateDamage(int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (EntityUtils.isElite(target)) {
			return event.getDamage() * (1 + DAMAGE_BONUS_PER_LEVEL * level);
		} else if (EntityUtils.isBoss(target)) {
			return event.getDamage() * (1 + BOSS_BONUS_PER_LEVEL * level);
		} else {
			return event.getDamage();
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		event.setDamage(calculateDamage(level, target, event));
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
	}

	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY)) {
			event.setDamage(calculateDamage(proj.getMetadata(LEVEL_METAKEY).get(0).asInt(), target, event));
		}
	}
}
