package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class TuathanBlessing extends Effect {

	public static int RADIUS = 12;
	public static double AMOUNT = 0.05;
	public static String DEFENSE_EFFECT = "TuathanBlessingDefense";
	public static String SPEED_EFFECT = "TuathanBlessingSpeed";

	public TuathanBlessing(int duration) {
		super(duration);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			Plugin.getInstance().mEffectManager.addEffect(entity, SPEED_EFFECT, new PercentSpeed(25, AMOUNT, SPEED_EFFECT));
			for (Player player : PlayerUtils.playersInRange(entity.getLocation(), RADIUS, false)) {
				Plugin.getInstance().mEffectManager.addEffect(player, DEFENSE_EFFECT, new PercentDamageReceived(25, -1 * AMOUNT));
			}
		}
	}

	@Override
	public String toString() {
		return String.format("TuathanBlessing duration:%d", this.getDuration());
	}

}
