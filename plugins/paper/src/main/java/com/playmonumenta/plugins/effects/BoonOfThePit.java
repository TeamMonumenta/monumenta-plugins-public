package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.HashSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class BoonOfThePit extends ZeroArgumentEffect {
	public static final String effectID = "BoonOfThePit";

	public static double DAMAGE_BONUS = 1.2;
	public static double HEAL_REDUCTION = 0.9;

	public HashSet<Entity> mEffectedMobs = new HashSet<>();

	public BoonOfThePit(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity enemy) {
		if (mEffectedMobs.contains(enemy)) {
			return;
		}
		event.setDamage(event.getDamage() * DAMAGE_BONUS);
		mEffectedMobs.add(enemy);
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * HEAL_REDUCTION);
		return HEAL_REDUCTION > -1;
	}

	public static BoonOfThePit deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new BoonOfThePit(duration);
	}

	@Override
	public String toString() {
		return String.format("BoonOfThePit duration:%d", this.getDuration());
	}
}
