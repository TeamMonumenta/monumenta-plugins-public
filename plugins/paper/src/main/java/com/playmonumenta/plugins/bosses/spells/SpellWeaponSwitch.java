package com.playmonumenta.plugins.bosses.spells;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;

public class SpellWeaponSwitch extends Spell {
	private static final Set<Material> RANGED_MATERIALS = EnumSet.of(Material.BOW, Material.CROSSBOW, Material.TRIDENT);

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

		if (curItem == null || offItem == null) {
			return;
		}

		if (mLauncher.getLocation().distance(target.getLocation()) > 6) {
			// Switch to ranged weapon if not already equipped

			if (!RANGED_MATERIALS.contains(curItem.getType())
				&& RANGED_MATERIALS.contains(offItem.getType())) {
				// Need to switch hands - offhand is ranged
				mLauncher.getEquipment().setItemInMainHand(offItem);
				mLauncher.getEquipment().setItemInOffHand(curItem);
			}
		} else {
			// Switch to non-ranged weapon if not already equipped
			if (RANGED_MATERIALS.contains(curItem.getType())
				&& !RANGED_MATERIALS.contains(offItem.getType())) {
				// Need to switch hands - offhand is not ranged and mainhand is
				mLauncher.getEquipment().setItemInMainHand(offItem);
				mLauncher.getEquipment().setItemInOffHand(curItem);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 10;
	}
}
