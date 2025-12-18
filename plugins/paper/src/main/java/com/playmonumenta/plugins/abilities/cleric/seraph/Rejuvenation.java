package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.RejuvenationHealing;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Rejuvenation extends Ability {
	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL = TICKS_PER_SECOND * 3;
	public static final double PERCENT_HEAL = 0.05;
	private static final double HEALTH_LIMIT = 0.5;
	public static final double DJ_MULTIPLIER = 0.1;

	private static final String REGENERATION_EFFECT = "RejuvenationRegenerationEffect";

	public static final String CHARM_RADIUS = "Rejuvenation Radius";
	public static final String CHARM_HEALING = "Rejuvenation Healing";
	public static final String CHARM_THRESHOLD = "Rejuvenation Health Threshold";

	public static final AbilityInfo<Rejuvenation> INFO =
		new AbilityInfo<>(Rejuvenation.class, "Rejuvenation", Rejuvenation::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Cleric.SERAPH_SPEC_ID);

	private final double mRadius;
	private final double mHealing;
	private final double mThreshold;
	private final double mPercentDamage;

	public Rejuvenation(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, PERCENT_HEAL);
		mThreshold = HEALTH_LIMIT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_THRESHOLD);
		mPercentDamage = DJ_MULTIPLIER;
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		if (!mPlayer.isDead()) {
			for (final Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
				if (player.getScoreboardTags().contains("disable_class")) {
					continue;
				}
				mPlugin.mEffectManager.addEffect(player, REGENERATION_EFFECT,
					new RejuvenationHealing(5, mHealing, mThreshold, HEAL_INTERVAL, mPlayer, mPlugin)
						.displaysTime(false).deleteOnLogout(true).deleteOnAbilityUpdate(true));
			}
		}
	}

	public double getDJBonus() {
		return mPercentDamage;
	}

	public static Description<Rejuvenation> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("You and other players within %d")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addLine("blocks heal %p HP every %t while")
				.statValues(
					stat(a -> a.mHealing, PERCENT_HEAL),
					stat(HEAL_INTERVAL))
			.addLine("under %p HP.")
				.statValues(stat(a -> a.mThreshold, HEALTH_LIMIT))
			.addLine()
			.addLine("Increase *Divine Justice*'s bonus").styles(UNDERLINED)
			.addLine("damage multiplier by +%p.")
				.statValues(stat(a -> a.mPercentDamage, DJ_MULTIPLIER));
	}
}
