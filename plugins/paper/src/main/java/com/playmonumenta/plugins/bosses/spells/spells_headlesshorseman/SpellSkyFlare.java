package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellSkyFlare extends Spell {

	private LivingEntity mBoss;
	private Location mFloor;
	private int t;

	public SpellSkyFlare(LivingEntity boss) {
		mBoss = boss;
		mFloor = mBoss.getLocation().subtract(0, 1, 0);
		t = 20;
	}

	@Override
	public void run() {
		t -= 5;

		if (t <= 0) {
			t = 20;
			World world = mBoss.getWorld();
			for (Player player : PlayerUtils.playersInRange(mFloor, 50)) {
				if (player.getLocation().getY() - mFloor.getY() >= 3) {
					BossUtils.bossDamagePercent(mBoss, player, 0.1);
					player.setFireTicks(20 * 8);
					world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
					world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 15, 0.4, 0.4, 0.4, 0.1);
					world.spawnParticle(Particle.FLAME, player.getLocation(), 25, 0.4, 0.4, 0.4, 0.075);
				}
			}
		}
	}

	@Override
	public int duration() {
		return 0;
	}

}
