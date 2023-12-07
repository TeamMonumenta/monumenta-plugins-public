package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class PassiveSpider extends Spell {
	public static int SPAWN_COOLDOWN = 80;
	public static int MAX_MOBS = 16;

	private final LivingEntity mBoss;
	private final List<Location> mLocations;

	private int mTimer = 0;
	private boolean mEnabled = true;

	public PassiveSpider(LivingEntity boss) {
		mBoss = boss;
		List<Entity> spawnerMarkers = new ArrayList<>(boss.getLocation().getNearbyEntities(60, 60, 60).stream().filter(e -> e.getScoreboardTags().contains("spider_spawner")).toList());
		mLocations = spawnerMarkers.stream().map(Entity::getLocation).toList();
	}

	@Override
	public void run() {
		if (mTimer >= SPAWN_COOLDOWN && mEnabled) {
			// Spawn adds if within the mob cap. Note: Broodmother's core and limbs will count towards nearby mobs.
			// Also, eggs hatching, and the elites spawned by breaking all of her legs will bypass this cap,
			// but obviously still add to the nearby mobs count.
			int nearbyMobsCount = EntityUtils.getNearbyMobs(mBoss.getLocation(), Broodmother.detectionRange).size();
			if (nearbyMobsCount < MAX_MOBS) {
				Location spawnLoc = mLocations.get((int) (Math.random() * mLocations.size()));
				LoSPool.fromString("~DD2_Broodmother_Passive").spawn(spawnLoc);
			}
			mTimer = 0;
		}
		mTimer++;
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	public void setEnabled(boolean enabled) {
		mEnabled = enabled;
	}
}
