package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;


public class AstralOmenIceStacks extends Effect {
	public static final String effectID = "AstralOmenIceStacks";
	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 209, 255), 1f);

	private final int mLevel;

	public AstralOmenIceStacks(int duration, int level) {
		super(duration, effectID);

		mLevel = level;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			Location location = entity.getLocation().add(0, 1, 0);
			new PartialParticle(Particle.ENCHANTMENT_TABLE, location, 2, 0, 0, 0, 4).spawnAsEnemyBuff();
			new PartialParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsEnemyBuff();
		}
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s magnitude:%s",
			this.getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}
}
