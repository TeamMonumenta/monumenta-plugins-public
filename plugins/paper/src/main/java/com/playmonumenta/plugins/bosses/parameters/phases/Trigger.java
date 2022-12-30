package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public abstract class Trigger {

	private boolean mIsNegated;

	private @Nullable TriggerOperation mOperation = null;

	public abstract boolean test(LivingEntity boss);

	public abstract void reset(LivingEntity boss);

	public final boolean realTest(LivingEntity boss) {
		boolean testResult = test(boss);
		return (testResult && !isNegated()) || (!testResult && isNegated());
	}


	public void setNegated(boolean negate) {
		mIsNegated = negate;
	}

	public boolean isNegated() {
		return mIsNegated;
	}

	public void setOperation(TriggerOperation operation) {
		mOperation = operation;
	}

	public @Nullable TriggerOperation getOperation() {
		return mOperation;
	}

	public boolean onSpawn(LivingEntity boss) {
		return false;
	}

	public boolean onDeath(LivingEntity boss) {
		return false;
	}

	public boolean onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		return false;
	}

	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		return false;
	}

	public boolean tick(LivingEntity boss, int ticks) {
		return false;
	}

	public boolean onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		return false;
	}

	public boolean custom(LivingEntity boss, String key) {
		return false;
	}



	public enum TriggerOperation {
		AND, OR, XOR;
	}

	@FunctionalInterface
	public interface TriggerBuilder {
		ParseResult<Trigger> buildTrigger(StringReader reader);
	}

}
