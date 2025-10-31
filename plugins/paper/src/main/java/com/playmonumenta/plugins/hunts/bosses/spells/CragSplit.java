package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class CragSplit extends Spell {
	// cooldown times between attacks
	private static final int RANDOM_ATTACK_MIN = 4 * 20;
	private static final int RANDOM_ATTACK_MAX = 7 * 20;

	private static final int RIFT_COUNT = 6;

	private static final int TELEGRAPH_DURATION = 25;

	// distance from the boss for the cutoff for the attack hitbox to widen
	private static final double WIDE_CUTOFF = 5;

	private static final int ATTACK_DAMAGE = 75;

	// the maximum distance the rift can travel and the step distance for iteration
	private static final double RIFT_DISTANCE = 20;
	private static final double RIFT_STEP = 0.275;
	private static final int STEPS_PER_TICK = 4;

	// the maximum value for randomization for the turning factor (how much the rift turns after each step)
	private static final double MAX_TURN = 0.175 / (Math.PI * 2);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;

	private int mTicks = 0;
	private int mAttackDelay;

	public CragSplit(Plugin plugin, LivingEntity boss, Uamiel uamiel) {
		mPlugin = plugin;
		mBoss = boss;
		mUamiel = uamiel;

		setAttackDelay();
	}

	@Override
	public void run() {
		if (mTicks == mAttackDelay) {
			if (mBoss.isOnGround() && !mUamiel.mIsBanishing) {
				mTicks = 0;
				setAttackDelay();

				double baseTheta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
				double turnFactor = FastUtils.randomDoubleInRange(-MAX_TURN, MAX_TURN);
				Location startLocation = mBoss.getLocation().clone();

				telegraphAttack(baseTheta, turnFactor, startLocation);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> runAttack(baseTheta, turnFactor, startLocation), TELEGRAPH_DURATION);
			}
		} else {
			mTicks++;
		}
	}

	private void setAttackDelay() {
		mAttackDelay = FastUtils.randomIntInRange(RANDOM_ATTACK_MIN, RANDOM_ATTACK_MAX);
	}

	private void telegraphAttack(double baseTheta, double turnFactor, Location startLocation) {
		mBoss.getWorld().playSound(startLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 3f, 0.7f);

		for (int i = 0; i < RIFT_COUNT; i++) {
			double finalTheta = baseTheta + (Math.PI * 2 / RIFT_COUNT) * i;
			new BukkitRunnable() {
				int mTicks = 0;
				double mTheta = finalTheta % (Math.PI * 2);
				Location mLocation = startLocation.clone().add(0, 0.5, 0);

				@Override
				public void run() {
					for (int j = 0; j < STEPS_PER_TICK; j++) {
						Vector direction = new Vector(FastUtils.cos(mTheta), 0, FastUtils.sin(mTheta));
						mTheta = (mTheta + turnFactor) % (Math.PI * 2);

						mLocation.add(direction.multiply(RIFT_STEP));
						mLocation = LocationUtils.fallToGround(mLocation.add(0, 3, 0), startLocation.getY() - 5).add(0, 0.5, 0);
						for (int p = 0; p < 3; p++) {
							new PartialParticle(Particle.SCRAPE, LocationUtils.varyInCircle(mLocation, 0.15))
								.count(1)
								.directionalMode(true)
								.delta(0, 1, 0)
								.extra(6)
								.spawnAsBoss();
						}
					}

					if (mTicks % 2 == 0) {
						mBoss.getWorld().playSound(startLocation, Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1f, 0.85f - 0.35f * ((float) mTicks / 16));
					}

					this.mTicks++;
					if (mLocation.distance(startLocation) > RIFT_DISTANCE || mTicks > 150 || mBoss.isDead()) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void runAttack(double baseTheta, double turnFactor, Location startLocation) {
		if (mBoss.isDead()) {
			return;
		}

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3f, 0.75f);

		for (int i = 0; i < RIFT_COUNT; i++) {
			double finalTheta = baseTheta + (Math.PI * 2 / RIFT_COUNT) * i;
			new BukkitRunnable() {
				int mTicks = 0;
				double mTheta = finalTheta % (Math.PI * 2);
				Location mLocation = startLocation.clone().add(0, 0.5, 0);
				final List<Player> mHitPlayers = new ArrayList<>();

				@Override
				public void run() {
					for (int j = 0; j < STEPS_PER_TICK; j++) {
						Vector direction = new Vector(FastUtils.cos(mTheta), 0, FastUtils.sin(mTheta));
						mTheta = (mTheta + turnFactor) % (Math.PI * 2);

						mLocation.add(direction.multiply(RIFT_STEP));
						mLocation = LocationUtils.fallToGround(mLocation.add(0, 3, 0), startLocation.getY() - 5).add(0, 0.5, 0);
						Hitbox hitbox;
						if (mLocation.distance(startLocation) < WIDE_CUTOFF) {
							new PartialParticle(Particle.SMOKE_NORMAL, mLocation).directionalMode(true).delta(0, 1, 0).extra(0.125).count(3).spawnAsBoss();
							hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(mLocation, 0.3, 1.5, 0.3));
						} else {
							new PartialParticle(Particle.SMOKE_LARGE, mLocation).directionalMode(true).delta(0, 1, 0).extra(0.05).count(1).spawnAsBoss();
							hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(mLocation, 0.75, 1.5, 0.75));
						}
						new PartialParticle(Particle.CRIT, mLocation.clone().add(0, -0.45, 0)).count(1).spawnAsBoss();

						for (Player player : hitbox.getHitPlayers(true)) {
							if (!mHitPlayers.contains(player)) {
								mHitPlayers.add(player);
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, ATTACK_DAMAGE, null, false, false, "Crag Split");
								MovementUtils.knockAway(mBoss.getLocation(), player, 0.05f, 0.6f, false);

								player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.7f, 0.5f);
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 1f, 0.65f);
							}
						}
					}

					if (mTicks % 2 == 0) {
						mBoss.getWorld().playSound(startLocation, Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1f, 0.85f - 0.35f * ((float) mTicks / 16));
						mBoss.getWorld().playSound(startLocation, Sound.BLOCK_AZALEA_BREAK, SoundCategory.HOSTILE, 1f, 0.85f - 0.35f * ((float) mTicks / 16));
					}

					this.mTicks++;
					if (mLocation.distance(startLocation) > RIFT_DISTANCE || mTicks > 150 || mBoss.isDead()) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
