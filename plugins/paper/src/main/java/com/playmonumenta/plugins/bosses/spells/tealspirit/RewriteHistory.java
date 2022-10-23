package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPExplosion;
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

public class RewriteHistory extends Spell {
	private static final String ABILITY_NAME = "Rewrite History";
	private int mHealthCastTime;
	private Plugin mPlugin;
	private static int mStack = 1;
	private int mRange;
	private Location mSpawnLoc;
	private LivingEntity mBoss;
	private ChargeUpManager mChargeHealth;

	public RewriteHistory(Plugin mPlugin, LivingEntity mBoss, int mHealthCastTime, int mRange, Location mSpawnLoc) {
		this.mHealthCastTime = mHealthCastTime;
		this.mPlugin = mPlugin;
		this.mRange = mRange;
		this.mSpawnLoc = mSpawnLoc;
		this.mBoss = mBoss;
		this.mChargeHealth = new ChargeUpManager(mBoss, mHealthCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, mRange);
	}

	@Override
	public void run() {
		mChargeHealth.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeHealth.nextTick()) {
					PlayerUtils.playersInRange(mSpawnLoc, mRange, true).forEach(p -> {
						EntityUtils.removeAttribute(p, Attribute.GENERIC_MAX_HEALTH, "TealSpirit-" + mBoss.getUniqueId());
						EntityUtils.addAttribute(p, Attribute.GENERIC_MAX_HEALTH, new AttributeModifier( "TealSpirit-" + mBoss.getUniqueId(), (-10 * mStack) / 100.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
						new PPExplosion(Particle.WAX_ON, p.getLocation())
							.speed(1)
							.count(120)
							.extraRange(0.15, 1)
							.spawnAsBoss();
						p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 0.0f);
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
