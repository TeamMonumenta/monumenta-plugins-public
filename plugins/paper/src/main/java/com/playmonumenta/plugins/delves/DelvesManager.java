package com.playmonumenta.plugins.delves;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.delves.abilities.Chivalrous;
import com.playmonumenta.plugins.delves.abilities.Colossal;
import com.playmonumenta.plugins.delves.abilities.Infernal;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DelvesManager implements Listener {
	public static final String KEY_DELVES_PLUGIN_DATA = "MonumentaDelves";

	/**
	 * This structure contains All the delves mods picked by online players (in this shard)
	 *
	 * it's structured in this way:
	 * <PlayerID, <DungeonID, [TimeWhenStarted, <ModifierID, points>]>>
	 */
	public static final Map<UUID, Map<String, DungeonDelveInfo>> PLAYER_DELVE_DUNGEON_MOD_MAP = new HashMap<>();


	public static final Set<String> DUNGEONS = new HashSet<>();

	//List of all the shard where we can use delves
	static {
		DUNGEONS.add("white");
		DUNGEONS.add("orange");
		DUNGEONS.add("magenta");
		DUNGEONS.add("lightblue");
		DUNGEONS.add("yellow");
		DUNGEONS.add("willows");
		DUNGEONS.add("reverie");
		DUNGEONS.add("lime");
		DUNGEONS.add("pink");
		DUNGEONS.add("gray");
		DUNGEONS.add("lightgray");
		DUNGEONS.add("cyan");
		DUNGEONS.add("purple");
		DUNGEONS.add("teal");
		DUNGEONS.add("forum");
		DUNGEONS.add("shiftingcity");
		DUNGEONS.add("dev1");
		DUNGEONS.add("dev2");
		DUNGEONS.add("dev3");
		DUNGEONS.add("mobs");
		DUNGEONS.add("depths");
	}

	private static final String HAS_DELVE_MODIFIER_TAG = "DelveModifiersApplied";
	public static final String AVOID_MODIFIERS = "boss_delveimmune";

	public static int getRank(Player player, DelvesModifier modifier) {
		if (player == null || modifier == null) {
			return 0;
		}

		if (PLAYER_DELVE_DUNGEON_MOD_MAP.containsKey(player.getUniqueId())) {
			Map<String, DungeonDelveInfo> playerDDinfo = PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(player.getUniqueId(), new HashMap<>());
			if (playerDDinfo.containsKey(ServerProperties.getShardName())) {
				DungeonDelveInfo info = playerDDinfo.get(ServerProperties.getShardName());

				return info.get(modifier);
			}
		}
		return 0;
	}

	//return all players running this dungeon/poi
	protected static List<Player> getParty(Location loc) {
		List<Player> players = new ArrayList<>(PlayerUtils.playersInLootScalingRange(loc));

		if (players.isEmpty()) {
			//somehow a mobs delve mobs spawned with a distance longer then MAX_PARTY_DISTANCE from any player - probably a bug.
			//make a log and add the nearest player to party
			List<Player> playerInWorld = new ArrayList<>(loc.getWorld().getPlayers());
			playerInWorld.sort((player1, player2) -> (int) (loc.distance(player1.getLocation()) - loc.distance(player2.getLocation())));
			Player player = playerInWorld.get(0);
			MMLog.warning("[DelveManager] Party Empty when spawned a delveMob - Real Distance: " + loc.distance(player.getLocation()));
			players.add(player);
		}

		return players;

	}

	private static void loadPlayerData(Player player) {
		JsonObject obj;
		try {
			obj = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_DELVES_PLUGIN_DATA);
		} catch (Exception e) {
			MMLog.warning("[DelveManager] error while loading player info. Reason: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		if (obj == null) {
			return;
		}

		JsonArray dungeonArr = obj.getAsJsonArray("dungeons");
		if (dungeonArr == null || dungeonArr.size() == 0) {
			return;
		}

		PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(player.getUniqueId(), new HashMap<>());

		for (int i = 0; i < dungeonArr.size(); i++) {
			JsonObject dungeonObj = dungeonArr.get(i).getAsJsonObject();
			try {
				String name = dungeonObj.get("dungeonName").getAsString();

				DungeonDelveInfo info = new DungeonDelveInfo();

				JsonArray delveModArr = dungeonObj.getAsJsonArray("delveMods");
				if (delveModArr == null) {
					continue;
				}
				for (int delveIterator = 0; delveIterator < delveModArr.size(); delveIterator++) {
					JsonObject delveObj = delveModArr.get(delveIterator).getAsJsonObject();
					DelvesModifier delveMod = DelvesModifier.fromName(delveObj.getAsJsonPrimitive("delveModName").getAsString());
					if (delveMod == null) {
						continue;
					}

					int lvl = delveObj.getAsJsonPrimitive("delveModLvl").getAsInt();

					if (lvl > 0) {
						info.put(delveMod, lvl);
					}


				}

				PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()).putIfAbsent(name, info);
				DelvesUtils.updateDelveScoreBoard(player);
			} catch (Exception e) {
				MMLog.warning("[DelveManager] error while loading player info. Reason: " + e.getMessage());
				e.printStackTrace();
				player.sendMessage(Component.text("Some of your delve data have not loaded correctly, try reloading!", NamedTextColor.RED).hoverEvent(HoverEvent.showText(Component.text(e.toString()))));
			}
		}
	}

	public static void savePlayerData(Player player, String dungeon, Map<DelvesModifier, Integer> mods) {
		PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(player.getUniqueId(), new HashMap<>());

		Map<String, DungeonDelveInfo> playerDungeonInfo = PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId());

		DungeonDelveInfo ddinfo = playerDungeonInfo.getOrDefault(dungeon, new DungeonDelveInfo());
		ddinfo.mModifierPoint.clear();
		for (DelvesModifier mod : mods.keySet()) {
			ddinfo.mModifierPoint.put(mod, mods.get(mod));
		}
		ddinfo.recalculateTotalPoint();
		playerDungeonInfo.put(dungeon, ddinfo);
		PLAYER_DELVE_DUNGEON_MOD_MAP.put(player.getUniqueId(), playerDungeonInfo);
	}

	protected static JsonObject convertPlayerData(Player player) {
		if (PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()) == null) {
			return null;
		}

		JsonObject obj = new JsonObject();
		JsonArray dungeons = new JsonArray();
		obj.add("dungeons", dungeons);

		for (Map.Entry<String, DungeonDelveInfo> playerDungeonInfo : PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()).entrySet()) {
			JsonObject dungeonObj = new JsonObject();
			dungeonObj.addProperty("dungeonName", playerDungeonInfo.getKey());

			JsonArray delveModArr = new JsonArray();
			dungeonObj.add("delveMods", delveModArr);
			for (Map.Entry<DelvesModifier, Integer> modLevelEntry : playerDungeonInfo.getValue().mModifierPoint.entrySet()) {
				JsonObject dungeonMod = new JsonObject();
				dungeonMod.addProperty("delveModName", modLevelEntry.getKey().name());
				dungeonMod.addProperty("delveModLvl", modLevelEntry.getValue());
				delveModArr.add(dungeonMod);
			}
			dungeons.add(dungeonObj);

		}

		return obj;
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		JsonObject data = convertPlayerData(event.getPlayer());
		if (data != null) {
			event.setPluginData(KEY_DELVES_PLUGIN_DATA, data);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entitySpawnEvent(EntitySpawnEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}
		Entity entity = event.getEntity();

		/*
		 * Since this intercepts the CreatureSpawnEvent and SpawnerSpawnEvent,
		 * it seems that some modifiers get applied twice, which is annoying.
		 *
		 * Making sure that this only runs once per entity just future proofs
		 * it in case more events get added along the line that extend the
		 * EntitySpawnEvent.
		 */
		Set<String> tags = entity.getScoreboardTags();
		if (tags.contains(HAS_DELVE_MODIFIER_TAG) || tags.contains(AVOID_MODIFIERS)) {
			return;
		}

		if (entity instanceof LivingEntity livingEntity) {
			PotionEffect resistance = livingEntity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			if (resistance != null && (resistance.getAmplifier() > 4 || resistance.getDuration() > 20 * 7)) {
				return;
			}
		}

		if (EntityUtils.isHostileMob(entity) && entity instanceof LivingEntity livingEntity) {
			List<Player> playerParty = getParty(entity.getLocation());

			if (!playerParty.isEmpty()) {
				Map<DelvesModifier, Integer> delvesApplied = new HashMap<>();

				for (Player delvePlayer : playerParty) {
					for (DelvesModifier mod : DelvesModifier.values()) {
						delvesApplied.put(mod, Math.max(delvesApplied.getOrDefault(mod, 0), getRank(delvePlayer, mod)));
					}
				}


				int totalLevel = 0;
				List<DelvesModifier> mods = DelvesModifier.valuesList();
				mods.remove(DelvesModifier.ENTROPY);
				mods.remove(DelvesModifier.TWISTED);

				for (DelvesModifier mod : mods) {
					mod.applyDelve(livingEntity, delvesApplied.getOrDefault(mod, 0));
					totalLevel += delvesApplied.getOrDefault(mod, 0);
				}

				DelvesModifier.TWISTED.applyDelve(livingEntity, delvesApplied.getOrDefault(DelvesModifier.TWISTED, 0));
				totalLevel += (delvesApplied.getOrDefault(DelvesModifier.TWISTED, 0) * DelvesUtils.TWISTED_DEPTH_POINTS);
				//twisted point value 5 time a normal one
				livingEntity.addScoreboardTag(HAS_DELVE_MODIFIER_TAG);

				StatMultiplier.applyModifiers(livingEntity, totalLevel);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		//load the delves
		loadPlayerData(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PLAYER_DELVE_DUNGEON_MOD_MAP.remove(player.getUniqueId());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerHurt(DamageEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}

		if (event.getDamagee() instanceof Player delvePlayer) {
			int maxInfernal = 0;

			for (Player player : getParty(delvePlayer.getLocation())) {
				maxInfernal = Math.max(maxInfernal, getRank(player, DelvesModifier.INFERNAL));
			}
			Infernal.applyDamageModifiers(event, maxInfernal);

		}

		//hard coded since magma cubes from Chivalrous should not do damage to player
		if (event.getDamager() != null && Chivalrous.MOUNT_NAMES[1].equals(event.getDamager().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}
		if (event.getBlock().getType() != Material.SPAWNER) {
			return;
		}

		int maxColossal = 0;
		Location loc = event.getBlock().getLocation();

		for (Player player : getParty(loc)) {
			maxColossal = Math.max(maxColossal, getRank(player, DelvesModifier.COLOSSAL));
		}

		Colossal.applyModifiers(loc, maxColossal);

	}



	//utility class to store date and modifier lvl on each player
	public static class DungeonDelveInfo {
		private final Map<DelvesModifier, Integer> mModifierPoint = new HashMap<>();

		public int mTotalPoint = 0;

		@Override public String toString() {
			return "DungeonDelveInfo{" +
					   "TotalPoint=" + mTotalPoint +
				       ",ModifierPoint=" + mModifierPoint +
				       '}';
		}

		protected DungeonDelveInfo cloneDelveInfo() {
			DungeonDelveInfo info = new DungeonDelveInfo();
			info.mModifierPoint.putAll(this.mModifierPoint);
			info.mTotalPoint = this.mTotalPoint;
			return info;
		}

		public void put(DelvesModifier mod, int level) {
			mModifierPoint.put(mod, level);
			recalculateTotalPoint();
		}

		public int get(DelvesModifier mod) {
			return mModifierPoint.getOrDefault(mod, 0);
		}

		public void recalculateTotalPoint() {
			mTotalPoint = 0;
			for (Map.Entry<DelvesModifier, Integer> entry : mModifierPoint.entrySet()) {
				if (entry.getKey() == DelvesModifier.TWISTED) {
					mTotalPoint += DelvesUtils.TWISTED_DEPTH_POINTS;
				} else if (entry.getKey() != DelvesModifier.ENTROPY) {
					mTotalPoint += entry.getValue();
				}
			}
			mTotalPoint = Math.min(mTotalPoint, DelvesUtils.MAX_DEPTH_POINTS);
		}
	}
}
