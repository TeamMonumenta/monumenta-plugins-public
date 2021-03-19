package com.playmonumenta.plugins.effects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class AstralOmenStacks extends Effect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1.0f);
	private static final int DURATION = 10 * 20;

	private double mAmount;
	private final Player mPlayer;
	private final String mModifierName;

	public AstralOmenStacks(int duration, double amount, Player player, String modifierName) {
		super(duration);
		mAmount = amount;
		mPlayer = player;
		mModifierName = modifierName;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}
	
	public void setMagnitude(double mag) {
		mAmount = mag;
	}
	
	public void clearEffect() {
		mAmount = 0;
		setDuration(0);
	}
	
	@Override
	public void entityLoseEffect(Entity entity) {
		if (mAmount > 1) {
			double newAmount = mAmount - 1;
			setMagnitude(newAmount);
			setDuration(DURATION);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			World world = entity.getWorld();
			Location loc = entity.getLocation().add(0, 1, 0);
			for (int i = 0; i < mAmount; i++) {
				world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 8, 0, 0, 0, 4);
				world.spawnParticle(Particle.REDSTONE, loc, 8, 0.2, 0.2, 0.2, 0.1, COLOR);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("AstralOmenStacks duration:%d player:%s amount:%f name:%s", this.getDuration(), mPlayer.getName(), mAmount, mModifierName);
	}
}
