package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.Masterwork;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Nullable;

public class MasterworkUtils {

	private static final String HYPER_ARCHOS_RING = "epic:r3/items/currency/hyperchromatic_archos_ring";
	private static final String PULSATING_SHARD = "epic:r3/items/currency/pulsating_shard";
	private static final String PULSATING_DIAMOND = "epic:r3/items/currency/pulsating_diamond";
	private static final String FORTITUDE_AUGMENT = "epic:r3/items/currency/fortitude_augment";
	private static final String POTENCY_AUGMENT = "epic:r3/items/currency/potency_augment";
	private static final String ALACRITY_AUGMENT = "epic:r3/items/currency/alacrity_augment";
	private static final String INVALID_ITEM = "epic:r3/masterwork/invalid_masterwork_selection";

	private static final String FOREST_FRAG = "epic:r3/fragments/forest_fragment";
	private static final String FOREST_MAT = "epic:r3/items/currency/fenian_flower";

	private static final String KEEP_FRAG = "epic:r3/fragments/keep_fragment";
	private static final String KEEP_MAT = "epic:r3/items/currency/iridium_catalyst";

	private static final String SKT_FRAG = "epic:r3/fragments/silver_fragment";
	private static final String SKT_MAT = "epic:r3/items/currency/silver_remnant";

	private static final String BLUE_FRAG = "epic:r3/fragments/blue_fragment";
	private static final String BLUE_MAT = "epic:r3/items/currency/sorceress_stave";

	private static final String BROWN_FRAG = "epic:r3/fragments/brown_fragment";
	private static final String BROWN_MAT = "epic:r3/items/currency/broken_god_gearframe";

	private static final String PORTAL_FRAG = "epic:r3/fragments/companion_fragment";
	private static final String PORTAL_MAT = "epic:r3/items/currency/corrupted_circuit";

	private static final String MASK_FRAG = "epic:r3/fragments/masquerader_fragment";
	private static final String MASK_MAT = "epic:r3/items/currency/shattered_mask";

	private static final String GALLEY_MAT = "epic:r3/gallery/items/torn_canvas";

	private static final String GODSPORE_MAT = "epic:r3/godspore/items/fungal_remnants";

	// Exalted Dungeons
	private static final String WHITE_MAT = "epic:r1/delves/white/auxiliary/delve_material";
	private static final String ORANGE_MAT = "epic:r1/delves/orange/auxiliary/delve_material";

