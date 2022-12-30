package com.playmonumenta.plugins.infinitytower.mobs;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class TowerMobStats {

	public double mAtk;
	//the base atk of this mob

	public double mHP;
	//the base HP of this mob

	public int mWeight = 1;


	public int mCost = 1;

	public int mLimit = 3;


	public TowerMobStats(double atk, double hp) {
		mAtk = atk;
		mHP = hp;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();

		obj.addProperty("baseAtk", mAtk);
		obj.addProperty("baseHP", mHP);
		obj.addProperty("weight", mWeight);
		obj.addProperty("cost", mCost);
		obj.addProperty("limit", mLimit);

		return obj;
	}

	@Contract("!null -> !null")
	public static @Nullable TowerMobStats fromJson(JsonObject obj) {
		if (obj == null) {
			return null;
		}

		double atk = obj.getAsJsonPrimitive("baseAtk").getAsDouble();
		double hp = obj.getAsJsonPrimitive("baseHP").getAsDouble();

		TowerMobStats stats = new TowerMobStats(atk, hp);

		int weight = obj.getAsJsonPrimitive("weight").getAsInt();
		int cost = obj.getAsJsonPrimitive("cost").getAsInt();
		int limit = obj.getAsJsonPrimitive("limit").getAsInt();

		if (weight != 0) {
			stats.mWeight = weight;
		}

		if (cost != 0) {
			stats.mCost = cost;
		}

		if (limit != 0) {
			stats.mLimit = limit;
		}

		return stats;
	}

}
