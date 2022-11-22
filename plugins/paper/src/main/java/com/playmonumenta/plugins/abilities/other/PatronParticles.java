package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;


public abstract class PatronParticles extends Ability {

	private final Particle mParticle;
	private final Object mParticleData;

	public PatronParticles(
		Plugin plugin,
		Player player,
		Particle particle,
		@Nullable Object particleData,
		AbilityInfo<? extends PatronParticles> info
	) {
		super(plugin, player, info);
		mParticle = particle;
		mParticleData = particleData;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(mPlayer)) {
			return;
		}
		double widthDelta = PartialParticle.getWidthDelta(mPlayer);
		new PartialParticle(
			mParticle,
			LocationUtils.getHeightLocation(mPlayer, 0.25),
			Constants.QUARTER_TICKS_PER_SECOND,
			widthDelta,
			PartialParticle.getHeightDelta(mPlayer) / 2,
			widthDelta,
			0.01,
			mParticleData
		).spawnAsPlayerPassive(mPlayer);
	}

}
