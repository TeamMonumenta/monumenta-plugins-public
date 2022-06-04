package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.delves.abilities.Arcanic;
import com.playmonumenta.plugins.delves.abilities.Bloodthirsty;
import com.playmonumenta.plugins.delves.abilities.Carapace;
import com.playmonumenta.plugins.delves.abilities.Chivalrous;
import com.playmonumenta.plugins.delves.abilities.Colossal;
import com.playmonumenta.plugins.delves.abilities.Dreadful;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.delves.abilities.Infernal;
import com.playmonumenta.plugins.delves.abilities.Legionary;
import com.playmonumenta.plugins.delves.abilities.Pernicious;
import com.playmonumenta.plugins.delves.abilities.Relentless;
import com.playmonumenta.plugins.delves.abilities.Spectral;
import com.playmonumenta.plugins.delves.abilities.Transcendent;
import com.playmonumenta.plugins.delves.abilities.Twisted;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum DelvesModifier {
	RELENTLESS(1, Relentless::applyModifiers, createIcon(Material.ELYTRA, Component.text("Relentless", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Relentless.DESCRIPTION), Relentless.RANK_DESCRIPTIONS, 1),
	ARCANIC(2, Arcanic::applyModifiers, createIcon(Material.NETHER_STAR, Component.text("Arcanic", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Arcanic.DESCRIPTION), Arcanic.RANK_DESCRIPTIONS, 2),
	INFERNAL(3, Infernal::applyModifiers, createIcon(Material.LAVA_BUCKET, Component.text("Infernal", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Infernal.DESCRIPTION), Infernal.RANK_DESCRIPTIONS, 3),
	TRANSCENDENT(4, Transcendent::applyModifiers, createIcon(Material.ENDER_EYE, Component.text("Transcendent", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Transcendent.DESCRIPTION), Transcendent.RANK_DESCRIPTIONS, 4),
	SPECTRAL(5, Spectral::applyModifiers, createIcon(Material.PHANTOM_MEMBRANE, Component.text("Spectral", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Spectral.DESCRIPTION), Spectral.RANK_DESCRIPTIONS, 5),
	DREADFUL(6, Dreadful::applyModifiers, createIcon(Material.BONE, Component.text("Dreadful", NamedTextColor.DARK_GRAY, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Dreadful.DESCRIPTION), Dreadful.RANK_DESCRIPTIONS, 6),
	COLOSSAL(7, null, createIcon(Material.IRON_BARS, Component.text("Colossal", NamedTextColor.DARK_BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Colossal.DESCRIPTION), Colossal.RANK_DESCRIPTIONS, 7),
	CHIVALROUS(8, Chivalrous::applyModifiers, createIcon(Material.MAGMA_CREAM, Component.text("Chivalrous", NamedTextColor.DARK_GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Chivalrous.DESCRIPTION), Chivalrous.RANK_DESCRIPTIONS, 10),
	BLOODTHIRSTY(9, Bloodthirsty::applyModifiers, createIcon(Material.ROTTEN_FLESH, Component.text("Bloodthirsty", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Bloodthirsty.DESCRIPTION), Bloodthirsty.RANK_DESCRIPTIONS, 11),
	PERNICIOUS(10, Pernicious::applyModifiers, createIcon(Material.MUSIC_DISC_11, Component.text("Pernicious", NamedTextColor.DARK_AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Pernicious.DESCRIPTION), Pernicious.RANK_DESCRIPTIONS, 12),
	LEGIONARY(11, Legionary::applyModifiers, createIcon(Material.IRON_SWORD, Component.text("Legionary", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Legionary.DESCRIPTION), Legionary.RANK_DESCRIPTIONS, 13),
	CARAPACE(12, Carapace::applyModifiers, createIcon(Material.NETHERITE_HELMET, Component.text("Carapace", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Carapace.DESCRIPTION), Carapace.RANK_DESCRIPTIONS, 14),
	ENTROPY(13, null, createIcon(Material.STRUCTURE_VOID, Component.text("Entropy", NamedTextColor.BLUE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Entropy.DESCRIPTION), Entropy.RANK_DESCRIPTIONS, 15),
	TWISTED(14, Twisted::applyModifiers, createIcon(Material.TIPPED_ARROW, Component.text("Twisted", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), Twisted.DESCRIPTION), Twisted.RANK_DESCRIPTIONS, 16);

	private static final List<DelvesModifier> DEATH_TRIGGER_MODIFIERS = List.of(SPECTRAL, DREADFUL);
	private static final List<DelvesModifier> SPAWN_TRIGGER_MODIFIERS = List.of(RELENTLESS, ARCANIC, INFERNAL, TRANSCENDENT, CHIVALROUS, BLOODTHIRSTY, PERNICIOUS, LEGIONARY, CARAPACE, TWISTED);

	private final int mIndex;
	private final BiConsumer<LivingEntity, Integer> mApplyFunc;
	private final ItemStack mIcon;
	private final String[][] mRankDescriptions;
	private final int mOldColumn;

	DelvesModifier(int index, BiConsumer<LivingEntity, Integer> applying, ItemStack stack, String[][] rankDescriptions, int column) {
		mIndex = index;
		mApplyFunc = applying;
		mIcon = stack;
		mRankDescriptions = rankDescriptions;
		mOldColumn = column;
	}

	public int getIndex() {
		return mIndex;
	}

	public ItemStack getIcon() {
		return mIcon;
	}

	public String[][] getRankDescriptions() {
		return mRankDescriptions;
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

	public static @Nullable DelvesModifier fromIndex(int index) {
		if (index < 1 || index > values().length) {
			return null;
		}
		return values()[index - 1];
	}

	private static ItemStack createIcon(Material material, Component name, String description) {
		ItemStack stack = new ItemStack(material);

		ItemMeta meta = stack.getItemMeta();
		meta.displayName(name);
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(description, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		stack.setItemMeta(meta);

		return stack;
	}
}