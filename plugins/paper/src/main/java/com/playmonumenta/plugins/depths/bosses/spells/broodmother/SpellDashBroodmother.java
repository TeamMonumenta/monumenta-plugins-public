package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAbstractRectangleAttack;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellDashBroodmother extends Spell {

	public static final String SPELL_NAME = "Dash";
	public static final int CAST_TIME = 100;
	public static final int CAST_TIME_A15_DECREASE = 20;
	public static final int SWEEP_DELAY = 2;
	public static final int COOLDOWN = 240;
	public static final int INTERNAL_COOLDOWN = 600;
	public static final double DAMAGE = 200;
	public static final int ANIMATION_LINGER_TIME = 20;
	public static final int SOUND_INCREASES = 10;
	public static final float SOUND_PITCH_INCREASE = 0.1f;
	public static final int TELEGRAPH_UNITS = 100;
	public static final int TELEGRAPH_PULSES = 4;
	public static final float PARTICLE_SPEED = 0.14f;
	public static final int DASH_FORWARD_INTERPOLATION_TICKS = 10;
	public static final int DASH_RETURN_INTERPOLATION_TICKS = 4;
	public static final double SWEEP_Z_OFFSET = 14.5;
	public static final double SWEEP_DZ_VALUE = -29;
	public static final double SWEEP_SIZE_A15_INCREASE = 3;

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweep1;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweep2;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweep3;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweep4;
	private final SpellBaseAbstractRectangleAttack mSweepAttack1;
	private final SpellBaseAbstractRectangleAttack mSweepAttack2;
	private final SpellBaseAbstractRectangleAttack mSweepAttack3;
	private final SpellBaseAbstractRectangleAttack mSweepAttack4;
	private final ChargeUpManager mChargeUp;
	private final int mFinalCooldown;
	private final int mFinalCastTime;
	private final int mSoundIncreaseModulo;

	private boolean mOnCooldown = false;

	public SpellDashBroodmother(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mPlugin = Plugin.getInstance();

		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mFinalCastTime = getCastTime(party);
		mSoundIncreaseModulo = mFinalCastTime / SOUND_INCREASES;
		double zOffset = getSweepZOffset(party);
		double dzValue = getSweepDzValue(party);

		// Parts of the dash
		mSweep1 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(9, -1, zOffset), -20.5, dzValue);
		mSweep2 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-11.5, -1, zOffset), -19, dzValue);
		mSweep3 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-30.5, -1, zOffset), -19, dzValue);
		mSweep4 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-49.5, -1, zOffset), -19, dzValue);

		mSweepAttack1 = new SpellBaseAbstractRectangleAttack(
			mSweep1, TELEGRAPH_UNITS, TELEGRAPH_PULSES, mFinalCastTime, PARTICLE_SPEED, Particle.SQUID_INK, DamageEvent.DamageType.MELEE_SKILL, DAMAGE,
			true, true, SPELL_NAME, Particle.SQUID_INK,
			mPlugin, mBoss, (b) -> {

		}
		);

		mSweepAttack2 = new SpellBaseAbstractRectangleAttack(
			mSweep2, TELEGRAPH_UNITS, TELEGRAPH_PULSES, mFinalCastTime, PARTICLE_SPEED, Particle.SQUID_INK, DamageEvent.DamageType.MELEE_SKILL, DAMAGE,
			true, true, SPELL_NAME, Particle.SQUID_INK,
			mPlugin, mBoss, (b) -> {

		}
		);

		mSweepAttack3 = new SpellBaseAbstractRectangleAttack(
			mSweep3, TELEGRAPH_UNITS, TELEGRAPH_PULSES, mFinalCastTime, PARTICLE_SPEED, Particle.SQUID_INK, DamageEvent.DamageType.MELEE_SKILL, DAMAGE,
			true, true, SPELL_NAME, Particle.SQUID_INK,
			mPlugin, mBoss, (b) -> {

		}
		);

		mSweepAttack4 = new SpellBaseAbstractRectangleAttack(
			mSweep4, TELEGRAPH_UNITS, TELEGRAPH_PULSES, mFinalCastTime, PARTICLE_SPEED, Particle.SQUID_INK, DamageEvent.DamageType.MELEE_SKILL, DAMAGE,
			true, true, SPELL_NAME, Particle.SQUID_INK,
			mPlugin, mBoss, (b) -> {

		}
		);

		mChargeUp = new ChargeUpManager(mBoss, mFinalCastTime, Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
			BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, 100);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		// Start the delayed "attacks"
		doSweepSequence();
		BukkitRunnable runnable = new BukkitRunnable() {
			float mPitchIncrease = 0;

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
				}
				if (mChargeUp.getTime() % mSoundIncreaseModulo == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 1 + mPitchIncrease);
					mPitchIncrease += SOUND_PITCH_INCREASE;
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);

		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, INTERNAL_COOLDOWN);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	@Override
	public int castTicks() {
		return mFinalCastTime;
	}

	private void doSweepSequence() {
		ArrayList<BlockDisplay> blockDisplays = new ArrayList<>();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (!mBoss.isValid()) {
				return;
			}

			// Turn the boss into block displays
			blockDisplays.addAll(DisplayEntityUtils.turnBlockCuboidIntoBlockDisplays(mBoss.getLocation().clone().add(-8, -1, -12), mBoss.getLocation().clone().add(19, 9, 12), true));
			// Remove a percentage of the block displays for performance reasons
			ArrayList<BlockDisplay> toRemove = new ArrayList<>();
			blockDisplays.forEach(blockDisplay -> {
				if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
					toRemove.add(blockDisplay);
					blockDisplay.remove();
				}
			});
			blockDisplays.removeIf(toRemove::contains);
			// Move the block displays forward
			DisplayEntityUtils.translate(blockDisplays, -60, 0, 0, DASH_FORWARD_INTERPOLATION_TICKS);
			// Reset the spider at the original position, after some more time
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				// Animate the boss quickly going back
				DisplayEntityUtils.translate(blockDisplays, 60, 0, 0, DASH_RETURN_INTERPOLATION_TICKS);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> blockDisplays.forEach(Entity::remove), DASH_RETURN_INTERPOLATION_TICKS);
				StructuresAPI.loadAndPasteStructure("BikeSpiderBase", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			}, ANIMATION_LINGER_TIME + SWEEP_DELAY * 3);
		}, mFinalCastTime);

		// First sweep
		mSweepAttack1.run();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			sweepAesthetics(mSweep1);
		}, mFinalCastTime);

		// Second sweep: after SWEEP_DELAY from the first.
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mSweepAttack2.run();
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				sweepAesthetics(mSweep2);
			}, mFinalCastTime);
		}, SWEEP_DELAY);

		// Third sweep: after SWEEP_DELAY * 2 from the first.
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mSweepAttack3.run();
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				sweepAesthetics(mSweep3);
			}, mFinalCastTime);
		}, SWEEP_DELAY * 2);

		// Fourth sweep: after SWEEP_DELAY * 3 from the first.
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mSweepAttack4.run();
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				sweepAesthetics(mSweep4);
			}, mFinalCastTime);
		}, SWEEP_DELAY * 3);
	}

	private void sweepAesthetics(SpellBaseAbstractRectangleAttack.RectangleInfo sweepInfo) {
		mBoss.getWorld().playSound(sweepInfo.getCenter(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 5f, 2f);
		mBoss.getWorld().playSound(sweepInfo.getCenter(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 5f, 2f);
		for (int i = 0; i < 100; i++) {
			double deltaX = (Math.random() * sweepInfo.getDx()) - sweepInfo.getHalfDx();
			double deltaY = 5 + (Math.random() * 4) - 2;
			double deltaZ = (Math.random() * sweepInfo.getDz()) - sweepInfo.getHalfDz();
			new PartialParticle(Particle.FIREWORKS_SPARK, sweepInfo.getCenter().clone().add(deltaX, deltaY, deltaZ), 1).extra(1)
				.directionalMode(true).delta(-1, 0, 0).spawnAsEntityActive(mBoss);
		}
	}

	private int getCastTime(@Nullable DepthsParty party) {
		int castTime = CAST_TIME;
		if (party != null && party.getAscension() >= 15) {
			castTime -= CAST_TIME_A15_DECREASE;
		}
		return castTime;
	}

	private double getSweepZOffset(@Nullable DepthsParty party) {
		double offset = SWEEP_Z_OFFSET;
		if (party != null && party.getAscension() >= 15) {
			offset += SWEEP_SIZE_A15_INCREASE;
		}
		return offset;
	}

	private double getSweepDzValue(@Nullable DepthsParty party) {
		double value = SWEEP_DZ_VALUE;
		if (party != null && party.getAscension() >= 15) {
			value -= SWEEP_SIZE_A15_INCREASE * 2;
		}
		return value;
	}
}