	public enum MasterworkCost {
		FOREST_ONE("forest_1", FOREST_FRAG, 1, HYPER_ARCHOS_RING, 1),
		FOREST_TWO("forest_2", FOREST_FRAG, 1, HYPER_ARCHOS_RING, 3),
		FOREST_THREE("forest_3", FOREST_FRAG, 1, HYPER_ARCHOS_RING, 4),
		FOREST_FOUR("forest_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		FOREST_FIVE("forest_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		FOREST_SIX("forest_6", FOREST_MAT, 16, HYPER_ARCHOS_RING, 4),
		FOREST_SEVENA("forest_7a", FOREST_MAT, 32, FORTITUDE_AUGMENT, 16),
		FOREST_SEVENB("forest_7b", FOREST_MAT, 32, POTENCY_AUGMENT, 16),
		FOREST_SEVENC("forest_7c", FOREST_MAT, 32, ALACRITY_AUGMENT, 16),

		KEEP_ONE("keep_1", KEEP_FRAG, 1, HYPER_ARCHOS_RING, 1),
		KEEP_TWO("keep_2", KEEP_FRAG, 1, HYPER_ARCHOS_RING, 3),
		KEEP_THREE("keep_3", KEEP_FRAG, 1, HYPER_ARCHOS_RING, 4),
		KEEP_FOUR("keep_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		KEEP_FIVE("keep_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		KEEP_SIX("keep_6", KEEP_MAT, 16, HYPER_ARCHOS_RING, 4),
		KEEP_SEVENA("keep_7a", KEEP_MAT, 32, FORTITUDE_AUGMENT, 16),
		KEEP_SEVENB("keep_7b", KEEP_MAT, 32, POTENCY_AUGMENT, 16),
		KEEP_SEVENC("keep_7c", KEEP_MAT, 32, ALACRITY_AUGMENT, 16),

		SKT_ONE("silver_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		SKT_TWO("silver_2", SKT_FRAG, 1, HYPER_ARCHOS_RING, 3),
		SKT_THREE("silver_3", SKT_FRAG, 1, HYPER_ARCHOS_RING, 4),
		SKT_FOUR("silver_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		SKT_FIVE("silver_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		SKT_SIX("silver_6", SKT_MAT, 16, HYPER_ARCHOS_RING, 4),
		SKT_SEVENA("silver_7a", SKT_MAT, 32, FORTITUDE_AUGMENT, 16),
		SKT_SEVENB("silver_7b", SKT_MAT, 32, POTENCY_AUGMENT, 16),
		SKT_SEVENC("silver_7c", SKT_MAT, 32, ALACRITY_AUGMENT, 16),

		BLUE_ONE("blue_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		BLUE_TWO("blue_2", BLUE_FRAG, 1, HYPER_ARCHOS_RING, 3),
		BLUE_THREE("blue_3", BLUE_FRAG, 1, HYPER_ARCHOS_RING, 4),
		BLUE_FOUR("blue_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		BLUE_FIVE("blue_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		BLUE_SIX("blue_6", BLUE_MAT, 16, HYPER_ARCHOS_RING, 4),
		BLUE_SEVENA("blue_7a", BLUE_MAT, 32, FORTITUDE_AUGMENT, 16),
		BLUE_SEVENB("blue_7b", BLUE_MAT, 32, POTENCY_AUGMENT, 16),
		BLUE_SEVENC("blue_7c", BLUE_MAT, 32, ALACRITY_AUGMENT, 16),

		BROWN_ONE("brown_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		BROWN_TWO("brown_2", BROWN_FRAG, 1, HYPER_ARCHOS_RING, 3),
		BROWN_THREE("brown_3", BROWN_FRAG, 1, HYPER_ARCHOS_RING, 4),
		BROWN_FOUR("brown_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		BROWN_FIVE("brown_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		BROWN_SIX("brown_6", BROWN_MAT, 16, HYPER_ARCHOS_RING, 4),
		BROWN_SEVENA("brown_7a", BROWN_MAT, 32, FORTITUDE_AUGMENT, 16),
		BROWN_SEVENB("brown_7b", BROWN_MAT, 32, POTENCY_AUGMENT, 16),
		BROWN_SEVENC("brown_7c", BROWN_MAT, 32, ALACRITY_AUGMENT, 16),

		PORTAL_ONE("science_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		PORTAL_TWO("science_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		PORTAL_THREE("science_3", PORTAL_FRAG, 1, HYPER_ARCHOS_RING, 4),
		PORTAL_FOUR("science_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		PORTAL_FIVE("science_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		PORTAL_SIX("science_6", PORTAL_MAT, 16, HYPER_ARCHOS_RING, 4),
		PORTAL_SEVENA("science_7a", PORTAL_MAT, 32, FORTITUDE_AUGMENT, 16),
		PORTAL_SEVENB("science_7b", PORTAL_MAT, 32, POTENCY_AUGMENT, 16),
		PORTAL_SEVENC("science_7c", PORTAL_MAT, 32, ALACRITY_AUGMENT, 16),

		MASK_ONE("bluestrike_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		MASK_TWO("bluestrike_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		MASK_THREE("bluestrike_3", MASK_FRAG, 1, HYPER_ARCHOS_RING, 4),
		MASK_FOUR("bluestrike_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		MASK_FIVE("bluestrike_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		MASK_SIX("bluestrike_6", MASK_MAT, 16, HYPER_ARCHOS_RING, 4),
		MASK_SEVENA("bluestrike_7a", MASK_MAT, 32, FORTITUDE_AUGMENT, 16),
		MASK_SEVENB("bluestrike_7b", MASK_MAT, 32, POTENCY_AUGMENT, 16),
		MASK_SEVENC("bluestrike_7c", MASK_MAT, 32, ALACRITY_AUGMENT, 16),

		HALLS_ONE("gallery1_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		HALLS_TWO("gallery1_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		HALLS_THREE("gallery1_3", GALLEY_MAT, 12, HYPER_ARCHOS_RING, 4),
		HALLS_FOUR("gallery1_4", GALLEY_MAT, 24, HYPER_ARCHOS_RING, 4),
		HALLS_FIVE("gallery1_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		HALLS_SIX("gallery1_6", GALLEY_MAT, 34, HYPER_ARCHOS_RING, 4),
		HALLS_SEVENA("gallery1_7a", GALLEY_MAT, 64, FORTITUDE_AUGMENT, 16),
		HALLS_SEVENB("gallery1_7b", GALLEY_MAT, 64, POTENCY_AUGMENT, 16),
		HALLS_SEVENC("gallery1_7c", GALLEY_MAT, 64, ALACRITY_AUGMENT, 16),

		GODSPORE_ONE("godspore_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		GODSPORE_TWO("godspore_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		GODSPORE_THREE("godspore_3", GODSPORE_MAT, 6, HYPER_ARCHOS_RING, 4),
		GODSPORE_FOUR("godspore_4", GODSPORE_MAT, 12, HYPER_ARCHOS_RING, 4),
		GODSPORE_FIVE("godspore_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		GODSPORE_SIX("godspore_6", GODSPORE_MAT, 18, HYPER_ARCHOS_RING, 4),
		GODSPORE_SEVENA("godspore_7a", GODSPORE_MAT, 32, FORTITUDE_AUGMENT, 16),
		GODSPORE_SEVENB("godspore_7b", GODSPORE_MAT, 32, POTENCY_AUGMENT, 16),
		GODSPORE_SEVENC("godspore_7c", GODSPORE_MAT, 32, ALACRITY_AUGMENT, 16),

		TRUENORTH_ONE("truenorth_1", "epic:r3/items/currency/godtree_carving", 4, HYPER_ARCHOS_RING, 1),
		TRUENORTH_TWO("truenorth_2", PULSATING_DIAMOND, 2, HYPER_ARCHOS_RING, 3),
		TRUENORTH_THREE("truenorth_3", "epic:r3/shrine/curse_of_the_dark_soul", 1, HYPER_ARCHOS_RING, 4),
		TRUENORTH_FOUR("truenorth_4", INVALID_ITEM, 1, INVALID_ITEM, 1),
		TRUENORTH_FIVE("truenorth_5", INVALID_ITEM, 1, INVALID_ITEM, 1),
		TRUENORTH_SIX("truenorth_6", INVALID_ITEM, 1, INVALID_ITEM, 1),
		TRUENORTH_SEVENA("truenorth_7a", INVALID_ITEM, 1, INVALID_ITEM, 1),
		TRUENORTH_SEVENB("truenorth_7b", INVALID_ITEM, 1, INVALID_ITEM, 1),
		TRUENORTH_SEVENC("truenorth_7c", INVALID_ITEM, 1, INVALID_ITEM, 1),

		MISC_ONE("misc_1", PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 1),
		MISC_TWO("misc_2", PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 3),
		MISC_THREE("misc_3", PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 4),
		MISC_FOUR("misc_4", PULSATING_SHARD, 12, HYPER_ARCHOS_RING, 4),
		MISC_FIVE("misc_5", PULSATING_SHARD, 18, HYPER_ARCHOS_RING, 4),
		MISC_SIX("misc_6", PULSATING_DIAMOND, 8, HYPER_ARCHOS_RING, 4),
		MISC_SEVENA("misc_7a", PULSATING_SHARD, 32, FORTITUDE_AUGMENT, 16),
		MISC_SEVENB("misc_7b", PULSATING_SHARD, 32, POTENCY_AUGMENT, 16),
		MISC_SEVENC("misc_7c", PULSATING_SHARD, 32, ALACRITY_AUGMENT, 16),

		DEFAULT("default", INVALID_ITEM, 1, INVALID_ITEM, 1),

		// Exalted
		WHITE_ONE("white_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		WHITE_TWO("white_2", WHITE_MAT, 3, HYPER_ARCHOS_RING, 3),
		WHITE_THREE("white_3", WHITE_MAT, 6, HYPER_ARCHOS_RING, 4),
		WHITE_FOUR("white_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		WHITE_FIVE("white_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		WHITE_SIX("white_6", WHITE_MAT, 16, HYPER_ARCHOS_RING, 4),
		WHITE_SEVENA("white_7a", WHITE_MAT, 32, FORTITUDE_AUGMENT, 16),
		WHITE_SEVENB("white_7b", WHITE_MAT, 32, POTENCY_AUGMENT, 16),
		WHITE_SEVENC("white_7c", WHITE_MAT, 32, ALACRITY_AUGMENT, 16),

		// Exalted
		ORANGE_ONE("orange_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		ORANGE_TWO("orange_2", ORANGE_MAT, 3, HYPER_ARCHOS_RING, 3),
		ORANGE_THREE("orange_3", ORANGE_MAT, 6, HYPER_ARCHOS_RING, 4),
		ORANGE_FOUR("orange_4", PULSATING_DIAMOND, 3, HYPER_ARCHOS_RING, 4),
		ORANGE_FIVE("orange_5", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		ORANGE_SIX("orange_6", ORANGE_MAT, 16, HYPER_ARCHOS_RING, 4),
		ORANGE_SEVENA("orange_7a", ORANGE_MAT, 32, FORTITUDE_AUGMENT, 16),
		ORANGE_SEVENB("orange_7b", ORANGE_MAT, 32, POTENCY_AUGMENT, 16),
		ORANGE_SEVENC("orange_7c", ORANGE_MAT, 32, ALACRITY_AUGMENT, 16);

		private final String mLabel;
		private final String mPathA;
		private final int mCostA;
		private final String mPathB;
		private final int mCostB;

		MasterworkCost(String label, String pathA, int costA, String pathB, int costB) {
			mLabel = label;
			mPathA = pathA;
			mCostA = costA;
			mPathB = pathB;
			mCostB = costB;
		}

		public static MasterworkCost getMasterworkCost(@Nullable String label) {
			if (label == null) {
				return DEFAULT;
			}
			for (MasterworkCost selection : MasterworkCost.values()) {
				if (selection.getLabel().equals(label)) {
					return selection;
				}
			}

			String[] splitLabel = label.split("_");
			String miscLabel = "misc_" + splitLabel[splitLabel.length - 1];
			for (MasterworkCost selection : MasterworkCost.values()) {
				if (selection.getLabel().equals(miscLabel)) {
					return selection;
				}
			}

			return DEFAULT;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getPathA() {
			return mPathA;
		}

		public String getPathB() {
			return mPathB;
		}

		public int getCostA() {
			return mCostA;
		}

		public int getCostB() {
			return mCostB;
		}
	}

	public static List<String> getCostStringList(MasterworkCost m, Player p) {
		String itemName;
		String strA = m.getCostA() + " ";
		itemName = ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
			NamespacedKeyUtils.fromString(m.getPathA())));
		strA += itemName;
		if (m.getCostA() > 1) {
			strA += itemNameSuffix(p, itemName);
		}
		strA += " and";

		String strB = m.getCostB() + " ";
		itemName = ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
			NamespacedKeyUtils.fromString(m.getPathB())));
		strB += itemName;
		if (m.getCostB() > 1) {
			strB += itemNameSuffix(p, itemName);
		}
		return List.of(strA, strB);
	}

	private static String itemNameSuffix(Player p, String itemName) {
		if (itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
			NamespacedKeyUtils.fromString(GALLEY_MAT)))) || itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
			NamespacedKeyUtils.fromString(GODSPORE_MAT))))) {
			//Torn Canvas or Fungal Remnants, do nothing
			return "";
		}
		return "s";
	}

	public static boolean canPayCost(MasterworkCost m, Player p, boolean isRefund) {
		if (p.getGameMode() == GameMode.CREATIVE || isRefund) {
			return true;
		}

		PlayerInventory inventory = p.getInventory();
		ItemStack itemA = InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(m.getPathA()));
		ItemStack itemB = InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(m.getPathB()));
		if (!inventory.containsAtLeast(itemA, m.getCostA()) || !inventory.containsAtLeast(itemB, m.getCostB())) {
			return false;
		}

		return true;
	}

	public static void payCost(MasterworkCost m, Player p, boolean isRefund) {
		//if the player is in creative -> free upgrade
		if (p.getGameMode() == GameMode.CREATIVE) {
			AuditListener.log("[Masterwork] Player " + p.getName() + (isRefund ? " downgraded" : " upgraded") + " an item in creative mode (cost=" + m.mLabel + ")");
			return;
		}

		PlayerInventory inventory = p.getInventory();
		ItemStack itemA = InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(), NamespacedKeyUtils.fromString(m.getPathA()));
		ItemStack itemB = InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(), NamespacedKeyUtils.fromString(m.getPathB()));

		itemA.setAmount(m.getCostA());
		itemB.setAmount(m.getCostB());

		if (isRefund) {
			InventoryUtils.giveItem(p, itemA);
			InventoryUtils.giveItem(p, itemB);
		} else {
			inventory.removeItem(itemA);
			inventory.removeItem(itemB);
		}
	}

	public static boolean isMasterwork(ItemStack item) {
		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING) {
			return false;
		} else if (m == Masterwork.ZERO || m == Masterwork.I || m == Masterwork.II || m == Masterwork.III
			                                                                                                                      || m == Masterwork.IV || m == Masterwork.V || m == Masterwork.VI || m == Masterwork.VIIA
			                                                                                                                      || m == Masterwork.VIIB || m == Masterwork.VIIC) {
			return true;
		}
		return false;
	}

	public static List<ItemStack> getAllMasterworks(ItemStack item, Player p) {
		String basePath = "epic:r3/masterwork/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/" + toCleanPathName(ItemUtils.getPlainName(item));
		ArrayList<String> paths = new ArrayList<>();
		Masterwork m = ItemStatUtils.getMasterwork(item);
		//If nonexistent yet on this page, display nothing
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING) {
			return new ArrayList<>();
		}

		for (int i = 0; i < 7; i++) {
			paths.add(basePath + "_m" + i);
		}
		String abc = "abc";
		for (int i = 0; i < abc.length(); i++) {
			paths.add(basePath + "m7" + abc.charAt(i));
		}

		//TODO: Replace with next max level
		List<ItemStack> realItems = paths.stream().filter(s -> InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(s)) != null)
			                            .filter(s -> s.substring(s.lastIndexOf('m') + 1).matches("[0123]"))
			                            .map(s -> InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(s))).collect(Collectors.toList());
		return realItems;
	}

	public static @Nullable ItemStack getBaseMasterwork(ItemStack item, Player p) {
		List<ItemStack> allMasterworks = getAllMasterworks(item, p);
		return allMasterworks.isEmpty() ? null : allMasterworks.get(0);
	}

	public static String getNextItemPath(ItemStack item) {
		Masterwork nextM;
		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING
			    || m == Masterwork.VIIA || m == Masterwork.VIIB || m == Masterwork.VIIC || m == Masterwork.VI) {
			nextM = Masterwork.ERROR;
		} else {
			nextM = switch (Objects.requireNonNull(m)) {
				case ZERO -> Masterwork.I;
				case I -> Masterwork.II;
				case II -> Masterwork.III;
				case III -> Masterwork.IV;
				case IV -> Masterwork.V;
				case V -> Masterwork.VI;
				default -> Masterwork.I;
			};
		}
		return getItemPath(item, nextM);
	}

	public static String getPrevItemPath(ItemStack item) {
		Masterwork prevM;
		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING
			|| m == Masterwork.VIIA || m == Masterwork.VIIB || m == Masterwork.VIIC) {
			prevM = Masterwork.ERROR;
		} else {
			prevM = switch (Objects.requireNonNull(m)) {
				case VI -> Masterwork.V;
				case V -> Masterwork.IV;
				case IV -> Masterwork.III;
				case III -> Masterwork.II;
				case II -> Masterwork.I;
				case I -> Masterwork.ZERO;
				default -> Masterwork.ZERO;
			};
		}
		return getItemPath(item, prevM);
	}

