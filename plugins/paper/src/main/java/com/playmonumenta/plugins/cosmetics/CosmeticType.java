package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.cosmetics.poses.GravePoses;
import com.playmonumenta.plugins.cosmetics.punches.PlayerPunches;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import java.util.Locale;
import java.util.function.Function;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public enum CosmeticType {
	TITLE("Player Title", "Player Titles", Material.NAME_TAG),
	ELITE_FINISHER("Elite Finisher", "Elite Finishers", Material.FIREWORK_ROCKET, EliteFinishers::getDisplayItem),
	PLOT_BORDER("Plot Border", "Plot Borders", Material.BARRIER),
	VANITY("Vanity Item", "Vanity Items", Material.GOLDEN_CHESTPLATE),
	COSMETIC_SKILL("Cosmetic Skill", "Cosmetic Skills", Material.BLAZE_POWDER, CosmeticSkills::getDisplayItem),
	GRAVE_POSE("Grave Pose", "Grave Poses", Material.ARMOR_STAND, GravePoses::getDisplayItem),
	PLAYER_PUNCH("Player Punch", "Player Punches", Material.FEATHER, PlayerPunches::getDisplayItem),
	;

	private final String mDisplayName;
	private final String mDisplayNamePlural;
	private final Material mDisplayItem;
	private final @Nullable Function<String, Material> mDisplayItemSupplier;

	CosmeticType(String displayName, String displayNamePlural, Material material) {
		this(displayName, displayNamePlural, material, null);
	}

	CosmeticType(String displayName, String displayNamePlural, Material material,
	             @Nullable Function<String, Material> displayItemSupplier) {
		mDisplayName = displayName;
		mDisplayNamePlural = displayNamePlural;
		mDisplayItem = material;
		mDisplayItemSupplier = displayItemSupplier;
	}

	public String getType() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public String getDisplayNamePlural() {
		return mDisplayNamePlural;
	}

	public Material getDisplayItem(@Nullable String name) {
		return (name == null || mDisplayItemSupplier == null) ? mDisplayItem : mDisplayItemSupplier.apply(name);
	}

	public boolean isEquippable() {
		return this != VANITY;
	}

	public boolean canEquipMultiple() {
		return this == ELITE_FINISHER || this == PLAYER_PUNCH;
	}

	public static @Nullable CosmeticType getTypeSelection(String type) {
		if (type == null) {
			return null;
		}
		for (CosmeticType selection : values()) {
			if (selection.getType().equals(type)) {
				return selection;
			}
		}
		return null;
	}
}
