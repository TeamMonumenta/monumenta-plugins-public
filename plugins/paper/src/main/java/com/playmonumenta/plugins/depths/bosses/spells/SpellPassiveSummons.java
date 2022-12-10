package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellPassiveSummons extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mSummonRange;
	private double mDetectRange;
	private int mSummonTime;
	private double mY;
	private List<UUID> mSummoned = new ArrayList<UUID>();
	private int mFightNumber;
	private Nucleus mBossInstance;

	private Location mSpawnLoc;

	public int mTicks = 0;

	private static final List<String> CONSTRUCTS = Arrays.asList("EldritchSpawn", "EldritchHunter", "EngorgedFloater");
	private static final List<String> CONSTRUCTS_ELITE = Arrays.asList("Alarinkiri", "UnavoidableDespair", "RequiemoftheFlames");
	private static final int ELITE_CHANCE_PER_FLOOR = 5;


	public SpellPassiveSummons(Plugin plugin, LivingEntity boss, double summonRange, int summonTime, double y, Location spawnLoc, int fightNumber, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mDetectRange = 30;
		mSummonTime = summonTime;
		mY = y;
		mSpawnLoc = spawnLoc;
		mFightNumber = fightNumber;
		mBossInstance = bossInstance;
	}

	@Override
	public void run() {
		mTicks += 5;

		//Don't run in start animation, or if spawning is disabled
		if (mTicks < 6 * 20 || (mBossInstance != null && !mBossInstance.mCanSpawnMobs)) {
			return;
		}

		double multiplier = 2 - (PlayerUtils.playersInRange(mSpawnLoc, mDetectRange, true).size() * .25);

		if ((int) (mTicks % (25 * 20 * multiplier)) == 0) {
			summon(true);
		} else if (mTicks % (4 * 20 * multiplier) == 0) {
			summon(false);
		}
	}

	public void summon(boolean isElite) {
		Location loc = mBoss.getLocation();
		if (mY > 0) {
			loc.setY(mY);
		}
		List<Player> players = PlayerUtils.playersInRange(loc, mDetectRange, true);


		int amt = 1;
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
					LivingEntity ele = (isElite || isEliteSummon()) ? (LivingEntity) LibraryOfSoulsIntegration.summon(spawn, CONSTRUCTS_ELITE.get(FastUtils.RANDOM.nextInt(CONSTRUCTS.size()))) : (LivingEntity) LibraryOfSoulsIntegration.summon(spawn, CONSTRUCTS.get(FastUtils.RANDOM.nextInt(CONSTRUCTS.size())));
					Location scLoc = sLoc.clone();
					if (ele != null && !mSummoned.contains(ele.getUniqueId())) {
						mSummoned.add(ele.getUniqueId());
						ele.setAI(false);
						ele.setPersistent(true);

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
									new PartialParticle(Particle.SMOKE_LARGE, mPLoc, 6, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);
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

	public boolean isEliteSummon() {
		Random r = new Random();
		int roll = r.nextInt(100);
		if (roll < (Math.sqrt(mFightNumber) - 1) * ELITE_CHANCE_PER_FLOOR) {
			return true;
		}
		return false;
	}


	@Override
	public int castTicks() {
		return mSummonTime;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
