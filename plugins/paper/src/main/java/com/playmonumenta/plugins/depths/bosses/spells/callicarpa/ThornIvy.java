package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ThornIvy extends Spell {

	public static final String SPELL_NAME = "Thorn Ivy";
	public static final int CAST_TIME = 40;
	// Note: this also increases the Ivy radius
	public static final int CAST_TIME_A15_DECREASE = 8;
	public static final int DURATION = 80;
	public static final int VINE_POINTS = 8;
	public static final int VINES = 4;
	public static final int VINE_STACKS = 3;
	public static final double POINTS_DISTANCE = 0.5;
	public static final double STACK_DISTANCE = 1;
	public static final double STACK_THETA_OFFSET = Math.PI / 4;
	public static final double ROTATION_SPEED_INIT = Math.PI / 180;
	public static final double ROTATION_SPEED_MAX = Math.PI / 60;
	public static final int MAX_ROTATION_SPEED_TICKS = 40;
	public static final double DAMAGE = 50;
	public static final Color[] VINE_COLORS = {Color.fromRGB(55, 173, 26), Color.fromRGB(43, 120, 24), Color.fromRGB(112, 72, 17)};

	private final Particle.DustOptions[] mVineOptions = {
		new Particle.DustOptions(VINE_COLORS[0], 1f),
		new Particle.DustOptions(VINE_COLORS[0], 1f),
		new Particle.DustOptions(VINE_COLORS[1], 1f),
		new Particle.DustOptions(VINE_COLORS[1], 1f),
		new Particle.DustOptions(VINE_COLORS[1], 1f),
		new Particle.DustOptions(VINE_COLORS[2], 1f)
	};
	private final LivingEntity mBoss;
	private final int mFloorY;
	private final int mFinalCooldown;
	private final @Nullable DepthsParty mParty;
	private final Callicarpa mCallicarpa;
	private final int mFinalCastTime;

	public ThornIvy(LivingEntity boss, int floorY, @Nullable DepthsParty party, Callicarpa callicarpa) {
		mBoss = boss;
		mFloorY = floorY;
		mFinalCastTime = getCastTime(party);
		mFinalCooldown = DepthsParty.getAscensionEightCooldown(DURATION * 2 + mFinalCastTime, party);
		mParty = party;
		mCallicarpa = callicarpa;
	}

	@Override
	public void run() {
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, DURATION, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD)), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, 200);

		BukkitRunnable vinesRunnable = new BukkitRunnable() {
			final double mPointOffset = 2 * Math.PI / VINES;
			final int mAnimationTicksIncrement = mFinalCastTime / VINE_POINTS;
			final int mChargingTickSpeed = DURATION / mFinalCastTime;
			// This is used to increase the speed as time goes on. Add a certain portion that allows the attack to
			// reach max speed within DURATION - MAX_ROTATION_SPEED_TICKS ticks, remaining at max speed for MAX_ROTATION_SPEED_TICKS.
			final double mRotationSpeedChange = (ROTATION_SPEED_MAX - ROTATION_SPEED_INIT) / (double) (DURATION - MAX_ROTATION_SPEED_TICKS);
			final int mIframeTicks = 10;
			final Map<UUID, Integer> mIframeMap = new HashMap<>();

			int mTicks = 0;
			double mTheta = 0;
			double mTotalThetaOffset = 0;
			boolean mDischarging = false;
			int mCurrentPoints = 1;
			double mCurrentRotationSpeed = ROTATION_SPEED_INIT;

			@Override
			public void run() {
				Location center = mBoss.getLocation().clone().add(0, 0.1, 0);
				mTotalThetaOffset = 0;
				boolean hasBrokenBlock = false;
				for (int k = 0; k < VINE_STACKS; k++) {
					for (int i = 0; i < VINES; i++) {
						double angle = mTheta + mTotalThetaOffset + i * mPointOffset;
						for (int j = 0; j < mCurrentPoints; j++) {
							double xOffset = FastUtils.cos(angle) * (j + 1) * POINTS_DISTANCE;
							double zOffset = FastUtils.sin(angle) * (j + 1) * POINTS_DISTANCE;
							Location pointLoc = center.clone().add(xOffset, 0, zOffset);
							new PartialParticle(Particle.REDSTONE, pointLoc, 1).extra(0).data(getRandomVineOptions()).distanceFalloff(20).spawnAsBoss();

							// Try breaking the block at the point, and store the result if positive.
							// This will be used to play a block breaking sound, but only once per spin,
							// even if multiple blocks have been broken at once (to not make players go deaf).
							if (tryDeleteBlock(pointLoc.getBlock())) {
								hasBrokenBlock = true;
							}

							// Only attempt damaging players when the attack is actually in action.
							// Do not do so during the cast phase, where the vines start growing out of her.
							if (mDischarging) {
								Hitbox hitbox = new Hitbox.SphereHitbox(pointLoc, 0.3);
								hitbox.getHitPlayers(true).forEach(
									hitPlayer -> {
										int ticks = Bukkit.getCurrentTick();
										if (mIframeMap.containsKey(hitPlayer.getUniqueId())) {
											if (mIframeMap.get(hitPlayer.getUniqueId()) <= ticks - mIframeTicks) {
												DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, SPELL_NAME);
												mIframeMap.put(hitPlayer.getUniqueId(), ticks);
											}
										} else {
											DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, SPELL_NAME);
											mIframeMap.put(hitPlayer.getUniqueId(), ticks);
										}
									}
								);
							}
						}
					}
					center.add(0, STACK_DISTANCE, 0);
					mTotalThetaOffset += STACK_THETA_OFFSET;
				}

				if (mDischarging) {
					playSpinSounds();
				} else {
					playGrowSounds();
				}

				if (hasBrokenBlock) {
					playBreakSounds();
				}

				// Animation for the vines slowly growing out of Hedera.
				if (!mDischarging && mTicks != 0 && mTicks % mAnimationTicksIncrement == 0) {
					mCurrentPoints++;
				}

				if (mDischarging && chargeUp.previousTick()) {
					mCallicarpa.isReflectingProjectiles(false);
					this.cancel();
				}

				if (!mDischarging && chargeUp.nextTick(mChargingTickSpeed)) {
					chargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD)));
					mDischarging = true;
					if (shouldReflectProjectiles()) {
						mCallicarpa.isReflectingProjectiles(true);
					}
				}

				mTicks++;
				// Only rotate the vines when the attack is in action.
				if (mDischarging) {
					mTheta += mCurrentRotationSpeed;
					// If below the speed cap, increase the speed as time goes by.
					if (mCurrentRotationSpeed < ROTATION_SPEED_MAX) {
						mCurrentRotationSpeed += mRotationSpeedChange;
					}
				}
			}

			private void playGrowSounds() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1f, 0f);
			}

			private void playSpinSounds() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1f, 0f);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.HOSTILE, 1f, 0f);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1f, 1.2f);
			}

			private void playBreakSounds() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2f, 1f);
			}

			private boolean tryDeleteBlock(Block block) {
				if (block.getY() > mFloorY && block.isCollidable() && BlockUtils.canBeBroken(block)) {
					block.setType(Material.AIR);
					return true;
				}
				return false;
			}
		};

		vinesRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	private Particle.DustOptions getRandomVineOptions() {
		return mVineOptions[FastUtils.randomIntInRange(0, mVineOptions.length - 1)];
	}

	private boolean shouldReflectProjectiles() {
		return mParty != null && mParty.getAscension() >= 15;
	}

	private int getCastTime(@Nullable DepthsParty party) {
		int time = CAST_TIME;
		if (party != null && party.getAscension() >= 15) {
			time -= CAST_TIME_A15_DECREASE;
		}
		return time;
	}
}
