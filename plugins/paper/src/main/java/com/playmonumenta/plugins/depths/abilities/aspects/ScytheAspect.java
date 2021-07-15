package com.playmonumenta.plugins.depths.abilities.aspects;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ScytheAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Scythe";
	public static final double DAMAGE_MODIFIER = 0.95;

	public ScytheAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_HOE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		//Life steal aspect

		if (event.getCause().equals(DamageCause.ENTITY_ATTACK) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			if (PlayerUtils.isFallingAttack(mPlayer)) {
				PlayerUtils.healPlayer(mPlayer, 1.0);
				mPlayer.getWorld().spawnParticle(Particle.HEART, event.getEntity().getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.001);
			} else {
				double attackSpeed = 4;
				double multiplier = 1;
				if (mPlayer.getInventory().getItemInMainHand() != null && mPlayer.getInventory().getItemInMainHand().hasItemMeta()) {
					ItemMeta meta = mPlayer.getInventory().getItemInMainHand().getItemMeta();
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

				PlayerUtils.healPlayer(mPlayer, 0.5 / Math.sqrt(attackSpeed * multiplier) * mPlayer.getCooledAttackStrength(0));
				mPlayer.getWorld().spawnParticle(Particle.HEART, event.getEntity().getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.001);
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {

		if (InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() * DAMAGE_MODIFIER);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "While holding a scythe, you gain an independent level of life drain and 5% damage reduction.";
	}
}

