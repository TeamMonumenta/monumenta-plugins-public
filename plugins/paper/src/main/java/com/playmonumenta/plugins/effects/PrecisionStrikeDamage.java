package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PrecisionStrikeDamage extends Effect {
	public static final String effectID = "PrecisionStrikeDamage";
	private final int mStacks;
	private final double mAmount;
	private final double mDistanceSquared;

	public PrecisionStrikeDamage(int duration, int stacks, double amount, double distanceSquared) {
		super(duration, effectID);
		mStacks = stacks;
		mAmount = amount;
		mDistanceSquared = distanceSquared;
		displaysTime(false);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.TRUE && event.getType() != DamageEvent.DamageType.OTHER && event.getAbility() != ClassAbility.PRECISION_STRIKE && entity.getLocation().distanceSquared(enemy.getLocation()) >= mDistanceSquared) {
			if (getDuration() > 1 && entity instanceof Player player) {
				player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.5f + 0.1f * mStacks, 1.5f);
			}
			// Remove the effect, but continue to activate during this tick for AOEs and such
			setDuration(1);
			DamageUtils.damage(entity, enemy, DamageEvent.DamageType.PROJECTILE_SKILL, mAmount * mStacks, ClassAbility.PRECISION_STRIKE, true, false);
		}
	}

	public int getStacks() {
		return mStacks;
	}

	@Override
	public double getMagnitude() {
		return mAmount * mStacks;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("stacks", mStacks);
		object.addProperty("amount", mAmount);
		object.addProperty("distanceSquared", mDistanceSquared);

		return object;
	}

	public static PrecisionStrikeDamage deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int stacks = object.get("stacks").getAsInt();
		double amount = object.get("amount").getAsDouble();
		double distanceSquared = object.get("distanceSquared").getAsDouble();

		return new PrecisionStrikeDamage(duration, stacks, amount, distanceSquared);
	}

	@Override
	public String toString() {
		return String.format("PrecisionStrikeDamage duration:%d stacks:%d amount:%f distanceSquared:%f", getDuration(), mStacks, mAmount, mDistanceSquared);
	}
}
