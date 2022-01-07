package com.playmonumenta.plugins.bosses.spells.rkitxet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class SpellShardShield extends Spell {
	public static final int SHIELD_INTERVAL_1 = 35 * 20;
	public static final int SHIELD_INTERVAL_2 = 20 * 20;
	public static final int MIN_TIME_UNTIL_SHIELD_1 = 15 * 20;
	public static final int MIN_TIME_UNTIL_SHIELD_2 = 5 * 20;

	private LivingEntity mBoss;
	private int mTicks;

	private boolean mShielded;
	private int mUnshieldableTime;

	private int mShieldInterval;
	private int mMinTimeUntilShield;

	public SpellShardShield(LivingEntity boss) {
		mBoss = boss;
		mTicks = 0;

		mShielded = true;
		mUnshieldableTime = 0;

		mShieldInterval = SHIELD_INTERVAL_1;
		mMinTimeUntilShield = MIN_TIME_UNTIL_SHIELD_1;
	}

	@Override
	public void run() {
		//This function runs every 5 ticks
		mTicks += 5;

		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		if (mShielded) {

			for (double deg = 0; deg < 360; deg += 8) {
				world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 0.75, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
				world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 1.25, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
				world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 1.75, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
			}

			world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 1, 0), 10, 1, 1, 1);
		} else if (mUnshieldableTime == 0 && mTicks % mShieldInterval == 0) {
			applyShield();
		}

		if (mUnshieldableTime > 0) {
			mUnshieldableTime -= 5;
		}
	}

	public void applyShield() {
		if (!mShielded) {
			mTicks = 0;
			mShielded = true;

			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), RKitxet.detectionRange, true)) {
				player.sendMessage(ChatColor.AQUA + "The protective shield reforms around R'Kitxet.");
			}

			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 3f, 0.5f);
			world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 3f, 2f);
		}
	}

	public void removeShield() {
		if (mShielded) {
			mShielded = false;
			mUnshieldableTime = mMinTimeUntilShield;

			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), RKitxet.detectionRange, true)) {
				player.sendMessage(ChatColor.AQUA + "The shield shatters.");
			}

			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
			world.spawnParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 15, 0.5, 0, 0.5);
		}
	}

	public void activatePhase2() {
		mShieldInterval = SHIELD_INTERVAL_2;
		mMinTimeUntilShield = MIN_TIME_UNTIL_SHIELD_2;
	}

	public boolean isShielded() {
		return mShielded;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
