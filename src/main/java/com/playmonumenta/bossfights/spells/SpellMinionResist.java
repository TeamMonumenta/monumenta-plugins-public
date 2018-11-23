package com.playmonumenta.bossfights.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;


public class SpellMinionResist extends Spell {
	private LivingEntity mLauncher;
	private PotionEffect mPotion;
	private int mRange;
	private int mApplyPeriod;
	private Scoreboard mScoreboard;
	private Team mTeam;
	private int mTicks;

	/*
	 * Applies potion effect to launcher whenever other members of his team are within range
	 *
	 * Because this is expected to be called often (passive effect), this only actually does the check
	 * every applyPeriod invocations
	 */
	public SpellMinionResist(LivingEntity launcher, PotionEffect potion, int range, int applyPeriod) {
		mScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		mTeam = mScoreboard.getEntryTeam(launcher.getUniqueId().toString());
		mLauncher = launcher;
		mPotion = potion;
		mRange = range;
		mApplyPeriod = applyPeriod;
		mTicks = mApplyPeriod;
	}

	@Override
	public void run() {
		mTicks++;
		if (mTicks >= mApplyPeriod) {
			mTicks = 0;

			for (Entity e : mLauncher.getNearbyEntities(mRange, mRange, mRange)) {
				if (mTeam.equals(mScoreboard.getEntryTeam(e.getUniqueId().toString()))) {
					mLauncher.addPotionEffect(mPotion, true);
				}
			}
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}
