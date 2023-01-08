package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.events.DamageEvent;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class OnDamageTrigger extends Trigger {

	private final @Nullable String mCustomAbilityName;
	private final @Nullable DamageEvent.DamageType mDamageType;
	private final Long mTotalDamage;
	private double mCurrentDamage = 0;

	public OnDamageTrigger(String name, Long damage) {
		mTotalDamage = damage;
		mCustomAbilityName = name;
		mDamageType = null;
	}

	public OnDamageTrigger(DamageEvent.DamageType type, Long damage) {
		mTotalDamage = damage;
		mDamageType = type;
		mCustomAbilityName = null;
	}

	@Override public boolean onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		if (mDamageType == event.getType()) {
			mCurrentDamage += event.getDamage();
		} else if ((event.getBossSpellName() != null && event.getBossSpellName().equals(mCustomAbilityName)) || "ALL".equals(mCustomAbilityName)) {
			mCurrentDamage += event.getDamage();
		}

		return mCurrentDamage >= mTotalDamage;
	}

	@Override public boolean test(LivingEntity boss) {
		return mCurrentDamage >= mTotalDamage;
	}

	@Override public void reset(LivingEntity boss) {
		mCurrentDamage = 0;
	}

	public static ParseResult<Trigger> fromReader(StringReader reader) {
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "this object requires brackets")));
		}

		DamageEvent.DamageType type = reader.readEnum(DamageEvent.DamageType.values());
		String customAbilityName = reader.advance("ALL") ? "ALL" : null;
		if (type == null && customAbilityName == null) {
			customAbilityName = reader.readString();
			if (customAbilityName == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(DamageEvent.DamageType.values().length + 1);
				String soFar = reader.readSoFar();
				for (DamageEvent.DamageType valid : DamageEvent.DamageType.values()) {
					suggArgs.add(Tooltip.ofString(soFar + valid.name(), "Damage type"));
				}
				suggArgs.add(Tooltip.ofString(soFar + "ALL", "ALL damage type"));
				suggArgs.add(Tooltip.ofString(soFar + "\"CoolCustomAbility name\"", "custom name"));
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "kkk")));
		}

		Long value = reader.readLong();
		if (value == null || value < 0) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "10", "damage > 0")));
		}


		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "this object requires brackets")));
		}

		if (type != null) {
			return ParseResult.of(new OnDamageTrigger(type, value));
		}

		return ParseResult.of(new OnDamageTrigger(Objects.requireNonNull(customAbilityName), value));

	}
}
