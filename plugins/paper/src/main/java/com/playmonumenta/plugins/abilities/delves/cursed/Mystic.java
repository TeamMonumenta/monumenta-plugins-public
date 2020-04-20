package com.playmonumenta.plugins.abilities.delves.cursed;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * MYSTIC: 25% / 50% of mobs within 16 blocks gain +20 health and a random ability:
 * Pulse Laser, Charge Strong, Flame Nova.
 */

public class Mystic extends Ability {

	private static final int MYSTIC_CHALLENGE_SCORE = 13;
	private static final int MYSTIC_RADIUS = 16;
	private static final int MYSTIC_HEALTH = 30;
	private static final double MYSTIC_1_CHANCE = 0.25;
	private static final double MYSTIC_2_CHANCE = 0.5;

	private static final String[] MYSTIC_POSSIBLE_ABILITIES = {
		" boss_pulselaser",
		" boss_chargerstrong",
		" boss_flamenova"
	};

	private static Set<LivingEntity> mHarryPotters = new HashSet<LivingEntity>();
	private static BukkitRunnable mHarryPottersCleaner;

	private final double mChance;

	public Mystic(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mChance = ServerProperties.getClassSpecializationsEnabled() ? MYSTIC_2_CHANCE : MYSTIC_1_CHANCE;

		// Only run one of these because multiple players may be contributing to the same set
		if (mHarryPottersCleaner == null || mHarryPottersCleaner.isCancelled()) {
			mHarryPottersCleaner = new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<LivingEntity> iter = mHarryPotters.iterator();
					while (iter.hasNext()) {
						LivingEntity mob = iter.next();
						if (mob.isDead() || !mob.isValid()) {
							iter.remove();
						}
					}
				}
			};
			mHarryPottersCleaner.runTaskTimer(mPlugin, 0, 20 * 10);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, MYSTIC_CHALLENGE_SCORE);
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), MYSTIC_RADIUS)) {
				if (!mHarryPotters.contains(mob)) {
					mHarryPotters.add(mob);
					if (mRandom.nextDouble() < mChance) {
						mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MYSTIC_HEALTH + mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
						mob.setHealth(mob.getHealth() + MYSTIC_HEALTH);
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
								"bossfight " + mob.getUniqueId() + MYSTIC_POSSIBLE_ABILITIES[mRandom.nextInt(MYSTIC_POSSIBLE_ABILITIES.length)]);
					}
				}
			}
		}
	}

}
