package com.playmonumenta.plugins.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.nms.utils.NmsCommandUtils;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.events.BossAbilityDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;

public class PlayerInventory {
	private static boolean mParsingCommandFailed = false;
	private static NmsCommandUtils.ParsedCommandWrapper mCachedParsedCommand = null;
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 */
	private Map<BaseEnchantment, Integer> mCurrentProperties = new HashMap<BaseEnchantment, Integer>();
	private Map<BaseEnchantment, Integer> mPreviousProperties = new HashMap<BaseEnchantment, Integer>();

	private Material mPrevOffhandMat = null;
	private List<String> mPrevOffhandLore = null;

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
		// Parse the command and cache it
		if (!mParsingCommandFailed && mCachedParsedCommand == null) {
			try {
				mCachedParsedCommand = NmsCommandUtils.parseCommand("function monumenta:mechanisms/buyback/mobile_buyback");
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to parse buyback command: " + e.getMessage());
				e.printStackTrace();
				mParsingCommandFailed = true;
				mCachedParsedCommand = null;
			}
		}

		if (mCachedParsedCommand != null) {
			// Offhand item change detection if the parsed command worked
			ItemStack offhand = player.getInventory().getItemInOffHand();

			if (offhand.getType() != mPrevOffhandMat || (offhand.hasItemMeta() && offhand.getItemMeta().hasLore() && !offhand.getItemMeta().getLore().equals(mPrevOffhandLore))) {
				mPrevOffhandMat = offhand.getType();
				mPrevOffhandLore = offhand.hasItemMeta() && offhand.getItemMeta().hasLore() ? offhand.getItemMeta().getLore() : null;

				try {
					NmsCommandUtils.runParsedCommand(mCachedParsedCommand, player);
				} catch (Exception e) {
					plugin.getLogger().warning("Failed to run buyback command: " + e.getMessage());
					e.printStackTrace();

					// Don't try again
					mParsingCommandFailed = true;
					mCachedParsedCommand = null;
				}
			}
		}


		// Swap current and previous lists
		Map<BaseEnchantment, Integer> temp = mPreviousProperties;
		mPreviousProperties = mCurrentProperties;
		mCurrentProperties = temp;

		// Clear the current map and update it with current properties
		mCurrentProperties.clear();
		try {
			plugin.mEnchantmentManager.getItemProperties(mCurrentProperties, player);
		} catch (Exception e) {
			e.printStackTrace();
		}

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

	public void onKill(Plugin plugin, Player player, Entity target, EntityDeathEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onKill(plugin, player, level, target, event);
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

	public void onBossDamage(Plugin plugin, Player player, BossAbilityDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onBossDamage(plugin, player, level, event);
		}
	}


	public void onHurtByEntity(Plugin plugin, Player player, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onHurtByEntity(plugin, player, level, event);
		}
	}

	public void onEvade(Plugin plugin, Player player, EvasionEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onEvade(plugin, player, level, event);
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
