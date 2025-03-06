package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.effects.BrownPolarityDisplay;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BrownPositiveBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_brown_positive";

	public static class Parameters extends BossParameters {
		@BossParam(help = "If a player has opposite charge, multiply the launcher's damage against the player by this")
		public double PLAYER_DAMAGE_RESIST = 0.8;

		@BossParam(help = "If a player has opposite charge, multiply the player's damage against the launcher by this")
		public double ENEMY_DAMAGE_VULN = 1.2;
	}

	private final double mBossVuln;
	private final double mPlayerResist;
	private double mLastDamageTick;

	public BrownPositiveBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mBossVuln = p.ENEMY_DAMAGE_VULN;
		mPlayerResist = p.PLAYER_DAMAGE_RESIST;
		mLastDamageTick = mBoss.getTicksLived();
	}

	@Override
	public void onDamage(final DamageEvent event, final LivingEntity damagee) {
		if (damagee instanceof Player player && ScoreboardUtils.checkTag(player, BrownPolarityDisplay.NEGATIVE_TAG)) {
			event.setFlatDamage(event.getDamage() * mPlayerResist);
		}
	}

	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		if (source instanceof Player player && ScoreboardUtils.checkTag(player, BrownPolarityDisplay.NEGATIVE_TAG)) {
			event.setFlatDamage(event.getFlatDamage() * mBossVuln);
			playAesthetic();
		}
	}

	private void playAesthetic() {
		if (mLastDamageTick < mBoss.getTicksLived() - 10) {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 1, 0)).count(10).delta(0.5)
				.extra(1).spawnAsEnemy();
			mLastDamageTick = mBoss.getTicksLived();
		}
	}
}
