package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAbstractRectangleAttack;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellLegSweep extends Spell {

	public static final String SPELL_NAME = "Leg Sweep";
	public static final int COOLDOWN = 160;
	public static final int CAST_FIRST_DELAY = 60;
	public static final int CAST_FIRST_DELAY_A15_DECREASE = 20;
	public static final int ANIMATION_TIME = 10;
	public static final int ANIMATION_LINGER_TIME = 15;
	public static final double DAMAGE = 50;
	public static final int PARTICLE_AMOUNT = 30;
	public static final int TELEGRAPH_PULSES = 4;
	public static final double PARTICLE_SPEED = 0.06;

	private final LivingEntity mBoss;
	private final Broodmother mBroodmother;
	private final Plugin mPlugin;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweepRight1;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweepRight2;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweepLeft1;
	private final SpellBaseAbstractRectangleAttack.RectangleInfo mSweepLeft2;
	private final ChargeUpManager mChargeUp;
	private final ArrayList<BukkitTask> mLegTasks = new ArrayList<>();
	private final int mFinalCooldown;
	private final int mFinalCastFirstDelay;
	private final int mFinalCastSecondDelay;

	public SpellLegSweep(LivingEntity boss, @Nullable DepthsParty party, Broodmother broodmother) {
		mBoss = boss;
		mBroodmother = broodmother;
		mPlugin = Plugin.getInstance();
		// 3 away, 4 in, 1 away
		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mFinalCastFirstDelay = getFirstCastDelay(party);
		mFinalCastSecondDelay = mFinalCastFirstDelay / 2;

		mSweepRight1 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-5, -1, 2.5), -18.5, 13);
		mSweepRight2 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-5, -1, 2.5), 14, 18);
		mSweepLeft1 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-5, -1, -2.5), -18.5, -13);
		mSweepLeft2 = new SpellBaseAbstractRectangleAttack.RectangleInfo(mBoss.getLocation().clone().add(-5, -1, -2.5), 14, -18);
		mChargeUp = new ChargeUpManager(mBoss, mFinalCastFirstDelay,
			Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_RED, TextDecoration.BOLD)),
			BossBar.Color.RED, BossBar.Overlay.PROGRESS, 100
		);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_RED, TextDecoration.BOLD)));

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 3f, 0.5f);

		boolean startWithRight = FastUtils.randomBoolean();
		if (startWithRight) {
			sweep(mSweepRight1, true, false);
			sweep(mSweepRight2, true, false);
		} else {
			sweep(mSweepLeft1, false, false);
			sweep(mSweepLeft2, false, false);
		}
		// Then the other
		mLegTasks.add(
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (startWithRight) {
					sweep(mSweepLeft1, false, true);
					sweep(mSweepLeft2, false, true);
				} else {
					sweep(mSweepRight1, true, true);
					sweep(mSweepRight2, true, true);
				}
			}, mFinalCastFirstDelay)
		);

		BukkitRunnable runnable = new BukkitRunnable() {
			boolean mSwitchedDirection = false;

			@Override
			public void run() {
				if (!mSwitchedDirection) {
					if (mChargeUp.nextTick()) {
						mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_RED, TextDecoration.BOLD)));
						mSwitchedDirection = true;
						mChargeUp.setChargeTime(mFinalCastSecondDelay);
						mChargeUp.setTime(mFinalCastSecondDelay);
					}
				} else {
					if (mChargeUp.previousTick()) {
						mChargeUp.setChargeTime(mFinalCastFirstDelay);
						this.cancel();
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	public void stopLegTasks() {
		mLegTasks.forEach(BukkitTask::cancel);
	}

	private void sweep(SpellBaseAbstractRectangleAttack.RectangleInfo sweepInfo, boolean right, boolean isSecondCast) {
		// Telegraph the attack
		new SpellBaseAbstractRectangleAttack(sweepInfo, PARTICLE_AMOUNT, TELEGRAPH_PULSES, (isSecondCast ? mFinalCastSecondDelay : mFinalCastFirstDelay), PARTICLE_SPEED,
			Particle.END_ROD, DamageEvent.DamageType.MELEE, DAMAGE, true, true, SPELL_NAME,
			Particle.SWEEP_ATTACK, mPlugin, mBoss,
			(boss) -> {
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 1);
				new PartialParticle(Particle.SWEEP_ATTACK, sweepInfo.getCenter().clone().add(0, 0.1, 0), 100).extra(0)
					.delta(sweepInfo.getHalfDx(), 0.25, sweepInfo.getHalfDz()).spawnAsEntityActive(mBoss);
			}
		).run();
		// Leg sweep animation ChargeUp
		if (right) {
			StructuresAPI.loadAndPasteStructure("BikeSpiderSweepRight0", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			mBroodmother.moveLimb(0, new Vector(0, 0, 1.5));
		} else {
			StructuresAPI.loadAndPasteStructure("BikeSpiderSweepLeft0", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			mBroodmother.moveLimb(1, new Vector(0, 0, -1.5));
		}

		// Leg sweep animation sweep
		mLegTasks.add(
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (right) {
					StructuresAPI.loadAndPasteStructure("BikeSpiderSweepRight1", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
					mBroodmother.moveLimb(0, new Vector(0, 0, -2));
				} else {
					StructuresAPI.loadAndPasteStructure("BikeSpiderSweepLeft1", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
					mBroodmother.moveLimb(1, new Vector(0, 0, 2));
				}
				// Leg sweep animation reset
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (right) {
						StructuresAPI.loadAndPasteStructure("BikeSpiderRightLegReset", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
						mBroodmother.moveLimb(0, new Vector(0, 0, 0.5));
					} else {
						StructuresAPI.loadAndPasteStructure("BikeSpiderLeftLegReset", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
						mBroodmother.moveLimb(1, new Vector(0, 0, -0.5));
					}
				}, ANIMATION_LINGER_TIME);
			}, isSecondCast ? mFinalCastSecondDelay - ANIMATION_TIME : mFinalCastFirstDelay - ANIMATION_TIME)
		);
	}

	private int getFirstCastDelay(@Nullable DepthsParty party) {
		int delay = CAST_FIRST_DELAY;
		if (party != null && party.getAscension() >= 15) {
			delay -= CAST_FIRST_DELAY_A15_DECREASE;
		}
		return delay;
	}
}
