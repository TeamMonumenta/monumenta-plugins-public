package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class OnHurtTrigger extends Trigger {

	private final @Nullable ClassAbility mClassAbility;
	private final @Nullable DamageEvent.DamageType mDamageType;
	private final Double mTotalDamage;
	private double mCurrentDamage = 0;


	private OnHurtTrigger(Double totalDamage) {
		mTotalDamage = totalDamage;
		mDamageType = null;
		mClassAbility = null;
	}

	private OnHurtTrigger(DamageEvent.DamageType type, Double totalDamage) {
		mTotalDamage = totalDamage;
		mDamageType = type;
		mClassAbility = null;
	}

	private OnHurtTrigger(ClassAbility ability, Double totalDamage) {
		mTotalDamage = totalDamage;
		mClassAbility = ability;
		mDamageType = null;
	}


	@Override public boolean test(LivingEntity boss) {
		return mCurrentDamage >= mTotalDamage;
	}

	@Override public void reset(LivingEntity boss) {
		mCurrentDamage = 0;
	}

	@Override
	public boolean onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		if (event.getAbility() == mClassAbility && mClassAbility != null) {
			mCurrentDamage += event.getFinalDamage(true);
		} else if (event.getType() == mDamageType) {
			mCurrentDamage += event.getFinalDamage(true);
		} else if (mClassAbility == null && mDamageType == null) {
			mCurrentDamage += event.getFinalDamage(true);
		}

		return mCurrentDamage >= mTotalDamage;
	}


	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", "this object requires brackets")));
		}

		ClassAbility classAbility = reader.readEnum(ClassAbility.values());
		DamageEvent.DamageType type = reader.readEnum(DamageEvent.DamageType.values());
		String totalDamageType = reader.advance("ALL") ? "ALL" : null;
		if (classAbility == null && totalDamageType == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(ClassAbility.values().length + DamageEvent.DamageType.values().length + 1);
			String soFar = reader.readSoFar();
			for (ClassAbility valid : ClassAbility.values()) {
				suggArgs.add(Tooltip.of(soFar + valid.name(), "Class type"));
			}
			for (DamageEvent.DamageType valid : DamageEvent.DamageType.values()) {
				suggArgs.add(Tooltip.of(soFar + valid.name(), "Damage type"));
			}
			suggArgs.add(Tooltip.of(soFar + "ALL", "ALL damage type"));
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", "kkk")));
		}

		Double value = reader.readDouble();
		if (value == null || value < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "10", "damage > 0")));
		}


		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", "this object requires brackets")));
		}

		if (type != null) {
			return ParseResult.of(new OnHurtTrigger(type, value));
		}

		if (classAbility != null) {
			return ParseResult.of(new OnHurtTrigger(classAbility, value));
		}


		return ParseResult.of(new OnHurtTrigger(value));

	}
}
