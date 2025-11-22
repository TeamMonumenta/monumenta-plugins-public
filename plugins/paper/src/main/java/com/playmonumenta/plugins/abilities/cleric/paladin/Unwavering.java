package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Unwavering extends Ability {
	public static final double DJ_MULTIPLIER = 0.1;
	public static final double KBR_INCREMENT = 0.08;
	public static final double MAX_KBR = 0.4;
	public static final double RADIUS = 8;
	private static final String SOURCE = "UnwaveringKBR";

	public static final AbilityInfo<Unwavering> INFO =
		new AbilityInfo<>(Unwavering.class, "Unwavering", Unwavering::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Cleric.PALADIN_SPEC_ID);

	private final double mPercentDamage;
	private final double mKBRIncrement;
	private final double mMaxKBR;
	private final double mRadius;

	public Unwavering(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mPercentDamage = DJ_MULTIPLIER;
		mKBRIncrement = KBR_INCREMENT;
		mMaxKBR = MAX_KBR;
		mRadius = RADIUS;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (twoHertz) {
			List<LivingEntity> heretics = EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius).stream().filter(Crusade::enemyTriggersAbilities).toList();
			if (!heretics.isEmpty()) {
				mPlugin.mEffectManager.addEffect(mPlayer, SOURCE, new PercentKnockbackResist(19, Math.min(mMaxKBR, mKBRIncrement * heretics.size()), SOURCE).displaysTime(false));
			}
		}
	}

	public double getDJBonus() {
		return mPercentDamage;
	}

	private static Description<Unwavering> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Divine Justice's % multiplier is increased by ")
			.addPercent(a -> a.mPercentDamage, DJ_MULTIPLIER)
			.add(", and you gain ")
			.addPercent(a -> a.mKBRIncrement, KBR_INCREMENT)
			.add(" Knockback Resistance (up to ")
			.addPercent(a -> a.mMaxKBR, MAX_KBR)
			.add(") for every Heretic within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of you.");
	}
}
