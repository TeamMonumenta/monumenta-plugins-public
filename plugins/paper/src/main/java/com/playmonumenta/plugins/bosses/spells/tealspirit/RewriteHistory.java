package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.particle.PPExplosion;
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

public class RewriteHistory extends Spell {
	private static final String ABILITY_NAME = "Rewrite History";
	public static final String PERCENT_HEALTH_EFFECT = "TealSpiritPercentHealthEffect";

	private final int mHealthCastTime;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private int mStack = 1;
	private final int mRange;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeHealth;

	public RewriteHistory(Plugin plugin, LivingEntity boss, int healthCastTime, int range, Location spawnLoc) {
		mHealthCastTime = healthCastTime;
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mSpawnLoc = spawnLoc;
		mChargeHealth = new ChargeUpManager(mBoss, mHealthCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)),
			BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange);
	}

	@Override
	public void run() {
		mChargeHealth.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mChargeHealth.getTime() % 2 == 0) {
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						p.playSound(p.getLocation(), Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 2f, 0.5f + (mChargeHealth.getTime() / 80f) * 1.5f);
					});
				}
				if (mChargeHealth.nextTick()) {
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, PERCENT_HEALTH_EFFECT, new PercentHealthBoost(72000, (-20 * mStack) / 100.0, PERCENT_HEALTH_EFFECT).displaysTime(false).deleteOnLogout(true));
						new PPExplosion(Particle.WAX_ON, p.getLocation())
							.speed(1)
							.count(120)
							.extraRange(0.15, 1)
							.spawnAsBoss();
						p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1f, 0.0f);
					});
					mStack += 1;
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mHealthCastTime + 20 * 5;
	}
}
