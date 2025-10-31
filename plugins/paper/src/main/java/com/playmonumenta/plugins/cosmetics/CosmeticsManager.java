package com.playmonumenta.plugins.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.cosmetics.finishers.PlayingFinisher;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.cosmetics.poses.GravePoses;
import com.playmonumenta.plugins.cosmetics.punches.PlayerPunches;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CosmeticsManager implements Listener {

	public static final String KEY_PLUGIN_DATA = "Cosmetics";
	public static final String KEY_COSMETICS = "cosmetics";
	public static final String KEY_BULLY_PUNCH_COOLDOWN = "bully_punch_cosmetic_cooldown";
	public static final String KEY_VICTIM_PUNCH_COOLDOWN = "victim_punch_cosmetic_cooldown";
	public static final String KEY_OPT_OUT_PUNCH_COOLDOWN = "opt_out_punch_cosmetic_cooldown";

	private static final long BULLY_PUNCH_COOLDOWN = 30000; // 30 seconds
	private static final long VICTIM_PUNCH_COOLDOWN = 60000; // 60 seconds
	public static final long OPT_OUT_PUNCH_COOLDOWN = 60000; // 60 seconds

	public static final CosmeticsManager INSTANCE = new CosmeticsManager();

	public final Map<UUID, List<Cosmetic>> mPlayerCosmetics = new HashMap<>();
	public final Map<UUID, PlayingFinisher> mPlayingFinishers = new HashMap<>();

	private final Map<UUID, Long> mBullyPunchCooldowns = new HashMap<>();
	private final Map<UUID, Long> mVictimPunchCooldowns = new HashMap<>();
	public final Map<UUID, Long> mOptOutPunchCooldowns = new HashMap<>();

	private CosmeticsManager() {
	}

	public static CosmeticsManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns true if the list contains the cosmetic with given name and type
	 */
	private boolean listHasCosmetic(List<Cosmetic> cosmetics, CosmeticType type, String name) {
		for (Cosmetic c : cosmetics) {
			if (c.getName().equals(name) && c.getType() == type) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the player has unlocked the cosmetic with given name and type
	 * This is called by external methods such as plot border GUI
	 */
	public boolean playerHasCosmetic(Player player, CosmeticType type, @Nullable String name) {
		if (name == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			return listHasCosmetic(playerCosmetics, type, name);
		}
		return false;
	}

	public boolean addCosmetic(@Nullable Player player, CosmeticType type, String name) {
		return addCosmetic(player, type, name, false);
	}

	/**
	 * Unlocks a new cosmetic for the player with given name and type.
	 * Checks to make sure there isn't a duplicate.
	 */
	public boolean addCosmetic(@Nullable Player player, CosmeticType type, String name, boolean equipImmediately) {
		if (player == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
		if (!listHasCosmetic(playerCosmetics, type, name)) {
			if (type != CosmeticType.COSMETIC_SKILL) {
				Cosmetic c = new Cosmetic(type, name);
				playerCosmetics.add(c);
				if (equipImmediately) {
					CosmeticsGUI.toggleCosmetic(player, c);
				}
				return true;
			} else {
				//Test only
				if (name.equals("ALL")) {
					for (String s : CosmeticSkills.getNames()) {
						playerCosmetics.add(CosmeticSkills.getCosmeticByName(s));
					}
					return true;
				}

				Cosmetic c = CosmeticSkills.getCosmeticByName(name);
				if (c != null) {
					playerCosmetics.add(c);
					if (equipImmediately) {
						CosmeticsGUI.toggleCosmetic(player, c);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Removes the cosmetic of given name from the player's collection
	 */
	public boolean removeCosmetic(Player player, CosmeticType type, String name) {
		if (player == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			for (Cosmetic c : playerCosmetics) {
				if (c.getType() == type && c.getName().equals(name)) {
					playerCosmetics.remove(c);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Clears all the player's cosmetics of a certain type (dangerous!)
	 */
	public void clearCosmetics(Player player, CosmeticType type) {
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			playerCosmetics.removeIf(c -> c.getType() == type);
		}
	}

	public void toggleCosmetic(Player player, Cosmetic cosmetic) {
		equipCosmetic(player, cosmetic, !cosmetic.mEquipped);
	}

	public boolean equipCosmetic(Player player, CosmeticType cosmeticType, String name, boolean equipped) {
		Cosmetic cosmetic = getCosmetic(player, cosmeticType, name);
		if (cosmetic != null) {
			equipCosmetic(player, cosmetic, equipped);
			return true;
		}
		return false;
	}

	public void equipCosmetic(Player player, Cosmetic cosmetic, boolean equipped) {
		if (equipped) {
			if (!cosmetic.getType().canEquipMultiple()) {
				for (Cosmetic c : getCosmeticsOfTypeAlphabetical(player, cosmetic.mType, cosmetic.mAbility)) {
					c.mEquipped = false;
				}
			}
			cosmetic.mEquipped = true;
		} else {
			cosmetic.mEquipped = false;
		}
	}

	/**
	 * Gets a list of all unlocked cosmetics for the given player
	 */
	public List<Cosmetic> getCosmetics(Player player) {
		return mPlayerCosmetics.getOrDefault(player.getUniqueId(), Collections.emptyList());
	}

	/**
	 * Gets a list of unlocked cosmetic of certain type, sorted alphabetically by name
	 */
	public List<Cosmetic> getCosmeticsOfTypeAlphabetical(Player player, CosmeticType type, @Nullable ClassAbility ability) {
		if (type != CosmeticType.COSMETIC_SKILL) {
			return getCosmetics(player).stream()
				.filter(c -> c.getType() == type)
				.sorted(Comparator.comparing(Cosmetic::getName))
				.toList();
		} else if (ability != null) {
			return getCosmetics(player).stream()
				.filter(c -> c.getType() == type)
				.filter(c -> c.getAbility() == ability)
				.sorted(Comparator.comparing(Cosmetic::getName))
				.toList();
		} else {
			return getCosmetics(player).stream()
				.filter(c -> c.getType() == type)
				.sorted(Comparator.comparing(Cosmetic::getName))
				.toList();
		}
	}

	public List<Cosmetic> getCosmeticsOfTypeAlphabetical(Player player, CosmeticType type, @Nullable AbilityInfo<?> ability) {
		return getCosmeticsOfTypeAlphabetical(player, type, ability == null ? null : ability.getLinkedSpell());
	}

	public List<Cosmetic> getCosmeticsOfTypeAlphabetical(Player player, CosmeticType type) {
		return getCosmeticsOfTypeAlphabetical(player, type, (ClassAbility) null);
	}

	public @Nullable Cosmetic getCosmetic(Player player, CosmeticType type, String name) {
		for (Cosmetic cosmetic : getCosmetics(player)) {
			if (cosmetic.getType() == type
				&& cosmetic.getName().equals(name)) {
				return cosmetic;
			}
		}
		return null;
	}

	public @Nullable Cosmetic getCosmetic(Player player, CosmeticType type, String name, @Nullable ClassAbility ability) {
		for (Cosmetic cosmetic : getCosmetics(player)) {
			if (cosmetic.getType() == type
				&& cosmetic.getName().equals(name)
				&& cosmetic.getAbility() == ability) {
				return cosmetic;
			}
		}
		return null;
	}

	/**
	 * Gets the currently equipped cosmetic of given type for the player
	 */
	public @Nullable Cosmetic getActiveCosmetic(Player player, CosmeticType type) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			for (Cosmetic c : cosmetics) {
				if (c.getType() == type && c.isEquipped()) {
					return c;
				}
			}
		}
		return null;
	}

	public List<Cosmetic> getActiveCosmetics(Player player, CosmeticType type) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			return cosmetics.stream().filter(c -> c.getType() == type && c.isEquipped()).toList();
		}
		return Collections.emptyList();
	}

	public List<Cosmetic> getActiveCosmetics(Player player, CosmeticType type, @Nullable ClassAbility ability) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			return cosmetics.stream().filter(c -> c.getType() == type && c.isEquipped() && c.getAbility() == ability).toList();
		}
		return Collections.emptyList();
	}

	public @Nullable Cosmetic getRandomActiveCosmetic(Player player, CosmeticType type) {
		List<Cosmetic> activeCosmetics = getActiveCosmetics(player, type);
		if (!activeCosmetics.isEmpty()) {
			return activeCosmetics.get(FastUtils.RANDOM.nextInt(activeCosmetics.size()));
		}
		return null;
	}

	// Get current cosmetic skill of an ability
	public String getSkillCosmeticName(@Nullable Player player, ClassAbility ability) {
		if (player == null) {
			return "";
		}

		List<Cosmetic> activeCosmetics = getActiveCosmetics(player, CosmeticType.COSMETIC_SKILL);
		if (activeCosmetics != null) {
			for (Cosmetic c : activeCosmetics) {
				if (c.isEquipped() && c.getAbility() == ability) {
					return c.getName();
				}
			}
		}
		return "";
	}

	public void registerPlayingFinisher(PlayingFinisher playingFinisher) {
		PlayingFinisher lastFinisher = mPlayingFinishers.put(playingFinisher.playerUuid(), playingFinisher);
		if (lastFinisher != null) {
			lastFinisher.cancel();
		}
	}

	public void cancelPlayingFinisher(Player player) {
		PlayingFinisher playingFinisher = mPlayingFinishers.remove(player.getUniqueId());
		if (playingFinisher != null) {
			playingFinisher.cancel();
		}
	}

	//Handlers for player lifecycle events

	//Discard cosmetic and punch cooldown data a few ticks after player leaves shard
	//(give time for save event to register)
	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		cancelPlayingFinisher(player);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mPlayerCosmetics.remove(playerUuid);
				mBullyPunchCooldowns.remove(playerUuid);
				mVictimPunchCooldowns.remove(playerUuid);
				mOptOutPunchCooldowns.remove(playerUuid);
			}
		}, 100);
	}

	//Store local cosmetic data into plugin data
	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		List<Cosmetic> cosmetics = mPlayerCosmetics.get(playerUuid);
		if (cosmetics != null) {
			JsonObject data = new JsonObject();
			JsonArray cosmeticArray = new JsonArray();
			data.add(KEY_COSMETICS, cosmeticArray);

			for (Cosmetic cosmetic : cosmetics) {
				JsonObject cosmeticObj = new JsonObject();
				cosmeticObj.addProperty("name", cosmetic.getName());
				cosmeticObj.addProperty("type", cosmetic.getType().getType());
				cosmeticObj.addProperty("enabled", cosmetic.isEquipped());
				if (cosmetic.getAbility() != null) {
					cosmeticObj.addProperty("ability", cosmetic.getAbility().getName());
				}
				cosmeticArray.add(cosmeticObj);
			}

			if (mBullyPunchCooldowns.containsKey(playerUuid)) {
				data.addProperty(KEY_BULLY_PUNCH_COOLDOWN, mBullyPunchCooldowns.get(playerUuid));
			}

			if (mVictimPunchCooldowns.containsKey(playerUuid)) {
				data.addProperty(KEY_VICTIM_PUNCH_COOLDOWN, mVictimPunchCooldowns.get(playerUuid));
			}

			if (mOptOutPunchCooldowns.containsKey(playerUuid)) {
				data.addProperty(KEY_OPT_OUT_PUNCH_COOLDOWN, mOptOutPunchCooldowns.get(playerUuid));
			}

			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local cosmetic data
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID playerUuid = player.getUniqueId();

		JsonObject cosmeticData = MonumentaRedisSyncAPI.getPlayerPluginData(playerUuid, KEY_PLUGIN_DATA);
		if (cosmeticData != null) {
			if (cosmeticData.has(KEY_COSMETICS)) {
				JsonArray cosmeticArray = cosmeticData.getAsJsonArray(KEY_COSMETICS);
				List<Cosmetic> playerCosmetics = new ArrayList<>();
				for (JsonElement cosmeticElement : cosmeticArray) {
					JsonObject data = cosmeticElement.getAsJsonObject();
					if (data.has("name") && data.has("type") && data.has("enabled")) {
						CosmeticType type = CosmeticType.getTypeSelection(data.getAsJsonPrimitive("type").getAsString());
						if (type == null) {
							MMLog.warning("Discarding cosmetic of unknown type " + data.getAsJsonPrimitive("type").getAsString());
							continue;
						}
						Cosmetic toAdd = new Cosmetic(type, data.getAsJsonPrimitive("name").getAsString(),
							data.getAsJsonPrimitive("enabled").getAsBoolean(), null);
						if (data.has("ability")) {
							toAdd.mAbility = ClassAbility.getAbility(data.getAsJsonPrimitive("ability").getAsString());
						}

						// Renamed cosmetic. There's currently no simple automation way to change these, and only few people have this skin,
						// so this can just be left in for a week or two and then removed.
						if (toAdd.mAbility == ClassAbility.ALCHEMICAL_ARTILLERY && "Artillery Bomb".equals(toAdd.mName)) {
							toAdd.mName = "Arcane Artillery";
						}

						playerCosmetics.add(toAdd);
					}
				}
				//Check if we actually loaded any cosmetics
				if (!playerCosmetics.isEmpty()) {
					mPlayerCosmetics.put(playerUuid, playerCosmetics);
				}
			}

			// Load punch cooldowns
			if (cosmeticData.has(KEY_BULLY_PUNCH_COOLDOWN)) {
				mBullyPunchCooldowns.put(playerUuid, cosmeticData.get(KEY_BULLY_PUNCH_COOLDOWN).getAsLong());
			}

			if (cosmeticData.has(KEY_VICTIM_PUNCH_COOLDOWN)) {
				mVictimPunchCooldowns.put(playerUuid, cosmeticData.get(KEY_VICTIM_PUNCH_COOLDOWN).getAsLong());
			}

			if (cosmeticData.has(KEY_OPT_OUT_PUNCH_COOLDOWN)) {
				mOptOutPunchCooldowns.put(playerUuid, cosmeticData.get(KEY_OPT_OUT_PUNCH_COOLDOWN).getAsLong());
			}
		}
		// call the "event listener" of the vanity manager after the cosmetics manager loaded cosmetics
		Plugin.getInstance().mVanityManager.playerJoinEvent(event);

		EliteFinishers.handleLogin(player);
		GravePoses.handleLogin(player);
		PlayerPunches.handleLogin(player);
	}

	// Elite Finisher handler
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity mob = event.getEntity();
		Player player = mob.getKiller();

		if (player != null
			&& EntityUtils.isElite(mob)
			&& !mob.getScoreboardTags().contains(EntityUtils.IGNORE_DEATH_TRIGGERS_TAG)) {
			PlayingFinisher playingFinisher = mPlayingFinishers.get(player.getUniqueId());
			if (playingFinisher != null) {
				playingFinisher.registerKill(mob, mob.getLocation());
				return;
			}

			Cosmetic activeCosmetic = getInstance().getRandomActiveCosmetic(player, CosmeticType.ELITE_FINISHER);
			if (activeCosmetic != null) {
				EliteFinishers.activateFinisher(player, mob, mob.getLocation(), activeCosmetic.getName());
			}
		}
	}

	// Player Punch handler
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onPlayerPunch(PlayerAnimationEvent event) {
		if (PlayerPunches.isOnWhitelistedShard()) {
			Player bully = event.getPlayer();
			Cosmetic activeCosmetic = getInstance().getRandomActiveCosmetic(bully, CosmeticType.PLAYER_PUNCH);

			/* The bully cannot:
			- (be less than a tier 1 patron, be a non-moderator, or be a non-developer) and (be opted out of player punches)
			- have an unequipped punch cosmetic
			- be vanished or in spectator mode
			- be holding anything in their mainhand
			 */
			if (!PlayerPunches.canAccess(bully) ||
				activeCosmetic == null ||
				PremiumVanishIntegration.isInvisibleOrSpectator(bully) ||
				!bully.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
				return;
			}

			@Nullable Player victim = getVictimToBully(bully);
			if (victim == null) {
				return;
			}

			UUID bullyUuid = bully.getUniqueId();
			UUID victimUuid = victim.getUniqueId();
			long currentTime = System.currentTimeMillis();

			long lastBullyPunch = mBullyPunchCooldowns.getOrDefault(bullyUuid, 0L);
			if (currentTime - lastBullyPunch < BULLY_PUNCH_COOLDOWN) {
				bully.sendMessage(Component.text("Your punch ability is on cooldown!", NamedTextColor.RED));
				return;
			}

			long lastVictimLaunch = mVictimPunchCooldowns.getOrDefault(victimUuid, 0L);
			if (currentTime - lastVictimLaunch < VICTIM_PUNCH_COOLDOWN) {
				bully.sendMessage(Component.text("This person was recently punched and cannot be punched yet!", NamedTextColor.RED));
				return;
			}

			PlayerPunches.activatePunch(bully, victim, activeCosmetic.getName(), false);
			mBullyPunchCooldowns.put(bullyUuid, currentTime);
			mVictimPunchCooldowns.put(victimUuid, currentTime);
			mOptOutPunchCooldowns.put(bullyUuid, currentTime);
		}
	}

	@Nullable
	private Player getVictimToBully(Player bully) {
		// TODO: The logic to find a target isn't perfect and could use improvements but is currently sufficient
		double maxDistance = 3.0;
		double fovThreshold = 0.8; // 1 = looking directly, 0 = perpendicular, -1 = opposite

		Vector playerDirection = bully.getLocation().getDirection();

		for (Player target : bully.getWorld().getPlayers()) {
			/* The target:
			- cannot be the bully themselves
			- (must be friends with the bully OR be a developer/moderator) and (must not have the bully blocked and vice versa) and (must not be opted out of Player Punches)
			- cannot be vanished or in spectator mode
			 */
			if (target == bully ||
				!PlayerPunches.canBePunched(bully, target) ||
				PremiumVanishIntegration.isInvisibleOrSpectator(target)) {
				continue;
			}

			Vector toTarget = target.getLocation().toVector().subtract(bully.getEyeLocation().toVector()).normalize();

			// Dot product to check if target is in front
			if (playerDirection.dot(toTarget) > fovThreshold &&
				bully.hasLineOfSight(target) &&
				target.getLocation().distance(bully.getEyeLocation()) <= maxDistance) {
				return target;
			}
		}

		return null;
	}
}
