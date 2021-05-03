package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class TwoHanded implements BaseEnchantment {
	//Two Handed: If holding an item in offhand, melee damage is reduced by 40% and speed is reduced by 30% (Non-cleansable).
	//In addition, projectiles cannot be fired and shields cannot block.
	//For rogues, a two handed sword can trigger class abilities.


	public static String PROPERTY_NAME = ChatColor.RED + "Two Handed";

	private static final String PERCENT_SPEED_EFFECT_NAME = "TwoHandedPercentSpeedEffect";
	private static final int PERCENT_SPEED_DURATION = 1000000;
	private static final double PERCENT_SPEED = -0.4;

	private static final double PERCENT_DAMAGE_REDUCTION = 0.6;


	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
	}

	public boolean checkForOffhand(Player player) {
		PlayerInventory inventory = player.getInventory();
		if (inventory.getItemInOffHand().getType() != Material.AIR && inventory.getItemInMainHand().getType() != Material.AIR) {
			return true;
		}
		return false;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (checkForOffhand(player)) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(PERCENT_SPEED_DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
		} else {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		//This handles all launched projectiles besides Tridents (because they're quirky and not like the other projectiles).
		//Tridents are handled in AttributeThrowRate.java
		if (checkForOffhand(player)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (checkForOffhand(player)) {
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
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (checkForOffhand(player)) {
			event.setDamage(event.getDamage() * (1 - PERCENT_DAMAGE_REDUCTION));
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (checkForOffhand(player) && event.getCause() == DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * (1 - PERCENT_DAMAGE_REDUCTION));
		}
	}
}