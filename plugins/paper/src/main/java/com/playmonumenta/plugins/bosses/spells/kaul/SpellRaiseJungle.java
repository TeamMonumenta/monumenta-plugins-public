package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Raise Jungle/Earth Elementals (dirt): In random spots in the arena,
 * Earth Elementals start rising slowly from below the ground.
 * While they are partially stuck in the ground, they are vulnerable
 * to melee attacks, but they have a very high level of projectile
 * protection. After 40 seconds, they are no longer stuck in the ground
 * and they can move around freely. They are extremely strong and fast,
 * strongly encouraging players to kill them while they are still stuck
 * in the ground. (The number of elementals spawned is equivalent to 2*
 * the number of players.)
 */

public class SpellRaiseJungle extends Spell {
	private static final BlockData PARTICLE_DATA = Material.COARSE_DIRT.createBlockData();
	private static final int ARENA_FLOOR = 8;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mSummonRange;
	private double mDetectRange;
	private int mSummonTime;
	private double mY;
	private List<UUID> mSummoned = new ArrayList<UUID>();

	private int mCooldown;
	private boolean mOnCooldown = false;
	private ChargeUpManager mChargeUp;

	public SpellRaiseJungle(Plugin plugin, LivingEntity boss, double summonRange, double detectRange, int summonTime, int cooldown) {
		this(plugin, boss, summonRange, detectRange, summonTime, cooldown, -1);
	}

	public SpellRaiseJungle(Plugin plugin, LivingEntity boss, double summonRange, double detectRange, int summonTime, int cooldown, double y) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mDetectRange = detectRange;
		mSummonTime = summonTime;
		mCooldown = cooldown;
		mY = y;

		mChargeUp = new ChargeUpManager(mBoss, mSummonTime, ChatColor.GREEN + "Channeling " + ChatColor.DARK_GREEN + "Raise Jungle...",
			BarColor.GREEN, BarStyle.SEGMENTED_10, 50);
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Location loc = mBoss.getLocation();
		if (mY > 0) {
			loc.setY(mY);
		}
		List<Player> players = PlayerUtils.playersInRange(loc, mDetectRange, true);
		players.removeIf(p -> p.getLocation().getY() >= 61);
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
					Location sLoc = loc.clone().add(x, 0, z);
					sLoc.setY(ARENA_FLOOR + 0.25); //so that they do not summon midair if Kaul/Primordial happens to be above the floor
					while (sLoc.getBlock().getType().isSolid() || sLoc.getBlock().isLiquid()) { // cannot spawn in a location where after raising is in a block or a liquid
						x = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
						z = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
						sLoc = loc.clone().add(x, 0, z);
						sLoc.setY(ARENA_FLOOR + 0.25);
					}
					Location spawn = sLoc.clone().subtract(0, 1.75, 0); // should end up 1.5 blocks below the arena floor
					LivingEntity ele = (LivingEntity)LibraryOfSoulsIntegration.summon(spawn, "EarthElemental");
					ele.setCustomName("Mushroom Elemental");
					Location scLoc = sLoc.clone();
					if (ele != null && !mSummoned.contains(ele.getUniqueId())) {
						mSummoned.add(ele.getUniqueId());
						ele.setAI(false);
						new BukkitRunnable() {
							int mTicks = 0;
							Location mPLoc = scLoc;
							double mYInc = 1.6 / mSummonTime;
							boolean mRaised = false;
							@Override
							public void run() {
								mTicks++;

								if (!mRaised) {
									ele.teleport(ele.getLocation().add(0, mYInc, 0));
								}

								if (mTicks >= mSummonTime && !mRaised) {
									mRaised = true;
									ele.setAI(true);
									mPLoc.getWorld().spawnParticle(Particle.BLOCK_DUST, mPLoc, 6, 0.25, 0.1, 0.25, 0.25, PARTICLE_DATA);
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									ele.setHealth(0);
									this.cancel();
									return;
								}

								if (mRaised) {
									Block block = ele.getLocation().getBlock();
									if (block.getType().isSolid() || block.isLiquid()) {
										MovementUtils.knockAway(mBoss.getLocation(), ele, -2.25f, 0.7f);
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

					@Override
					public synchronized void cancel() {
						super.cancel();
						mChargeUp.reset();
					}

					@Override
					public void run() {
						mChargeUp.nextTick();
						if (mChargeUp.getTime() % 5 == 0) {
							for (Player player : players) {
								player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_HIT, 1f, 0.5f);
							}
						}

						if (mSummonTime <= mChargeUp.getTime() && !mSummoned.isEmpty()) {
							for (Player player : players) {
								player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
							}
							this.cancel();
						}

						if (mSummoned.isEmpty()) {
							this.cancel();
						}

						if (mBoss.isDead() || !mBoss.isValid()) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}

		}.runTaskLater(mPlugin, 20);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mSummoned.size() > 0) {
			event.setDamage(event.getDamage() * 0.4);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_GRAVEL_HIT, 1, 0.5f);
			mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, mBoss.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.25, PARTICLE_DATA);
		}
	}

	@Override
	public boolean canRun() {
		return mSummoned.size() <= 0 && !mOnCooldown;
	}

	@Override
	public int cooldownTicks() {
		return mSummonTime + (20 * 18);
	}

	@Override
	public int castTicks() {
		return mSummonTime;
	}

}
