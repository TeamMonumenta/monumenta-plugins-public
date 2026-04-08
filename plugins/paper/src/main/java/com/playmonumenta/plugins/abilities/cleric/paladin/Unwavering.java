package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

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

	public static Description<Unwavering> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("You gain +%p *Knockback Resistance* per").styles(WHITE)
				.statValues(stat(a -> a.mKBRIncrement, KBR_INCREMENT))
			.addLine("*Heretic* within %d blocks, up to +%p.").styles(Cleric.HERETIC_COLOR)
				.statValues(
					stat(a -> a.mRadius, RADIUS),
					stat(a -> a.mMaxKBR, MAX_KBR))
			.addLine()
			.addLine("Increase *Divine Justice*'s bonus").styles(UNDERLINED)
			.addLine("damage multiplier by +%p.")
				.statValues(stat(a -> a.mPercentDamage, DJ_MULTIPLIER));
	}
}
