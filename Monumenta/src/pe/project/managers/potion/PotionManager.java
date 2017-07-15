package pe.project.managers.potion;

import java.io.File;
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

import pe.project.Main;
import pe.project.utils.FileUtils;
import pe.project.utils.PotionUtils.PotionInfo;

public class PotionManager {
	Main mPlugin = null;
	//	Player ID / Player Potion Info
	public HashMap<UUID, PlayerPotionInfo> mPotionManager;
	
	public enum PotionID {
		APPLIED_POTION(0, "APPLIED_POTION"),
		ABILITY_SELF(1, "ABILITY_SELF"),
		ABILITY_OTHER(2, "ABILITY_OTHER"),
		SAFE_ZONE(3, "SAFE_ZONE");
		
		private int value;
		private String name;
		private PotionID(int value, String name)	{	this.value = value;	this.name = name;	}
		public int getValue()		{	return value;	}
		public String getName()		{	return name;	}
		
		public static PotionID getFromString(String name) {
			if (name.equals(PotionID.ABILITY_SELF.getName())) {
				return PotionID.ABILITY_SELF;
			} else if (name.equals(PotionID.ABILITY_OTHER.getName())) {
				return PotionID.ABILITY_OTHER;
			} else if (name.equals(PotionID.SAFE_ZONE.getValue())) {
				return PotionID.SAFE_ZONE;
			} else {
				return PotionID.APPLIED_POTION;
			}
		}
	}
	
	public PotionManager(Main plugin) {
		mPlugin = plugin;
		mPotionManager = new HashMap<UUID, PlayerPotionInfo>();
	}
	
	public void loadPlayerPotionData(Player player) {
		clearAllPotions(player, false);
		
		final String fileLocation = mPlugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";
		try {
			String content = FileUtils.getCreateFile(fileLocation);
			if (content != null && content != "") {
				Gson gson = new Gson();

				loadFromJsonObject(player, gson.fromJson(content, JsonObject.class));
			}
		} catch (Exception e) {
		}
		
		refreshClassEffects(player);
	}
	
	public void savePlayerPotionData(Player player) {
		if (mPlugin != null) {
			final String fileLocation = mPlugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";
			
			try {
				String content = FileUtils.getCreateFile(fileLocation);
				if (content != null) {
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					
					JsonObject object = getAsJsonObject(player);
					String jsonStr = gson.toJson(object);
					
					FileUtils.writeFile(fileLocation, jsonStr);
				}
			} catch (Exception e) {
			}
		}
	}
	
	public void addPotion(Player player, PotionID id, Collection<PotionEffect> effects) {
		for (PotionEffect effect : effects) {
			addPotion(player, id, new PotionInfo(effect.getType(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
		}
	}
	
	public void addPotion(Player player, PotionID id, PotionEffect effect) {
		addPotion(player, id, new PotionInfo(effect.getType(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
	}
	
	public void addPotion(Player player, PotionID id, PotionInfo info) {
		UUID uuid = player.getUniqueId();
		PlayerPotionInfo potionInfo = mPotionManager.get(uuid);
		if (potionInfo != null) {
			potionInfo.addPotionInfo(player, id, info);	
		} else {
			PlayerPotionInfo newPotionInfo = new PlayerPotionInfo();
			newPotionInfo.addPotionInfo(player, id, info);
			mPlugin.mPotionManager.mPotionManager.put(uuid, newPotionInfo);
		}
	}
	
	public void removePotion(Player player, PotionID id, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPotionManager.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.removePotionInfo(player, id, type);
		}	
	}
	
	public void clearAllPotions(Player player, boolean fromManager) {
		Collection<PotionEffect> effects = player.getActivePotionEffects();
		for (PotionEffect effect : effects) {
			player.removePotionEffect(effect.getType());
		}
		
		if (fromManager) {
			mPotionManager.remove(player.getUniqueId());
		}
	}
	
	public void clearPotionIDType(Player player, PotionID id) {
		PlayerPotionInfo potionInfo = mPotionManager.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionIDType(player, id);
		}
	}
	
	public void updatePotionStatus(Player player, int ticks) {
		PlayerPotionInfo potionInfo = mPotionManager.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.updatePotionStatus(player, ticks);
		}
	}
	
	public void applyBestPotionEffect(Player player) {
		PlayerPotionInfo potionInfo = mPlugin.mPotionManager.mPotionManager.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.applyBestPotionEffect(player);
		}	
	}
	
	public void refreshClassEffects(Player player) {
		//	We can just get rid of the ABILITY_SELF, it's useless to us as it's possibly unreliable and we want
		//	to refresh the timers on those that should be applied anyways.
		clearPotionIDType(player, PotionID.ABILITY_SELF);
			
		//	Next we want to get this players class and call into an initialization function to make sure they have the correct potion
		//	effect types applied.
		mPlugin.getClass(player).setupClassPotionEffects(player);
			
		//	Once all the potion stuff is setup apply the best effects.
		applyBestPotionEffect(player);
	}
	
	//	TODO: Abstract this out to a general Player Profile so we can have a general player data saving system.
	//	This can be used with Bungee to pass over player data we want to share between servers.
	JsonObject getAsJsonObject(Player player) {
		JsonObject object = new JsonObject();
		
		PlayerPotionInfo info = mPotionManager.get(player.getUniqueId());
		if (info != null) {
			object.add("potion_info", info.getAsJsonObject());
		}
		
		return object;
	}
	
	void loadFromJsonObject(Player player, JsonObject object) {
		JsonElement potionInfo = object.get("potion_info");
		if (potionInfo != null) {
			PlayerPotionInfo info = new PlayerPotionInfo();
			info.loadFromJsonObject(potionInfo.getAsJsonObject());
			
			mPotionManager.put(player.getUniqueId(), info);
		}
	}
}
