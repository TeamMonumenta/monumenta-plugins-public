package com.playmonumenta.plugins.abilities.delves.twisted;

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
 * ARCANIC: 50% / 100% of mobs within 16 blocks of you gain +40 health and a random ability:
 * Rejuvenation, Teleport, Bomb Toss Strong No Block Break, Pulse Laser, Charge Strong, Flame Nova. Also, mobs are highly blast resistant.
 */

public class Arcanic extends Ability {

	private static final int ARCANIC_CHALLENGE_SCORE = 23;
	private static final int ARCANIC_RADIUS = 16;
	private static final int ARCANIC_HEALTH = 60;
	private static final double ARCANIC_1_CHANCE = 0.5;
	private static final double ARCANIC_2_CHANCE = 1;

	private static final String[] ARCANIC_POSSIBLE_ABILITIES = {
		" boss_rejuvenation",
		" boss_tpbehind",
		" boss_bombtossstrongnoblockbreak",
		" boss_pulselaser",
		" boss_chargerstrong",
		" boss_flamenova"
	};

	private static Set<LivingEntity> mDumbledores = new HashSet<LivingEntity>();
	private static BukkitRunnable mDumbledoresCleaner;

	private final double mChance;

	public Arcanic(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mChance = ServerProperties.getClassSpecializationsEnabled() ? ARCANIC_2_CHANCE : ARCANIC_1_CHANCE;

		// Only run one of these because multiple players may be contributing to the same set
		if (mDumbledoresCleaner == null || mDumbledoresCleaner.isCancelled()) {
			mDumbledoresCleaner = new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<LivingEntity> iter = mDumbledores.iterator();
					while (iter.hasNext()) {
						LivingEntity mob = iter.next();
						if (mob.isDead() || !mob.isValid()) {
							iter.remove();
						}
					}
				}
			};
			mDumbledoresCleaner.runTaskTimer(mPlugin, 0, 20 * 10);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, ARCANIC_CHALLENGE_SCORE);
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), ARCANIC_RADIUS)) {
				if (!mDumbledores.contains(mob)) {
					mDumbledores.add(mob);
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
							"bossfight " + mob.getUniqueId() + " boss_blastresist");
					if (mRandom.nextDouble() < mChance) {
						mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(ARCANIC_HEALTH + mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
						mob.setHealth(mob.getHealth() + ARCANIC_HEALTH);
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
								"bossfight " + mob.getUniqueId() + ARCANIC_POSSIBLE_ABILITIES[mRandom.nextInt(ARCANIC_POSSIBLE_ABILITIES.length)]);
					}
				}
			}
		}
	}

}
