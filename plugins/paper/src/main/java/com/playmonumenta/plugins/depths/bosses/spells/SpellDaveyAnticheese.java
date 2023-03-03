package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellDaveyAnticheese extends Spell {

	public LivingEntity mBoss;
	public Location mStartLoc;
	public int mTicks = 0;

	public SpellDaveyAnticheese(LivingEntity boss, Location startLoc) {
		mBoss = boss;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {

		//Anticheese
		if (mTicks % 20 == 0) {
			for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Davey.detectionRange, true)) {
				if ((p.getLocation().getY() > mStartLoc.getY() + 3 && PlayerUtils.isOnGround(p)) || p.getLocation().distance(mStartLoc) > 40) {
					BossUtils.bossDamagePercent(mBoss, p, .1);
					p.sendMessage(Component.text("That hurt! Looks like the arena is pulling you down..", NamedTextColor.RED));
				}
			}

			for (Entity e : mBoss.getWorld().getNearbyEntities(mBoss.getLocation(), 3, 3, 3)) {
				if (e instanceof Boat) {
					e.remove();
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
