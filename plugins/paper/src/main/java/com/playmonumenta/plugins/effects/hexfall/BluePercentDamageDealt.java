package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public final class BluePercentDamageDealt extends PercentDamageDealt {
	public static final String effectID = "BluePercentDamageDealt";

	public BluePercentDamageDealt(final int duration, final double amount,
	                              final @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes,
	                              final String effectID) {
		super(duration, amount, affectedDamageTypes, effectID);
	}

	@Override
	public Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount)
			.append(Component.text(StringUtils.getDamageTypeString(mAffectedDamageTypes) + "Damage Dealt to Harrakfar"));
	}

	@Override
	public String toString() {
		StringBuilder types = new StringBuilder("any");
		if (mAffectedDamageTypes != null) {
			types = new StringBuilder();
			for (DamageEvent.DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types.append(",");
				}
				types.append(type.name());
			}
		}
		return String.format("BluePercentDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
