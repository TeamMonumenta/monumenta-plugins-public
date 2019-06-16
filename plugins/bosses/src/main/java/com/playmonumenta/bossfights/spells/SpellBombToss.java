package com.playmonumenta.bossfights.spells;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBombToss extends Spell {

	private final LivingEntity mBoss;
	private final int mRange;

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range) {
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
		Location SLoc = mBoss.getLocation();
		SLoc.setY(SLoc.getY() + 1.7f);
		SLoc.getWorld().playSound(SLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon tnt " + SLoc.getX() + " " + SLoc.getY() + " " + SLoc.getZ() + " {Fuse:50}");
		List<Entity> tnt = mBoss.getNearbyEntities(0.2, 2.5, 0.2);
		Location pLoc = target.getLocation();
		Location tLoc = tnt.get(0).getLocation();
		Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
		vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
		tnt.get(0).setVelocity(vect);
	}
}
