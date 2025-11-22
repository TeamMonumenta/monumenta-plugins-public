package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.QUARTER_TICKS_PER_SECOND;

public class AbilityCooldownRechargeRate extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "AbilityCooldownRechargeRate";
	public static final String effectID = "AbilityCooldownRechargeRate";

	public double mOverflow;
	private final @Nullable ClassAbility mExcept;

	public AbilityCooldownRechargeRate(int duration, double amount) {
		super(duration, amount, effectID);
		mExcept = null;
		mOverflow = 0;
	}

	public AbilityCooldownRechargeRate(int duration, double amount, @Nullable ClassAbility except) {
		super(duration, amount, effectID);
		mExcept = except;
		mOverflow = 0;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		// This logic should run on the same interval as Plugin.getInstance().mTimers is updated, see Plugin.java
		if (fourHertz && entity instanceof Player player) {
			int reduction = (int) Math.floor(QUARTER_TICKS_PER_SECOND * mAmount);

			// handle overflow: we can't reduce by decimal ticks, so sum up the overflow each time and reduce by an additional tick when overflow > 1.
			double overflow = QUARTER_TICKS_PER_SECOND * mAmount - reduction;
			mOverflow += overflow;

			if (mOverflow >= 1) {
				mOverflow -= 1;
				reduction += 1;
			}
			if (mExcept != null) {
				Plugin.getInstance().mTimers.updateCooldownsExcept(player, mExcept, reduction);
			} else {
				Plugin.getInstance().mTimers.updateCooldowns(player, reduction);
			}
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Cooldown Recharge Rate";
	}

	@Override
	public boolean isBuff() {
		return mAmount >= 0;
	}

	public static AbilityCooldownRechargeRate deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new AbilityCooldownRechargeRate(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("AbilityCooldownRechargeRate duration:%d amount:%f", getDuration(), mAmount);
	}
}
