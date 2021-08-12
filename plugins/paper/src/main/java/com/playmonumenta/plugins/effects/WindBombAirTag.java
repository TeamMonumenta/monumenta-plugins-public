package com.playmonumenta.plugins.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class WindBombAirTag extends Effect {

	private final double mAmount;
	private final Player mPlayer;

	public WindBombAirTag(int duration, double amount, Player player) {
		super(duration);
		mAmount = amount;
		mPlayer = player;
	}

	// This needs to trigger after any percent damage
	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent && mPlayer.equals(((EntityDamageByEntityEvent) event).getDamager())) {
			Entity entity = event.getEntity();
			if (entity instanceof LivingEntity) {
				event.setDamage(event.getDamage() * (1 + mAmount));
			}
		} else if (((EntityDamageByEntityEvent) event).getDamager() instanceof Projectile) {
			if (mPlayer.equals(((Projectile) ((EntityDamageByEntityEvent) event).getDamager()).getShooter())) {
				event.setDamage(event.getDamage() * (1 + mAmount));
			}
		}
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			World world = entity.getWorld();
			Location loc = entity.getLocation();
			world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02);
			world.spawnParticle(Particle.CLOUD, loc, 1, 0.25, 0.5, 0.25, 0);
			if (entity.isOnGround()) {
				setDuration(0);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("WindBombAirTag duration:%d player:%s amount:%f", this.getDuration(), mPlayer.getName(), mAmount);
	}
}
