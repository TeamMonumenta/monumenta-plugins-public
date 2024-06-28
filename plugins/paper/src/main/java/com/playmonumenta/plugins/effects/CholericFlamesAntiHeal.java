package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class CholericFlamesAntiHeal extends Effect {
	public static final String effectID = "CholericFlamesAntiHeal";

	public CholericFlamesAntiHeal(int duration) {
		super(duration, effectID);
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(0);
		return false;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.DAMAGE_INDICATOR, LocationUtils.getHalfHeightLocation(entity), 3, 0.35, 0.35, 0.35, 0).spawnAsEnemyBuff();
	}

	public static CholericFlamesAntiHeal deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new CholericFlamesAntiHeal(duration);
	}

	@Override
	public String toString() {
		return String.format("CholericFlamesAntiHeal duration:%d", this.getDuration());
	}
}
