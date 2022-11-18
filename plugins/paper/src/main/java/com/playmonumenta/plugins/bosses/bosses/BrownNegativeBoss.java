package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.effects.BrownPolarityDisplay;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BrownNegativeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_brown_negative";

	private final double mBossVuln;
	private final double mPlayerResist;
	private double mLastDamageTick;

	public static class Parameters extends BossParameters {
		@BossParam(help = "If player is of opposite charge, Boss' damage is multiplied by this (default 0.8)")
		public double PLAYER_DAMAGE_RESIST = 0.8;
		@BossParam(help = "If player is of opposite charge, Player's damage is multiplied by this (default 1.2)")
		public double ENEMY_DAMAGE_VULN = 1.2;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BrownNegativeBoss(plugin, boss);
	}

	public BrownNegativeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		BrownNegativeBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new BrownNegativeBoss.Parameters());
		mBossVuln = p.ENEMY_DAMAGE_VULN;
		mPlayerResist = p.PLAYER_DAMAGE_RESIST;
		mLastDamageTick = mBoss.getTicksLived();
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && ScoreboardUtils.checkTag(player, BrownPolarityDisplay.POSITIVE_TAG)) {
			event.setDamage(event.getDamage() * mPlayerResist);
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (source instanceof Player player && ScoreboardUtils.checkTag(player, BrownPolarityDisplay.POSITIVE_TAG)) {
			event.setDamage(event.getDamage() * mBossVuln);
			playAesthetic();
		}
	}

	private void playAesthetic() {
		if (mLastDamageTick < mBoss.getTicksLived() - 10) {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 0.5f);
			new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 1, 0), 10, 0.5, 1).spawnAsEnemy();
			mLastDamageTick = mBoss.getTicksLived();
		}
	}
}
