package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Arrays;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum DepthsTree {
	DAWNBRINGER("Dawnbringer", DepthsUtils.DAWNBRINGER, Material.SUNFLOWER, "Bestows passive and active buffs to allies including speed, damage, resistance, and healing."),
	EARTHBOUND("Earthbound", DepthsUtils.EARTHBOUND, Material.LEATHER_CHESTPLATE, "Resolute tank with capabilities of taking aggro and granting resistance to self, armed with minor crowd control."),
	FLAMECALLER("Flamecaller", DepthsUtils.FLAMECALLER, Material.FIRE_CHARGE, "Caster of strong burst AOE abilities and potent damage over time."),
	FROSTBORN("Frostborn", DepthsUtils.FROSTBORN, Material.ICE, "Manipulates the flow of combat by debuffing enemies with ice generating abilities and high damage potential."),
	SHADOWDANCER("Shadowdancer", DepthsUtils.SHADOWDANCER, Material.IRON_SWORD, "Skilled in single target melee damage, especially against bosses and elites."),
	STEELSAGE("Steelsage", DepthsUtils.STEELSAGE, Material.CROSSBOW, "Master of ranged abilities with dual AOE and single target damage capabilities."),
	WINDWALKER("Windwalker", DepthsUtils.WINDWALKER, Material.FEATHER, "An arsenal of movement abilities and crowd control, allowing precise maneuvers and quick escapes."),
	PRISMATIC("Prismatic", DepthsTree::prismaticColor, Material.AMETHYST_SHARD, "You should not see this. Please report this bug."),
	CURSE("Zenith Curse", DepthsTree::curseColor, Material.NETHER_WART, "You should not see this. Please report this bug."),
	GIFT("Celestial Gift", DepthsTree::prismaticColor, Material.AMETHYST_SHARD, "You should not see this. Please report this bug.");

	private final String mDisplayName;
	private final Function<String, Component> mColorer;
	private final Material mMaterial;
	private final String mDescription;

	DepthsTree(String displayName, int color, Material material, String description) {
		this(displayName, getColorer(color), material, description);
	}

	DepthsTree(String displayName, Function<String, Component> colorer, Material material, String description) {
		mDisplayName = displayName;
		mColorer = colorer;
		mMaterial = material;
		mDescription = description;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public Component color(String string) {
		return mColorer.apply(string);
	}

	public Component getNameComponent() {
		return color(mDisplayName).decoration(TextDecoration.ITALIC, false);
	}

	public ItemStack createItem() {
		return GUIUtils.createBasicItem(mMaterial, 1, getNameComponent(), mDescription, NamedTextColor.GRAY, 30, true);
	}

	private static Function<String, Component> getColorer(int color) {
		TextColor textColor = TextColor.color(color);
		return s -> Component.text(s, textColor);
	}

	private static Component prismaticColor(String s) {
		return MessagingUtils.addGradient(s, "e2ff9c", "25f6f5", "ff9cf0");
	}

	private static Component curseColor(String s) {
		return MessagingUtils.addGradient(s, "c41300", "a9112b", "8e013e");
	}

	public static final DepthsTree[] OWNABLE_TREES = Arrays.stream(values()).filter(t -> t != PRISMATIC && t != CURSE && t != GIFT).toArray(DepthsTree[]::new);

	public ItemStack createItemWithDescription(String description) {
		return GUIUtils.createBasicItem(mMaterial, 1, getNameComponent(), description, NamedTextColor.GRAY, 30, true);
	}
}
