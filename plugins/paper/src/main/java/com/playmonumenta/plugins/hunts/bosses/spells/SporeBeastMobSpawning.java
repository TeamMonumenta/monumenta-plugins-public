package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SporeBeastMobSpawning extends Spell {
	private static final int SPAWN_COOLDOWN = 20 * 12;
	private static final int MAX_SPAWN_RANGE = 8;
	private static final int MIN_SPAWN_RANGE = 8;
	private static final double PLAYERS_PER_ELITE = 5;
	private static final int HALVE_SUMMON_THRESHOLD = 12;
	private static final int MIN_MOBS = 4;

	private final LoSPool NORMAL_POOL = new LoSPool.LibraryPool(SporousAmalgam.NORMAL_POOL_NAME);
	private final LoSPool ELITE_POOL = new LoSPool.LibraryPool(SporousAmalgam.ELITE_POOL_NAME);
	private final LoSPool PARTY_POOL = new LoSPool.LibraryPool(SporousAmalgam.PARTY_POOL_NAME);

	private final SporousAmalgam mSporeBeast;

	private int mTicks;
	private double mSpawnElites;
	private boolean mHastened = false;

	public SporeBeastMobSpawning(SporousAmalgam sporeBeast) {
		mSporeBeast = sporeBeast;
		mTicks = 0;
		mSpawnElites = 0;
	}

	@Override
	public void run() {
		if (mTicks % 20 == 0) {
			mSporeBeast.updateSummonsList();
		}
		if (mTicks % SPAWN_COOLDOWN == 0) {
			mHastened = false;
			int playerAmount = mSporeBeast.getPlayersInOutRange().size();
			int fodderMobAmount = getFodderMobAmount(playerAmount);
			int platoonAmount = fodderMobAmount / 4;
			fodderMobAmount -= platoonAmount * 4;

			for (int i = 0; i < platoonAmount; i++) {
				spawnMobPlatoon();
			}

			for (int i = 0; i < fodderMobAmount; i++) {
				spawnMob();
			}

			mSpawnElites += (0.3 + playerAmount / PLAYERS_PER_ELITE);

			while (mSpawnElites >= 1) {
				spawnElite();
				mSpawnElites--;
			}
		}
		mTicks++;
	}

	public void spawnMobPlatoon() {
		List<Entity> entities = PARTY_POOL.spawnAll(getRandomSpawnLocation(), new Vector(MAX_SPAWN_RANGE, 0, MAX_SPAWN_RANGE));
		for (Entity e : entities) {
			if (e instanceof LivingEntity entity) {
				mSporeBeast.addSummon(entity);
			}
		}
	}

	public void spawnMob() {
		spawnFromPool(NORMAL_POOL);
	}

	public void spawnElite() {
		spawnFromPool(ELITE_POOL);
	}

	public void spawnFromPool(LoSPool pool) {
		Entity e = pool.spawn(getRandomSpawnLocation());
		if (e instanceof LivingEntity entity) {
			mSporeBeast.addSummon(entity);
		}
	}

	protected int getFodderMobAmount(int size) {
		int amount = 0;
		for (int i = 0; i < size; i++) {
			amount += 1;
		}

		if (mSporeBeast.summons() >= HALVE_SUMMON_THRESHOLD) {
			amount /= 2;
		}
		return Math.max(amount, MIN_MOBS);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private Location getRandomSpawnLocation() {
		return mSporeBeast.getRandomLocationInArena(MIN_SPAWN_RANGE, MAX_SPAWN_RANGE, 0);
	}

	public void hastenWave() {
		if (mHastened) {
			return;
		}
		mHastened = true;
		int temp = mTicks % SPAWN_COOLDOWN;
		mTicks = mTicks + (SPAWN_COOLDOWN - temp) / 2;
	}
}
