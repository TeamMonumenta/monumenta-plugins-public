package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LavaCannonBoss extends BossAbilityGroup {
	private static final Particle.DustOptions ORANGE = new Particle.DustOptions(Color.fromRGB(255, 128, 0), 0.5f);
	public static final int BULLET_DURATION = 2 * 20;
	private static final Material WARNING_BLOCK = Material.NETHER_WART_BLOCK;

	public static final String identityTag = "boss_lavacannon";
	private static final String SPELL_NAME = "Lava Cannon";

	private final Samwell mSamwell;
	private final int mPhase;

	private boolean mShot;
	private final List<Player> mHitMap = new ArrayList<>();

	private final PartialParticle mPWarning;
	private final PartialParticle mPWarningLava;
	private final PartialParticle mPWarning2;
	private final PartialParticle mPWarning3;
	private final PartialParticle mPWarning4;
	private boolean mLockedTrajectory = false;
	private boolean mForecastBlocks = false;
	private final Map<Location, Material> mOldBlocks = new HashMap<>();

	public static @Nullable LavaCannonBoss deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable LavaCannonBoss construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Samwell.
		Samwell samwell = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Samwell.identityTag)) {
				samwell = BossUtils.getBossOfClass(mob, Samwell.class);
				break;
			}
		}
		if (samwell == null) {
			MMLog.warning("LavaCannonBoss: Samwell wasn't found! (This is a bug)");
			return null;
		}
		return new LavaCannonBoss(plugin, boss, samwell);
	}

	private LavaCannonBoss(Plugin plugin, LivingEntity boss, Samwell samwell) {
		super(plugin, identityTag, boss);
		Team darkRedTeam = ScoreboardUtils.getExistingTeamOrCreate("DarkRed", NamedTextColor.DARK_RED);
		darkRedTeam.addEntry(boss.getUniqueId().toString());
		boss.setGlowing(true);

		mSamwell = samwell;

		mPhase = mSamwell.mPhase;
		Player target = EntityUtils.getNearestPlayer(mBoss.getLocation(), 100);
		mPWarning = new PartialParticle(Particle.FLAME, mBoss.getLocation(), 2, 0, 0, 0, 0.01);
		mPWarningLava = new PartialParticle(Particle.LAVA, mBoss.getLocation(), 1, 0, 0, 0, 0.01);
		mPWarning2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 2, 0, 0, 0, 0.01);
		mPWarning3 = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 10, 0.25, 0.25, 0.25, 0, ORANGE);
		mPWarning4 = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 5, 0.25, 0.25, 0.25, 0);

		List<Spell> passives = Arrays.asList(
			new SpellBlockBreak(mBoss) // This is going to be interesting
		);

		super.constructBoss(SpellManager.EMPTY, passives, 100, null);

		if (target == null) {
			return;
		}

		new BukkitRunnable() {
			int mT = 0;
			Vector mVector = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation());

			@Override
			public void run() {
				if (target.getWorld() != mBoss.getWorld()) {
					this.cancel();
					return;
				}
				if (mT <= chargeTime(mPhase) * 0.6) {
					mVector = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation());
					Location loc = mBoss.getLocation();
					loc.setYaw((float) (Math.atan2(mVector.getZ(), mVector.getX()) * 180.0 / Math.PI));
					mBoss.teleport(loc);
				} else {
					mLockedTrajectory = true;
				}

				if (mT % (chargeTime(mPhase) / 6) == 0) {
					warningParticles(mBoss.getLocation(), mVector, mLockedTrajectory);
				}

				if (mT >= chargeTime(mPhase)) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 5, 0);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5, 1);
					mShot = true;
					launch(mVector);
					this.cancel();
					return;
				}

				if (mPhase != mSamwell.mPhase || mSamwell.mDefeated) {
					mBoss.remove();
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void launch(Vector dir) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 100, true);
		double hitbox = mBoss.getHeight() / 2;
		double velocity = 1.5;

		new BukkitRunnable() {
			int mT = 0;
			final BoundingBox mBox = BoundingBox.of(LocationUtils.getEntityCenter(mBoss), hitbox, hitbox + 0.5, hitbox);

			@Override
			public void run() {
				mBox.shift(dir.clone().setY(0).normalize().multiply(velocity));
				Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
				for (Player player : players) {
					if (player.getBoundingBox().overlaps(mBox)) {
						hit(loc, player);
					}
				}

				mT++;
				Location teleportLoc = loc.clone();
				teleportLoc.setY(mBoss.getLocation().getY());
				mBoss.teleport(teleportLoc);

				if (mT >= BULLET_DURATION || mPhase != mSamwell.mPhase || mSamwell.mDefeated) {
					mBoss.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void warningParticles(Location origin, Vector dir, boolean lockedTrajectory) {
		Location particleLoc = origin.clone();
		new BukkitRunnable() {
			int mT = 0;
			boolean mPlaceBlocks = false;

			@Override
			public void run() {
				if (!mForecastBlocks && lockedTrajectory) {
					mPlaceBlocks = true;
					mForecastBlocks = true;
				}

				mT += 1;
				particleLoc.add(dir.clone().setY(0).normalize());
				if (lockedTrajectory) {
					mPWarning.location(particleLoc).spawnAsBoss();
					mPWarningLava.location(particleLoc).spawnAsBoss();
				} else {
					mPWarning2.location(particleLoc).spawnAsBoss();
				}
				mPWarning3.location(particleLoc).spawnAsBoss();
				mPWarning4.location(particleLoc).spawnAsBoss();

				if (mPlaceBlocks) {
					// Place 3 x 3 Square
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							// Climb down 5 blocks.
							for (int y = 0; y >= -5; y--) {
								Location bLoc = particleLoc.clone().add(x, y, z);

								if (bLoc.getBlock().isSolid()
									    && !BlockUtils.isMechanicalBlock(bLoc.getBlock().getType())
									    && !BlockUtils.isValuableBlock(bLoc.getBlock().getType())
									    && bLoc.getBlock().getType() != WARNING_BLOCK) {
									mOldBlocks.put(bLoc, bLoc.getBlock().getType());
									bLoc.getBlock().setType(WARNING_BLOCK);
									break;
								}
							}
						}
					}
				}

				mT += 1;
				particleLoc.add(dir.clone().setY(0).normalize());
				if (lockedTrajectory) {
					mPWarning.location(particleLoc).spawnAsBoss();
					mPWarningLava.location(particleLoc).spawnAsBoss();
				} else {
					mPWarning2.location(particleLoc).spawnAsBoss();
				}
				mPWarning3.location(particleLoc).spawnAsBoss();
				mPWarning4.location(particleLoc).spawnAsBoss();

				if (mPlaceBlocks) {
					mBoss.getWorld().playSound(particleLoc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 10f, 1.5f);
					// Place 3 x 3 Square
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							// Climb down 5 blocks.
							for (int y = 0; y >= -5; y--) {
								Location bLoc = particleLoc.clone().add(x, y, z);

								if (bLoc.getBlock().isSolid()
									    && !BlockUtils.isMechanicalBlock(bLoc.getBlock().getType())
									    && !BlockUtils.isValuableBlock(bLoc.getBlock().getType())
									    && bLoc.getBlock().getType() != WARNING_BLOCK) {
									mOldBlocks.put(bLoc, bLoc.getBlock().getType());
									bLoc.getBlock().setType(WARNING_BLOCK);
									break;
								}
							}
						}
					}
				}

				if (mT >= 300 || mShot || mPhase != mSamwell.mPhase) {
					this.cancel();
					if (mPlaceBlocks) {
						new BukkitRunnable() {
							int mTicks = 0;

							@Override
							public void run() {
								mTicks += 2;
								if (mBoss.isDead() || !mBoss.isValid() || mPhase != mSamwell.mPhase || mTicks > (BULLET_DURATION + chargeTime(mPhase) + 20)) {
									for (Map.Entry<Location, Material> e : mOldBlocks.entrySet()) {
										if (e.getKey().getBlock().getType() != Material.AIR) {
											e.getKey().getBlock().setType(e.getValue());
										}
										this.cancel();
									}
								} else {
									for (Map.Entry<Location, Material> e : mOldBlocks.entrySet()) {
										if (e.getKey().getBlock().getType() != WARNING_BLOCK && e.getKey().getBlock().getType() != Material.CRYING_OBSIDIAN) {
											e.getKey().getBlock().setType(WARNING_BLOCK);
										}
									}
								}
							}
						}.runTaskTimer(mPlugin, 0, 2);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void hit(Location loc, Player player) {
		if (!mHitMap.contains(player)) {
			MovementUtils.knockAway(loc, player, 3f, 0.5f);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 5, 0.5f);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 5, 0.5f);
			mHitMap.add(player);
			BossUtils.bossDamagePercent(mBoss, player, 0.55, SPELL_NAME);
		}
	}

	public static int chargeTime(int phase) {
		if (phase <= 3) {
			return 5 * 20;
		} else {
			return 70;
		}
	}
}
