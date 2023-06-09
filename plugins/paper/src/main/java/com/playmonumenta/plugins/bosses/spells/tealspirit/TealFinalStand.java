package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TealFinalStand extends Spell {

	private static final String ABILITY_NAME = "Midnight Toll";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mDamage;
	private final int mHealthReduceInterval;
	private final int mHealthCastTime;
	private final int mDamageInterval;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeHealth;
	private int mT = 0;
	private int mStack = 1;

	public TealFinalStand(Plugin mPlugin, LivingEntity mBoss, int mRange, int mDamage, int mHealthReduceInterval, int healthCastTime, int mDamageInterval, Location mSpawnLoc) {
		this.mPlugin = mPlugin;
		this.mBoss = mBoss;
		this.mRange = mRange;
		this.mDamage = mDamage;
		this.mHealthReduceInterval = mHealthReduceInterval;
		this.mHealthCastTime = healthCastTime;
		this.mDamageInterval = mDamageInterval;
		this.mSpawnLoc = mSpawnLoc;

		this.mChargeHealth = new ChargeUpManager(mBoss, mHealthCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text("Rewrite History", NamedTextColor.YELLOW)),
			BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange);
	}

	@Override
	public void run() {

		if (mT % mHealthReduceInterval == 0) {
			mChargeHealth.setProgress(0);
			mChargeHealth.setTime(0);
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mChargeHealth.nextTick()) {
						PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, RewriteHistory.PERCENT_HEALTH_EFFECT, new PercentHealthBoost(72000, (-10 * mStack) / 100.0, RewriteHistory.PERCENT_HEALTH_EFFECT).displaysTime(false));
							new PPExplosion(Particle.PORTAL, p.getLocation())
								.speed(1)
								.count(120)
								.extraRange(0.15, 1)
								.spawnAsBoss();
							p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 0.5f, 0.0f);
						});
						mStack += 1;
						this.cancel();
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		}

		if (mT % mDamageInterval == 0) {
			PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
				DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
			});
		}

		mT += 1;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
