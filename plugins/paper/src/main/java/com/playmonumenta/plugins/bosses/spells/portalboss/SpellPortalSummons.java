package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.PortalBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Davey;
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

public class SpellPortalSummons extends Spell {
	private static final String SUMMON_NAME_1 = "ChemicalSpill";
	private static final String SUMMON_NAME_2 = "HostileCube";
	private static final String SUMMON_NAME_3 = "ProjectileRepeaterMK07";
	private static final int SPAWN_COUNT = 4; // Summon count 8-16 depending on players alive
	private static final int RANGE = 25;
	private static final int MAX_MOBS = 15;

	private final LivingEntity mBoss;
	private final Location mStartLoc;
	private int mCooldownTicks;
	private final PortalBoss mPortalBoss;

	public SpellPortalSummons(LivingEntity boss, int cooldown, Location startLoc, PortalBoss portalBoss) {
		mBoss = boss;
		mCooldownTicks = cooldown;
		mStartLoc = startLoc;
		mPortalBoss = portalBoss;
	}

	@Override
	public void run() {
		Location loc = mStartLoc;
		loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 20, 1);
		int summonCount = SPAWN_COUNT + (2 * PlayerUtils.playersInRange(mBoss.getLocation(), PortalBoss.detectionRange, true).size());

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), PortalBoss.detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" FABRICATING… ATTACK INTELLIGENCE INSTALLED. HORDE, PURGE INTRUDERS.\",\"color\":\"red\"}]");

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
					while (x == -1 || loc.getWorld().getBlockAt(loc.clone().add(x, .25, z)).getType() != Material.AIR || loc.clone().add(x, .25, z).distance(mStartLoc) < 20) {
						x = FastUtils.randomDoubleInRange(-RANGE, RANGE);
						z = FastUtils.randomDoubleInRange(-RANGE, RANGE);

						attempts++;
						//Prevent infinite loop
						if (attempts > 50) {
							break;
						}
					}
					//Summon the mob using our location
					Location sLoc = loc.clone().add(x, 0.25, z);
					loc.getWorld().playSound(sLoc, Sound.BLOCK_GRAVEL_BREAK, 1, 0.75f);
					new PartialParticle(Particle.BLOCK_DUST, sLoc, 16, 0.25, 0.1, 0.25, 0.25, Material.GRAVEL.createBlockData()).spawnAsEntityActive(mBoss);
					Random r = new Random();
					int roll = r.nextInt(3);
					Entity summonedMob = null;

					if (roll == 0) {
						summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_1);
					} else if (roll == 1) {
						summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_2);
					} else if (roll == 2) {
						summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_3);
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

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return EntityUtils.getNearbyMobs(mBoss.getLocation(), Davey.detectionRange).size() < MAX_MOBS && mPortalBoss.mIsHidden;
	}

}
