package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class SpreadEffectOnDeath extends Effect {
	// mSource is the source of the OTHER effect, not this one! Be careful when using this effect
	private final String mSource;
	private final double mRadius;
	private final int mNewDuration;

	public SpreadEffectOnDeath(int duration, String source, double radius, int newDuration) {
		super(duration);
		mSource = source;
		mRadius = radius;
		mNewDuration = newDuration;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		EffectManager manager = Plugin.getInstance().mEffectManager;
		Effect effect = manager.getActiveEffect(entity, mSource);
		if (effect != null) {
			effect.setDuration(mNewDuration);
			Location loc = entity.getLocation();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius, entity)) {
				manager.addEffect(mob, mSource, effect);
			}

			entity.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1);
		}
	}

	@Override
	public String toString() {
		return String.format("SpreadEffectOnDeath duration:%d source:%s radius:%f newduration:%d", this.getDuration(), mSource, mRadius, mNewDuration);
	}
}
