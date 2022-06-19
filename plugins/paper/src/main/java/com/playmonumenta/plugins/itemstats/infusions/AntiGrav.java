package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AntiGrav implements Infusion {

	private static final double KBR_REDUCTION = -0.04;
	private static final int DURATION = 3 * 20;
	private static final String EFFECT = "AntiGravEffect";

	@Override
	public String getName() {
		return "Anti-Grav";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ANTIGRAV;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (!EntityUtils.isBoss(enemy)) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			plugin.mEffectManager.addEffect(enemy, EFFECT, new PercentKnockbackResist(DURATION, KBR_REDUCTION * modifiedLevel, EFFECT));
		}
	}

}
