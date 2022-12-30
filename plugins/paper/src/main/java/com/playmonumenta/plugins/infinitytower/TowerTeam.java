package com.playmonumenta.plugins.infinitytower;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class TowerTeam {

	protected String mPlayerName;
	public List<TowerMob> mMobs;
	public int mCurrentSize = 0;


	public TowerTeam(String playername, List<TowerMob> mobs) {
		mPlayerName = playername;
		mMobs = mobs;
		mCurrentSize = 0;
		for (TowerMob mob : mobs) {
			mCurrentSize += mob.mInfo.mMobStats.mWeight;
		}
		mCurrentSize = mobs.size();
	}

	public boolean addMob(TowerMob mob) {
		mMobs.add(mob);
		mCurrentSize += mob.mInfo.mMobStats.mWeight;
		return true;
	}

	public boolean remove(TowerMob mob) {
		if (mMobs.contains(mob)) {
			mCurrentSize -= mob.mInfo.mMobStats.mWeight;
		}
		return mMobs.remove(mob);
	}


	public void removeAll(TowerMobInfo info) {
		mMobs.removeIf(mob -> mob.isSameBaseMob(info));
	}

	public boolean clear() {
		mCurrentSize = 0;
		mMobs.clear();
		return true;
	}

	public void summonAll(TowerGame game, List<LivingEntity> mobs, boolean playerSummon) {
		if (game.isGameEnded()) {
			return;
		}

		try {
			for (TowerMob mob : mMobs) {
				LivingEntity mobSpawned = mob.spawn(game, playerSummon);
				if (game.isGameEnded() || mobSpawned == null) {
					return;
				}
				TowerGameUtils.startMob(mobSpawned, mob, game, playerSummon);
				mobs.add(mobSpawned);
			}
		} catch (Exception e) {
			TowerFileUtils.warning("Catch an exception while spawning a mob. ");
			e.printStackTrace();
			game.forceStopGame();
		}


	}

	//------------------------JSON---------------------------------------

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("playername", mPlayerName);

		JsonArray array = new JsonArray();

		for (TowerMob mob : mMobs) {
			array.add(mob.toJson());
		}

		object.add("mobs", array);

		return object;
	}

	public static TowerTeam fromJson(JsonObject object) {

		String name = "";
		if (object.getAsJsonPrimitive("playername") != null) {
			name = object.getAsJsonPrimitive("playername").getAsString();
		}

		JsonArray arr = object.getAsJsonArray("mobs");

		List<TowerMob> mobs = new ArrayList<>();
		if (arr != null) {
			for (int i = 0; i < arr.size(); i++) {
				TowerMob towerMob = TowerMob.fromJson((JsonObject) arr.get(i));
				if (towerMob != null) {
					mobs.add(towerMob);
				} else {
					MMLog.severe("Could not load Blitz mob from JSON: " + arr.get(i));
				}
			}
		}

		return new TowerTeam(name, mobs);

	}
}
