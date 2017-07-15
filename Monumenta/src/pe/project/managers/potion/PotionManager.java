package pe.project.managers.potion;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.Gson;

import pe.project.Main;
import pe.project.json.objects.PotionManagerObject;
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
		final String fileLocation = mPlugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";
		
		try {
			String content = FileUtils.getCreateFile(fileLocation);
			if (content != null) {
				Gson gson = new Gson();

				PotionManagerObject object = gson.fromJson(content, PotionManagerObject.class);
				
				if (object != null) {
					HashMap<UUID, PlayerPotionInfo> playerPotionInfo = object.convertToPotionManager();
					
					mPotionManager = playerPotionInfo;
					
					Collection<PotionEffect> effects = player.getActivePotionEffects();
					for (PotionEffect effect : effects) {
						player.removePotionEffect(effect.getType());
					}
				}
			}
		} catch(Exception e) {
			Bukkit.broadcastMessage("Load Player Failed - " + e.getMessage());
		}
		
		//	Refresh the players class effects.
		refreshClassEffects(player);
	}
	
	public void savePlayerPotionData(Player player) {
		if (mPlugin != null) {
			final String fileLocation = mPlugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";
			
			try {
				String content = FileUtils.getCreateFile(fileLocation);
				if (content != null) {
					Gson gson = new Gson();

					PotionManagerObject object = new PotionManagerObject(mPotionManager);

					String jsonStr = gson.toJson(object, PotionManagerObject.class);
					
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
		PlayerPotionInfo potionInfo = mPlugin.mPotionManager.mPotionManager.get(uuid);
		if (potionInfo != null) {
			potionInfo.addPotionInfo(player, id, info);	
		} else {
			PlayerPotionInfo newPotionInfo = new PlayerPotionInfo();
			newPotionInfo.addPotionInfo(player, id, info);
			mPlugin.mPotionManager.mPotionManager.put(uuid, newPotionInfo);
		}
	}
	
	public void removePotion(Player player, PotionID id, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPlugin.mPotionManager.mPotionManager.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.removePotionInfo(player, id, type);
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
}
