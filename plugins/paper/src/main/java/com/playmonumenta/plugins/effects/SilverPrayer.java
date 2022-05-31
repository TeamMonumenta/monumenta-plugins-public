package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SilverPrayer extends Effect {

	public static final String EFFECT_NAME = "SilverPrayerKnockbackRes";
	public static final int EFFECT_DURATION = 2 * 20;

	public SilverPrayer(int duration) {
		super(duration);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player player) {
				if (player.getHealth() / EntityUtils.getMaxHealth(player) <= 0.5) {
					Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_NAME, new PercentKnockbackResist(EFFECT_DURATION, 1, EFFECT_NAME));
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format("SilverPrayer duration:%d", this.getDuration());
	}

}
