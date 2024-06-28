package com.playmonumenta.plugins.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SerializationUtils {
	public static void storeDataOnEntity(LivingEntity entity, String identityTag, String data) throws Exception {
		NBT.modifyPersistentData(entity, nbt -> {
			nbt.setString("monumenta:" + identityTag, data);
		});
	}

	public static String retrieveDataFromEntity(LivingEntity entity, String identityTag) {
		return NBT.getPersistentData(entity, nbt -> {
			return nbt.getString("monumenta:" + identityTag);
		});
	}

	@FunctionalInterface
	public interface BossConstructor {
		@Nullable BossAbilityGroup run(Location spawnLoc, Location endLoc);
	}

	public static @Nullable BossAbilityGroup statefulBossDeserializer(LivingEntity boss, String identityTag,
	                                                                  BossConstructor constructor) throws Exception {
		String content = SerializationUtils.retrieveDataFromEntity(boss, identityTag);

		if (content == null || content.isEmpty()) {
			throw new Exception("Can't instantiate " + identityTag + " with no serialized data");
		}

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);

		if (!(object.has("spawnX") && object.has("spawnY") && object.has("spawnZ") &&
		        object.has("endX") && object.has("endY") && object.has("endZ"))) {
			throw new Exception("Failed to instantiate " + identityTag + ": missing required data element");
		}

		Location spawnLoc = new Location(boss.getWorld(), object.get("spawnX").getAsDouble(),
		                                 object.get("spawnY").getAsDouble(), object.get("spawnZ").getAsDouble());
		Location endLoc = new Location(boss.getWorld(), object.get("endX").getAsDouble(),
		                               object.get("endY").getAsDouble(), object.get("endZ").getAsDouble());

		return constructor.run(spawnLoc, endLoc);
	}

	public static String statefulBossSerializer(Location spawnLoc, Location endLoc) {
		Gson gson = new GsonBuilder().create();
		JsonObject root = new JsonObject();

		root.addProperty("spawnX", spawnLoc.getX());
		root.addProperty("spawnY", spawnLoc.getY());
		root.addProperty("spawnZ", spawnLoc.getZ());
		root.addProperty("endX", endLoc.getX());
		root.addProperty("endY", endLoc.getY());
		root.addProperty("endZ", endLoc.getZ());

		return gson.toJson(root);
	}
}
