package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
	private static final String SPELL_NAME = "Raise Jungle";
	private static final BlockData PARTICLE_DATA = Material.COARSE_DIRT.createBlockData();
	private static final int ARENA_FLOOR = 8;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mSummonRange;
	private final int mSummonTime;
	private final Location mCenter;
	private final List<UUID> mSummoned = new ArrayList<>();

	private final int mCooldown;
	private boolean mOnCooldown = false;
	private final ChargeUpManager mChargeUp;

	public SpellRaiseJungle(Plugin plugin, LivingEntity boss, double summonRange, int summonTime, int cooldown, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mSummonRange = summonRange;
		mSummonTime = summonTime;
		mCenter = center;
		mCooldown = cooldown;

		mChargeUp = new ChargeUpManager(mBoss, mSummonTime, Component.text("Channeling ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_GREEN)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 50);
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Location loc = mBoss.getLocation();
		loc.setY(ARENA_FLOOR + 0.25);
		List<Player> players = Kaul.getArenaParticipants(mCenter);

		int num = 0;
		if (players.size() == 1) {
			num = 4;
		} else if (players.size() < 5) {
			num += 3 * players.size();
		} else if (players.size() < 11) {
			num += 12 + (2 * (players.size() - 4));
		} else {
			num += 24 + (players.size() - 10);
		}
		int amt = num;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (int i = 0; i < amt; i++) {
				Location sLoc = LocationUtils.randomSafeLocationInCircle(loc, mSummonRange, location -> {
					Block block = location.getBlock();
					return !block.isSolid() && !block.isLiquid();
				});
				Location spawn = sLoc.clone().subtract(0, 1.75, 0); // should end up 1.5 blocks below the arena floor
				LivingEntity ele = (LivingEntity) LibraryOfSoulsIntegration.summon(spawn, "EarthElemental");
				if (ele == null) {
					MMLog.severe("[Kaul] Soul \"EarthElemental\" does not exist!");
					return;
				}

				ele.setAI(false);
				mSummoned.add(ele.getUniqueId());

				BukkitRunnable runnable = new BukkitRunnable() {
					int mTicks = 0;
					final Location mPLoc = sLoc;
					final double mYInc = 1.5 / mSummonTime;
					boolean mRaised = false;

					@Override
					public void run() {
						if (!mRaised) {
							ele.teleport(ele.getLocation().add(0, mYInc, 0));
							if (mTicks >= mSummonTime) {
								mRaised = true;
								ele.setAI(true);
								new PartialParticle(Particle.BLOCK_CRACK, mPLoc, 6, 0.25, 0.1, 0.25, 0.25, PARTICLE_DATA).spawnAsEntityActive(mBoss);
							}
						} else {
							if (ele.getLocation().getBlock().isLiquid()) {
								MovementUtils.knockAway(mBoss.getLocation(), ele, -2.25f, 0.7f);
							}
						}

						if (!ele.isValid()) {
							this.cancel();

							mSummoned.remove(ele.getUniqueId());
							if (mSummoned.isEmpty()) {
								Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, mCooldown);
							}
						}

						mTicks++;
					}

					@Override
					public synchronized void cancel() throws IllegalStateException {
						super.cancel();
						if (!mBoss.isValid()) {
							ele.setHealth(0);
						}
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runnable);
			}

			BukkitRunnable masterRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mSummoned.isEmpty()) {
						this.cancel();
						return;
					}

					if (mChargeUp.getTime() % 5 == 0) {
						for (Player player : players) {
							player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_HIT, SoundCategory.HOSTILE, 1f, 0.5f);
						}
					}

					if (mChargeUp.nextTick()) {
						for (Player player : players) {
							player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1, 1f);
						}
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mChargeUp.reset();
				}
			};
			masterRunnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(masterRunnable);
		}, 20);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!mSummoned.isEmpty()) {
			event.setFlatDamage(event.getFlatDamage() * 0.4);

			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_GRAVEL_HIT, SoundCategory.HOSTILE, 1, 0.5f);
			new PartialParticle(Particle.BLOCK_CRACK, mBoss.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.25, PARTICLE_DATA).spawnAsEntityActive(mBoss);
		}
	}

	@Override
	public boolean canRun() {
		return mSummoned.isEmpty() && !mOnCooldown;
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
