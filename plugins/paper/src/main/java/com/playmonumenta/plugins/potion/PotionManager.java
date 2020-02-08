package com.playmonumenta.plugins.potion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class PotionManager {
	//  Player ID / Player Potion Info
	public HashMap<UUID, PlayerPotionInfo> mPlayerPotions = new HashMap<UUID, PlayerPotionInfo>();

	public enum PotionID {
		APPLIED_POTION(0, "APPLIED_POTION"),
		ABILITY_SELF(1, "ABILITY_SELF"),
		ABILITY_OTHER(2, "ABILITY_OTHER"),
		SAFE_ZONE(3, "SAFE_ZONE"),
		ITEM(4, "ITEM"),
		ALL(5, "ALL");

		private int mValue;
		private String mName;
		private PotionID(int value, String name)    {
			this.mValue = value;
			this.mName = name;
		}
		public int getValue()       {
			return mValue;
		}
		public String getName()     {
			return mName;
		}

		public static PotionID getFromString(String name) {
			if (name.equals(PotionID.APPLIED_POTION.getName())) {
				return PotionID.APPLIED_POTION;
			} else if (name.equals(PotionID.ABILITY_SELF.getName())) {
				return PotionID.ABILITY_SELF;
			} else if (name.equals(PotionID.ABILITY_OTHER.getName())) {
				return PotionID.ABILITY_OTHER;
			} else if (name.equals(PotionID.SAFE_ZONE.getName())) {
				return PotionID.SAFE_ZONE;
			} else if (name.equals(PotionID.ITEM.getName())) {
				return PotionID.ITEM;
			} else {
				return null;
			}
		}
	}

	public void addPotion(Player player, PotionID id, Collection<PotionEffect> effects, double intensity) {
		for (PotionEffect effect : effects) {
			addPotion(player, id, effect, intensity);
		}
	}

	public void addPotion(Player player, PotionID id, Collection<PotionEffect> effects) {
		addPotion(player, id, effects, 1.0);
	}

	public void addPotion(Player player, PotionID id, PotionEffect effect, double intensity) {
		addPotion(player, id, new PotionInfo(effect.getType(), (int)(((double)effect.getDuration()) * intensity),
		                                     effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
	}

	public void addPotion(Player player, PotionID id, PotionEffect effect) {
		addPotion(player, id, effect, 1.0);
	}

	public void addPotion(Player player, PotionID id, PotionInfo info) {
		// Instant potions do not need to be tracked
		if (Constants.POTION_MANAGER_ENABLED
			&& info != null
		    && !info.type.equals(PotionEffectType.HARM)
		    && !info.type.equals(PotionEffectType.HEAL)) {

			UUID uuid = player.getUniqueId();
			PlayerPotionInfo potionInfo = mPlayerPotions.get(uuid);
			if (potionInfo != null) {
				potionInfo.addPotionInfo(player, id, info);
			} else {
				PlayerPotionInfo newPotionInfo = new PlayerPotionInfo();
				newPotionInfo.addPotionInfo(player, id, info);
				mPlayerPotions.put(uuid, newPotionInfo);
			}
		}
	}

	public void removePotion(Player player, PotionID id, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.removePotionInfo(player, id, type);
		}
	}


	public void clearAllPotions(Player player) {
		mPlayerPotions.remove(player.getUniqueId());

		// Make a copy of the list to prevent ConcurrentModificationException's
		for (PotionEffect type : new ArrayList<PotionEffect>(player.getActivePotionEffects())) {
			player.removePotionEffect(type.getType());
		}
	}

	public void clearPotionIDType(Player player, PotionID id) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionIDType(player, id);
		}
	}

	public void clearPotionEffectType(Player player, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionEffectType(player, type);
		}
		player.removePotionEffect(type);
	}

	public void updatePotionStatus(Player player, int ticks) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.updatePotionStatus(player, ticks);
		}
	}

	public JsonObject getAsJsonObject(Player player) {
		PlayerPotionInfo info = mPlayerPotions.get(player.getUniqueId());
		if (info != null) {
			return info.getAsJsonObject();
		}

		return null;
	}

	public void loadFromJsonObject(Player player, JsonObject object) throws Exception {
		JsonElement potionInfo = object.get("potion_info");
		if (potionInfo != null) {
			clearAllPotions(player);

			PlayerPotionInfo info = new PlayerPotionInfo();
			info.loadFromJsonObject(potionInfo.getAsJsonObject());

			mPlayerPotions.put(player.getUniqueId(), info);
		}
	}

	public void loadFromPlayer(Player player) {
		mPlayerPotions.remove(player.getUniqueId());

		for (PotionEffect type : player.getActivePotionEffects()) {
			/*
			 * Assume that any potions greater than 30 minutes were not
			 * potions the player drank - and clear them from the player
			 */
			if (type.getDuration() < Constants.THIRTY_MINUTES) {
				addPotion(player, PotionID.APPLIED_POTION, type);
			} else {
				player.removePotionEffect(type.getType());
			}
		}
	}

	public String printInfo(Player player) {
		JsonObject object = getAsJsonObject(player);
		if (object != null) {

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			return gson.toJson(object);
		} else {
			return "{}";
		}
	}
}
