package com.playmonumenta.plugins.delves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

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
	REMNANTS(4, "Returning Remnants", 1, Material.SKELETON_SKULL, Map.ofEntries(
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
		entry(DelvesModifier.TRANSCENDENT, 4),
		entry(DelvesModifier.LEGIONARY, 3),
		entry(DelvesModifier.CHIVALROUS, 3),
		entry(DelvesModifier.BLOODTHIRSTY, 2)
	)),
	TWO(13, "Take Two", 3, Material.ROSE_BUSH, Map.ofEntries(
		entry(DelvesModifier.PERNICIOUS, 2),
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
	)),
	MOBILITY(16, "Mobility Mayhem", 3, Material.IRON_BOOTS, Map.ofEntries(
		entry(DelvesModifier.CHIVALROUS, 5),
		entry(DelvesModifier.BLOODTHIRSTY, 5),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.LEGIONARY, 4),
		entry(DelvesModifier.COLOSSAL, 2),
		entry(DelvesModifier.TRANSCENDENT, 1)
	)),
	REVENANTS(17, "Resurrecting Revenants", 3, Material.WITHER_SKELETON_SKULL, Map.ofEntries(
		entry(DelvesModifier.SPECTRAL, 5),
		entry(DelvesModifier.DREADFUL, 5),
		entry(DelvesModifier.COLOSSAL, 3),
		entry(DelvesModifier.PERNICIOUS, 3),
		entry(DelvesModifier.TRANSCENDENT, 3),
		entry(DelvesModifier.CHIVALROUS, 3)
	)),
	WHITE(51, "white", 100, Material.WHITE_WOOL, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 10),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.SPECTRAL, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.PERNICIOUS, 10),
		entry(DelvesModifier.BLOODTHIRSTY, 3)
	)),
	ORANGE(52, "orange", 100, Material.ORANGE_WOOL, Map.ofEntries(
		entry(DelvesModifier.ARCANIC, 5),
		entry(DelvesModifier.CHIVALROUS, 5),
		entry(DelvesModifier.CARAPACE, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.BLOODTHIRSTY, 10),
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.COLOSSAL, 3),
		entry(DelvesModifier.UNYIELDING, 1)
	)),
	MAGENTA(53, "magenta", 100, Material.MAGENTA_WOOL, Map.ofEntries(
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.DREADFUL, 10),
		entry(DelvesModifier.SPECTRAL, 7),
		entry(DelvesModifier.COLOSSAL, 3),
		entry(DelvesModifier.INFERNAL, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.LEGIONARY, 2),
		entry(DelvesModifier.CARAPACE, 3),
		entry(DelvesModifier.HAUNTED, 1)
	)),
	LIGHTBLUE(54, "lightblue", 100, Material.LIGHT_BLUE_WOOL, Map.ofEntries(
		entry(DelvesModifier.ARCANIC, 17),
		entry(DelvesModifier.TRANSCENDENT, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.CARAPACE, 8),
		entry(DelvesModifier.INFERNAL, 5),
		entry(DelvesModifier.ASTRAL, 1)
	)),
	YELLOW(55, "yellow", 100, Material.YELLOW_WOOL, Map.ofEntries(
		entry(DelvesModifier.COLOSSAL, 5),
		entry(DelvesModifier.VENGEANCE, 13),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.INFERNAL, 5),
		entry(DelvesModifier.UNYIELDING, 1),
		entry(DelvesModifier.BLOODTHIRSTY, 7),
		entry(DelvesModifier.RIFTBORN, 1)
	)),
	WILLOWS(56, "willows", 100, Material.MOSSY_COBBLESTONE, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.DREADFUL, 5),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.RIFTBORN, 1),
		entry(DelvesModifier.BLOODTHIRSTY, 3),
		entry(DelvesModifier.VENGEANCE, 10),
		entry(DelvesModifier.ASSASSINS, 1),
		entry(DelvesModifier.UNYIELDING, 1)
	)),
	REVERIE(57, "reverie", 100, Material.NETHER_WART_BLOCK, Map.ofEntries(
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.PERNICIOUS, 6),
		entry(DelvesModifier.RIFTBORN, 1),
		entry(DelvesModifier.VENGEANCE, 34)
	)),
	LIME(58, "lime", 100, Material.LIME_WOOL, Map.ofEntries(
		entry(DelvesModifier.INFERNAL, 7),
		entry(DelvesModifier.ARCANIC, 13),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.LEGIONARY, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.RIFTBORN, 1),
		entry(DelvesModifier.TRANSCENDENT, 5)
	)),
	PINK(59, "pink", 100, Material.PINK_WOOL, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.VENGEANCE, 13),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.RIFTBORN, 1),
		entry(DelvesModifier.TRANSCENDENT, 10),
		entry(DelvesModifier.UNYIELDING, 1)
	)),
	GRAY(60, "gray", 100, Material.GRAY_WOOL, Map.ofEntries(
		entry(DelvesModifier.SPECTRAL, 15),
		entry(DelvesModifier.DREADFUL, 10),
		entry(DelvesModifier.COLOSSAL, 5),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.ASTRAL, 1),
		entry(DelvesModifier.HAUNTED, 1)
	)),
	LIGHTGRAY(61, "lightgray", 100, Material.LIGHT_GRAY_WOOL, Map.ofEntries(
		entry(DelvesModifier.VENGEANCE, 17),
		entry(DelvesModifier.CARAPACE, 10),
		entry(DelvesModifier.ARCANIC, 3),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.ASTRAL, 1),
		entry(DelvesModifier.RIFTBORN, 1)
	)),
	CYAN(62, "cyan", 100, Material.CYAN_WOOL, Map.ofEntries(
		entry(DelvesModifier.INFERNAL, 17),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.SPECTRAL, 5),
		entry(DelvesModifier.BLOODTHIRSTY, 6),
		entry(DelvesModifier.RIFTBORN, 1)
	)),
	PURPLE(63, "purple", 100, Material.PURPLE_WOOL, Map.ofEntries(
		entry(DelvesModifier.PERNICIOUS, 8),
		entry(DelvesModifier.COLOSSAL, 5),
		entry(DelvesModifier.BLOODTHIRSTY, 17),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.HAUNTED, 1),
		entry(DelvesModifier.ASSASSINS, 1)
	)),
	TEAL(64, "teal", 100, Material.CYAN_WOOL, Map.ofEntries(
		entry(DelvesModifier.TRANSCENDENT, 12),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.PERNICIOUS, 8),
		entry(DelvesModifier.ARCANIC, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.ASSASSINS, 1),
		entry(DelvesModifier.RIFTBORN, 1)
	)),
	SHIFTING(65, "shiftingcity", 100, Material.PRISMARINE, Map.ofEntries(
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.COLOSSAL, 5),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.ARCANIC, 5),
		entry(DelvesModifier.TRANSCENDENT, 10),
		entry(DelvesModifier.DREADFUL, 10),
		entry(DelvesModifier.ASSASSINS, 1)
	)),
	FORUM(66, "forum", 100, Material.BOOKSHELF, Map.ofEntries(
		entry(DelvesModifier.LEGIONARY, 7),
		entry(DelvesModifier.TRANSCENDENT, 10),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.PERNICIOUS, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.BLOODTHIRSTY, 5),
		entry(DelvesModifier.DREADFUL, 8)
	)),
	BLUE(67, "blue", 100, Material.BLUE_WOOL, Map.ofEntries(
		entry(DelvesModifier.INFERNAL, 10),
		entry(DelvesModifier.TRANSCENDENT, 10),
		entry(DelvesModifier.ASTRAL, 1),
		entry(DelvesModifier.PERNICIOUS, 5),
		entry(DelvesModifier.ARCANIC, 10),
		entry(DelvesModifier.TWISTED, 5),
		entry(DelvesModifier.RIFTBORN, 1)
	)),
	BROWN(68, "brown", 100, Material.BROWN_WOOL, Map.ofEntries(
		entry(DelvesModifier.CARAPACE, 15),
		entry(DelvesModifier.COLOSSAL, 5),
		entry(DelvesModifier.CHRONOLOGY, 1),
		entry(DelvesModifier.PERNICIOUS, 10),
		entry(DelvesModifier.BLOODTHIRSTY, 10),
		entry(DelvesModifier.INFERNAL, 5)
	));

	public static final String PRESET_SCOREBOARD = "Daily3DelvePreset";

	public final String mName;
	public final int mLevel;
	public final Map<DelvesModifier, Integer> mModifiers;
	public final int mId;
	public final Material mDisplayItem;

	DelvePreset(int id, String name, int level, Material displayItem, Map<DelvesModifier, Integer> modifiers) {
		Validate.isTrue(id >= 1, "id must be >= 1", id);
		mName = name;
		mDisplayItem = displayItem;
		mModifiers = modifiers;
		mLevel = level;
		mId = id;
	}

	public int getId() {
		return mId;
	}

	public boolean isDungeonChallengePreset() {
		return mLevel >= 100;
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

	public static @Nullable DelvePreset getDelvePreset(String name) {
		for (DelvePreset selection : DelvePreset.values()) {
			if (selection.mName.equals(name)) {
				return selection;
			}
		}
		return null;
	}

	public static List<DelvePreset> getRandomPresets(int level) {
		List<DelvePreset> presets = new ArrayList<>();
		if (level <= 0 || level > 3) {
			return Collections.emptyList();
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
		return Collections.unmodifiableList(presets);
	}

	public static boolean validatePresetModifiers(DelvesManager.DungeonDelveInfo delveMods, DelvePreset preset, boolean exact) {
		return validatePresetModifiers(delveMods.getMap(), preset, exact);
	}

	public static boolean validatePresetModifiers(Map<DelvesModifier, Integer> delveMods, DelvePreset preset, boolean exact) {
		if (exact) {
			return delveMods.equals(preset.mModifiers);
		}
		for (Map.Entry<DelvesModifier, Integer> entry : preset.mModifiers.entrySet()) {
			if (delveMods.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}
}
