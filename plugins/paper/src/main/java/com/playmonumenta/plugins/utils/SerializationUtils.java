package com.playmonumenta.plugins.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class SerializationUtils {
	private static final String SERIALCONST = "SERIALDATA=";
	private static final int SERIALLEN = 50;

	private static List<String> serializeStringToLore(String data) {
		List<String> ret = new LinkedList<String>();
		for (int start = 0; start < data.length(); start += SERIALLEN) {
			ret.add(SERIALCONST + data.substring(start, Math.min(data.length(), start + SERIALLEN)));
		}
		return ret;
	}

	private static String deserializeStringFromLore(List<String> lore) {
		String retval = "";
		for (String str : lore) {
			if (str.startsWith(SERIALCONST)) {
				retval += str.substring(SERIALCONST.length());
			}
		}
		return retval;
	}


	public static void storeDataOnEntity(LivingEntity entity, String data) throws Exception {
		boolean placeholderItem = false;

		EntityEquipment equip = entity.getEquipment();
		if (equip == null) {
			throw new Exception("Boss equipment is null!");
		}

		ItemStack item = equip.getItemInOffHand();
		if (item == null || item.getType() == Material.AIR) {
			item = new ItemStack(Material.MUSIC_DISC_13);
			placeholderItem = true;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			throw new Exception("Boss item meta is null!");
		}

		if (placeholderItem) {
			meta.setDisplayName(SERIALCONST);
		}

		List<String> addLore = serializeStringToLore(data);
		if (meta.hasLore()) {
			List<String> currentLore = meta.getLore();

			/* Remove existing serialization data, if any */
			currentLore.removeIf(lore -> lore.startsWith(SERIALCONST));

			currentLore.addAll(addLore);
			meta.setLore(currentLore);
		} else {
			meta.setLore(addLore);
		}

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);
		equip.setItemInOffHand(item);
	}

	public static String retrieveDataFromEntity(LivingEntity entity) {
		EntityEquipment equip = entity.getEquipment();
		if (equip == null) {
			return "";
		}

		ItemStack item = equip.getItemInOffHand();
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return "";
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore()) {
			return "";
		}

		List<String> currentLore = meta.getLore();
		String data = deserializeStringFromLore(currentLore);

		/* Don't leave any serialization data on the entity */
		currentLore.removeIf(lore -> lore.startsWith(SERIALCONST));

		/* If this item's only purpose for existing was to hold data, remove it */
		if (meta.hasDisplayName() && meta.getDisplayName().equals(SERIALCONST)) {
			equip.setItemInOffHand(null);
		}

		return data;
	}

	public static void stripSerializationDataFromDrops(EntityDeathEvent event) {
		ListIterator<ItemStack> iter = event.getDrops().listIterator();
		while (iter.hasNext()) {
			ItemStack item = iter.next();
			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();

				if (meta.hasDisplayName() && meta.getDisplayName().equals(SERIALCONST)) {
					iter.remove();
				}

				if (meta.hasLore()) {
					meta.getLore().removeIf(lore -> lore.startsWith(SERIALCONST));
				}
			}
		}
	}

	@FunctionalInterface
	public interface BossConstructor {
		BossAbilityGroup run(Location spawnLoc, Location endLoc);
	}

	public static BossAbilityGroup statefulBossDeserializer(LivingEntity boss, String identityTag,
	                                                        BossConstructor constructor) throws Exception {
		String content = SerializationUtils.retrieveDataFromEntity(boss);

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
