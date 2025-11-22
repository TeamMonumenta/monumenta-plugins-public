package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class GearDamageIncrease extends Effect {
	public static final String GENERIC_NAME = "GearDamageIncrease";
	public static final String effectID = "GearDamageIncrease";

	protected final double mAmount;
	protected @Nullable EnumSet<DamageType> mAffectedDamageTypes;
	protected int mPriority;
	private @Nullable BiPredicate<LivingEntity, LivingEntity> mPredicate;

	private GearDamageIncrease(final int duration, final double amount,
	                           final @Nullable EnumSet<DamageType> affectedDamageTypes, final int priority,
	                           final @Nullable BiPredicate<LivingEntity, LivingEntity> predicate, final String id) {
		super(duration, id);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mPriority = priority;
		mPredicate = predicate;
	}

	/**
	 * Create a new <code>GearDamageIncrease</code> Effect.
	 *
	 * @param duration Time in ticks the effect lasts
	 * @param amount   Potency of the effect where amount < 0 is a debuff and amount > 0 is a buff
	 */
	public GearDamageIncrease(final int duration, final double amount) {
		this(duration, amount, null, 0, null, effectID);
	}

	/**
	 * Restricts what DamageTypes an instance of GearDamageIncrease can apply to
	 *
	 * @param affectedDamageTypes Which DamageTypes the effect should modify
	 * @return Modified GearDamageIncrease instance
	 */
	public GearDamageIncrease damageTypes(final @Nullable EnumSet<DamageType> affectedDamageTypes) {
		mAffectedDamageTypes = affectedDamageTypes;
		return this;
	}

	/**
	 * Modifies the EffectPriority of an instance of GearDamageIncrease. Default is 0 (early)<br>
	 * Note: This is set to <code>protected</code> since this builder method isn't used outside of this class. Unless
	 * you have a good reason to override an effect's priority, don't change this method to public
	 *
	 * @param priority Priority ordering of this effect
	 * @return Modified GearDamageIncrease instance
	 */
	protected GearDamageIncrease priority(final int priority) {
		mPriority = priority;
		return this;
	}

	/**
	 * Restricts what DamageTypes an instance of GearDamageIncrease can apply to
	 *
	 * @param predicate Determines what DamageEvents should be modified using damagee and damager
	 * @return Modified GearDamageIncrease instance
	 */
	public GearDamageIncrease predicate(final @Nullable BiPredicate<LivingEntity, LivingEntity> predicate) {
		mPredicate = predicate;
		return this;
	}

	// This needs to trigger before any flat damage
	@Override
	public EffectPriority getPriority() {
		if (mPriority == 1) {
			return EffectPriority.NORMAL;
		} else if (mPriority == 2) {
			return EffectPriority.LATE;
		} else {
			return EffectPriority.EARLY;
		}
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public void onDamage(final LivingEntity entity, final DamageEvent event, final LivingEntity enemy) {
		if (event.getType() == DamageType.TRUE) {
			return;
		}
		if (mPredicate != null && !mPredicate.test(entity, enemy)) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())
			|| (mAffectedDamageTypes.contains(DamageType.PROJECTILE_SKILL)
			&& AbilityUtils.hasSpecialProjSkillScaling(event.getAbility()))) {
			event.updateGearDamageWithMultiplier(Math.max(0, 1 + mAmount));
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount)
			.append(Component.text(StringUtils.getDamageTypeString(mAffectedDamageTypes) + " " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Gear Damage Dealt";
	}

	@Override
	public JsonObject serialize() {
		final JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		if (mAffectedDamageTypes != null) {
			final JsonArray jsonArray = new JsonArray();
			for (final DamageType damageType : mAffectedDamageTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

		object.addProperty("priority", mPriority);
		object.addProperty("hasPredicate", mPredicate != null);

		return object;
	}

	public static @Nullable GearDamageIncrease deserialize(final JsonObject object, final Plugin plugin) {
		if (object.has("hasPredicate") && object.get("hasPredicate").getAsBoolean()) {
			// Noper nope nope not dealing with this
			return null;
		}

		final int duration = object.get("duration").getAsInt();
		final double amount = object.get("amount").getAsDouble();
		final int priority = object.get("priority").getAsInt();

		if (object.has("type")) {
			final JsonArray damageTypes = object.getAsJsonArray("type");
			final List<DamageType> damageTypeList = new ArrayList<>();
			for (final JsonElement element : damageTypes) {
				damageTypeList.add(DamageType.valueOf(element.getAsString()));
			}

			final EnumSet<DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new GearDamageIncrease(duration, amount).damageTypes(damageTypeSet).priority(priority);
		} else {
			return new GearDamageIncrease(duration, amount).priority(priority);
		}
	}

	@Override
	public String toString() {
		StringBuilder types = new StringBuilder("any");
		if (mAffectedDamageTypes != null) {
			types = new StringBuilder();
			for (final DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types.append(",");
				}
				types.append(type.name());
			}
		}
		return String.format("PercentDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
