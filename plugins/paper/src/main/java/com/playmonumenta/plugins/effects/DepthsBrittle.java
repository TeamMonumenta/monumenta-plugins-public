package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DepthsBrittle extends Effect {
	public static final String effectID = "DepthsBrittle";

	private final float mKnockbackSpeed;

	public DepthsBrittle(int duration, float knockbackSpeed) {
		super(duration, effectID);
		mKnockbackSpeed = knockbackSpeed;
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity damager) {
		if (damager instanceof Player player && event.getType() == DamageEvent.DamageType.MELEE) {
			World world = player.getWorld();
			world.playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.7f, 1.7f);
			world.playSound(entity.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 0.7f, 1.7f);
			world.playSound(entity.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.7f, 0.9f);
			world.playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.7f, 1.3f);

			MovementUtils.knockAway(damager, entity, mKnockbackSpeed, 0.4f, false);
			clearEffect();
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("knockbackSpeed", mKnockbackSpeed);
		return object;
	}

	public static DepthsBrittle deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		float knockbackSpeed = object.get("knockbackSpeed").getAsFloat();
		return new DepthsBrittle(duration, knockbackSpeed);
	}

	@Override
	public String toString() {
		return String.format("%s duration:%d knockbackSpeed:%f", effectID, mDuration, mKnockbackSpeed);
	}
}
