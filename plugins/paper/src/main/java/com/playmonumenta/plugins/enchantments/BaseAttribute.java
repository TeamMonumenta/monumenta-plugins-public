package com.playmonumenta.plugins.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import com.playmonumenta.plugins.Plugin;

public interface BaseAttribute {
	/*
	 * Required - the name of the property
	 */
	String getProperty();

	/*
	 * The onShootAttack() method will be called whenever the player damages something with a projectile while
	 * they have any levels of this property
	 */

	//Bow attributes
	default void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) { }

	//Thorns attribute
	default void onHurtByEntity(Plugin plugin, Player player, double value, EntityDamageByEntityEvent event) { }
}
