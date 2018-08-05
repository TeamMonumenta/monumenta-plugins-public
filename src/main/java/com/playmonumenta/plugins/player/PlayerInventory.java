package com.playmonumenta.plugins.player;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemProperty;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager;

public class PlayerInventory {
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 */
	Map<ItemProperty, Integer> mCurrentProperties = new HashMap<ItemProperty, Integer>();

	public PlayerInventory(Plugin plugin, Player player) {
		updateEquipmentProperties(plugin, player);
	}

	public void tick(Plugin plugin, World world, Player player) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.tick(plugin, world, player, level);
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player) {
		/*
		 * Loop through existing equipment properties, remove all of them
		 *
		 * TODO: Modify this so only the removed effects are removed
		 * This is probably hard to do without making any new objects
		 * (which defeats the purpose)
		 */
		removeProperties(plugin, player);

		/* Once that's done, loop through the current players inventory and re-register the properties */
		getAndApplyProperties(plugin, player);
	}

	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, DamageCause cause) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			damage = property.onAttack(plugin, world, player, target, damage, level, cause);
		}

		return damage;
	}

	public double onShootAttack(Plugin plugin, Player player, Projectile proj, EntityDamageByEntityEvent event) {
		double damage = event.getDamage();

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			damage = property.onShootAttack(plugin, player, level, proj, event);
		}

		return damage;
	}

	public void removeProperties(Plugin plugin, Player player) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();

			property.removeProperty(plugin, player);
		}

		mCurrentProperties.clear();
	}

	private void getAndApplyProperties(Plugin plugin, Player player) {
		ItemPropertyManager.getItemProperties(mCurrentProperties, player);

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.applyProperty(plugin, player, level);
		}
	}
}
