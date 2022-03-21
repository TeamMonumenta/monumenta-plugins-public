package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NegateDamage extends Effect {
	private int mCount;
	private @Nullable EnumSet<DamageEvent.DamageType> mAffectedTypes;

	public NegateDamage(int duration, int count) {
		super(duration);
		mCount = count;
		mAffectedTypes = null;
	}

	public NegateDamage(int duration, int count, @Nullable EnumSet<DamageEvent.DamageType> affectedTypes) {
		super(duration);
		mCount = count;
		mAffectedTypes = affectedTypes;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		//TODO this might have order issues, i.e. triggering after riposte
		if (mCount > 0 && (mAffectedTypes == null || mAffectedTypes.contains(event.getType())) && !event.isCancelled() && !event.isBlockedByShield()) {
			event.setCancelled(true);
			World world = entity.getWorld();
			Location loc = entity.getLocation();
			world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1, 1.2f);
			mCount--;
		}
	}

	@Override
	public double getMagnitude() {
		return mCount;
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedTypes != null) {
			types = "";
			for (DamageEvent.DamageType type : mAffectedTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("NegateDamage duration:%d types:%s count:%d", this.getDuration(), types, mCount);
	}
}
