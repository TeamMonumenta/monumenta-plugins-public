package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class SpellSwapOnDismount extends Spell {

	private LivingEntity mBoss;
	private boolean mSwapped;

	public SpellSwapOnDismount(LivingEntity boss) {
		mBoss = boss;
		mSwapped = false;
	}

	@Override
	public void run() {
		if (!mBoss.isInsideVehicle() && !mSwapped) {
			mSwapped = true;
			ItemStack curItem = mBoss.getEquipment().getItemInMainHand();
			ItemStack offItem = mBoss.getEquipment().getItemInOffHand();
			mBoss.getEquipment().setItemInMainHand(offItem);
			mBoss.getEquipment().setItemInOffHand(curItem);
		}
	}

	@Override
	public int cooldownTicks() {
		// TODO Auto-generated method stub
		return 0;
	}

}
