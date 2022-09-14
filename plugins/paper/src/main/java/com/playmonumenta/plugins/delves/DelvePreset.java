package com.playmonumenta.plugins.delves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Map.entry;

public enum DelvePreset {

	MOTIVATED(1, "Motivated Mobs", 1, Material.BEACON, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 3),
		entry(DelvesModifier.BLOODTHIRSTY, 3),
		entry(DelvesModifier.DREADFUL, 2)
	)),
	TOUGH(2, "Tough Titans", 1, Material.IRON_CHESTPLATE, Map.ofEntries(
		entry(DelvesModifier.DREADFUL, 3),
		entry(DelvesModifier.CARAPACE, 3),
		entry(DelvesModifier.TRANSCENDENT, 1),
		entry(DelvesModifier.COLOSSAL, 1)
	)),
	BADDIES(3, "Bunch of Baddies", 1, Material.CREEPER_HEAD, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 5),
		entry(DelvesModifier.PERNICIOUS, 3)
	)),
	REMNANTS(4, "Returning Remnants", 1, Material.WITHER_SKELETON_SKULL, Map.ofEntries(
		entry(DelvesModifier.SPECTRAL, 3),
		entry(DelvesModifier.LEGIONARY, 3),
		entry(DelvesModifier.CARAPACE, 2)
	)),
	ARTILLERY(5, "Artillery Attack", 1, Material.SLIME_BLOCK, Map.ofEntries(
		entry(DelvesModifier.CHIVALROUS, 3),
		entry(DelvesModifier.ARCANIC, 2),
		entry(DelvesModifier.DREADFUL, 2)
	)),
	CHAOS(6, "Crumbling Chaos", 2, Material.GRAVEL, Map.ofEntries(
		entry(DelvesModifier.CARAPACE, 5),
		entry(DelvesModifier.DREADFUL, 3),
		entry(DelvesModifier.PERNICIOUS, 3),
		entry(DelvesModifier.LEGIONARY, 3),
		entry(DelvesModifier.SPECTRAL, 1),
		entry(DelvesModifier.COLOSSAL, 1)
	)),
	FERVOR(7, "Flaming Fervor", 2, Material.MAGMA_BLOCK, Map.ofEntries(
		entry(DelvesModifier.INFERNAL, 5),
		entry(DelvesModifier.VENGEANCE, 3),
		entry(DelvesModifier.CHIVALROUS, 3),
		entry(DelvesModifier.PERNICIOUS, 3)
	)),
	ARMAMENTS(8, "Advanced Armaments", 2, Material.IRON_SWORD, Map.ofEntries(
		entry(DelvesModifier.ARCANIC, 5),
		entry(DelvesModifier.LEGIONARY, 5),
		entry(DelvesModifier.TRANSCENDENT, 3),
		entry(DelvesModifier.COLOSSAL, 2)
	)),
	SCUFFLE(9, "Sporadic Shuffle", 2, Material.FEATHER, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 3),
		entry(DelvesModifier.ARCANIC, 3),
		entry(DelvesModifier.INFERNAL, 3),
		entry(DelvesModifier.CHIVALROUS, 3),
		entry(DelvesModifier.BLOODTHIRSTY, 3)
	)),
	UNION(10, "Undying Union", 2, Material.TOTEM_OF_UNDYING, Map.ofEntries(
		entry(DelvesModifier.CARAPACE, 5),
		entry(DelvesModifier.SPECTRAL, 3),
		entry(DelvesModifier.DREADFUL, 3),
		entry(DelvesModifier.VENGEANCE, 2),
		entry(DelvesModifier.BLOODTHIRSTY, 2)
	)),
	SWARM(11, "Superior-less Swarm", 3, Material.DEAD_FIRE_CORAL, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 4),
		entry(DelvesModifier.CARAPACE, 4),
		entry(DelvesModifier.ARCANIC, 3),
		entry(DelvesModifier.INFERNAL, 3),
		entry(DelvesModifier.VENGEANCE, 2),
		entry(DelvesModifier.SPECTRAL, 2),
		entry(DelvesModifier.PERNICIOUS, 2),
		entry(DelvesModifier.BLOODTHIRSTY, 2)
	)),
	ARSENAL(12, "Ability Arsenal", 3, Material.END_CRYSTAL, Map.ofEntries(
		entry(DelvesModifier.ARCANIC, 5),
		entry(DelvesModifier.INFERNAL, 5),
		entry(DelvesModifier.LEGIONARY, 3),
		entry(DelvesModifier.VENGEANCE, 3),
		entry(DelvesModifier.TRANSCENDENT, 3),
		entry(DelvesModifier.CHIVALROUS, 3)
	)),
	TWO(13, "Take Two", 3, Material.ROSE_BUSH, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 2),
		entry(DelvesModifier.ARCANIC, 2),
		entry(DelvesModifier.INFERNAL, 2),
		entry(DelvesModifier.TRANSCENDENT, 2),
		entry(DelvesModifier.SPECTRAL, 2),
		entry(DelvesModifier.DREADFUL, 2),
		entry(DelvesModifier.COLOSSAL, 2),
		entry(DelvesModifier.CHIVALROUS, 2),
		entry(DelvesModifier.BLOODTHIRSTY, 2),
		entry(DelvesModifier.LEGIONARY, 2),
		entry(DelvesModifier.CARAPACE, 2)
	)),
	EMPIRE(14, "Elite Empire", 3, Material.GOLD_INGOT, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 5),
		entry(DelvesModifier.CARAPACE, 4),
		entry(DelvesModifier.TRANSCENDENT, 3),
		entry(DelvesModifier.DREADFUL, 3),
		entry(DelvesModifier.COLOSSAL, 3),
		entry(DelvesModifier.ARCANIC, 2),
		entry(DelvesModifier.INFERNAL, 2)
	)),
	DEMISE(15, "Dreadful Demise", 3, Material.CRIMSON_NYLIUM, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 5),
		entry(DelvesModifier.SPECTRAL, 3),
		entry(DelvesModifier.DREADFUL, 3),
		entry(DelvesModifier.COLOSSAL, 3),
		entry(DelvesModifier.BLOODTHIRSTY, 3),
		entry(DelvesModifier.CARAPACE, 3),
		entry(DelvesModifier.PERNICIOUS, 2)
	));

	public static final String PRESET_SCOREBOARD = "Daily3DelvePreset";

	public final String mName;
	public final int mLevel;
	public final Map<DelvesModifier, Integer> mModifiers;
	public final int mId;
	public final Material mDisplayItem;

	DelvePreset(int id, String name, int level, Material displayItem, Map<DelvesModifier, Integer> modifiers) {
		mName = name;
		mDisplayItem = displayItem;
		mModifiers = modifiers;
		mLevel = level;
		mId = id;
	}

	public int getId() {
		return mId;
	}

	public static @Nullable DelvePreset getDelvePreset(int id) {
		if (id <= 0) {
			return null;
		}
		for (DelvePreset selection : DelvePreset.values()) {
			if (selection.getId() == id) {
				return selection;
			}
		}
		return null;
	}

	public static @Nullable List<DelvePreset> getRandomPresets(int level) {
		List<DelvePreset> presets = new ArrayList<>();
		if (level <= 0 || level > 3) {
			return null;
		}
		List<DelvePreset> allPresets = Arrays.asList(DelvePreset.values());
		Collections.shuffle(allPresets);
		for (DelvePreset preset : allPresets) {
			if (preset.mLevel == level) {
				presets.add(preset);
			}
			if (presets.size() >= 3) {
				break;
			}
		}

		return presets;
	}

	public static boolean validatePresetModifiers(DelvesManager.DungeonDelveInfo delveMods, DelvePreset preset) {
		for (DelvesModifier modifier : preset.mModifiers.keySet()) {
			if (delveMods.get(modifier) < preset.mModifiers.get(modifier)) {
				return false;
			}
		}

		return true;
	}
}
