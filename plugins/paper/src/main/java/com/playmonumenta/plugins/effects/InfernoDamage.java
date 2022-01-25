package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class InfernoDamage extends Effect {

	private final int mLevel;
	private final Player mPlayer;

	public InfernoDamage(int duration, int level, Player player) {
		super(duration);
		mLevel = level;
		mPlayer = player;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof LivingEntity le) {
			double damage = mLevel;
			if (le.getFireTicks() <= 0 || EntityUtils.isFireResistant(le)) {
				damage *= 0.5;
			}
			DamageUtils.damage(mPlayer, le, DamageType.AILMENT, damage, null, true, false);
			le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
		}
	}

	@Override
	public String toString() {
		return String.format("Inferno duration:%d modifier:%s level:%d", this.getDuration(), "CustomDamageOverTime", mLevel);
	}

}
