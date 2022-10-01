package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;

public class SpellShockStatic extends Effect {
	public static final String effectID = "SpellShockStatic";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	private boolean mTriggered = false;

	public SpellShockStatic(int duration) {
		super(duration, effectID);
	}

	public boolean isTriggered() {
		return mTriggered;
	}

	public void trigger() {
		mTriggered = true;
		setDuration(0);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = entity.getLocation();
		new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.6, 0.2, 1).minimumMultiplier(false).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.3, 0.6, 0.3, COLOR).minimumMultiplier(false).spawnAsEnemyBuff();
	}

	@Override
	public String toString() {
		return String.format("SpellShockStatic duration=%d", this.getDuration());
	}
}
