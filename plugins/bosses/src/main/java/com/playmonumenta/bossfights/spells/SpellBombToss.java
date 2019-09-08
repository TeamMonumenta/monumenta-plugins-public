package com.playmonumenta.bossfights.spells;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBombToss extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {
		// Choose random player within range that has line of sight to boss
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		Collections.shuffle(players);
		for (Player player : players) {
			if (Utils.hasLineOfSight(mBoss.getEyeLocation(), player)) {
				launch(player);
				break;
			}
		}
	}

	@Override
	public int duration() {
		return 160; //8 seconds
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		sLoc.getWorld().playSound(sLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
		try {
			Entity tnt = Utils.summonEntityAt(sLoc, EntityType.PRIMED_TNT, "{Fuse:50}");
			Location pLoc = target.getLocation();
			Location tLoc = tnt.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
			tnt.setVelocity(vect);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon TNT for bomb toss: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
