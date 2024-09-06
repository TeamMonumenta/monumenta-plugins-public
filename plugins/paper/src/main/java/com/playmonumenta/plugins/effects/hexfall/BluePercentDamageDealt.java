package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BluePercentDamageDealt extends PercentDamageDealt {

	public static final String GENERIC_NAME = "BluePercentDamageDealt";

	public BluePercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes, int priority, @Nullable BiPredicate<LivingEntity, LivingEntity> predicate) {
		super(duration, amount, affectedDamageTypes, priority, predicate);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(StringUtils.getDamageTypeString(mAffectedDamageTypes) + " Damage Dealt to Harrakfar"));
	}

	@Override
	public String toString() {
		StringBuilder types = new StringBuilder("any");
		if (mAffectedDamageTypes != null) {
			types = new StringBuilder();
			for (DamageEvent.DamageType type : mAffectedDamageTypes) {
				if (types.length() > 0) {
					types.append(",");
				}
				types.append(type.name());
			}
		}
		return String.format("BluePercentDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
