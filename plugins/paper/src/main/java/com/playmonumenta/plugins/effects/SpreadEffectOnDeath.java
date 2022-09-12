package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class SpreadEffectOnDeath extends Effect {
	public static final String effectID = "SpreadEffectOnDeath";
	// mSource is the source of the OTHER effect, not this one! Be careful when using this effect
	private final String mSource;
	private final double mRadius;
	private final int mNewDuration;
	private final boolean mRecurse;

	public SpreadEffectOnDeath(int duration, String source, double radius, int newDuration, boolean recurse) {
		super(duration, effectID);
		mSource = source;
		mRadius = radius;
		mNewDuration = newDuration;
		mRecurse = recurse;
	}

	// Dummy constructor for copying
	public SpreadEffectOnDeath() {
		this(0, null, 0, 0, false);
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		EffectManager manager = Plugin.getInstance().mEffectManager;
		Effect effect = manager.getActiveEffect(entity, mSource);
		if (effect != null) {
			Location loc = entity.getLocation();
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius, entity);
			for (LivingEntity mob : mobs) {
				Effect copy = effect.getCopy();
				if (copy != null) {
					copy.setDuration(mNewDuration);
					manager.addEffect(mob, mSource, copy);
				}
			}

			// If mRecurse is true, also spread to other mobs the spread effect, so this effect can be chained theoretically infinitely
			if (mRecurse) {
				String source = manager.getSource(entity, this);
				if (source != null) {
					for (LivingEntity mob : mobs) {
						Effect copy = this.getCopy();
						if (copy != null) {
							copy.setDuration(mNewDuration);
							manager.addEffect(mob, source, copy);
						}
					}
				}
			}

			entity.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.1f);
		}
	}

	@Override
	public String toString() {
		return String.format("SpreadEffectOnDeath duration:%d source:%s radius:%f newduration:%d", this.getDuration(), mSource, mRadius, mNewDuration);
	}
}
