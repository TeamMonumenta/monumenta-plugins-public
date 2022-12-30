package com.playmonumenta.plugins.gallery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.effects.GalleryEffect;
import com.playmonumenta.plugins.utils.FileUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class GalleryManager implements Listener {

	public static final String TAG_MOB_ELITE = "GalleryElite";
	public static final String TAG_MOB_IGNORE_SCALING = "GalleryIgnoreScaling";
	public static final String MOB_TAG_FROM_SPAWNER = "GalleryMobFromSpawner";
	public static final String TAG_UTIL_LOCATION = "GalleryUtilLocation";
	public static final String TAG_SPAWNING_LOC = "GalleryUtilSpawn";
	public static final String TAG_DEAD_BOX_LOC = "GalleryUtilDeadBox";
	protected static final Map<UUID, GalleryGame> GAMES = new HashMap<>();
	@SuppressWarnings("NullAway.Init")
	public static Plugin mPlugin;

	private static @Nullable BukkitRunnable mRunnable = null;

	public GalleryManager(Plugin plugin) {
		mPlugin = plugin;

		loadOldGames();
		ensureRunnable();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onPlayerSwapEvent(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		if (GalleryUtils.isHoldingTrinket(player)) {
			event.setCancelled(true);
			GalleryGame game = GAMES.get(player.getWorld().getUID());
			if (game != null) {
				game.playerInteractWithObject(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		GalleryGame game = GAMES.get(player.getWorld().getUID());
		if (game != null) {
			game.playerQuitEvent(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		GalleryGame game = GAMES.get(player.getWorld().getUID());
		if (game != null) {
			game.playerJoinEvent(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		GalleryGame game = GAMES.get(entity.getWorld().getUID());
		if (game == null) {
			return;
		}

		if (entity instanceof Player player && event instanceof PlayerDeathEvent playerDeathEvent) {
			game.playerDeathEvent(playerDeathEvent, player, ticks);
		} else {
			game.mobDeathEvent(event, entity, ticks);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplodeEvent(EntityExplodeEvent event) {
		Entity entity = event.getEntity();

		GalleryGame game = GAMES.get(entity.getWorld().getUID());
		if (game == null) {
			return;
		}
		if (entity instanceof Creeper) {
			event.blockList().clear();
		}
		game.onMobExplodeEvent(event, entity, ticks);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();
		GalleryGame game = GAMES.get(damagee.getWorld().getUID());

		if (game == null) {
			return;
		}

		if (damagee instanceof Player player) {
			game.onPlayerHurtEvent(event, player, damager, source);
		}

		if (source instanceof Player player) {
			game.onPlayerDamageEvent(event, player, damagee);
		}

		if (damager instanceof Player player) {
			game.onPlayerDamageEvent(event, player, damagee);
		}

	}

	protected static int ticks = 0;

	private static void ensureRunnable() {
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				@Override public void run() {
					boolean oneHertz = ticks % 20 == 0;
					boolean twoHertz = ticks % 10 == 0;
					GalleryGame crashedGame = null;
					try {
						//we need to make a copy since game.tick(..) could remove the game from GAMES
						for (GalleryGame game : new ArrayList<>(GAMES.values())) {
							crashedGame = game;
							game.tick(oneHertz, twoHertz, ticks);
						}
					} catch (Exception e) {
						if (crashedGame != null) {
							//GalleryGame.tick(..) should not throws any exception.
							//but if it throws any, means something HARD BROKE!
							//print all the info about what game broke and what its status then stop that game.
							GalleryUtils.printDebugMessage("GalleryGame.tick(..) BROKE! saving game status to: " + crashedGame.mUUIDGame.toString() + ".json");
							e.printStackTrace();
							removeGame(crashedGame);
							try {
								FileUtils.writeJson(mPlugin.getDataFolder() + "/Gallery/Crashed/" + crashedGame.mUUIDGame + ".json", crashedGame.toJson());
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}

					ticks++;
				}
			};
			mRunnable.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public static void addGame(GalleryGame game) {
		GAMES.put(game.mUUIDGame, game);
		ensureRunnable();
	}

	public static void removeGame(GalleryGame game) {
		GAMES.remove(game.mUUIDGame);
	}

	public static @Nullable GalleryGame getGame(Player player) {
		return GAMES.get(player.getWorld().getUID());
	}

	public static @Nullable GalleryPlayer getGalleryPlayer(Player player) {
		GalleryGame game = getGame(player);
		if (game != null) {
			return game.getGalleryPlayer(player.getUniqueId());
		}
		return null;
	}

	public static List<GalleryEffect> getGalleryEffects(Player player) {
		List<GalleryEffect> effects = new ArrayList<>();
		GalleryPlayer galleryPlayer = getGalleryPlayer(player);
		if (galleryPlayer != null) {
			effects.addAll(galleryPlayer.getAllEffects());
		}
		return effects;
	}

	public static void refreshEffects(Player player) {
		GalleryPlayer galleryPlayer = getGalleryPlayer(player);
		if (galleryPlayer != null) {
			galleryPlayer.refreshEffects();
		}
	}

	public static void close() {
		JsonObject games = new JsonObject();
		JsonArray gameArr = new JsonArray();
		games.add("games", gameArr);
		for (GalleryGame game : GAMES.values()) {
			game.saveObjects();
			gameArr.add(game.toJson());
		}
		try {
			FileUtils.writeJson(mPlugin.getDataFolder() + "/Gallery/GalleryGames.json", games);
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Error while saving files - This is a serious bug! Reason: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadOldGames() {
		JsonObject obj = null;
		try {
			obj = FileUtils.readJson(mPlugin.getDataFolder() + "/Gallery/GalleryGames.json");
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Exception while reading the file with old games! if this happen on weekly update is not a bug! reason: " + e.getMessage());
		}

		if (obj == null) {
			return;
		}
		JsonArray arr = obj.get("games").getAsJsonArray();

		for (JsonElement element : arr) {
			try {
				GalleryGame game = GalleryGame.fromJson(element.getAsJsonObject());
				GAMES.put(game.mUUIDGame, game);
			} catch (Exception e) {
				GalleryUtils.printDebugMessage("Error while converting json to game - This is a serious bug! Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

		GalleryUtils.printDebugMessage("Loaded " + GAMES.size() + " old games");

	}


}
