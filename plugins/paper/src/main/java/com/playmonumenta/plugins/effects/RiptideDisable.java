package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class RiptideDisable extends Effect {

	public RiptideDisable(int duration) {
		super(duration);
	}

	@Override
	public String toString() {
		return String.format("RiptideDisable duration:%d", this.getDuration());
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity.isOnGround() || entity.isInWater()) {
			setDuration(0);
			Plugin.getInstance().mPotionManager.clearPotionEffectType((Player) entity, PotionEffectType.SLOW_FALLING);
		}
	}
}
