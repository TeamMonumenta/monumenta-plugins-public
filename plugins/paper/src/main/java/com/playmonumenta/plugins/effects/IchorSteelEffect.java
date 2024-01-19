package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class IchorSteelEffect extends Effect {
	public static final String effectID = "IchorSteelEffect";
	private final double mDamage;
	private final boolean mPrismatic;
	private static final EnumSet<DamageType> AFFECTED_PRISMATIC_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE,
			DamageType.MELEE_ENCH,
			DamageType.MELEE_SKILL,
			DamageType.PROJECTILE,
			DamageType.PROJECTILE_SKILL,
			DamageType.MAGIC
	);
	private static final EnumSet<DamageType> AFFECTED_PROJECTILE_DAMAGE_TYPES = EnumSet.of(
			DamageType.PROJECTILE,
			DamageType.PROJECTILE_SKILL
	);

	public IchorSteelEffect(int duration, double damage, boolean prismatic) {
		super(duration, effectID);
		mDamage = damage;
		mPrismatic = prismatic;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (LocationUtils.isAirborne(entity) &&
				(AFFECTED_PROJECTILE_DAMAGE_TYPES.contains(type) || (mPrismatic && AFFECTED_PRISMATIC_DAMAGE_TYPES.contains(type)))) {
			event.setDamage(event.getDamage() * (1 + mDamage));
			((Player) entity).playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
		}
	}

	@Override
	public String toString() {
		return String.format("IchorSteelEffect duration:%d", this.getDuration());
	}

	@Override
	public @Nullable String getDisplayedName() {
		return IchorListener.ITEM_NAME + " - Steelsage";
	}
}
