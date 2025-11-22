package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PetrifyingRoar extends Spell {
	// The radius of the attack
	private static final int ATTACK_RADIUS = 7;

	// The duration between the telegraph and the hit in ticks
	private static final int WINDUP_DURATION = 30;

	// The duration of the petrification done by the attack in ticks
	private static final int PETRIFY_DURATION = 4 * 20;

	// The damage of the attack
	private static final int ATTACK_DAMAGE = 80;

	// custom effect tags
	private static final String PETRIFY_SLOW_ATTR_TAG = "UamielPetrifySlow";
	private static final String PETRIFY_VULN_ATTR_TAG = "UamielPetrifyVuln";

	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;

	private final int mCooldownModifier;

	public PetrifyingRoar(Plugin plugin, LivingEntity boss, Uamiel uamiel, int cooldownModifier) {
		mPlugin = plugin;
		mBoss = boss;
		mUamiel = uamiel;
		mCooldownModifier = cooldownModifier;

		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
	}

	@Override
	public boolean canRun() {
		return mUamiel.canRunSpell(this);
	}

	@Override
	public void run() {
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_CELEBRATE, SoundCategory.HOSTILE, 5f, 1.32f);

		new PPExplosion(Particle.CRIT, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()))
			.count(80)
			.extra(3)
			.spawnAsBoss();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss instanceof Creature creature) {
					creature.setTarget(null);
					creature.getPathfinder().stopPathfinding();
				}

				if (mTicks % 4 == 0) {
					new PPCircle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 0.05, 0), ATTACK_RADIUS - 0.5)
						.countPerMeter(5)
						.delta(0.04)
						.ringMode(false)
						.randomizeAngle(true)
						.spawnAsBoss();
					new PPCircle(Particle.CRIT_MAGIC, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()), 2.25)
						.countPerMeter(3)
						.spawnAsBoss();
					new PPCircle(Particle.GLOW, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()), ATTACK_RADIUS)
						.countPerMeter(2)
						.spawnAsBoss();
				}

				if (mTicks == WINDUP_DURATION) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_STUNNED, SoundCategory.HOSTILE, 5f, 1.2f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 5f, 1.67f);

					new PPExplosion(Particle.END_ROD, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()))
						.count(80)
						.extra(0.5)
						.spawnAsBoss();

					new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()))
						.count(4)
						.spawnAsBoss();

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mBoss.getLocation(), 4, ATTACK_RADIUS);
					for (Player player : hitbox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, ATTACK_DAMAGE, null, false, true, "Petrifying Roar");

						mMonumentaPlugin.mEffectManager.addEffect(player, PETRIFY_SLOW_ATTR_TAG, new PercentSpeed(PETRIFY_DURATION, -1, PETRIFY_SLOW_ATTR_TAG));
						mMonumentaPlugin.mEffectManager.addEffect(player, PETRIFY_VULN_ATTR_TAG, new PercentDamageReceived(PETRIFY_DURATION, 0.5));
					}
				}

				mTicks++;
				if (mTicks > WINDUP_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				if (mBoss instanceof Creature creature) {
					creature.setTarget(EntityUtils.getNearestPlayer(mBoss.getLocation(), 15));
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return WINDUP_DURATION + 50 + mCooldownModifier;
	}
}
