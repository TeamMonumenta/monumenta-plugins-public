package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class HealPlayerOnDeath extends Effect {
	private final double mAmount;
	private final Player mPlayer;

	public HealPlayerOnDeath(int duration, double amount, Player player) {
		super(duration);
		mAmount = amount;
		mPlayer = player;
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		PlayerUtils.healPlayer(Plugin.getInstance(), mPlayer, mAmount, mPlayer);
	}

	@Override
	public String toString() {
		return String.format("FlatDamageDealt duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
