package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class AntiRangeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_antirange";

	@BossParam(help = "The launcher gains damage immunity to long range attacks")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 40;
		@BossParam(help = "Attacks originating from a damager outside this radius are mitigated")
		public int DISTANCE = 12;
	}

	private final int mDistanceSquared;

	public AntiRangeBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new AntiRangeBoss.Parameters());
		mDistanceSquared = p.DISTANCE * p.DISTANCE;
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, null);
	}

	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		Location loc = mBoss.getLocation();

		if (loc.distanceSquared(source.getLocation()) > mDistanceSquared) {
			event.setCancelled(true);
			new PartialParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0)).count(20).delta(0)
				.extra(0.3).spawnAsEntityActive(mBoss);
			mBoss.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, 1.5f);
		}
	}
}
