package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Twisted {

	private static final double[] SPAWN_CHANCE = {
		0.005,
		0.010,
		0.013,
		0.020,
		0.025
	};

	private static final String POOL_NAME = "~Twisted";
	private static final String POOL_NAME_WATER = "~TwistedWater";

	private static final int ANIMATION_DURATION = 20 * 2;
	private static final int MAX_SPIRAL_ANIMATOR_COUNT = 3;

	public static final String DESCRIPTION = "Something, everything is wrong...";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				ChatColor.MAGIC + "M" + ChatColor.RESET + "or" + ChatColor.MAGIC + "tu" + ChatColor.RESET + "i non mo" + ChatColor.MAGIC + "rd" + ChatColor.RESET + "ent",
			}, {
				ChatColor.MAGIC + "Mors" + ChatColor.RESET + " non a" + ChatColor.MAGIC + "ccip" + ChatColor.RESET + "it excusatio" + ChatColor.MAGIC + "nes"
			}, {
				"Quid" + ChatColor.MAGIC + "quid in" + ChatColor.RESET + " altum " + ChatColor.MAGIC + "for" + ChatColor.RESET + "tuna " + ChatColor.MAGIC + "tulit" + ChatColor.RESET + ", ruitura " + ChatColor.MAGIC + "levat."
			}, {
				ChatColor.MAGIC + "Nec" + ChatColor.RESET + " vita " + ChatColor.MAGIC + "nec" + ChatColor.RESET + " fortuna " + ChatColor.MAGIC + "hominibus " + ChatColor.RESET + " perpes " + ChatColor.MAGIC + "est"
			}, {
				"For" + ChatColor.MAGIC + "tu" + ChatColor.RESET + "na " + ChatColor.MAGIC + "fav" + ChatColor.RESET + "et fo" + ChatColor.MAGIC + "rtib" + ChatColor.RESET + "u" + ChatColor.MAGIC + "s"
			}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE[level - 1] && DelvesUtils.isValidTwistedMob(mob)) {
			//spawn a twisted mob
			spawnTwisted(mob);
		}
	}

	public static void spawnTwisted(LivingEntity mob) {
		List<LivingEntity> mobsInArea = EntityUtils.getNearbyMobs(mob.getLocation(), 16);
		mobsInArea.remove(mob);
		Location spawningLoc = mob.getLocation().clone();

		mob.getWorld().playSound(spawningLoc, Sound.ENTITY_WITHER_SPAWN, 10, 0.5f);
		int count = MAX_SPIRAL_ANIMATOR_COUNT;
		for (LivingEntity le : mobsInArea) {
			final int countFinal = count;
			new BukkitRunnable() {
				int mTimer = 0;
				final boolean mShouldSpawnSpiral = countFinal > 0;
				final Location mLocation = le.getEyeLocation();
				final double mDistance = mLocation.distance(spawningLoc);
				final Vector mDirection = spawningLoc.clone().subtract(mLocation).toVector().normalize().multiply(mDistance / (ANIMATION_DURATION / 2.0));
				final PartialParticle mPPRay = new PartialParticle(Particle.REDSTONE, mLocation, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f));
				final PartialParticle mPPRay2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mLocation, 1, 0, 0, 0);
				final PartialParticle mPPSpiral = new PartialParticle(Particle.REDSTONE, mLocation, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f));
				final PartialParticle mPPSpiral2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mLocation, 1, 0, 0, 0);
				final PartialParticle mPPSpiral3 = new PartialParticle(Particle.SMOKE_LARGE, mLocation, 1, 0, 0, 0);

				final double mZDiff = mLocation.getZ() - spawningLoc.getZ();
				final double mXDiff = mLocation.getX() - spawningLoc.getX();
				final double mHeightStep = 0.03;
				double mRadiant = 2 * Math.atan(mZDiff / (mXDiff + Math.sqrt(mXDiff * mXDiff + mZDiff * mZDiff)));
				double mCurrentHeight = mHeightStep;
				@Override
				public void run() {
					if (mTimer <= ANIMATION_DURATION / 2.0) {
						mPPRay.location(mLocation).spawnAsBoss();
						mPPRay2.location(mLocation).spawnAsBoss();
						mLocation.add(mDirection);
					} else if (mShouldSpawnSpiral) {
						for (double degree = mRadiant; degree <= mRadiant + 15; degree += 5) {
							Location l = spawningLoc.clone().add(FastUtils.cos(degree) * 1.2, mCurrentHeight, FastUtils.sin(degree) * 1.2);
							mCurrentHeight += mHeightStep;
							mPPSpiral.location(l).spawnAsBoss();
							mPPSpiral2.location(l).spawnAsBoss();
							mPPSpiral3.location(l).spawnAsBoss();
						}
						mRadiant += 15;
					}

					if (mTimer >= ANIMATION_DURATION) {
						cancel();
						return;
					}

					if (!mShouldSpawnSpiral && mTimer >= ANIMATION_DURATION / 2) {
						cancel();
						return;
					}
					mTimer++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
			count--;
		}

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			LivingEntity twistedMob = null;
			String pool = spawningLoc.getBlock().getType() == Material.WATER ? POOL_NAME_WATER : POOL_NAME;
			Map<Soul, Integer> mobsPool = LibraryOfSoulsIntegration.getPool(pool);
			if (mobsPool != null) {
				for (Map.Entry<Soul, Integer> entry : mobsPool.entrySet()) {
					twistedMob = (LivingEntity) entry.getKey().summon(spawningLoc);
				}
			}
			if (twistedMob == null) {
				MMLog.warning("[Delve - Twisted] summoned twisted with null object! THIS IS A BUG!");
				return;
			}
			twistedMob.setInvulnerable(true);
			twistedMob.setGravity(false);
			final LivingEntity twistedMobFinal = twistedMob;
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				twistedMobFinal.setGravity(true);
				twistedMobFinal.setInvulnerable(false);
				twistedMobFinal.getWorld().playSound(twistedMobFinal.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 10, 0.8f);
			}, ANIMATION_DURATION / 2);
		}, ANIMATION_DURATION / 2);



	}


}
