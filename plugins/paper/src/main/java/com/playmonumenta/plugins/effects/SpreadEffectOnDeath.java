package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		Plugin plugin = Plugin.getInstance();
		EffectManager manager = plugin.mEffectManager;
		Effect effect = manager.getActiveEffect(entity, mSource);
		if (effect != null) {
			try {
				JsonObject serializedEffect = effect.serialize();
				Location loc = entity.getLocation();
				List<LivingEntity> mobs = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(entity), mRadius).getHitMobs(entity);
				for (LivingEntity mob : mobs) {
					Effect deserializedEffect = EffectManager.getEffectFromJson(serializedEffect, plugin);
					if (deserializedEffect != null) {
						deserializedEffect.setDuration(mNewDuration);
						manager.addEffect(mob, mSource, deserializedEffect);
					}
				}

				// If mRecurse is true, also spread to other mobs the spread effect, so this effect can be chained theoretically infinitely
				if (mRecurse) {
					String source = manager.getSource(entity, this);
					if (source != null) {
						JsonObject serializedSpreadEffect = this.serialize();
						for (LivingEntity mob : mobs) {
							Effect deserializedSpreadEffect = manager.getEffectFromJson(serializedSpreadEffect, plugin);
							if (deserializedSpreadEffect != null) {
								deserializedSpreadEffect.setDuration(mNewDuration);
								manager.addEffect(mob, mSource, deserializedSpreadEffect);
							}
						}
					}
				}

				entity.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.6f, 1.1f);
			} catch (Exception e) {
				MMLog.warning("Caught exception when spreading effect: " + effect);
				e.printStackTrace();
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("source", mSource);
		object.addProperty("radius", mRadius);
		object.addProperty("newDuration", mNewDuration);
		object.addProperty("recurse", mRecurse);

		return object;
	}

	public static SpreadEffectOnDeath deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		String source = object.get("source").getAsString();
		double radius = object.get("radius").getAsDouble();
		int newDuration = object.get("newDuration").getAsInt();
		boolean recurse = object.get("recurse").getAsBoolean();

		return new SpreadEffectOnDeath(duration, source, radius, newDuration, recurse);
	}

	@Override
	public String toString() {
		return String.format("SpreadEffectOnDeath duration:%d source:%s radius:%f newduration:%d", this.getDuration(), mSource, mRadius, mNewDuration);
	}
}
