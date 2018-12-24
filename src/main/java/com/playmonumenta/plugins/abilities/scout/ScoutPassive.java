package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class ScoutPassive extends Ability {

	private static float PASSIVE_ARROW_SAVE = 0.20f;

	public ScoutPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 6;
		mInfo.specId = -1;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
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

					int arrowSlot = -1;
					if (firstArrow == -1 && firstTippedArrow > -1) {
						arrowSlot = firstTippedArrow;
					} else if (firstArrow > - 1 && firstTippedArrow == -1) {
						arrowSlot = firstArrow;
					} else {
						arrowSlot = Math.min(firstArrow, firstTippedArrow);
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



}
