package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellPlatformWave extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mNumWaves;
	private final int mCastTicks;
	private final Vesperidys mVesperidys;
	private static final double DAMAGE = 80;
	private static final int FIRST_CAST_TICKS = 40;

	private boolean mOnCooldown = false;

	public SpellPlatformWave(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, int numWaves, int castTicks) {
		mPlugin = plugin;
		mBoss = boss;
		mNumWaves = numWaves;
		mCastTicks = castTicks;
		mVesperidys = vesperidys;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 20);

		ArrayList<Directions> possibleDirections = new ArrayList<>(List.of(Directions.values()));
		Collections.shuffle(possibleDirections);

		List<List<Vesperidys.Platform>> platformOrder = new ArrayList<>();

		for (int i = 0; i < mNumWaves; i++) {
			Directions selectedDirection = possibleDirections.get(i);
			List<List<Vesperidys.Platform>> newPlatformOrder = selectedDirection.getPlatformOrder(mVesperidys);

			for (int j = 0; j < newPlatformOrder.size(); j++) {
				if (j < platformOrder.size()) {
					for (Vesperidys.Platform platform : newPlatformOrder.get(j)) {
						if (!platformOrder.get(j).contains(platform)) {
							platformOrder.get(j).add(platform);
						}
					}
				} else {
					platformOrder.add(newPlatformOrder.get(j));
				}
			}
		}

		BukkitRunnable runnableA = new BukkitRunnable() {
			int mTicks = 0;
			int mWave = 0;

			@Override
			public void run() {
				if (mWave >= platformOrder.size()) {
					this.cancel();
					return;
				}

				List<Vesperidys.Platform> platforms = platformOrder.get(mWave);

				if (mTicks >= getCastTicks(mWave)) {
					mTicks = -15;
					mWave += 1;
					List<Player> hitPlayers = new ArrayList<>();
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 5f, 1f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 5f, 2f);
					for (Vesperidys.Platform platform : platforms) {
						if (mVesperidys.mPhase >= 4 && (Math.abs(platform.mX) > 1 || Math.abs(platform.mY) > 1)) {
							continue;
						}

						for (Player player : platform.getPlayersOnPlatform()) {
							if (!hitPlayers.contains(player)) {
								hit(player);
								hitPlayers.add(player);
							}
						}

						for (int x = -3; x <= 3; x++) {
							for (int z = -3; z <= 3; z++) {
								Location loc = platform.getCenter().add(x, 1.2, z);
								new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
								if (FastUtils.randomIntInRange(0, 5) == 0) {
									if (FastUtils.randomIntInRange(0, 2) == 0) {
										new PartialParticle(Particle.BLOCK_CRACK, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.SHROOMLIGHT.createBlockData()).spawnAsEntityActive(mBoss);
									} else {
										new PartialParticle(Particle.BLOCK_CRACK, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.CRIMSON_HYPHAE.createBlockData()).spawnAsEntityActive(mBoss);
									}
								}

								if (FastUtils.randomIntInRange(0, 10) == 0) {
									new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
								}
							}
						}
					}
				} else if (mTicks >= 0 && mTicks % (getCastTicks(mWave) / 5) == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 5f, 0f);
					for (Vesperidys.Platform platform : platforms) {
						if (mVesperidys.mPhase >= 4 && (Math.abs(platform.mX) > 1 || Math.abs(platform.mY) > 1)) {
							continue;
						}

						for (int x = -3; x <= 3; x++) {
							for (int z = -3; z <= 3; z++) {
								Location loc = platform.getCenter().add(x, 1.2, z);
								if (FastUtils.randomIntInRange(0, 1) == 0) {
									new PartialParticle(Particle.SPELL_WITCH, loc, 1, 0.1, 0.1, 0.1, 0)
										.spawnAsEntityActive(mBoss);
								}
							}
						}
					}
				}

				mTicks += 1;
			}
		};

		runnableA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableA);
	}

	private int getCastTicks(int wave) {
		if (wave == 0) {
			return FIRST_CAST_TICKS;
		} else {
			return mCastTicks;
		}
	}

	@Override
	public int cooldownTicks() {
		return mVesperidys.mSpellCooldowns;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	private void hit(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, "Void Wave");
		MovementUtils.knockAway(mBoss.getLocation(), player, 0, .75f, false);

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
			mPlugin.mEffectManager.addEffect(player, "VesperidysMagicVuln", new PercentDamageReceived(15 * 20, 0.3, EnumSet.of(DamageEvent.DamageType.MAGIC)));
		}
	}

	private enum Directions {
		NORTH(true, true),
		SOUTH(true, false),
		EAST(false, true),
		WEST(false, false);

		final boolean mIsRow;
		final boolean mIsAscending;

		Directions(boolean isRow, boolean isAscending) {
			mIsRow = isRow;
			mIsAscending = isAscending;
		}

		private List<List<Vesperidys.Platform>> getPlatformOrder(Vesperidys vesperidys) {
			// First, scan through all of platform list to check for minX, maxX, minY, maxY.
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;

			Vesperidys.PlatformList platformList = vesperidys.mPlatformList;
			for (Integer x : platformList.mPlatformHashMap.keySet()) {
				HashMap<Integer, Vesperidys.Platform> innerMap = platformList.mPlatformHashMap.get(x);
				if (innerMap != null) {
					for (Integer y : innerMap.keySet()) {
						if (innerMap.get(y) != null && !innerMap.get(y).mBroken) {
							if (x < minX) {
								minX = Math.min(-1, x);
							}
							if (x > maxX) {
								maxX = Math.max(1, x);
							}
							if (y < minY) {
								minY = Math.min(-1, y);
							}
							if (y > maxY) {
								maxY = Math.max(1, y);
							}
						}
					}
				}
			}

			List<List<Vesperidys.Platform>> output = new ArrayList<>();
			if (mIsRow) {
				for (int x = minX; x <= maxX; x++) {
					output.add(platformList.getPlatformRow(x));
				}
			} else {
				for (int y = minY; y <= maxY; y++) {
					output.add(platformList.getPlatformColumn(y));
				}
			}

			if (!mIsAscending) {
				Collections.reverse(output);
			}

			return output;
		}
	}
}