	public static String getItemPath(ItemStack item, Masterwork masterwork) {
		if (masterwork == Masterwork.ERROR) {
			return "epic:r3/masterwork/invalid_masterwork_selection";
		}
		return "epic:r3/masterwork" + "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/"
			       + toCleanPathName(ItemUtils.getPlainName(item)) + "_m" + masterwork.getName();
	}

	public static String getItemPath(ItemStack item) {
		String path = "epic:r3/masterwork";

		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING) {
			path += "/invalid_masterwork_selection";
		} else if (m == Masterwork.ZERO) {
			path += "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/"
				        + toCleanPathName(ItemUtils.getPlainName(item));
		} else {
			path += "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/" +
				        toCleanPathName(ItemUtils.getPlainName(item)) + "_m" + m.getName();
		}

		return path;
	}

	public static String getSixItemPath(ItemStack item) {
		String path = "epic:r3/masterwork";

		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING) {
			path += "/invalid_masterwork_selection";
		} else {
			path += "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/"
				        + toCleanPathName(ItemUtils.getPlainName(item)) + "_m6";
		}

		return path;
	}

	public static String getSevenItemPath(ItemStack item, Masterwork sevenSelection) {
		String path = "epic:r3/masterwork";

		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING
			    || (sevenSelection != Masterwork.VIIA && sevenSelection != Masterwork.VIIB && sevenSelection != Masterwork.VIIC)) {
			path += "/invalid_masterwork_selection";
		} else {
			path += "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/"
				        + toCleanPathName(ItemUtils.getPlainName(item)) + "_m" + sevenSelection.getName();
		}

		return path;
	}

	public static int getMasterworkAsInt(Masterwork m) {
		return switch (Objects.requireNonNull(m)) {
			case ZERO -> 0;
			case I -> 1;
			case II -> 2;
			case III -> 3;
			case IV -> 4;
			case V -> 5;
			case VI -> 6;
			case VIIA, VIIB, VIIC -> 7;
			default -> -1;
		};
	}

	public static void animate(Player player, Masterwork tier) {
		Color colorChoice = Color.ORANGE;
		if (getMasterworkAsInt(tier) == 7) {
			if (tier == Masterwork.VIIA) {
				colorChoice = Color.RED;
			} else if (tier == Masterwork.VIIB) {
				colorChoice = Color.AQUA;
			} else if (tier == Masterwork.VIIC) {
				colorChoice = Color.YELLOW;
			}
		}

		Location loc = player.getLocation();
		EntityUtils.fireworkAnimation(loc, List.of(Color.GRAY, Color.WHITE, colorChoice), FireworkEffect.Type.BURST, 5);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> player.playSound(loc, Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.f, 1.f), 5);
	}

	public static ItemStack preserveModified(ItemStack base, ItemStack upgrade) {
		ItemStack newUpgrade = ItemUtils.clone(upgrade);

		NBTItem playerItemNbt = new NBTItem(base);
		NBTItem newUpgradeNbt = new NBTItem(newUpgrade);
		NBTCompound playerModified = ItemStatUtils.getPlayerModified(playerItemNbt);

		if (playerModified != null) {
			ItemStatUtils.addPlayerModified(newUpgradeNbt).mergeCompound(playerModified);
			newUpgrade = newUpgradeNbt.getItem();
			ItemStatUtils.generateItemStats(newUpgrade);
		}

		// Carry over the durability to not make the trade repair items (a possible shattered state is copied via lore)
		if (newUpgrade.getItemMeta() instanceof Damageable newResultMeta && base.getItemMeta() instanceof Damageable playerItemMeta) {
			newResultMeta.setDamage(playerItemMeta.getDamage());
			newUpgrade.setItemMeta(newResultMeta);
		}

		// Carry over the current arrow of a crossbow if the player item has an arrow but the result item doesn't have one
		if (newUpgrade.getItemMeta() instanceof CrossbowMeta newResultMeta && base.getItemMeta() instanceof CrossbowMeta playerItemMeta
			    && !newResultMeta.hasChargedProjectiles() && playerItemMeta.hasChargedProjectiles()) {
			newResultMeta.setChargedProjectiles(playerItemMeta.getChargedProjectiles());
			newUpgrade.setItemMeta(newResultMeta);
		}

		// Carry over leather armor dye
		if (newUpgrade.getItemMeta() instanceof LeatherArmorMeta newResultMeta && base.getItemMeta() instanceof LeatherArmorMeta playerItemMeta) {
			newResultMeta.setColor(playerItemMeta.getColor());
			newUpgrade.setItemMeta(newResultMeta);
		}

		// Carry over shield pattern
		if (base.getType() == Material.SHIELD && upgrade.getType() == Material.SHIELD
			    && newUpgrade.getItemMeta() instanceof BlockStateMeta newResultMeta && base.getItemMeta() instanceof BlockStateMeta playerItemMeta) {
			newResultMeta.setBlockState(playerItemMeta.getBlockState());
			newUpgrade.setItemMeta(newResultMeta);
		}

		return newUpgrade;
	}

	private static String toCleanPathName(String str) {
		// Copied logic from automation code
		str = str.toLowerCase();
		str = str.replaceAll("\\s+", "_");
		str = str.replaceAll("~", "_");
		str = str.replaceAll("[^a-zA-Z0-9-_]", "");
		return str;
	}

}
