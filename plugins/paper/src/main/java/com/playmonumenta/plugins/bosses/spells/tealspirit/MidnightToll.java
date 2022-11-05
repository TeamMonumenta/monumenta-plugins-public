package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MidnightToll extends Spell {

	private static final String ABILITY_NAME = "Midnight Toll";
	private int mDamage;
	private int mRange;
	private Location mSpawnLoc;
	private LivingEntity mBoss;
	private int mDamageCastTime;
	private ChargeUpManager mChargeDamage;
	private Plugin mPlugin;

	public MidnightToll(Plugin plugin, LivingEntity boss, int damageCastTime, int damage, int range, Location spawnLoc) {
		this.mPlugin = plugin;
		this.mBoss = boss;
		this.mDamage = damage;
		this.mRange = range;
		this.mSpawnLoc = spawnLoc;
		this.mDamageCastTime = damageCastTime;
		if (mDamage >= 2000) {
			this.mChargeDamage = new ChargeUpManager(mBoss, mDamageCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME + "(Enrage)",
				BarColor.YELLOW, BarStyle.SOLID, mRange);
		}
		this.mChargeDamage = new ChargeUpManager(mBoss, mDamageCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, mRange);
	}

	@Override
	public void run() {
		mChargeDamage.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeDamage.getTime() % 5 == 0) {
					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
					}

					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 5f, 2.0f);
					});
				}
				if (mChargeDamage.nextTick()) {
					new PPExplosion(Particle.SOUL_FIRE_FLAME, mBoss.getLocation())
						.speed(1)
						.count(120)
						.extraRange(4, 4)
						.spawnAsBoss();
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, "Stasis");
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(p, VoodooBonds.EFFECT_NAME);
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
						p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.0f);
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2f, 2f);
					});
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mDamageCastTime + 20 * 5;
	}
}
