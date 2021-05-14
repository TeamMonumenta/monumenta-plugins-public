package com.playmonumenta.plugins.attributes;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;



public interface BaseAttribute {
	/*
	 * Required - the name of the property
	 */
	String getProperty();

	//Bow attributes
	default void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) { }

	//Thorns attribute
	default void onHurtByEntity(Plugin plugin, Player player, double value, EntityDamageByEntityEvent event) { }

	//Damage attributes
	//Currently only used for ability power.
	default void onDamage(Plugin plugin, Player player, double value, EntityDamageByEntityEvent event) { }
}