package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.NavigableSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ToxicRepulsion extends Spell {
	private static final int TELEGRAPH_DURATION = 25;
	private static final double ACTIVATION_RANGE = 7;

	private static final int SCREAM_WAVES = 10;
	private static final int SCREAM_WAVE_TIME = 4;

	// radius increases by 0.75 per wave until max
	private static final double SCREAM_RADIUS_START = 3;
	private static final double SCREAM_RADIUS = 7.5;

	private static final int ATTACK_DAMAGE = 10;

	private static final int EFFECT_DURATION = 10 * 20;
	private static final String WEAKNESS_TAG = "ExperimentRepulsionWeakness";
	private static final double WEAKNESS_AMOUNT = 0.15;
	private static final String VULNERABILITY_TAG = "ExperimentRepulsionVulnerability";
	private static final double VULNERABILITY_AMOUNT = 0.15;

	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperimentSeventyOne;

	private final int mCooldownModifier;

	public ToxicRepulsion(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experimentSeventyOne, int cooldownModifier) {
		mPlugin = plugin;
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperimentSeventyOne = experimentSeventyOne;
		mCooldownModifier = cooldownModifier;
	}

	@Override
	public boolean canRun() {
		return mExperimentSeventyOne.canRunSpell(this)
			&& !PlayerUtils.playersInRange(mBoss.getLocation(), ACTIVATION_RANGE, true).isEmpty();
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION + SCREAM_WAVE_TIME * SCREAM_WAVES);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_RETREAT, SoundCategory.HOSTILE, 4f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_RETREAT, SoundCategory.HOSTILE, 4f, 2f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, 4f, 0.8f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			double mScreamRadius = SCREAM_RADIUS_START;

			@Override
			public void run() {
				if (mTicks < TELEGRAPH_DURATION && mTicks % 5 == 0) {
					Location location = mBoss.getLocation().clone().add(0, mBoss.getBoundingBox().getHeight() * 0.75, 0);
					new PPParametric(Particle.CRIT, location, (parameter, builder) -> {
						double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						Vector direction = new Vector(FastUtils.cos(theta), 0, FastUtils.sin(theta));
						builder.offset(-direction.getX(), 0, -direction.getZ());
						builder.location(location.clone().add(direction.multiply(2.5)));
					})
						.count(80)
						.directionalMode(true)
						.extra(0.3)
						.spawnAsBoss();
				}
				if (mTicks >= TELEGRAPH_DURATION && (mTicks - TELEGRAPH_DURATION) % SCREAM_WAVE_TIME == 0) {
					Location location = mBoss.getLocation().clone().add(0, mBoss.getBoundingBox().getHeight() * 0.75, 0);
					new PPParametric(Particle.CRIT, location, (parameter, builder) -> {
						Vector direction = VectorUtils.randomUnitVector();
						if (direction.getY() < -0.3) {
							direction.setY(-direction.getY());
						}
						builder.offset(direction.getX(), direction.getY(), direction.getZ());
					})
						.count(80)
						.directionalMode(true)
						.extra(mScreamRadius * 0.45)
						.spawnAsBoss();

					Hitbox hitbox = new Hitbox.SphereHitbox(location, mScreamRadius);
					for (Player player : hitbox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ATTACK_DAMAGE, null, true, false, "Toxic Repulsion");
						MovementUtils.knockAway(mBoss.getLocation(), player, 0.3f, 0.3f, false);

						NavigableSet<Effect> vulnEffects = mMonumentaPlugin.mEffectManager.getEffects(player, VULNERABILITY_TAG);
						double currentVuln = 0;
						if (vulnEffects != null) {
							for (Effect effect : vulnEffects) {
								currentVuln = effect.getMagnitude();
							}
						}
						mMonumentaPlugin.mEffectManager.addEffect(player, VULNERABILITY_TAG, new PercentDamageReceived(EFFECT_DURATION, currentVuln + VULNERABILITY_AMOUNT));

						NavigableSet<Effect> weakEffects = mMonumentaPlugin.mEffectManager.getEffects(player, WEAKNESS_TAG);
						double currentWeak = 0;
						if (weakEffects != null) {
							for (Effect effect : weakEffects) {
								currentWeak = effect.getMagnitude();
							}
						}
						mMonumentaPlugin.mEffectManager.addEffect(player, WEAKNESS_TAG, new PercentDamageDealt(EFFECT_DURATION, -(currentWeak + WEAKNESS_AMOUNT)));
					}

					mScreamRadius = Math.min(mScreamRadius + 0.75, SCREAM_RADIUS);

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.5f, 1.2f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.HOSTILE, 0.5f, 1f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.HOSTILE, 0.5f, 0.5f);
				}

				if (mTicks % 5 == 0) {
					new PPCircle(Particle.REDSTONE, mBoss.getLocation().clone().add(0, mBoss.getBoundingBox().getHeight() * 0.75, 0), mScreamRadius)
						.countPerMeter(3)
						.ringMode(true)
						.data(new Particle.DustOptions(Color.fromRGB(200, 80, 0), 1.2f))
						.spawnAsBoss();
					if (mTicks > TELEGRAPH_DURATION) {
						new PPCircle(Particle.WAX_ON, mBoss.getLocation().clone().add(0, mBoss.getBoundingBox().getHeight() * 0.75, 0), mScreamRadius)
							.countPerMeter(2)
							.delta(0, 1, 0)
							.ringMode(true)
							.spawnAsBoss();
					}
				}

				mTicks++;
				if (mTicks > TELEGRAPH_DURATION + SCREAM_WAVE_TIME * SCREAM_WAVES || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 30 + TELEGRAPH_DURATION + SCREAM_WAVE_TIME * SCREAM_WAVES + mCooldownModifier;
	}
}
