package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class ScoutPassive extends Ability {

	private static float PASSIVE_ARROW_SAVE = 0.20f;

	public ScoutPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 6;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		// Stores velocity for ability damage calculations
		AbilityUtils.setArrowVelocityDamageMultiplier(mPlugin, arrow);

		// PASSIVE : 25% chance of not consuming an arrow
		if (mRandom.nextFloat() < PASSIVE_ARROW_SAVE) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			if (InventoryUtils.isBowItem(mainHand) || InventoryUtils.isBowItem(offHand)) {
				int infLevel = Math.max(mainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE), offHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE));
				if (infLevel == 0) {
					arrow.setPickupStatus(Arrow.PickupStatus.ALLOWED);
					Inventory playerInv = mPlayer.getInventory();
					int firstArrow = playerInv.first(Material.ARROW);
					int firstTippedArrow = playerInv.first(Material.TIPPED_ARROW);

					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.3f, 1.0f);

					final int arrowSlot;
					if (firstArrow == -1 && firstTippedArrow > -1) {
						arrowSlot = firstTippedArrow;
					} else if (firstArrow > - 1 && firstTippedArrow == -1) {
						arrowSlot = firstArrow;
					} else if (firstArrow > - 1 && firstTippedArrow > -1) {
						arrowSlot = Math.min(firstArrow, firstTippedArrow);
					} else {
						/* Player shot their last arrow - abort here */
						return true;
					}

					ItemStack arrowStack = playerInv.getItem(arrowSlot);
					int arrowQuantity = arrowStack.getAmount();
					if (arrowQuantity < 64) {
						arrowStack.setAmount(arrowQuantity);
						arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					}
					playerInv.setItem(arrowSlot, arrowStack);
				}
			}
		}
		return true;
	}

	// This method handles bow damage for all scout skills
	// Damage scaling abilities are applied to base arrow damage only (Volley, Pinning Shot)
	// Flat damage bonus abilities and enchantments are applied at the end (Bow Mastery, Sharpshooter)
	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata("ArrowQuickdraw")) {
			event.setDamage(AbilityUtils.getArrowBaseDamage(arrow));
		}
		double bonusDamage = AbilityUtils.getArrowVelocityDamageMultiplier(arrow) * AbilityUtils.getArrowBonusDamage(arrow);
		double multiplier = AbilityUtils.getArrowFinalDamageMultiplier(arrow);
		if (damagee.hasMetadata("PinningShotEnemyHasBeenPinned")
				&& damagee.getMetadata("PinningShotEnemyHasBeenPinned").get(0).asInt() != mPlayer.getTicksLived()
				&& damagee.hasMetadata("PinningShotEnemyIsPinned")) {
			multiplier *= damagee.getMetadata("PinningShotEnemyIsPinned").get(0).asDouble();
			damagee.removeMetadata("PinningShotEnemyIsPinned", mPlugin);
			mWorld.playSound(damagee.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, arrow.getLocation(), 20, 0, 0, 0, 0.2);
			mWorld.spawnParticle(Particle.SNOWBALL, arrow.getLocation(), 30, 0, 0, 0, 0.25);
			damagee.removePotionEffect(PotionEffectType.SLOW);
		}
		event.setDamage(event.getDamage() * multiplier + bonusDamage);
		return true;
	}

}
