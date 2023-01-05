package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class CrusadeTag extends ZeroArgumentEffect {
	public static final String effectID = "CrusadeSlayerTag";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(252, 211, 3), 1.0f);

	public CrusadeTag(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le) {
			Location loc = le.getLocation().add(0, le.getHeight() + 0.6, 0);
			new PartialParticle(Particle.REDSTONE, loc, 20, 0.01, 0.35, 0.01, COLOR).spawnAsEnemyBuff();
			new PartialParticle(Particle.REDSTONE, loc.add(0, 0.2, 0), 20, 0.175, 0.01, 0.01, COLOR).spawnAsEnemyBuff();
		}
	}

	public static CrusadeTag deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();

		return new CrusadeTag(duration);
	}


	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("CrusadeSlayerTag duration:%d", this.getDuration());
	}
}
