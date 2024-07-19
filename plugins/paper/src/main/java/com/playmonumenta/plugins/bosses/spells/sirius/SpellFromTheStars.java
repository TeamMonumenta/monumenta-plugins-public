package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellFromTheStars extends Spell {

	private final Sirius mSirius;
	private boolean mOnCooldown;
	private final Plugin mPlugin;
	private boolean mPrimed;
	private final PassiveTentacleManager mTentancleManager;
	private final PassiveDeclaration mDeclerations;
	private int mDamage = 50;
	private int mShockwaves = 1;

	private static final int COOLDOWN = 10 * 20;
	private static final int RADIUS = 55;
	private static final int JUMPHEIGHT = 15;


	public SpellFromTheStars(Sirius sirius, Plugin plugin, PassiveTentacleManager tentacles, PassiveDeclaration declaration) {
		mSirius = sirius;
		mOnCooldown = false;
		mPlugin = plugin;
		mTentancleManager = tentacles;
		mPrimed = false;
		mDeclerations = declaration;
	}


	@Override
	public void run() {
		mPrimed = true;
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!mSirius.mDamagePhase && !mSirius.mAnimationLock && !mSirius.mCheeseLock) {
					slam();
					mPrimed = false;
					this.cancel();
				}
				if (mTicks >= 20 * 20) {
					mPrimed = false;
					this.cancel();
				}
				mTicks += 5;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}

	private void slam() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, COOLDOWN + 20);
		//DELAY till sirius slams down. Probably goes up for 2 seconds and slams down in 0.5 second.
		List<Vector3f> mTranslations = new ArrayList<>();
		for (Display display : mSirius.mDisplays) {
			mTranslations.add(display.getTransformation().getTranslation());
		}
		mSirius.stopCollision();
		mTentancleManager.mCancelMovements = true;
		BukkitRunnable castRunnable = new BukkitRunnable() {
			int mTicks = 0;
			int mRadius = RADIUS;
			final Location mCircleLoc = LocationUtils.fallToGround(mSirius.mBoss.getLocation(), mSirius.mBoss.getLocation().subtract(0, 10, 0).getY());
			final ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, 50, Component.text("From The Stars", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 50);

			@Override
			public void run() {
				mManager.nextTick();
				if (mTicks == 0) {
					World world = mSirius.mBoss.getWorld();
					Location loc = mSirius.mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.4f, 1.2f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.4f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 2f);
					world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.4f, 1.4f);
					world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 1.4f);
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2f, 0.1f);
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.6f, 0.6f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.8f);

					if (mSirius.mBlocks <= 10) {
						mRadius += 10;
					}
					mSirius.mBoss.setVisibleByDefault(false); //hide it so it doesnt have to move with interpolation
					for (int i = 0; i < mSirius.mDisplays.size(); i++) {
						mSirius.mDisplays.get(i).setInterpolationDuration(39);
						Transformation trans = mSirius.mDisplays.get(i).getTransformation();
						mSirius.mDisplays.get(i).setTransformation(new Transformation(new Vector3f(0, JUMPHEIGHT, 0).add(mTranslations.get(i)), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						mSirius.mDisplays.get(i).setInterpolationDelay(-1);
					}
				}
				if (mTicks < 50 && mTicks % 5 == 0) {
					new PPCircle(Particle.SCRAPE, mCircleLoc, mRadius).ringMode(true).count(50).spawnAsBoss();
					new PPPillar(Particle.END_ROD, mSirius.mBoss.getLocation(), JUMPHEIGHT - mTicks / 39.0).delta(5, 0, 5).spawnAsBoss();
				}
				if (mTicks == 45) {
					for (int i = 0; i < mSirius.mDisplays.size(); i++) {
						mSirius.mDisplays.get(i).setInterpolationDuration(6);
						Transformation trans = mSirius.mDisplays.get(i).getTransformation();
						mSirius.mDisplays.get(i).setTransformation(new Transformation(mTranslations.get(i), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						mSirius.mDisplays.get(i).setInterpolationDelay(-1);
					}
				}
				if (mTicks == 50) {
					World world = mSirius.mBoss.getWorld();
					Location loc = mSirius.mBoss.getLocation().add(0, 10, 0);
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.0f, 0.1f);
					world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
					mManager.remove();

					if (mSirius.mBlocks <= 5) {
						mDamage = 80;
						mShockwaves = 3;
					} else if (mSirius.mBlocks <= 10) {
						mDamage = 65;
						mShockwaves = 2;
					} else {
						mDamage = 50;
						mShockwaves = 1;
					}
					for (int i = 0; i < mShockwaves; i++) {
						BukkitRunnable shockwaveRunnable = new BukkitRunnable() {

							double mR = 0;
							final Location mLoc = mSirius.mBoss.getLocation().subtract(0, 2, 0);
							final double mMinY = mLoc.getY() - 1;
							final double mBlockDensity = 0.4;
							final Display.Brightness mBrightness = new Display.Brightness(8, 8);
							final List<Material> mPossibleMaterials = List.of(Material.WARPED_WART_BLOCK, Material.STRIPPED_WARPED_HYPHAE);
							int mT = 0;

							@Override
							public void run() {

								if (mT % 2 == 0) {
									final List<Location> blockLocations = new ArrayList<>();

									for (int blockCounter = 0; blockCounter < mR * 2 * Math.PI * mBlockDensity; blockCounter++) {
										double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
										Location finalBlockLocation = LocationUtils.fallToGround(mLoc.clone().add(FastUtils.cos(theta) * mR, 0, FastUtils.sin(theta) * mR).toCenterLocation().add(0, -1.4, 0), mMinY);
										if (!blockLocations.contains(finalBlockLocation)) {
											blockLocations.add(finalBlockLocation);
										}
									}

									blockLocations.removeIf(location -> location.clone().subtract(0, 1, 0).getBlock().getType() == Material.AIR || location.getX() > mSirius.mBoss.getLocation().getX());

									blockLocations.forEach(l -> {
										BlockDisplay blockDisplay = mLoc.getWorld().spawn(l.clone().add(-0.5, -0.3, -0.5), BlockDisplay.class);
										blockDisplay.setBlock(mPossibleMaterials.get(FastUtils.randomIntInRange(0, mPossibleMaterials.size() - 1)).createBlockData());
										blockDisplay.setBrightness(mBrightness);
										blockDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
										blockDisplay.setInterpolationDuration(2);
										blockDisplay.addScoreboardTag("SiriusDisplay");
										EntityUtils.setRemoveEntityOnUnload(blockDisplay);

										BukkitRunnable runnable = new BukkitRunnable() {
											int mTicks = 0;
											final double mMaxHeight = 0.3;

											@Override
											public void run() {
												double currentHeight = mMaxHeight * (-0.3 * ((mTicks - 3) * (mTicks - 3)) + 1);
												blockDisplay.setTransformation(new Transformation(new Vector3f(0, (float) currentHeight, 0), blockDisplay.getTransformation().getLeftRotation(), blockDisplay.getTransformation().getScale(), blockDisplay.getTransformation().getRightRotation()));
												blockDisplay.setInterpolationDelay(-1);

												mTicks++;
												if (mTicks > 12 || mSirius.mDone) {
													this.cancel();
												}
											}

											@Override
											public synchronized void cancel() throws IllegalStateException {
												super.cancel();
												blockDisplay.remove();
											}
										};
										runnable.runTaskTimer(mPlugin, 0, 1);
										mActiveRunnables.add(runnable);
									});

									BukkitRunnable groundQuakeDelayForDamageRunnable = new BukkitRunnable() {
										final double mHitRadius = mR;
										int mTT = 0;
										final double MAX_HEIGHT_DELAY = 0.3;

										@Override
										public void run() {
											mTT++;
											if (mTT > 6 || (MAX_HEIGHT_DELAY * (-0.3 * ((mTT - 3) * (mTT - 3)) + 1) >= -0.5) || mSirius.mDone) {
												this.cancel();

												for (Player p : PlayerUtils.playersInRange(mSirius.getPlayers(), mLoc, RADIUS, false, true)) {
													Location yAdjusted = LocationUtils.fallToGround(p.getLocation(), mMinY);
													Location yAdjustedOrigin = mLoc.clone().set(mLoc.getX(), yAdjusted.getY(), mLoc.getZ());
													double distance = yAdjusted.distance(yAdjustedOrigin);
													if (distance <= mHitRadius + 0.5 && distance >= mHitRadius - 0.5
														    && Math.abs(p.getLocation().getY() - yAdjusted.getY()) <= 0.6
														    && p.getLocation().getX() < mSirius.mBoss.getLocation().getX()) {
														DamageUtils.damage(mSirius.mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, false, true, "From the Stars");
													}
												}
											}
										}
									};
									groundQuakeDelayForDamageRunnable.runTaskTimer(mPlugin, 0, 1);
									mActiveRunnables.add(groundQuakeDelayForDamageRunnable);
								}

								if (mR % 2 == 0) {
									for (Player p : mSirius.getPlayers()) {
										p.playSound(p, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.6f, 2f);
									}
								}

								mT++;
								mR += 0.25;
								if (mR > mRadius || mSirius.mDone) {
									this.cancel();
								}
							}
						};
						shockwaveRunnable.runTaskTimer(mPlugin, i * 40L, 1);
						mActiveRunnables.add(shockwaveRunnable);
					}

					mSirius.mBoss.setVisibleByDefault(true); //unhide it
					mSirius.startCollision();
					mTentancleManager.mCancelMovements = false;
					this.cancel();
					return;
				}
				mTicks++;

			}
		};
		castRunnable.runTaskTimer(mPlugin, 5, 1);
		mActiveRunnables.add(castRunnable);

	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mPrimed && !mDeclerations.mSwapping;
	}
}
