package com.playmonumenta.plugins.depths.charmfactory;

import com.playmonumenta.plugins.depths.DepthsTree;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public enum CharmNounItems {
	TRINKET("Trinket", null, Material.WOODEN_SWORD),
	BAUBLE("Bauble", null, Material.GRAY_DYE),
	CHARM("Charm", null, Material.LIGHT_GRAY_DYE),
	BANGLE("Bangle", null, Material.RED_DYE),
	BOON("Boon", null, Material.PINK_DYE),
	BROACH("Broach", null, Material.WHITE_DYE),
	BAND("Band", null, Material.LIME_DYE),
	SUNDIAL("Sundial", DepthsTree.DAWNBRINGER, Material.CLOCK),
	CROWN("Crown", DepthsTree.FROSTBORN, Material.BLAZE_POWDER),
	BIJOU("Bijou", null, Material.GOLD_INGOT),
	BEAD("Bead", null, Material.YELLOW_DYE),
	ARMILLA("Armilla", null, Material.SUNFLOWER),
	AEGIS("Aegis", DepthsTree.EARTHBOUND, Material.RAW_GOLD),
	CURIO("Curio", DepthsTree.STEELSAGE, Material.SHEARS),
	BRACERS("Bracers", DepthsTree.EARTHBOUND, Material.BRICK),
	CLOAK("Cloak", DepthsTree.SHADOWDANCER, Material.RABBIT_HIDE),
	CODEX("Codex", DepthsTree.DAWNBRINGER, Material.BOOK),
	BLADE("Blade", DepthsTree.SHADOWDANCER, Material.IRON_SWORD),
	CHALK("Chalk", DepthsTree.FLAMECALLER, Material.BONE),
	QUIVER("Quiver", DepthsTree.STEELSAGE, Material.LEATHER),
	CATALYST("Catalyst", DepthsTree.FLAMECALLER, Material.BLAZE_ROD),
	CAPE("Cape", DepthsTree.WINDWALKER, Material.LEATHER_HORSE_ARMOR),
	ICICLE("Icicle", DepthsTree.FROSTBORN, Material.LIGHT_BLUE_DYE),
	MEMENTO("Memento", null, Material.SPYGLASS),
	RELIC("Relic", null, Material.MAGMA_CREAM),
	TALISMAN("Talisman", null, Material.GHAST_TEAR),
	AMULET("Amulet", null, Material.RABBIT_FOOT),
	ORNAMENT("Ornament", null, Material.SLIME_BALL),
	DIADEM("Diadem", null, Material.PURPLE_DYE),
	SHEATH("Sheath", DepthsTree.STEELSAGE, Material.POINTED_DRIPSTONE),
	LAVALIERE("Lavaliere", DepthsTree.FLAMECALLER, Material.ORANGE_DYE),
	JEWEL("Jewel", null, Material.DIAMOND),
	CHAIN("Chain", DepthsTree.STEELSAGE, Material.FISHING_ROD),
	TOME("Tome", DepthsTree.FROSTBORN, Material.ENCHANTED_BOOK),
	GEM("Gem", null, Material.EMERALD),
	STAFF("Staff", null, Material.STICK),
	SCROLL("Scroll", null, Material.SKULL_BANNER_PATTERN),
	MEDALLION("Medallion", null, Material.SUNFLOWER),
	TABLET("Tablet", null, Material.BROWN_DYE),
	SCEPTER("Scepter", null, Material.BLAZE_ROD),
	LEAF("Leaf", null, Material.GREEN_DYE),
	VIAL("Vial", null, Material.MAGENTA_DYE),
	VESSEL("Vessel", null, Material.HONEY_BOTTLE),
	WHISTLE("Whistle", DepthsTree.WINDWALKER, Material.GOAT_HORN),
	COMPASS("Compass", null, Material.RECOVERY_COMPASS),
	SHARD("Shard", null, Material.ECHO_SHARD),
	ROSE("Rose", null, Material.WITHER_ROSE),
	WART("Wart", null, Material.NETHER_WART),
	ORB("Orb", null, Material.CLAY_BALL),
	RING("Ring", DepthsTree.DAWNBRINGER, Material.GOLD_NUGGET),
	INGOT("Ingot", DepthsTree.EARTHBOUND, Material.IRON_INGOT),
	EMBER("Ember", DepthsTree.FLAMECALLER, Material.REDSTONE_TORCH),
	ICE("Ice", DepthsTree.FROSTBORN, Material.ICE),
	DAGGER("Dagger", DepthsTree.SHADOWDANCER, Material.NETHERITE_SWORD),
	AMMUNITION("Ammunition", DepthsTree.STEELSAGE, Material.GUNPOWDER),
	WINGS("Wings", DepthsTree.WINDWALKER, Material.PHANTOM_MEMBRANE);


	public final String mName;
	public final @Nullable DepthsTree mTree;
	public final Material mBaseItem;

	CharmNounItems(String name, @Nullable DepthsTree tree, Material baseItem) {
		mName = name;
		mTree = tree;
		mBaseItem = baseItem;
	}
}
