package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellRKitxetSummon extends Spell {
	private static final int MAX_SUMMONS = 25;
	private static final int SUMMON_TIME = 3 * 20;

	private Plugin mPlugin;
	private RKitxet mRKitxet;
	private LivingEntity mBoss;
	private int mCooldown;
	public List<String> mSummonOptions;

	public SpellRKitxetSummon(Plugin plugin, RKitxet rKitxet, LivingEntity boss, int cooldown) {
		mPlugin = plugin;
		mRKitxet = rKitxet;
		mBoss = boss;
		mCooldown = cooldown;
		mSummonOptions = new ArrayList<>();
		mSummonOptions.add("BlightedWarden");
		mSummonOptions.add("DecayingTlaxan");
		mSummonOptions.add("TwistedRanger");
		mSummonOptions.add("TwistedTlaxan");
		mSummonOptions.add("ShadowOfTheForest");
	}

	@Override
	public void run() {
		mRKitxet.useSpell("Summon");

		int count = 0;
		List<Location> summonLocs = mRKitxet.mAgonyLocations;
		Collections.shuffle(summonLocs);
		for (Location loc : summonLocs) {
			if (count > MAX_SUMMONS) {
				break;
			}

			//Find a random location within the portal to spawn
			double r = Math.random() * 0.8 * SpellEndlessAgony.RADIUS;
			double theta = Math.random() * 2 * Math.PI;
			Location randomSpreadLoc = loc.clone().add(r * FastUtils.cos(theta), 0, r * FastUtils.sin(theta));

			Location loweredLoc = randomSpreadLoc.clone().subtract(0, 1.5, 0); //Starts spawning in the ground
			loweredLoc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 0.75f);
			new PartialParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.LIME_WOOL.createBlockData()).spawnAsEntityActive(mBoss);
			Collections.shuffle(mSummonOptions);
			Entity summon = LibraryOfSoulsIntegration.summon(loweredLoc, mSummonOptions.get(0));
			if (summon != null && summon instanceof LivingEntity) {
				LivingEntity summonedMob = (LivingEntity) summon;
				summonedMob.setAI(false);
				summonedMob.setPersistent(true);
				BukkitRunnable summonRunnable = new BukkitRunnable() {
					int mTicks = 0;
					double mYInc = 1.6 / SUMMON_TIME;
					boolean mRaised = false;

					@Override
					public void run() {
						mTicks++;

						if (!mRaised) {
							summonedMob.teleport(summonedMob.getLocation().add(0, mYInc, 0));
						}

						if (mTicks >= SUMMON_TIME && !mRaised) {
							mRaised = true;
							summonedMob.setAI(true);
							//Break 2 blocks where the mob is so it isn't trapped
							summonedMob.getLocation().getBlock().breakNaturally();
							summonedMob.getLocation().add(0, 1, 0).getBlock().breakNaturally();
							new PartialParticle(Particle.BLOCK_DUST, loc, 6, 0.25, 0.1, 0.25, 0.25, Material.GREEN_CONCRETE_POWDER.createBlockData()).spawnAsEntityActive(mBoss);
						}

						if (mBoss.isDead() || !mBoss.isValid()) {
							summonedMob.setHealth(0);
							this.cancel();
							return;
						}

						if (summonedMob == null || summonedMob.isDead() || !summonedMob.isValid()) {
							this.cancel();
						}
					}

				};
				summonRunnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(summonRunnable);
			}

			count++;
		}
	}

	@Override
	public boolean canRun() {
		return mRKitxet.mAgonyLocations.size() >= 3 && EntityUtils.getNearbyMobs(mRKitxet.getSpawnLocation(), RKitxet.detectionRange).size() < 25 && mRKitxet.canUseSpell("Summon");
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
