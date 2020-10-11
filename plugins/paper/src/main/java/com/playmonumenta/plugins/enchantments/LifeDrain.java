package com.playmonumenta.plugins.enchantments;

import java.util.Collection;
import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;

public class LifeDrain implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Life Drain";
	private static final double LIFE_DRAIN_CRIT_HEAL = 1;
	private static final double LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER = 0.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (PlayerUtils.isCritical(player)) {
			PlayerUtils.healPlayer(player, LIFE_DRAIN_CRIT_HEAL * Math.sqrt(level));
			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.001);
		} else {
			double attackSpeed = 4;
			double multiplier = 1;
			if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().hasItemMeta()) {
				ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
				if (meta.hasAttributeModifiers()) {
					Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_SPEED);
					if (modifiers != null) {
						for (AttributeModifier modifier : modifiers) {
							if (modifier.getSlot() == EquipmentSlot.HAND) {
								if (modifier.getOperation() == Operation.ADD_NUMBER) {
									attackSpeed += modifier.getAmount();
								} else if (modifier.getOperation() == Operation.ADD_SCALAR) {
									multiplier += modifier.getAmount();
								}
							}
						}
					}
				}
			}

			PlayerUtils.healPlayer(player, LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER / Math.sqrt(attackSpeed * multiplier) * Math.sqrt(level) * player.getCooledAttackStrength(0));
			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.001);
		}
	}

}
