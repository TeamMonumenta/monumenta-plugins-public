package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;

public class Despair extends DelveModifier {

	private static final String PERCENT_SPEED_EFFECT_NAME = "DespairPercentSpeedEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "DespairPercentDamageEffect";
	private static final String PERCENT_HEAL_EFFECT_NAME = "DespairPercentHealEffect";

	private static final double[] EFFECT_PER_HEALTH_PROPORTION = {
			-0.1,
			-0.2,
			-0.3,
			-0.4,
			-0.5
	};

	public static final String DESCRIPTION = "Injured players receive debuffs.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Players receive " + -EFFECT_PER_HEALTH_PROPORTION[0] + "% Slowness, Weaken, and Anti-Heal",
				"for every 1% Health below Max Health."
			}, {
				"Players receive " + -EFFECT_PER_HEALTH_PROPORTION[1] + "% Slowness, Weaken, and Anti-Heal",
				"for every 1% Health below Max Health."
			}, {
				"Players receive " + -EFFECT_PER_HEALTH_PROPORTION[2] + "% Slowness, Weaken, and Anti-Heal",
				"for every 1% Health below Max Health."
			}, {
				"Players receive " + -EFFECT_PER_HEALTH_PROPORTION[3] + "% Slowness, Weaken, and Anti-Heal",
				"for every 1% Health below Max Health."
			}, {
				"Players receive " + -EFFECT_PER_HEALTH_PROPORTION[4] + "% Slowness, Weaken, and Anti-Heal",
				"for every 1% Health below Max Health."
			}
	};

	private final double mEffectPerHealthProportion;

	public Despair(Plugin plugin, Player player) {
		super(plugin, player, Modifier.DESPAIR);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.DESPAIR);
		mEffectPerHealthProportion = EFFECT_PER_HEALTH_PROPORTION[rank - 1];
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			double effectAmount = mEffectPerHealthProportion
					* (1 - mPlayer.getHealth() / mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(20, effectAmount, PERCENT_SPEED_EFFECT_NAME));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
					new PercentDamageDealt(20, effectAmount));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME,
					new PercentHeal(20, effectAmount));
		}
	}

}
