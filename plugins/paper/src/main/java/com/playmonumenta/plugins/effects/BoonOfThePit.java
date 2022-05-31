package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import java.util.HashSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class BoonOfThePit extends ZeroArgumentEffect {

	public static double DAMAGE_BONUS = 1.2;
	public static double HEAL_REDUCTION = 0.9;

	public HashSet<Entity> mEffectedMobs = new HashSet<>();

	public BoonOfThePit(int duration) {
		super(duration);
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

	@Override
	public String toString() {
		return String.format("BoonOfThePit duration:%d", this.getDuration());
	}
}
