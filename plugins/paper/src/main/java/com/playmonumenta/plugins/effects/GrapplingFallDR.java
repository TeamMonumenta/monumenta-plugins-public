package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class GrapplingFallDR extends Effect {
	public static final String effectID = "GrapplingFallDR";
	public static final String GENERIC_NAME = "GrapplingFallDR";

	private final double mAmount;
	private boolean mAnimPlayedOnce = false;

	public GrapplingFallDR(int duration, double amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getType() != DamageEvent.DamageType.FALL) {
			return;
		}
		playAnim(entity, Math.max(0.5f, event.getDamage() / 5), true);

		event.setDamage(event.getDamage() * (1 + mAmount));
		setDuration(0);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static GrapplingFallDR deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new GrapplingFallDR(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("GrapplingFallDR duration:%d amount:%f", this.getDuration(), mAmount);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			new PartialParticle(Particle.SPELL_INSTANT, entity.getLocation(), 6, 0.25, 0, 0.25).spawnAsEntityActive(entity);
			if (entity.isOnGround() || entity.isInWater()) {
				playAnim(entity, 0.5, false);
				setDuration(0);
			}
		}
	}

	private void playAnim(Entity entity, double radius, boolean damage) {
		if (mAnimPlayedOnce) {
			return;
		}
		mAnimPlayedOnce = true;
		new PPCircle(Particle.WHITE_ASH, entity.getLocation(), radius).countPerMeter(10).delta(0).ringMode(false).spawnAsEntityActive(entity);
		new PPCircle(Particle.SNOWFLAKE, entity.getLocation(), radius).countPerMeter(10).delta(0).ringMode(false).spawnAsEntityActive(entity);
		new PPCircle(Particle.SPELL_INSTANT, entity.getLocation(), radius).countPerMeter(10).delta(0).spawnAsEntityActive(entity);
		entity.getWorld().playSound(entity, Sound.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.PLAYERS, 1f, 0.5f);
		entity.getWorld().playSound(entity, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, SoundCategory.PLAYERS, 1f, 1f);
		if (damage) {
			entity.getWorld().playSound(entity, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1f, 0.75f);
		}
	}
}
