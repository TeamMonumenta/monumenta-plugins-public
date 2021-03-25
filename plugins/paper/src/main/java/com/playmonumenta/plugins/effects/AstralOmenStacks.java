package com.playmonumenta.plugins.effects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;



public class AstralOmenStacks extends Effect {
	public static final Particle.DustOptions COLOR_PURPLE = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1f);

	private int mLevel;

	public AstralOmenStacks(int duration, int level) {
		super(duration);

		mLevel = level;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			World world = entity.getWorld();
			Location location = entity.getLocation().add(0, 1, 0);
			world.spawnParticle(Particle.ENCHANTMENT_TABLE, location, 8, 0, 0, 0, 4);
			world.spawnParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, COLOR_PURPLE);
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
