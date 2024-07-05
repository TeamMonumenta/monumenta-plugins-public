package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BoonOfTheFracturedTree extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "BoonOfTheFracturedTree";
	public static final String effectID = "BoonOfTheFracturedTree";
	public static final double DEALT_DAMAGE_MULTIPLIER = 1.075;
	public static final double RECEIVED_DAMAGE_MULTIPLIER = 1.05;

	public BoonOfTheFracturedTree(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (!EntityUtils.isInFieldOfView(enemy, entity)) {
			event.updateDamageWithMultiplier(DEALT_DAMAGE_MULTIPLIER);
		}
	}

	@Override
	public void onHurtByEntityWithSource(LivingEntity entity, DamageEvent event, Entity damager, LivingEntity source) {
		if (!EntityUtils.isInFieldOfView(entity, source)) {
			event.setDamage(event.getDamage() * RECEIVED_DAMAGE_MULTIPLIER);
		}
	}

	@Override
	public String toString() {
		return String.format("BoonOfTheFracturedTree duration:%d", this.getDuration());
	}

	public static BoonOfTheFracturedTree deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new BoonOfTheFracturedTree(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Boon of the Fractured Tree";
	}
}
