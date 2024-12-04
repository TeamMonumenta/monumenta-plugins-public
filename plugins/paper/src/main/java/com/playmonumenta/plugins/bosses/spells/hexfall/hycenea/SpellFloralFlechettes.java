package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFloralFlechettes extends Spell {

	private static final String ABILITY_NAME = "Floral Flechettes";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mRadius;
	private final int mDamage;
	private final int mChannelTime;
	private final int mInterval;
	private final int mLines;
	private final boolean mBackwardsLine;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final double mWidth;
	private final double mAnglePerIter;
	private final ChargeUpManager mChargeUp;

	public SpellFloralFlechettes(Plugin plugin, LivingEntity boss, int range, int radius, int damage, int channelTime, int interval, int lines, boolean backwardsLine, Location spawnLoc, int cooldown, double width, double anglePerIter) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mRadius = radius;
		mDamage = damage;
		mChannelTime = channelTime;
		mInterval = interval;
		mLines = lines;
		mBackwardsLine = backwardsLine;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mWidth = width;
		mAnglePerIter = anglePerIter;
		mChargeUp = new ChargeUpManager(boss, interval * lines, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		mChargeUp.setColor(BossBar.Color.YELLOW);
		mChargeUp.setTitle(Component.text("Channeling ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
		mChargeUp.setChargeTime(mChannelTime);

		BukkitRunnable runnable = new BukkitRunnable() {

			final Location mRandomLoc = LocationUtils.randomLocationInCircle(mSpawnLoc, mRange);
			final Vector mVec = LocationUtils.getDirectionTo(mSpawnLoc, mRandomLoc).normalize();

			@Override
			public void run() {

				if (mChargeUp.getTime() % 10 == 0) {
					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.BLOCK_MANGROVE_ROOTS_BREAK, SoundCategory.HOSTILE, 2f, 1.5f);
						player.playSound(player.getLocation(), Sound.BLOCK_MANGROVE_ROOTS_PLACE, SoundCategory.HOSTILE, 2f, 1.0f);
					}

					for (int r = 0; r < mRadius; r++) {
						new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(mVec.clone().multiply(r)).add(0, 0.2, 0), mWidth)
							.countPerMeter(2)
							.data(Material.SPORE_BLOSSOM.createBlockData())
							.spawnAsBoss();

						if (mBackwardsLine) {
							new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(mVec.clone().rotateAroundY(Math.toRadians(180)).multiply(r)).add(0, 0.2, 0), mWidth)
								.countPerMeter(2)
								.data(Material.SPORE_BLOSSOM.createBlockData())
								.spawnAsBoss();
						}
					}
				}

				if (mChargeUp.nextTick()) {

					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
					mChargeUp.setColor(BossBar.Color.RED);

					BukkitRunnable castRunnable = new BukkitRunnable() {
						int mT = 0;
						int mLinesCast = 0;

						@Override
						public void run() {
							if (mT > mLines * mInterval) {
								mChargeUp.remove();
								this.cancel();
								return;
							}

							mChargeUp.setProgress((double) mLinesCast / mLines);

							if (mT % mInterval == 0 && mLinesCast < mLines) {
								mLinesCast++;

								for (Player viewer : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
									viewer.playSound(viewer.getLocation(), Sound.BLOCK_MANGROVE_ROOTS_PLACE, SoundCategory.HOSTILE, 1f, 0.5f);
								}

								BukkitRunnable animationRunnable = new BukkitRunnable() {

									final List<Location> mLocs = new ArrayList<>();
									int mTT = 0;
									double mR = 0;
									final int mIterations = mRadius * 2;
									final int mIterPerTick = (int) Math.ceil((double) mIterations / (float) mInterval / 2);

									@Override
									public void run() {

										for (int i = 0; i < mIterPerTick; i++) {
											mR += 0.5;
											if (mR > mRadius) {
												break;
											}

											Location loc = mSpawnLoc.clone().add(mVec.clone().multiply(mR));
											mLocs.add(loc);

											new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(mVec.clone().multiply(mR)).add(0, 0.2, 0), mWidth)
												.countPerMeter(1.5)
												.data(Material.SPORE_BLOSSOM.createBlockData())
												.spawnAsBoss();

											if (mBackwardsLine) {
												Location backLoc = mSpawnLoc.clone().add(mVec.clone().rotateAroundY(Math.toRadians(180)).multiply(mR));
												mLocs.add(backLoc);

												new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(mVec.clone().rotateAroundY(Math.toRadians(180)).multiply(mR)).add(0, 0.2, 0), mWidth)
													.countPerMeter(1.5)
													.data(Material.SPORE_BLOSSOM.createBlockData())
													.spawnAsBoss();
											}

											for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
												for (Location location : mLocs) {
													if (location.distance(p.getLocation()) <= mWidth) {
														DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
														break;
													}
												}
											}
										}

										mTT++;
										if (mTT >= mInterval / 2) {
											this.cancel();
										}
									}
								};
								animationRunnable.runTaskTimer(mPlugin, 0, 1);
								mActiveRunnables.add(animationRunnable);

								if (mAnglePerIter > 0) {
									mVec.rotateAroundY(Math.toRadians(mAnglePerIter));
								} else {
									mVec.rotateAroundY(Math.toRadians((float) 180 / mLines));
								}

								for (int i = 0; i < 5; i++) {
									Vector start = mVec.clone().multiply(0).setY(Math.random());
									Vector middle = mVec.clone().multiply(mRadius).multiply(0.5).setY(i);
									Vector end = mVec.clone().multiply(mRadius).setY(Math.random());

									new PPBezier(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(start), mSpawnLoc.clone().add(middle), mSpawnLoc.clone().add(end))
										.count(15)
										.delay(mInterval)
										.data(Material.FLOWERING_AZALEA_LEAVES.createBlockData())
										.spawnAsBoss();

									if (mBackwardsLine) {
										new PPBezier(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(start.rotateAroundY(Math.toRadians(180))), mSpawnLoc.clone().add(middle.rotateAroundY(Math.toRadians(180))), mSpawnLoc.clone().add(end.rotateAroundY(Math.toRadians(180))))
											.count(15)
											.delay(mInterval)
											.data(Material.FLOWERING_AZALEA_LEAVES.createBlockData())
											.spawnAsBoss();
									}
								}
							}

							mT++;
						}
					};
					castRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(castRunnable);

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
