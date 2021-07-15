package com.playmonumenta.plugins.depths.bosses.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;

public class SpellPassiveEyes extends Spell {

	public LivingEntity mBoss;
	public Nucleus mBossInstance;
	public Location mStartLoc;
	public int mTicks = 0;

	public SpellPassiveEyes(LivingEntity boss, Nucleus bossInstance, Location startLoc) {
		mBoss = boss;
		mBossInstance = bossInstance;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {
		mBossInstance.updateEyes();
		mTicks += 5;

		//Don't run in start animation
		if (mTicks < 6 * 20) {
			return;
		}

		if (mTicks % (10 * 20) == 0 && mBossInstance.mIsHidden) {
			mBossInstance.spawnEye();
			mBossInstance.spawnEye();
			mBossInstance.spawnEye();
			if (mBossInstance.mEyesKilled == 0) {
				for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Nucleus.detectionRange, true)) {
					p.sendMessage(Component.text("Eyes Open. Doorways Close.", NamedTextColor.RED));
				}
			}
		}

		//Anticheese
		if (mTicks % 20 == 0) {
			for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Nucleus.detectionRange, true)) {
				if ((p.getLocation().getY() > mStartLoc.getY() + 3 && p.isOnGround()) || p.getLocation().distance(mStartLoc) > 28) {
					BossUtils.bossDamagePercent(mBoss, p, .1);
					p.sendMessage(ChatColor.RED + "That hurt! Looks like the arena is pulling you down..");
				}
			}
		}
	}


	@Override
	public int cooldownTicks() {
		return 0;
	}
}
