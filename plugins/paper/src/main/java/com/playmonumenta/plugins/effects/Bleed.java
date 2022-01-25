package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class Bleed extends Effect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	private static final String PERCENT_SPEED_EFFECT_NAME = "BleedPercentSpeed";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "BleedPercentDamageDealt";
	private static final double EFFECT_AMOUNT_PER_LEVEL = -0.1;

	private final int mLevel;
	private final Plugin mPlugin;

	public Bleed(int duration, int level, Plugin plugin) {
		super(duration);
		mLevel = level;
		mPlugin = plugin;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity) {
			LivingEntity le = (LivingEntity) entity;
			if (le.getHealth() <= EntityUtils.getMaxHealth(le) / 2) {
				Location loc = le.getLocation();
				World world = loc.getWorld();
				world.spawnParticle(Particle.REDSTONE, loc, 4, 0.3, 0.6, 0.3, COLOR);
				if (oneHertz) {
					// Delay this call to later since this method runs inside of a loop iterating over the player's effects
					new BukkitRunnable() {
						@Override
						public void run() {
							mPlugin.mEffectManager.addEffect(le, PERCENT_SPEED_EFFECT_NAME,
							                                 new PercentSpeed(20, mLevel * EFFECT_AMOUNT_PER_LEVEL, PERCENT_SPEED_EFFECT_NAME));
							mPlugin.mEffectManager.addEffect(le, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
									new PercentDamageDealt(20, mLevel * EFFECT_AMOUNT_PER_LEVEL));
						}
					}.runTaskLater(mPlugin, 0);
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Bleed duration:%d modifier:%s level:%d", this.getDuration(), "Bleed", mLevel);
	}
}
