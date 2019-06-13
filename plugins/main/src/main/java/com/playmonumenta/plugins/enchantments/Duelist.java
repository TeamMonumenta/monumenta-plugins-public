package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Duelist implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Duelist";
	private static final String LEVEL_METAKEY = "DuelistLevelMetakey";
	private static final double DAMAGE_PER_LEVEL = 2.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (target instanceof Vindicator || target instanceof Illusioner || target instanceof Evoker || target instanceof Vex || target instanceof Witch || target instanceof IronGolem) {
			event.setDamage(event.getDamage() + DAMAGE_PER_LEVEL * level);
		}
	}

	// Thrown trident damage handling
	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (target instanceof Vindicator || target instanceof Illusioner || target instanceof Evoker || target instanceof Vex || target instanceof Witch || target instanceof IronGolem) {
			if (proj.hasMetadata(LEVEL_METAKEY)) {
				int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
				event.setDamage(event.getDamage() + level * DAMAGE_PER_LEVEL);
			}
		}
	}
}
