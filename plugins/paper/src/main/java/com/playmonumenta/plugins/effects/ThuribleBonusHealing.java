package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class ThuribleBonusHealing extends SingleArgumentEffect {
	public static final String effectID = "ThuribleBonusHealing";

	public ThuribleBonusHealing(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public String toString() {
		return String.format("ThuribleBonusHealing duration=%d healing=%f", this.getDuration(), mAmount);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Rejuvenation Heal";
	}
}
