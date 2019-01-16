package com.playmonumenta.plugins.player;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemProperty;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager;

public class PlayerInventory {
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 */
	Map<ItemProperty, Integer> mCurrentProperties = new HashMap<ItemProperty, Integer>();
	Map<ItemProperty, Integer> mPreviousProperties = new HashMap<ItemProperty, Integer>();

	public PlayerInventory(Plugin plugin, Player player) {
		updateEquipmentProperties(plugin, player);
	}

	public void tick(Plugin plugin, World world, Player player) {
		// Players in spectator do not have ticking effects
		// TODO: Add vanish hook here also
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.tick(plugin, world, player, level);
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player) {
		// Swap current and previous lists
		Map<ItemProperty, Integer> temp = mPreviousProperties;
		mPreviousProperties = mCurrentProperties;
		mCurrentProperties = temp;

		// Clear the current map and update it with current properties
		mCurrentProperties.clear();
		ItemPropertyManager.getItemProperties(mCurrentProperties, player);

		// Remove properties from the player that were removed
		for (ItemProperty property : mPreviousProperties.keySet()) {
			if (!mCurrentProperties.containsKey(property)) {
				property.removeProperty(plugin, player);
			}
		}

		// Apply properties to the player that changed or have a new level
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			Integer oldLevel = mPreviousProperties.get(property);
			if (oldLevel == level) {
				// Don't need to do anything - player already had effects from this
			} else if (oldLevel == null) {
				// This didn't exist before - just apply the new property
				property.applyProperty(plugin, player, level);
			} else {
				// This existed before but was a different level - clear and re-add
				property.removeProperty(plugin, player);
				property.applyProperty(plugin, player, level);
			}
		}
	}

	public void onAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.onAttack(plugin, player, level, target, event);
		}
	}

	public void onShootAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.onShootAttack(plugin, player, level, target, event);
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();
			Integer level = iter.getValue();

			property.onExpChange(plugin, player, event, level);
		}
	}

	public void removeProperties(Plugin plugin, Player player) {
		for (Map.Entry<ItemProperty, Integer> iter : mCurrentProperties.entrySet()) {
			ItemProperty property = iter.getKey();

			property.removeProperty(plugin, player);
		}

		mCurrentProperties.clear();
	}
}
