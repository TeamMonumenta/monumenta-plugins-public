package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class CounterStrike extends Ability {

	private static final float COUNTER_STRIKE_DISTANCE = 7;
	private static final float COUNTER_STRIKE_DOT_ANGLE = 0.33f;
	private static final int COUNTER_STRIKE_COOLDOWN = 20 * 8;
	private static final int COUNTER_STRIKE_ACTIVATION_PERIOD = 20 * 2;
	private static final int COUNTER_STRIKE_1_DAMAGE = 6;
	private static final int COUNTER_STRIKE_2_DAMAGE = 12;
	private static final float COUNTER_STRIKE_RADIUS = 5.0f;

	private boolean mActive = false;
	private int mRiposteTriggeredTick = 0;

	public CounterStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CounterStrike";
		mInfo.linkedSpell = Spells.COUNTER_STRIKE;
		mInfo.cooldown = COUNTER_STRIKE_COOLDOWN;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		//  If we're not going to succeed in our Random we probably don't want to attempt to grab the scoreboard value anyways.
		if (mRandom.nextFloat() < 0.15f) {
			int counterStrike = getAbilityScore();
			Location loc = mPlayer.getLocation();
			mPlayer.spawnParticle(Particle.SWEEP_ATTACK, loc.getX(), loc.getY() + 1.5D, loc.getZ(), 20, 1.5D, 1.5D, 1.5D);
			mPlayer.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);

			double csDamage = counterStrike == 1 ? COUNTER_STRIKE_1_DAMAGE : COUNTER_STRIKE_2_DAMAGE;

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_RADIUS, mPlayer)) {
				EntityUtils.damageEntity(mPlugin, mob, csDamage, mPlayer);
			}
		}

		// Need to wait a tick so that Riposte has a chance to activate first
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.COUNTER_STRIKE)) {
					// Counterstrike becomes active when player is hit while blocking OR if hit and riposte has triggered in the last 5 ticks
					if (mPlayer.isBlocking() || (mPlayer.getTicksLived() - mRiposteTriggeredTick < 5)) {
						mPlayer.spawnParticle(Particle.CRIT, mPlayer.getLocation(), 10, 0, 0, 0, 1);
						mActive = true;
						new BukkitRunnable() {
							@Override
							public void run() {
								mActive = false;
								this.cancel();
							}
						}.runTaskLater(mPlugin, COUNTER_STRIKE_ACTIVATION_PERIOD);
					}
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);

		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mActive) {
			ParticleUtils.explodingConeEffect(mPlugin, mPlayer, COUNTER_STRIKE_DISTANCE, Particle.SWEEP_ATTACK, 1, Particle.SWEEP_ATTACK, 0, COUNTER_STRIKE_DOT_ANGLE);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);
			LivingEntity damagee = (LivingEntity) event.getEntity();
			int damage = getAbilityScore() == 1 ? COUNTER_STRIKE_1_DAMAGE : COUNTER_STRIKE_2_DAMAGE;

			if (PlayerUtils.isCritical(mPlayer)) {
				event.setDamage(event.getDamage() + damage);
			} else {
				event.setDamage(event.getDamage() + damage);
			}

			Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_DISTANCE, mPlayer)) {
				if (mob != damagee) {
					Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
					if (playerDir.dot(toMobVector) > COUNTER_STRIKE_DOT_ANGLE) {
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
					}
				}
			}

			putOnCooldown();
		}

		return true;
	}

	protected void riposteTriggered() {
		mRiposteTriggeredTick = mPlayer.getTicksLived();
	}
}
