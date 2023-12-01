package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class IchorSteelEffect extends Effect {
	public static final String effectID = "IchorSteelEffect";
	private final double mDamage;

	public IchorSteelEffect(int duration, double damage) {
		super(duration, effectID);
		mDamage = damage;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (LocationUtils.isAirborne(entity) && (event.getType() == DamageEvent.DamageType.PROJECTILE || event.getType() == DamageEvent.DamageType.PROJECTILE_SKILL)) {
			event.setDamage(event.getDamage() * (1 + mDamage));
			((Player) entity).playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
		}
	}

	@Override
	public String toString() {
		return String.format("IchorSteelEffect duration:%d", this.getDuration());
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return IchorListener.ITEM_NAME + " - Steelsage";
	}
}
