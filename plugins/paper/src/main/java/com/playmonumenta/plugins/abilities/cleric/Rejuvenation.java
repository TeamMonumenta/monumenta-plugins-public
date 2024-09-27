package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.RejuvenationHealing;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Player;

public class Rejuvenation extends Ability {

	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL = 20 * 5;
	private static final double PERCENT_HEAL = 0.05;
	private static final double HEALTH_LIMIT = 0.5;
	private final double mCharmHealthLimit;

	private static final String REGENERATION_EFFECT = "RejuvenationRegenerationEffect";

	public static final String CHARM_THRESHOLD = "Rejuvenation Health Threshold";

	public static final AbilityInfo<Rejuvenation> INFO =
		new AbilityInfo<>(Rejuvenation.class, null, Rejuvenation::new)
			.canUse(player -> AbilityUtils.getClassNum(player) == Cleric.CLASS_ID);

	public Rejuvenation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCharmHealthLimit = HEALTH_LIMIT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_THRESHOLD);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (!mPlayer.isDead()) {
			for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
				if (player.getScoreboardTags().contains("disable_class")) {
					continue;
				}
				mPlugin.mEffectManager.addEffect(player, REGENERATION_EFFECT, new RejuvenationHealing(5, PERCENT_HEAL, mCharmHealthLimit, HEAL_INTERVAL, mPlayer, mPlugin).displaysTime(false).deleteOnLogout(true));
			}
		}
	}

}
