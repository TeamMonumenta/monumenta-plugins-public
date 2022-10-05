package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BoonOfKnightlyPrayer extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "BoonOfKnightlyPrayer";
	public static final String effectID = "BoonOfKnightlyPrayer";

	public static final double HEALTH_THRESHOLD = 0.5;
	public static final double KNOCKBACK_RESIST_AMOUNT = 1;

	public BoonOfKnightlyPrayer(int duration) {
		super(duration, effectID);
	}

	public static BoonOfKnightlyPrayer deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new BoonOfKnightlyPrayer(duration);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			if (entity instanceof Player p) {
				if (p.getHealth() / p.getMaxHealth() <= HEALTH_THRESHOLD) {
					Location loc = entity.getLocation().add(0, 0.25, 0);
					new PartialParticle(Particle.SPELL_INSTANT, loc, 3, 0.25, 0.5, 0.25, 0.02).spawnAsEnemy();
					Plugin.getInstance().mEffectManager.addEffect(p, effectID, new PercentKnockbackResist(10, KNOCKBACK_RESIST_AMOUNT, effectID));
				}
			}

		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Boon of Knightly Prayer";
	}

	@Override
	public String toString() {
		return String.format("BoonOfKnightlyPrayer duration:%d", this.getDuration());
	}
}
