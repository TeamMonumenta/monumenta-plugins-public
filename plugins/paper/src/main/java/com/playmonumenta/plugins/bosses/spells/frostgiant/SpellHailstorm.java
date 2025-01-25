package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class SpellHailstorm extends Spell {
	private static final String SPELL_NAME = "Hailstorm";
	private static final String SLOWNESS_SRC = "HailstormSlowness";
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mStartLoc;
	private final double mRadius;
	private final List<UUID> mWarned = new ArrayList<>();
	private final Map<Player, BukkitRunnable> mDamage = new HashMap<>();
	private final PPCircle mInnerCircle;
	private final PPCircle mOuterCircle;

	private @Nullable BukkitRunnable mDelay;
	private boolean mDoDamage = true;

	public SpellHailstorm(final Plugin plugin, final LivingEntity boss, final double radius, final Location start) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mStartLoc = start;

		mInnerCircle = new PPCircle(Particle.REDSTONE, mBoss.getLocation(), mRadius - 0.75).count(60).delta(0.1).extra(1).data(LIGHT_BLUE_COLOR);
		mOuterCircle = new PPCircle(Particle.CLOUD, mBoss.getLocation(), mRadius + 5).count(30).delta(2).extra(0.075);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, Constants.TICKS_PER_SECOND * 10);
	}

	@Override
	public void run() {
		final Location bossLoc = mBoss.getLocation();
		Location offsetLoc = bossLoc.clone().add(0, 0.2, 0);
		mInnerCircle.location(offsetLoc).spawnAsEntityActive(mBoss);

		offsetLoc = bossLoc.clone().add(0, 2, 0);
		if (mDoDamage) {
			mOuterCircle.location(offsetLoc).spawnAsEntityActive(mBoss);
		} else {
			return;
		}

		for (final Player player : PlayerUtils.playersInRange(bossLoc, FrostGiant.detectionRange, true)) {
			/* Ignore difference in y values */
			final Location pLocY = player.getLocation();
			pLocY.setY(bossLoc.getY());

			if (pLocY.distanceSquared(bossLoc) > mRadius * mRadius && !mDamage.containsKey(player)
				&& mStartLoc.distanceSquared(pLocY) <= FrostGiant.ARENA_RADIUS * FrostGiant.ARENA_RADIUS) {
				if (!mWarned.contains(player.getUniqueId())) {
					player.sendMessage(Component.text("The " + SPELL_NAME + " is freezing! Move closer to the giant!", NamedTextColor.RED));
					mWarned.add(player.getUniqueId());
				}

				final BukkitRunnable perPlayerDamageRunnable = new BukkitRunnable() {
					int mTicks = 0;
					float mPitch = 1;

					@Override
					public void run() {
						if (player.isDead() || mBoss.isDead() || !mBoss.isValid()
							|| player.getLocation().distanceSquared(bossLoc) > FrostGiant.ARENA_RADIUS * FrostGiant.ARENA_RADIUS) {
							mDamage.remove(player);
							this.cancel();
							return;
						}

						mTicks++;
						mPitch += 0.1f;

						if (mTicks <= Constants.TICKS_PER_SECOND) {
							if (mTicks % 2 == 0) {
								player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, SoundCategory.HOSTILE, 1, mPitch);
							}
							return;
						}

						if (mTicks % (3 * Constants.TICKS_PER_SECOND / 4) == 0) {
							final Location pLocY = player.getLocation();
							final Location bossLoc = mBoss.getLocation();

							pLocY.setY(bossLoc.getY());
							if (mDoDamage && pLocY.distanceSquared(bossLoc) > mRadius * mRadius) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 2.0 + EntityUtils.getMaxHealth(player) * 0.1,
									null, true, false, SPELL_NAME);
								com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
									new PercentSpeed(Constants.TICKS_PER_SECOND * 2, -0.15, SLOWNESS_SRC));

								pLocY.getWorld().playSound(pLocY, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 1);
								new PartialParticle(Particle.FIREWORKS_SPARK, pLocY.clone().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SPIT, pLocY.clone().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.2).spawnAsEntityActive(mBoss);
							} else {
								mDamage.remove(player);
								this.cancel();
							}
						}
					}
				};
				perPlayerDamageRunnable.runTaskTimer(mPlugin, 0, 1);
				mDamage.put(player, perPlayerDamageRunnable);
			}
		}
	}

	public void delayDamage(final int delay) {
		if (mDelay != null && !mDelay.isCancelled()) {
			mDelay.cancel();
		}

		mDoDamage = false;
		mDelay = new BukkitRunnable() {
			@Override
			public void run() {
				mDoDamage = true;
			}
		};
		mDelay.runTaskLater(mPlugin, delay);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
