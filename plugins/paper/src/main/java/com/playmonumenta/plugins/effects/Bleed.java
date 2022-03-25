package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class Bleed extends SingleArgumentEffect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	private static final String PERCENT_SPEED_EFFECT_NAME = "BleedPercentSpeed";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "BleedPercentDamageDealt";

	private final Plugin mPlugin;

	public Bleed(int duration, double amount, Plugin plugin) {
		super(duration, amount);
		mPlugin = plugin;
	}

	@Override
	public double getMagnitude() {
		return mAmount / 0.1;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le) {
			if (le.getHealth() <= EntityUtils.getMaxHealth(le) / 2) {
				Location loc = le.getLocation();
				new PartialParticle(Particle.REDSTONE, loc, 4, 0.3, 0.6, 0.3, COLOR).spawnAsEnemyBuff();
				if (oneHertz) {
					// Delay this call to later since this method runs inside of a loop iterating over the player's effects
					new BukkitRunnable() {
						@Override
						public void run() {
							mPlugin.mEffectManager.addEffect(le, PERCENT_SPEED_EFFECT_NAME,
							                                 new PercentSpeed(20, -mAmount, PERCENT_SPEED_EFFECT_NAME));
							mPlugin.mEffectManager.addEffect(le, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
									new PercentDamageDealt(20, -mAmount));
						}
					}.runTaskLater(mPlugin, 0);
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Bleed duration:%d modifier:%s amount:%f", this.getDuration(), "Bleed", mAmount);
	}
}
