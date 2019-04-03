package com.playmonumenta.bossfights.spells;

import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;

public class SpellHandSwap extends Spell {
	private final Mob mLauncher;

	public SpellHandSwap(Mob launcher) {
		mLauncher = launcher;
	}

	@Override
	public void run() {
		ItemStack curItem = mLauncher.getEquipment().getItemInMainHand();
		ItemStack offItem = mLauncher.getEquipment().getItemInOffHand();
		mLauncher.getEquipment().setItemInMainHand(offItem);
		mLauncher.getEquipment().setItemInOffHand(curItem);
	}

	@Override
	public int duration() {
		return 140;
	}
}
