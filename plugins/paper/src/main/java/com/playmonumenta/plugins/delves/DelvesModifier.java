package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.delves.abilities.Arcanic;
import com.playmonumenta.plugins.delves.abilities.Assassins;
import com.playmonumenta.plugins.delves.abilities.Astral;
import com.playmonumenta.plugins.delves.abilities.Bloodthirsty;
import com.playmonumenta.plugins.delves.abilities.Carapace;
import com.playmonumenta.plugins.delves.abilities.Chivalrous;
import com.playmonumenta.plugins.delves.abilities.Chronology;
import com.playmonumenta.plugins.delves.abilities.Colossal;
import com.playmonumenta.plugins.delves.abilities.Dreadful;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.delves.abilities.Fragile;
import com.playmonumenta.plugins.delves.abilities.Haunted;
import com.playmonumenta.plugins.delves.abilities.Infernal;
import com.playmonumenta.plugins.delves.abilities.Legionary;
import com.playmonumenta.plugins.delves.abilities.Pernicious;
import com.playmonumenta.plugins.delves.abilities.Riftborn;
import com.playmonumenta.plugins.delves.abilities.Spectral;
import com.playmonumenta.plugins.delves.abilities.Transcendent;
import com.playmonumenta.plugins.delves.abilities.Twisted;
import com.playmonumenta.plugins.delves.abilities.Unyielding;
import com.playmonumenta.plugins.delves.abilities.Vengeance;
import java.util.ArrayList;
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
	VENGEANCE(1, Vengeance::applyModifiers, createIcon(Material.RED_BANNER, Component.text("Vengeance", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Vengeance.DESCRIPTION), Vengeance::rankDescription, 15, 1),
	ARCANIC(2, Arcanic::applyModifiers, createIcon(Material.NETHER_STAR, Component.text("Arcanic", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Arcanic.DESCRIPTION), Arcanic::rankDescription, 2, 1),
	INFERNAL(3, Infernal::applyModifiers, createIcon(Material.LAVA_BUCKET, Component.text("Infernal", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Infernal.DESCRIPTION), Infernal::rankDescription, 3, 1),
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
	TWISTED(14, Twisted::applyModifiers, createIcon(Material.WITHER_ROSE, Component.text("Twisted", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Twisted.DESCRIPTION), Twisted::rankDescription, 19, 1),
	FRAGILE(15, null, createIcon(Material.GLASS, Component.text("Fragile", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Fragile.DESCRIPTION), Fragile::rankDescription, 20, 5),
	// Keep rotating modifiers after here -----------------------------------------------
	ASSASSINS(16, Assassins::applyModifiers, createIcon(Material.NETHERITE_SWORD, Component.text("Assassins", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Assassins.DESCRIPTION), Assassins::rankDescription, 21, 5),
	ASTRAL(17, null, createIcon(Material.SPYGLASS, Component.text("Astral", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Astral.DESCRIPTION), Astral::rankDescription, 22, 5),
	UNYIELDING(18, Unyielding::applyModifiers, createIcon(Material.NETHERITE_CHESTPLATE, Component.text("Unyielding", NamedTextColor.DARK_GRAY, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Unyielding.DESCRIPTION), Unyielding::rankDescription, 23, 5),
	CHRONOLOGY(19, null, createIcon(Material.CLOCK, Component.text("Chronology", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Chronology.DESCRIPTION), Chronology::rankDescription, 24, 5),
	RIFTBORN(20, null, createIcon(Material.END_PORTAL_FRAME, Component.text("Riftborn", NamedTextColor.DARK_BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Riftborn.DESCRIPTION), Riftborn::rankDescription, 25, 5),
	HAUNTED(21, null, createIcon(Material.CARVED_PUMPKIN, Component.text("Haunted", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Haunted.DESCRIPTION), Haunted::rankDescription, 26, 5)
	;

	private static final List<DelvesModifier> DEATH_TRIGGER_MODIFIERS = List.of(SPECTRAL, DREADFUL);
	private static final List<DelvesModifier> ROTATING_DELVE_MODIFIERS = List.of(ASSASSINS, ASTRAL, UNYIELDING, CHRONOLOGY, RIFTBORN, HAUNTED);
	private static final List<DelvesModifier> SPAWN_TRIGGER_MODIFIERS = List.of(ARCANIC, INFERNAL, TRANSCENDENT, CHIVALROUS, BLOODTHIRSTY, PERNICIOUS, LEGIONARY, CARAPACE, TWISTED, ASSASSINS, VENGEANCE, UNYIELDING);

	private final int mIndex;
	private final @Nullable BiConsumer<LivingEntity, Integer> mApplyFunc;
	private final ItemStack mIcon;
	private final Function<Integer, String[]> mRankDescriptions;
	private final int mOldColumn;
	private final int mPointsPerLevel;

	DelvesModifier(int index, @Nullable BiConsumer<LivingEntity, Integer> applying, ItemStack stack, Function<Integer, String[]> rankDescriptions, int column, int pointsPerLevel) {
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

	public Function<Integer, String[]> getRankDescriptions() {
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

		for (DelvesModifier mod : DelvesModifier.values()) {
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

	public static List<DelvesModifier> entropyAssignable() {
		List<DelvesModifier> mods = valuesList();
		mods.remove(ENTROPY);
		mods.remove(TWISTED);
		mods.remove(FRAGILE);
		mods.removeAll(ROTATING_DELVE_MODIFIERS);
		return mods;
	}

	public static @Nullable DelvesModifier fromIndex(int index) {
		if (index < 1 || index > values().length) {
			return null;
		}
		return values()[index - 1];
	}

	public static ItemStack createIcon(Material material, Component name, String description) {
		return createIcon(material, name, new String[] {description});
	}

	public static ItemStack createIcon(Material material, Component name, String[] description) {
		ItemStack stack = new ItemStack(material);

		ItemMeta meta = stack.getItemMeta();
		meta.addItemFlags(ItemFlag.values());
		meta.displayName(name);
		List<Component> lore = new ArrayList<>();
		for (String descp : description) {
			lore.add(Component.text(descp, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(lore);
		stack.setItemMeta(meta);

		return stack;
	}
}
