package com.playmonumenta.bossfights.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellMinionResist implements Spell {
	private LivingEntity mLauncher;
	private PotionEffect mPotion;
	private int mRange;

	public SpellMinionResist(LivingEntity launcher, PotionEffect potion, int range) {
		mLauncher = launcher;
		mPotion = potion;
		mRange = range;
	}

	@Override
	public void run() {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Team team = scoreboard.getEntryTeam(mLauncher.getUniqueId().toString());
		List<Entity> en = mLauncher.getNearbyEntities(mRange, mRange, mRange);
		for(Entity e : en) {
			Team team2 = scoreboard.getEntryTeam(e.getUniqueId().toString());
			if(team == team2) {
				mLauncher.addPotionEffect(mPotion, true);
			}
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}
