package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class BonusSoulThreads extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "BonusSoulThreads";

	public BonusSoulThreads(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onKill(EntityDeathEvent event) {
		for (ItemStack drop : event.getDrops()) {
			String name = ItemUtils.getPlainName(drop);
			if (name != null && name.equals("Soul Thread")) {
				if (FastUtils.RANDOM.nextDouble() <= mAmount) {
					drop.setAmount(drop.getAmount() * 2);
				}
				return;
			}
		}
	}

	@Override
	public String toString() {
		return String.format("BonusSoulThreads duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
