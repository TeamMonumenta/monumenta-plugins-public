package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class WindBombAirTag extends Effect {
	public static final String effectID = "WindBombAirTag";

	private final double mAmount;
	private final Player mPlayer;

	public WindBombAirTag(int duration, double amount, Player player) {
		super(duration, effectID);
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
	public void onHurtByEntityWithSource(LivingEntity entity, DamageEvent event, Entity damager, LivingEntity source) {
		if (mPlayer.equals(source)) {
			event.setDamage(event.getDamage() * (1 + mAmount));
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			Location loc = entity.getLocation();
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02).spawnAsEnemy();
			new PartialParticle(Particle.CLOUD, loc, 1, 0.25, 0.5, 0.25, 0).spawnAsEnemy();
		}
	}

	@Override
	public String toString() {
		return String.format("WindBombAirTag duration:%d player:%s amount:%f", this.getDuration(), mPlayer.getName(), mAmount);
	}
}
