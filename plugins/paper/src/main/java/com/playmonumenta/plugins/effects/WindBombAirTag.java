package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
	public void onHurtByEntityWithSource(@NotNull LivingEntity entity, @NotNull DamageEvent event, @NotNull Entity damager, @NotNull LivingEntity source) {
		if (mPlayer.equals(source)) {
			event.setDamage(event.getDamage() * (1 + mAmount));
		}
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
