package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Paralyze extends Effect {
	private static final double CHANCE = 0.25;

	private static final Particle.DustOptions COLOR_YELLOW
		= new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOR_FAINT_YELLOW
		= new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	private Plugin mPlugin;

	public Paralyze(int duration, Plugin plugin) {
		super(duration);
		mPlugin = plugin;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof LivingEntity le && FastUtils.randomDoubleInRange(0, 1) < CHANCE) {
			EntityUtils.applySlow(mPlugin, 20, 1, le);

			// Particles
			Location halfHeightLocation = LocationUtils.getHalfHeightLocation(le);
			double widerWidthDelta = PartialParticle.getWidthDelta(le) * 1.5;
			// /particle dust 1 0.945 0.431 1 7053 78.9 7069 0.225 0.45 0.225 0 10
			PartialParticle partialParticle = new PartialParticle(
				Particle.REDSTONE,
				halfHeightLocation,
				6,
				widerWidthDelta,
				PartialParticle.getHeightDelta(le),
				widerWidthDelta,
				0,
				COLOR_FAINT_YELLOW
			).spawnAsEnemy();
			partialParticle.mExtra = 1;
			partialParticle.mData = COLOR_YELLOW;
			partialParticle.spawnAsEnemy();
			partialParticle.mParticle = Particle.FIREWORKS_SPARK;
			partialParticle.mCount = 8;
			partialParticle.mExtra = 0.4;
			partialParticle.mData = null;
			partialParticle.mDirectionalMode = true;
			partialParticle.mExtraVariance = 0.1;
			partialParticle.setDeltaVariance(PartialParticle.DeltaVarianceGroup.VARY_X, true);
			partialParticle.setDeltaVariance(PartialParticle.DeltaVarianceGroup.VARY_Z, true);
			partialParticle.mVaryPositiveY = true;
			partialParticle.spawnAsEnemy();

			World world = le.getWorld();
			Location enemyLocation = le.getLocation();
			world.playSound(
				enemyLocation,
				Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
				SoundCategory.PLAYERS,
				0.35f,
				3f
			);
		}
	}

	@Override
	public String toString() {
		return String.format("Paralyze duration:%d", this.getDuration());
	}

}
