package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class CustomRegeneration extends Effect {

	private final double mAmount;
	private final @Nullable Player mSourcePlayer;
	private final Plugin mPlugin;

	public CustomRegeneration(int duration, double amount, Plugin plugin) {
		this(duration, amount, null, plugin);
	}

	public CustomRegeneration(int duration, double amount, @Nullable Player sourcePlayer, Plugin plugin) {
		super(duration);
		mAmount = amount;
		mSourcePlayer = sourcePlayer;
		mPlugin = plugin;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player player) {
				PlayerUtils.healPlayer(mPlugin, player, mAmount, mSourcePlayer);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("CustomRegeneration duration:%d amount:%f", this.getDuration(), mAmount);
	}

}
