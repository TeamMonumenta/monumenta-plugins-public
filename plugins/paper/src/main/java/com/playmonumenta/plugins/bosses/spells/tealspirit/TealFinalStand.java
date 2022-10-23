package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TealFinalStand extends Spell {

	private static final String ABILITY_NAME = "Midnight Toll";
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mRange;
	private int mDamage;
	private double mPercentHP;
	private int mHealthReduceInterval;
	private int mHealthCastTime;
	private int mDamageInterval;
	private int mDamageCastTime;
	private Location mSpawnLoc;
	private ChargeUpManager mChargeHealth;
	private ChargeUpManager mChargeDamage;
	private ChargeUpManager mChargeFinal;
	private int mT = 0;
	private int mStack = 1;

	public TealFinalStand(Plugin mPlugin, LivingEntity mBoss, int mRange, int mDamage, double mPercentHP, int mHealthReduceInterval, int healthCastTime, int mDamageInterval, int damageCastTime, Location mSpawnLoc) {
		this.mPlugin = mPlugin;
		this.mBoss = mBoss;
		this.mRange = mRange;
		this.mDamage = mDamage;
		this.mPercentHP = mPercentHP;
		this.mHealthReduceInterval = mHealthReduceInterval;
		this.mHealthCastTime = healthCastTime;
		this.mDamageInterval = mDamageInterval;
		this.mDamageCastTime = damageCastTime;
		this.mSpawnLoc = mSpawnLoc;

		this.mChargeDamage = new ChargeUpManager(mBoss, mDamageCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + "Midnight Toll",
			BarColor.YELLOW, BarStyle.SOLID, mRange);
		this.mChargeHealth = new ChargeUpManager(mBoss, mHealthCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + "Rewrite History",
			BarColor.YELLOW, BarStyle.SOLID, mRange);
		this.mChargeFinal = new ChargeUpManager(mBoss, mHealthReduceInterval, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + "Destiny Undone",
			BarColor.YELLOW, BarStyle.SOLID, mRange);
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
							EntityUtils.removeAttribute(p, Attribute.GENERIC_MAX_HEALTH, "TealSpirit-" + mBoss.getUniqueId());
							EntityUtils.addAttribute(p, Attribute.GENERIC_MAX_HEALTH, new AttributeModifier( "TealSpirit-" + mBoss.getUniqueId(), (-10 * mStack) / 100.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
							new PPExplosion(Particle.PORTAL, p.getLocation())
								.speed(1)
								.count(120)
								.extraRange(0.15, 1)
								.spawnAsBoss();
							p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.0f);
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
