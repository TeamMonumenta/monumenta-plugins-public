package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

/**
 * Any mobs with CCImmuneEffect gets temporarily considered as a CCImmune mob.
 */
public class CCImmuneEffect extends ZeroArgumentEffect {
	public static final String effectID = "CCImmuneEffect";


	public CCImmuneEffect(int duration) {
		super(duration, effectID);
	}

	public static CCImmuneEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new CCImmuneEffect(duration);
	}

	@Override public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le) {
			le.removePotionEffect(PotionEffectType.SLOW);
			le.removePotionEffect(PotionEffectType.LEVITATION);
		}
	}

	@Override
	public void customEffectAppliedEvent(CustomEffectApplyEvent event) {
		Effect effect = event.getEffect();

		if (effect.getClass() == PercentSpeed.class && effect.getMagnitude() < 0) {
			effect.setDuration(0);
		}
	}

	@Override
	public String toString() {
		return String.format("CCImmuneEffect duration:%d", this.getDuration());
	}

}


