package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class DurabilitySaving extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "DurabilitySaving";

	public DurabilitySaving(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onDurabilityDamage(Player player, PlayerItemDamageEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAmount) {
			event.setCancelled(true);
		}
	}

	@Override
	public String toString() {
		return String.format("DurabilitySaving duration:%d amount:%f", getDuration(), mAmount);
	}
}
