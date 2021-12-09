package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class SpellHandSwap extends Spell {
	private final Mob mLauncher;

	public SpellHandSwap(Mob launcher) {
		mLauncher = launcher;
	}

	@Override
	public void run() {
		EntityEquipment equipment = mLauncher.getEquipment();
		if (equipment == null) {
			return;
		}
		ItemStack curItem = equipment.getItemInMainHand();
		ItemStack offItem = equipment.getItemInOffHand();
		equipment.setItemInMainHand(offItem);
		equipment.setItemInOffHand(curItem);
	}

	@Override
	public int cooldownTicks() {
		return 140;
	}
}
