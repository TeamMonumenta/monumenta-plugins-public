package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Elusive extends Ability {
	private static final double DAMAGE_REDUCTION = 0.025;
	private static final int MAX_STACKS = 4;

	public static final AbilityInfo<Elusive> INFO =
		new AbilityInfo<>(Elusive.class, "Elusive", Elusive::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Scout.RANGER_SPEC_ID);

	private final double mDR;
	private final int mMaxStacks;

	private int mStacks;

	public Elusive(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDR = DAMAGE_REDUCTION;
		mMaxStacks = MAX_STACKS;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mStacks < 1
			|| event.isBlocked()
			|| !event.getType().isDefendable()
			|| event.getType() == DamageEvent.DamageType.FIRE
			|| event.getType() == DamageEvent.DamageType.FALL) {
			return;
		}

		double dmgReduction = 1 - mDR * mStacks;

		mStacks = 0;

		event.setFlatDamage(event.getDamage() * dmgReduction);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		mStacks = Math.min(++mStacks, mMaxStacks);
		return true;
	}

	public static Description<Elusive> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Gain +%p resistance against the next")
			.statValues(stat(a -> a.mDR, DAMAGE_REDUCTION))
			.addLine("hit taken per ability used.")
			.addLine("(up to %d stacks)")
			.statValues(stat(a -> a.mMaxStacks, MAX_STACKS));
	}
}
