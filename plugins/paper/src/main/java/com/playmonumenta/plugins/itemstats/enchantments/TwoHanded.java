package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class TwoHanded implements Enchantment {

	private static final String PERCENT_SPEED_EFFECT_NAME = "TwoHandedPercentSpeedEffect";
	private static final int PERCENT_SPEED_DURATION = 1000000;
	private static final double PERCENT_SPEED = -0.4;
	private static final double PERCENT_DAMAGE_REDUCTION = 0.6;

	@Override
	public String getName() {
		return "Two Handed";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TWO_HANDED;
	}

	public static boolean checkForOffhand(Plugin plugin, Player player) {
		PlayerInventory inventory = player.getInventory();
		if (inventory.getItemInOffHand().getType() != Material.AIR && inventory.getItemInMainHand().getType() != Material.AIR
				&& plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.WEIGHTLESS) == 0) {
			return true;
		}
		return false;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player, double value) {
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TWO_HANDED) == 0) {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
			return;
		}
		if (checkForOffhand(plugin, player)) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(PERCENT_SPEED_DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
		} else {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		//This handles all launched projectiles besides Tridents (because they're quirky and not like the other projectiles).
		//Tridents are handled in AttributeThrowRate.java
		if (checkForOffhand(plugin, player)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		if (checkForOffhand(plugin, player)) {
			PlayerInventory inventory = player.getInventory();
			ItemStack offhand = inventory.getItemInOffHand();
			ItemStack mainhand = inventory.getItemInMainHand();
			if (offhand.getType() == Material.SHIELD && event.getHand() == EquipmentSlot.OFF_HAND) {
				player.setCooldown(offhand.getType(), 20 * 20);
			} else if (mainhand.getType() == Material.SHIELD && event.getHand() == EquipmentSlot.OFF_HAND) {
				player.setCooldown(mainhand.getType(), 20 * 20);
			}
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity target) {
		if (checkForOffhand(plugin, player)) {
			event.setDamage(event.getDamage() * (1 - PERCENT_DAMAGE_REDUCTION));
		}
	}

}
