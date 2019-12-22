package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;

public class SpellWeaponSwitch extends Spell {
	private final Mob mLauncher;

	public SpellWeaponSwitch(Mob launcher) {
		mLauncher = launcher;
	}

	@Override
	public void run() {
		LivingEntity target = mLauncher.getTarget();

		if (target == null) {
			return;
		}

		ItemStack curItem = mLauncher.getEquipment().getItemInMainHand();
		ItemStack offItem = mLauncher.getEquipment().getItemInOffHand();

		if (mLauncher.getLocation().distance(target.getLocation()) > 6) {
			// Switch to ranged weapon if not already equipped

			if (curItem.getType().equals(Material.BOW) || curItem.getType().equals(Material.TRIDENT)) {
				// Don't need to do anything - already have a ranged item equipped
			} else if (offItem.getType().equals(Material.BOW) || offItem.getType().equals(Material.TRIDENT)) {
				// Need to switch hands - offhand is ranged
				mLauncher.getEquipment().setItemInMainHand(offItem);
				mLauncher.getEquipment().setItemInOffHand(curItem);
			}
		} else {
			// Switch to non-ranged weapon if not already equipped

			if (curItem.getType().equals(Material.BOW) || curItem.getType().equals(Material.TRIDENT)) {
				// Need to switch - using a ranged weapon currently
				if (offItem.getType().equals(Material.BOW) || offItem.getType().equals(Material.TRIDENT)) {
					// Off hand is also ranged - no reason to do anything
				} else {
					// Need to switch hands - offhand is not ranged and mainhand is
					mLauncher.getEquipment().setItemInMainHand(offItem);
					mLauncher.getEquipment().setItemInOffHand(curItem);
				}
			}
		}
	}

	@Override
	public int duration() {
		return 10;
	}
}
