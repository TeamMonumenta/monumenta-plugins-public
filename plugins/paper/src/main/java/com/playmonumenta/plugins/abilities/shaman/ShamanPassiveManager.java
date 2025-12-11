package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.SpiritcatcherOrbs;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ShamanPassiveManager extends Ability {
	private static final HashMap<UUID, List<LivingEntity>> TOTEM_LIST = new HashMap<>();
	private static final HashMap<LivingEntity, TotemAbility> TOTEM_ABILITY_MAP = new HashMap<>(); // Maps all existing totems to their corresponding abilities

	public static final AbilityInfo<ShamanPassiveManager> INFO =
		new AbilityInfo<>(ShamanPassiveManager.class, "Shaman Passive Manager", ShamanPassiveManager::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Shaman.CLASS_ID);

	public ShamanPassiveManager(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void addTotem(Player player, LivingEntity stand, TotemAbility ability) {
		TOTEM_LIST.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).add(stand);
		TOTEM_ABILITY_MAP.put(stand, ability);
	}

	public static List<LivingEntity> getTotemList(Player player) {
		List<LivingEntity> totemList = TOTEM_LIST.get(player.getUniqueId());
		if (totemList != null) {
			return totemList;
		}
		return new ArrayList<>();
	}

	public static List<Location> getTotemLocations(Player player) {
		List<LivingEntity> totems = TOTEM_LIST.get(player.getUniqueId());
		ArrayList<Location> locs = new ArrayList<>();
		if (totems == null || totems.isEmpty()) {
			return locs;
		}
		for (LivingEntity totem : totems) {
			locs.add(totem.getEyeLocation());
		}
		return locs;
	}

	public static void removeTotem(Player player, LivingEntity stand) {
		List<LivingEntity> totemList = TOTEM_LIST.get(player.getUniqueId());
		if (totemList != null) {
			if (stand.isValid() && !player.isDead()) {
				tryProcSpiritcatcherOrbs(stand.getLocation(), player);
			}
			totemList.remove(stand);
		}
		TOTEM_ABILITY_MAP.remove(stand);
		stand.remove();
	}

	private static void tryProcSpiritcatcherOrbs(Location standLocation, Player player) {
		SpiritcatcherOrbs orbs = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, SpiritcatcherOrbs.class);
		if (orbs != null) {
			orbs.summonOrbs(standLocation);
		}
	}

	private static Description<ShamanPassiveManager> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Shaman passive manager, if you see this text something is wrong.");
	}

	public @Nullable List<Player> getPlayersInTotemRanges() {
		List<LivingEntity> activeList = TOTEM_LIST.get(mPlayer.getUniqueId());
		if (activeList == null || activeList.isEmpty()) {
			return null;
		}

		Set<Player> playerList = new HashSet<>();
		for (LivingEntity totem : activeList) {
			TotemAbility totemAbility = TOTEM_ABILITY_MAP.get(totem);
			if (totemAbility == null) {
				continue;
			}
			playerList.addAll(totemAbility.getPlayersInRange());
		}

		return playerList.stream().toList();
	}

	public List<TotemAbility> getTotemAbilities() {
		List<LivingEntity> activeList = TOTEM_LIST.get(mPlayer.getUniqueId());
		List<TotemAbility> abilityList = new ArrayList<>();

		if (activeList == null) {
			return abilityList;
		}

		for (LivingEntity totem : activeList) {
			if (TOTEM_ABILITY_MAP.containsKey(totem)) {
				abilityList.add(TOTEM_ABILITY_MAP.get(totem));
			}
		}

		return abilityList;
	}

	public static @Nullable TotemAbility getTotemAbility(LivingEntity totem) {
		return TOTEM_ABILITY_MAP.getOrDefault(totem, null);
	}
}
