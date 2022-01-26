package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BlackflameGolemNecromancy extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mSummonRange;
	private double mDetectRange;
	private int mSummonTime;
	private double mY;
	private List<UUID> mSummoned = new ArrayList<UUID>();

	private Location mSpawnLoc;

	private int mCooldown;
	private boolean mOnCooldown = false;

	private static final List<String> CONSTRUCTS = Arrays.asList("BlackflameConstruct", "DragonConstruct");

	private BeastOfTheBlackFlame mBossClass;

	public BlackflameGolemNecromancy(Plugin plugin, LivingEntity boss, double summonRange, double detectRange, int summonTime, int cooldown, double y, Location spawnLoc, BeastOfTheBlackFlame bossClass) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mDetectRange = detectRange;
		mSummonTime = summonTime;
		mCooldown = cooldown;
		mY = y;
		mSpawnLoc = spawnLoc;
		mBossClass = bossClass;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Location loc = mBoss.getLocation();
		if (mY > 0) {
			loc.setY(mY);
		}
		List<Player> players = PlayerUtils.playersInRange(loc, mDetectRange, true);
		int num = 0;
		if (players.size() == 1) {
			num = 4;
		} else if (players.size() < 5) {
			num += 3 * players.size();
		} else if (players.size() < 11) {
			num += 12 + (2 * (players.size() - 4));
		} else if (players.size() >= 11) {
			num += 24 + (1 * (players.size() - 10));
		}
		int amt = num;
		new BukkitRunnable() {

			@Override
			public void run() {
				for (int i = 0; i < amt; i++) {
					double x = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
					double z = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
					Location sLoc = loc.clone().add(x, 0.25, z);
					for (int j = 0; j < 30 && (sLoc.getBlock().getType().isSolid() || sLoc.getBlock().isLiquid()); j++) {
						x = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
						z = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
						sLoc = loc.clone().add(x, 0.25, z);
					}
					Location spawn = sLoc.clone().subtract(0, 1.75, 0);
					LivingEntity ele = (LivingEntity)LibraryOfSoulsIntegration.summon(spawn, CONSTRUCTS.get(FastUtils.RANDOM.nextInt(CONSTRUCTS.size())));
					Location scLoc = sLoc.clone();
					if (ele != null && !mSummoned.contains(ele.getUniqueId())) {
						mSummoned.add(ele.getUniqueId());
						ele.setAI(false);

						new BukkitRunnable() {
							int mT = 0;
							Location mPLoc = scLoc;
							double mYInc = 1.6 / mSummonTime;
							boolean mRaised = false;
							@Override
							public void run() {
								mT++;

								if (!mRaised) {
									ele.teleport(ele.getLocation().add(0, mYInc, 0));
								}

								if (mT >= mSummonTime && !mRaised) {
									mRaised = true;
									ele.setAI(true);
									mPLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, mPLoc, 6, 0.25, 0.1, 0.25, 0.25);
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									ele.setHealth(0);
									this.cancel();
									return;
								}

								if (mRaised) {
									Block block = ele.getLocation().getBlock();
									if (block.getType().isSolid() || block.isLiquid()) {
										MovementUtils.knockAway(mBoss.getLocation(), ele, -2.25f, 0.7f, false);
									} else if (mSpawnLoc.getY() - ele.getLocation().getY() >= 3) {
										ele.teleport(mSpawnLoc);
									}
								}

								if (ele == null || ele.isDead() || !ele.isValid()) {
									this.cancel();
									mSummoned.remove(ele.getUniqueId());
									if (mSummoned.size() <= 0) {
										new BukkitRunnable() {

											@Override
											public void run() {
												mOnCooldown = false;
											}

										}.runTaskLater(mPlugin, mCooldown);
									}
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
					}
				}
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						if (mTicks % 10 == 0) {
							for (Player player : players) {
								player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0f);
							}
						}

						if (mSummonTime <= mTicks && !mSummoned.isEmpty()) {
							for (Player player : players) {
								player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1f);
							}
							this.cancel();
						}

						if (mSummoned.isEmpty()) {
							this.cancel();
						}

						if (mBoss.isDead() || !mBoss.isValid()) {
							this.cancel();
						}

						mTicks++;
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}

		}.runTaskLater(mPlugin, 20);
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	@Override
	public int cooldownTicks() {
		return (int) (mSummonTime * mBossClass.mCastSpeed);
	}

	@Override
	public int castTicks() {
		return (int) (mSummonTime * mBossClass.mCastSpeed);
	}
}
