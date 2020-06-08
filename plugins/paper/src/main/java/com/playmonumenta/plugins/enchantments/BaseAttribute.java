package com.playmonumenta.plugins.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
	default void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) { }

	/*
	 * Will add more methods as becomes relevant; currently only used for bow attributes
	 */
}
