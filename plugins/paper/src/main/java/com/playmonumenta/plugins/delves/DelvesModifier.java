package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.delves.abilities.*;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public enum DelvesModifier {
	// Keep variant mods at index -1, as they aren't meant to show up in the GUIs by default
	VENGEANCE(1, Vengeance::applyModifiers, createIcon(Material.RED_BANNER, Component.text("Vengeance", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Vengeance.DESCRIPTION), Vengeance::rankDescription, 15, 1),
	ARCANIC(2, Arcanic::applyModifiers, createIcon(Material.NETHER_STAR, Component.text("Arcanic", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Arcanic.DESCRIPTION), Arcanic::rankDescription, 2, 1),
	INFERNAL(3, null, createIcon(Material.LAVA_BUCKET, Component.text("Infernal", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Infernal.DESCRIPTION), Infernal::rankDescription, 3, 1),
	TRANSCENDENT(4, Transcendent::applyModifiers, createIcon(Material.ENDER_EYE, Component.text("Transcendent", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Transcendent.DESCRIPTION), Transcendent::rankDescription, 4, 1),
	SPECTRAL(5, Spectral::applyModifiers, createIcon(Material.PHANTOM_MEMBRANE, Component.text("Spectral", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Spectral.DESCRIPTION), Spectral::rankDescription, 5, 1),
	DREADFUL(6, Dreadful::applyModifiers, createIcon(Material.BONE, Component.text("Dreadful", NamedTextColor.DARK_GRAY, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Dreadful.DESCRIPTION), Dreadful::rankDescription, 6, 1),
	COLOSSAL(7, null, createIcon(Material.IRON_BARS, Component.text("Colossal", NamedTextColor.DARK_BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Colossal.DESCRIPTION), Colossal::rankDescription, 7, 1),
	CHIVALROUS(8, Chivalrous::applyModifiers, createIcon(Material.MAGMA_CREAM, Component.text("Chivalrous", NamedTextColor.DARK_GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Chivalrous.DESCRIPTION), Chivalrous::rankDescription, 10, 1),
	BLOODTHIRSTY(9, Bloodthirsty::applyModifiers, createIcon(Material.ROTTEN_FLESH, Component.text("Bloodthirsty", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Bloodthirsty.DESCRIPTION), Bloodthirsty::rankDescription, 11, 1),
	PERNICIOUS(10, Pernicious::applyModifiers, createIcon(Material.MUSIC_DISC_11, Component.text("Pernicious", NamedTextColor.DARK_AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Pernicious.DESCRIPTION), Pernicious::rankDescription, 12, 1),
	LEGIONARY(11, Legionary::applyModifiers, createIcon(Material.IRON_SWORD, Component.text("Legionary", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Legionary.DESCRIPTION), Legionary::rankDescription, 13, 1),
	CARAPACE(12, Carapace::applyModifiers, createIcon(Material.NETHERITE_HELMET, Component.text("Carapace", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Carapace.DESCRIPTION), Carapace::rankDescription, 14, 1),
	ENTROPY(13, null, createIcon(Material.STRUCTURE_VOID, Component.text("Entropy", NamedTextColor.BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Entropy.DESCRIPTION), Entropy::rankDescription, 16, 1),
	TWISTED(14, Twisted::applyModifiers, createIconWithVariant(Material.WITHER_ROSE, Component.text("Twisted", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Twisted.DESCRIPTION, Twisted.DESCRIPTION_HOWTOVARIANT), Twisted::rankDescription, 19, 1),
	TWISTED_TORMENTED(-1, Twisted::applyModifiersTormented, createIconWithVariant(Material.CHORUS_FRUIT, Component.text("Tormented", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD, TextDecoration.UNDERLINED).decoration(TextDecoration.ITALIC, false), Twisted.DESCRIPTION_TORMENTED, Twisted.DESCRIPTION_TORMENT_HOWTOVARIANT), Twisted::rankDescription, 19, 2),
	// Keep rotating modifiers after here -----------------------------------------------
	FRAGILE(15, null, createIcon(Material.GLASS, Component.text("Fragile", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Fragile.DESCRIPTION), Fragile::rankDescription, 21, 5),
	ASSASSINS(16, Assassins::applyModifiers, createIcon(Material.NETHERITE_SWORD, Component.text("Assassins", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Assassins.DESCRIPTION), Assassins::rankDescription, 22, 5),
	ASTRAL(17, null, createIcon(Material.SPYGLASS, Component.text("Astral", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Astral.DESCRIPTION), Astral::rankDescription, 23, 5),
	UNYIELDING(18, Unyielding::applyModifiers, createIcon(Material.NETHERITE_CHESTPLATE, Component.text("Unyielding", NamedTextColor.DARK_GRAY, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Unyielding.DESCRIPTION), Unyielding::rankDescription, 24, 5),
	CHRONOLOGY(19, null, createIcon(Material.CLOCK, Component.text("Chronology", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Chronology.DESCRIPTION), Chronology::rankDescription, 25, 5),
	RIFTBORN(20, null, createIcon(Material.END_PORTAL_FRAME, Component.text("Riftborn", NamedTextColor.DARK_BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Riftborn.DESCRIPTION), Riftborn::rankDescription, 26, 5),
	HAUNTED(21, null, createIcon(Material.CARVED_PUMPKIN, Component.text("Haunted", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Haunted.DESCRIPTION), Haunted::rankDescription, 27, 5),
	IDOLATRY(22, null, createIcon(Material.TOTEM_OF_UNDYING, Component.text("Idolatry", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Idolatry.DESCRIPTION), Idolatry::rankDescription, 28, 5),
	// Keep experimental modifiers after here
	CHANCECUBES(201, null, createIcon(Material.GOLD_BLOCK, Component.text("Chance Cubes", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), ChanceCubes.DESCRIPTION), ChanceCubes::rankDescription, 28, 6),
	BERSERK(202, null, createIcon(Material.REDSTONE, Component.text("Berserk", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Berserk.DESCRIPTION), Berserk::rankDescription, 29, 5),
	HEALCUT(203, null, createIcon(Material.ROTTEN_FLESH, Component.text("Rot", NamedTextColor.DARK_GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), HealCut.DESCRIPTION), HealCut::rankDescription, 30, 3),
	GRAVITY(204, null, createIcon(Material.APPLE, Component.text("Gravity", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Gravity.DESCRIPTION), Gravity::rankDescription, 31, 5),
	REGENERATING(205, Regenerating::applyModifiers, createIcon(Material.FIRE_CORAL_FAN, Component.text("Regenerating", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Regenerating.DESCRIPTION), Regenerating::rankDescription, 32, 1),
	BLOODLUST(206, Bloodlust::applyModifiers, createIcon(Material.ROTTEN_FLESH, Component.text("Bloodlust", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Bloodlust.DESCRIPTION), Bloodlust::rankDescription, 33, 1),
	CLOAKED(207, Cloaked::applyModifiers, createIcon(Material.PHANTOM_MEMBRANE, Component.text("Cloaked", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Cloaked.DESCRIPTION), Cloaked::rankDescription, 34, 3),
	BOUNTIFUL(208, null, createIcon(Material.GOAT_HORN, Component.text("Bountiful", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Bountiful.DESCRIPTION), Bountiful::rankDescription, 36, 5);


	private static final List<DelvesModifier> DEATH_TRIGGER_MODIFIERS = List.of(SPECTRAL, DREADFUL, BLOODLUST);
	private static final List<DelvesModifier> ROTATING_DELVE_MODIFIERS = List.of(ASSASSINS, ASTRAL, UNYIELDING, CHRONOLOGY, RIFTBORN, HAUNTED, FRAGILE, IDOLATRY);
	private static final List<DelvesModifier> SPAWN_TRIGGER_MODIFIERS = List.of(ARCANIC, INFERNAL, TRANSCENDENT, CHIVALROUS, BLOODTHIRSTY, PERNICIOUS, LEGIONARY, CARAPACE, TWISTED, ASSASSINS, VENGEANCE, UNYIELDING, REGENERATING, CLOAKED, IDOLATRY);
	private static final List<DelvesModifier> EXPERIMENTAL_DELVE_MODIFIERS = List.of(CHANCECUBES, BERSERK, HEALCUT, GRAVITY, REGENERATING, BLOODLUST, CLOAKED, BOUNTIFUL);
	private static final List<DelvesModifier> VARIANT_MODIFIERS = List.of(TWISTED_TORMENTED);

	private final int mIndex;
	private final @Nullable BiConsumer<LivingEntity, Integer> mApplyFunc;
	private final ItemStack mIcon;
	private final Function<Integer, Component[]> mRankDescriptions;
	private final int mOldColumn;
	private final int mPointsPerLevel;

	DelvesModifier(int index, @Nullable BiConsumer<LivingEntity, Integer> applying, ItemStack stack, Function<Integer, Component[]> rankDescriptions, int column, int pointsPerLevel) {
		mIndex = index;
		mApplyFunc = applying;
		ItemMeta meta = stack.getItemMeta();
		meta.addItemFlags(ItemFlag.values());
		stack.setItemMeta(meta);
		mIcon = stack;
		mRankDescriptions = rankDescriptions;
		mOldColumn = column;
		mPointsPerLevel = pointsPerLevel;
	}

	public int getIndex() {
		return mIndex;
	}

	public ItemStack getIcon() {
		return mIcon;
	}

	public Function<Integer, Component[]> getRankDescriptions() {
		return mRankDescriptions;
	}

	public int getPointsPerLevel() {
		return mPointsPerLevel;
	}

	/**
	 * Use getIndex() instead for have a unique number for this DelvesModifier
	 */
	@Deprecated
	public int getColumn() {
		return mOldColumn;
	}

	public void applyDelve(LivingEntity mob, int level) {
		if (level > 0 && mApplyFunc != null) {
			mApplyFunc.accept(mob, level);
		}
	}

	public static @Nullable DelvesModifier fromName(String name) {
		if (name == null) {
			return null;
		}

		for (DelvesModifier mod : values()) {
			if (mod.name().equals(name)) {
				return mod;
			}
		}
		return null;
	}

	public static List<DelvesModifier> valuesList() {
		return new ArrayList<>(List.of(values()));
	}

	public static List<DelvesModifier> spawnTriggerDelvesModifier() {
		return new ArrayList<>(SPAWN_TRIGGER_MODIFIERS);
	}

	public static List<DelvesModifier> deathTriggerDelvesModifier() {
		return new ArrayList<>(DEATH_TRIGGER_MODIFIERS);
	}

	public static List<DelvesModifier> rotatingDelveModifiers() {
		return new ArrayList<>(ROTATING_DELVE_MODIFIERS);
	}

	public static List<DelvesModifier> experimentalDelveModifiers() {
		return new ArrayList<>(EXPERIMENTAL_DELVE_MODIFIERS);
	}

	public static List<DelvesModifier> variantDelveModifiers() {
		return new ArrayList<>(VARIANT_MODIFIERS);
	}

	public static List<DelvesModifier> entropyAssignable() {
		List<DelvesModifier> mods = valuesList();
		mods.remove(ENTROPY);
		mods.remove(TWISTED);
		mods.removeAll(ROTATING_DELVE_MODIFIERS);
		mods.removeAll(EXPERIMENTAL_DELVE_MODIFIERS);
		mods.removeAll(VARIANT_MODIFIERS);
		return mods;
	}

	public static @Nullable DelvesModifier fromIndex(int index) {
		if (index < 1 || index > values().length) {
			return null;
		}
		return values()[index - 1];
	}

	public static ItemStack createIcon(Material material, Component name, String description) {
		return createIcon(material, name, new String[]{description});
	}

	public static ItemStack createIcon(Material material, Component name, String[] description) {
		return GUIUtils.createBasicItem(material, 1, name, Arrays.asList(description), NamedTextColor.WHITE);
	}

	public static ItemStack createIconWithVariant(Material material, Component name, String description, Component[] variant) {
		List<Component> desc = new ArrayList<>();
		desc.add(Component.text(description, NamedTextColor.WHITE));
		desc.add(Component.text(""));
		desc.addAll(Arrays.asList(variant));

		return GUIUtils.createBasicItem(material, 1, name, desc, true);
	}
}
