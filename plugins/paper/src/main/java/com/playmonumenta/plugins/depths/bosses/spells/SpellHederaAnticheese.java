package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
				if ((p.getLocation().getY() > mStartLoc.getY() + 11 && PlayerUtils.isOnGround(p)) || p.getLocation().distance(mStartLoc) > 35) {
					BossUtils.bossDamagePercent(mBoss, p, .1);
					p.sendMessage(Component.text("That hurt! Looks like the arena is pulling you down..", NamedTextColor.RED));
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
