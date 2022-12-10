package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellEndlessHederaSummons extends Spell {
	private static final String SUMMON_NAME_1 = "LushArachnid";
	private static final String SUMMON_NAME_2 = "MossyRemains";
	private static final String SUMMON_NAME_3 = "FerociousBoar";
	private static final int SPAWN_COUNT = 2; // Summon count 4-8 depending on players alive
	private static final int RANGE = 10;
	private static final int MAX_MOBS = 10;
	private static final int ELITE_CHANCE_PER_FLOOR = 15;

	private final LivingEntity mBoss;
	private int mCooldownTicks;
	private int mFightNumber;

	public SpellEndlessHederaSummons(LivingEntity boss, int cooldown, int fightNumber) {
		mBoss = boss;
		mCooldownTicks = cooldown;
		mFightNumber = fightNumber;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 20, 1);
		int summonCount = SPAWN_COUNT + PlayerUtils.playersInRange(mBoss.getLocation(), Hedera.detectionRange, true).size();

		//PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), Davey.detectionRange, "tellraw @s [\"\",{\"text\":\"[Davey]\",\"color\":\"gold\"},{\"text\":\" Now ye've done it. She be watchin'. Help me, heathens of \",\"color\":\"blue\"},{\"text\":\"ngbgbggb\",\"obfuscated\":\"true\",\"color\":\"blue\"},{\"text\":\"!\",\"color\":\"blue\"}]");

		BukkitRunnable run = new BukkitRunnable() {
			int mTicks = 0;
			int mSummons = 0;

			@Override
			public void run() {
				mTicks++;

				if (mSummons >= summonCount) {
					this.cancel();
					return;
				}

				if (mTicks % 20 == 0) {
					double x = -1;
					double z = -1;
					int attempts = 0;
					//Summon the mob, every second
					//Try until we have air space to summon the mob
					while (x == -1 || loc.getWorld().getBlockAt(loc.clone().add(x, .25, z)).getType() != Material.AIR) {
						x = FastUtils.randomDoubleInRange(-RANGE, RANGE);
						z = FastUtils.randomDoubleInRange(-RANGE, RANGE);

						attempts++;
						//Prevent infinite loop
						if (attempts > 20) {
							break;
						}
					}
					//Summon the mob using our location
					Location sLoc = loc.clone().add(x, 0.25, z);
					loc.getWorld().playSound(sLoc, Sound.BLOCK_GRAVEL_BREAK, 1, 0.75f);
					new PartialParticle(Particle.BLOCK_DUST, sLoc, 16, 0.25, 0.1, 0.25, 0.25, Material.GRAVEL.createBlockData()).spawnAsEntityActive(mBoss);
					Random r = new Random();
					int roll = r.nextInt(2);
					Entity summonedMob = null;
					if (isEliteSummon()) {
						summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_3);
						if (summonedMob != null) {
							summonedMob.addScoreboardTag(DelvesUtils.DELVE_MOB_TAG);
						}
					} else {
						if (roll == 0) {
							summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_1);
						} else if (roll == 1) {
							summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_2);
						}
					}

					if (summonedMob != null) {
						summonedMob.setPersistent(true);
					}

					mSummons++;
				}

			}
		};
		run.runTaskTimer(Plugin.getInstance(), 0, 1);
		mActiveRunnables.add(run);
	}

	public boolean isEliteSummon() {
		Random r = new Random();
		int roll = r.nextInt(100);
		if (roll < (Math.sqrt(mFightNumber) - 1) * ELITE_CHANCE_PER_FLOOR) {
			return true;
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return EntityUtils.getNearbyMobs(mBoss.getLocation(), Hedera.detectionRange).size() < MAX_MOBS;
	}
}
