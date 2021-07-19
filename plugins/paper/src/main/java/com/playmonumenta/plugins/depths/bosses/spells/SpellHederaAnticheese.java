package com.playmonumenta.plugins.depths.bosses.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class SpellHederaAnticheese extends Spell {

	public LivingEntity mBoss;
	public Location mStartLoc;
	public int mTicks = 0;

	public SpellHederaAnticheese(LivingEntity boss, Location startLoc) {
		mBoss = boss;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {

		//Anticheese
		if (mTicks % 20 == 0) {
			for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Hedera.detectionRange, true)) {
				if ((p.getLocation().getY() > mStartLoc.getY() + 11 && p.isOnGround()) || p.getLocation().distance(mStartLoc) > 35) {
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
