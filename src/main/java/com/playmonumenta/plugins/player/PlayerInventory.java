package com.playmonumenta.plugins.player;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager;

public class PlayerInventory {
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 */
	Map<BaseEnchantment, Integer> mCurrentProperties = new HashMap<BaseEnchantment, Integer>();
	Map<BaseEnchantment, Integer> mPreviousProperties = new HashMap<BaseEnchantment, Integer>();

	public PlayerInventory(Plugin plugin, Player player) {
		updateEquipmentProperties(plugin, player);
	}

	public void tick(Plugin plugin, World world, Player player) {
		// Players in spectator do not have ticking effects
		// TODO: Add vanish hook here also
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.tick(plugin, world, player, level);
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player) {
		// Swap current and previous lists
		Map<BaseEnchantment, Integer> temp = mPreviousProperties;
		mPreviousProperties = mCurrentProperties;
		mCurrentProperties = temp;

		// Clear the current map and update it with current properties
		mCurrentProperties.clear();
		EnchantmentManager.getItemProperties(mCurrentProperties, player);

		// Remove properties from the player that were removed
		for (BaseEnchantment property : mPreviousProperties.keySet()) {
			if (!mCurrentProperties.containsKey(property)) {
				property.removeProperty(plugin, player);
			}
		}

		// Apply properties to the player that changed or have a new level
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
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
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onAttack(plugin, player, level, target, event);
		}
	}

	public void onDamage(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onDamage(plugin, player, level, target, event);
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, Projectile proj, ProjectileLaunchEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onLaunchProjectile(plugin, player, level, proj, event);
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onExpChange(plugin, player, event, level);
		}
	}

	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onBlockBreak(plugin, player, event, item, level);
		}
	}

	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onPlayerInteract(plugin, player, event, level);
		}
	}

	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onDeath(plugin, player, event, level);
		}
	}

	public void onHurt(Plugin plugin, Player player, EntityDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onHurt(plugin, player, level, event);
		}
	}

	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();
			property.onConsume(plugin, player, event, level);
		}
	}

	public void removeProperties(Plugin plugin, Player player) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();

			property.removeProperty(plugin, player);
		}

		mCurrentProperties.clear();
	}
}
