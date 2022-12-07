package com.playmonumenta.plugins.bosses.spells.shura;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellShuraPassiveSummon extends Spell {
	private Plugin mPlugin;
	private Location mCenter;
	private int mRadius = 16;
	private final int mCDTicks = 12 * 20;
	private int mT = 0;
	private PartialParticle mSmoke;
	public List<String> mSummons;


	public SpellShuraPassiveSummon(Plugin plugin, Location loc) {
		mPlugin = plugin;
		mCenter = loc;
		mSmoke = new PartialParticle(Particle.SMOKE_LARGE, mCenter, 5, 0, 0, 0, 0.05);
		mSummons = new ArrayList<>();
		mSummons.add("CorruptedDefender");
		mSummons.add("TlaxanSharpshooter");
		mSummons.add("UnyieldingSlinger");
		mSummons.add("JungleCrawler");
	}

	@Override
	public void run() {
		mT += 5;
		if (mT > mCDTicks) {
			summon();
			mT -= mCDTicks;
		}
	}

	private void summon() {
		int totalSummons = 4;
		// average 3 tries per summon
		int tries = 0;
		int maxTries = 9;
		while (totalSummons > 0 && tries < maxTries) {
			tries++;
			//Find a random location within the portal to spawn
			double r = Math.random() * mRadius;
			double theta = Math.random() * 2 * Math.PI;
			//checks from +5 y to -5 y as arena is not flat
			int terminate = 10;
			int inc = 0;
			Location spawnLoc = mCenter.clone().add(r * FastUtils.cos(theta), 5, r * FastUtils.sin(theta));
			while ((spawnLoc.getBlock().isPassable() || spawnLoc.getBlock().isEmpty()) &&
				       !spawnLoc.getBlock().isLiquid() && inc < terminate) {
				spawnLoc.add(0, -1, 0);
				inc++;
			}

			//summon the mob if the location is valid
			if (spawnLoc.getBlock().isSolid() && spawnLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
				//location found where spawn block is passable and can be stood on
				totalSummons--;
				Collections.shuffle(mSummons);
				String summonName = mSummons.get(0);
				World world = spawnLoc.getWorld();

				PPCircle indicator = new PPCircle(Particle.SMOKE_NORMAL, spawnLoc.add(0, 1, 0), 0).count(4).ringMode(true).delta(0.2, 0, 0.2);
				BukkitRunnable a = new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						mT++;
						indicator.radius((10 - mT) / 2.5).spawnAsBoss();
						if (mT >= 10) {
							this.cancel();
							Location loc = spawnLoc.add(0, 1, 0);
							LibraryOfSoulsIntegration.summon(loc, summonName);
							world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.5f, 1f);
							mSmoke.location(loc).spawnAsBoss();
						}
					}
				};
				a.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(a);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
