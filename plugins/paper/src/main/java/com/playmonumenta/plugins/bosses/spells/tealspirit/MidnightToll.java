package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Stasis;
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

public class MidnightToll extends Spell {
	private static final String ABILITY_NAME = "Midnight Toll";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mDamageCastTime;
	private final int mDamage;
	private final int mRange;
	private final Location mSpawnLoc;
	private final boolean mEnrage;
	private final ChargeUpManager mChargeDamage;

	public MidnightToll(Plugin plugin, LivingEntity boss, int damageCastTime, int damage, int range, Location spawnLoc, boolean enrage) {
		this.mPlugin = plugin;
		this.mBoss = boss;
		this.mDamageCastTime = damageCastTime;
		this.mDamage = damage;
		this.mRange = range;
		this.mSpawnLoc = spawnLoc;
		this.mEnrage = enrage;
		this.mChargeDamage = new ChargeUpManager(
			mBoss, mDamageCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(enrage ? ABILITY_NAME + " (☠)" : ABILITY_NAME, NamedTextColor.YELLOW)),
			BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange
		);
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
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.HOSTILE, 5f, 2.0f));
				}

				if (mChargeDamage.nextTick()) {
					new PPExplosion(Particle.SOUL_FIRE_FLAME, mBoss.getLocation())
						.speed(1)
						.count(120)
						.extraRange(4, 4)
						.spawnAsBoss();
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						if (mEnrage) {
							PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME + " (☠)");
						} else {
							com.playmonumenta.plugins.Plugin monumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
							monumentaPlugin.mEffectManager.clearEffects(p, Stasis.GENERIC_NAME);
							monumentaPlugin.mEffectManager.clearEffects(p, VoodooBonds.PROTECTION_EFFECT);
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
						}
						p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 2f, 0.0f);
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 2f, 2f);
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
