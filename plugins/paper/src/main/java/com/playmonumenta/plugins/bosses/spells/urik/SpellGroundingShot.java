package com.playmonumenta.plugins.bosses.spells.urik;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellGroundingShot extends Spell{

	private final String CENTER_MARKER = "centermarker";
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mDelay;
	private int mRange;
	private LivingEntity mTierMarker;

	public SpellGroundingShot(Plugin plugin, LivingEntity boss,
			int delay, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mDelay = delay;
		mRange = range;

	}

	@Override
	public void run() {
		List<ArmorStand> centerpoints = new ArrayList<ArmorStand>();
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains(CENTER_MARKER)) {
				centerpoints.add((ArmorStand) e);
			}
		}

		ArmorStand point = centerpoints.get(0);

		for (Player player : PlayerUtils.playersInRange(point.getLocation(), mRange)) {
			Location loc = player.getLocation();
			if (player.isOnGround()) {
				if (loc.getY() - point.getLocation().getY() >= 3) {
					Arrow arrow = mBoss.getWorld().spawnArrow(mBoss.getEyeLocation(), player.getLocation().toVector().subtract(mBoss.getEyeLocation().toVector()), 2f, 0);
					arrow.setKnockbackStrength(2);
					arrow.setDamage(3);
					arrow.setPierceLevel(1);
				}
			}
		}

	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 10;
	}

}
