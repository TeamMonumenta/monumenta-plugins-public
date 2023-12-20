package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.AuditListener;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
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

	private static final String STAR_FRAG = "epic:r3/fragments/star_point_fragment";
	private static final String STAR_MAT = "epic:r3/items/currency/dust_of_the_herald";

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

	private static final String GALLERY1_MAT = "epic:r3/gallery/items/torn_canvas";
	private static final String GALLERY2_MAT = "epic:r3/gallery/map2/deathly_piece_of_eight";

	private static final String ZENITH_MAT = "epic:r3/items/currency/indigo_blightdust";

	private static final String GODSPORE_MAT = "epic:r3/godspore/items/fungal_remnants";

	private static final String SIRIUS_FRAG = "epic:r3/fragments/starblight_fragment";

	private static final String FISH_MAT = "epic:r3/items/fishing/sand_dollar";

	// Exalted Dungeons
	private static final String WHITE_MAT = "epic:r1/delves/white/auxiliary/delve_material";
	private static final String ORANGE_MAT = "epic:r1/delves/orange/auxiliary/delve_material";
	private static final String MAGENTA_MAT = "epic:r1/delves/magenta/auxiliary/delve_material";
	private static final String LIGHTBLUE_MAT = "epic:r1/delves/lightblue/auxiliary/delve_material";
	private static final String YELLOW_MAT = "epic:r1/delves/yellow/auxiliary/delve_material";
	private static final String WILLOWS_MAT = "epic:r1/delves/willows/auxiliary/echoes_of_the_veil";
	private static final String REVERIE_MAT = "epic:r1/delves/reverie/auxiliary/delve_material";

	public record MasterworkCostLevel(String item1, int amount1, String item2, int amount2, @Nullable String item3, int amount3) {
		public List<String> getCostStringList(Player p) {
			String itemName;
			String strA = amount1 + " ";
			itemName = ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
				NamespacedKeyUtils.fromString(item1)));
			strA += itemName;
			if (amount1 > 1) {
				strA += itemNameSuffix(p, itemName);
			}

			if (item3 == null) {
				strA += " and";
			} else {
				strA += ",";
			}

			String strB = amount2 + " ";
			itemName = ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
				NamespacedKeyUtils.fromString(item2)));
			strB += itemName;
			if (amount2 > 1) {
				strB += itemNameSuffix(p, itemName);
			}

			if (item3 == null) {
				return List.of(strA, strB);
			} else {
				strB += " and";
			}

			String strC = amount3 + " ";
			itemName = ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p,
				NamespacedKeyUtils.fromString(item3)));
			strC += itemName;
			if (amount3 > 1) {
				strC += itemNameSuffix(p, itemName);
			}
			return List.of(strA, strB, strC);
		}

		public boolean tryPayCost(Player p, ItemStack item, boolean isRefund, Masterwork masterwork) {
			//if the player is in creative -> free upgrade
			if (p.getGameMode() == GameMode.CREATIVE) {
				AuditListener.log("[Masterwork] Player " + p.getName() + (isRefund ? " downgraded" : " upgraded") + " an item in creative mode");
				return true;
			}

			PlayerInventory inventory = p.getInventory();
			ItemStack itemA = InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(), NamespacedKeyUtils.fromString(item1));
			ItemStack itemB = InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(), NamespacedKeyUtils.fromString(item2));
			ItemStack itemC = item3 == null ? null : InventoryUtils.getItemFromLootTableOrThrow(p.getLocation(), NamespacedKeyUtils.fromString(item3));

			itemA.setAmount(amount1);
			itemB.setAmount(amount2);
			if (itemC != null) {
				itemC.setAmount(amount3);
			}

			if (isRefund) {
				String auditString = "[Masterwork] Refund - player=" + p.getName() + " item='" + ItemUtils.getPlainName(item) + "' from level=" + masterwork.getName() + " stack size=" + item.getAmount()
					+ " material refund=" + ItemUtils.getPlainName(itemA) + ":" + itemA.getAmount() + "," + ItemUtils.getPlainName(itemB) + ":" + itemB.getAmount();
				if (itemC != null) {
					auditString += "," + ItemUtils.getPlainName(itemC) + ":" + itemC.getAmount();
				}
				AuditListener.logPlayer(auditString);

				InventoryUtils.giveItem(p, itemA);
				InventoryUtils.giveItem(p, itemB);
				if (itemC != null) {
					InventoryUtils.giveItem(p, itemC);
				}
				return true;
			} else {
				if (WalletUtils.tryToPayFromInventoryAndWallet(p, itemC == null ? List.of(itemA, itemB) : List.of(itemA, itemB, itemC))) {
					String auditString = "[Masterwork] Purchase - player=" + p.getName() + " item='" + ItemUtils.getPlainName(item) + "' to level=" + masterwork.getName() + " stack size=" + item.getAmount()
						                     + " material refund=" + ItemUtils.getPlainName(itemA) + ":" + itemA.getAmount() + "," + ItemUtils.getPlainName(itemB) + ":" + itemB.getAmount();
					if (itemC != null) {
						auditString += "," + ItemUtils.getPlainName(itemC) + ":" + itemC.getAmount();
					}
					AuditListener.logPlayer(auditString);
					return true;
				}
				return false;
			}
		}
	}

	private static final MasterworkCostLevel DEFAULT = new MasterworkCostLevel(INVALID_ITEM, 1, INVALID_ITEM, 1, INVALID_ITEM, 1);

	public static class MasterworkCost {
		private final Map<Masterwork, MasterworkCostLevel> mLevelMap = new HashMap<>();
		private final Masterwork mMinLevel;

		protected MasterworkCost(Masterwork minLevel) {
			mMinLevel = minLevel;
		}

		protected void put(Masterwork masterwork, String item1, int amount1, String item2, int amount2) {
			put(masterwork, item1, amount1, item2, amount2, null, 0);
		}

		protected void put(Masterwork masterwork, String item1, int amount1, String item2, int amount2, @Nullable String item3, int amount3) {
			if (mMinLevel.lessThanOrEqualTo(masterwork)) {
				mLevelMap.put(masterwork, new MasterworkCostLevel(item1, amount1, item2, amount2, item3, amount3));
			}
		}

		public MasterworkCostLevel get(Masterwork masterwork) {
			return mLevelMap.getOrDefault(masterwork, DEFAULT);
		}
	}

	private static class Misc extends MasterworkCost {
		private Misc() {
			super(Masterwork.I);
			put(Masterwork.I, PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 1);
			put(Masterwork.II, PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 3);
			put(Masterwork.III, PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, PULSATING_SHARD, 12, HYPER_ARCHOS_RING, 5);
			put(Masterwork.V, PULSATING_SHARD, 18, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, PULSATING_DIAMOND, 8, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, PULSATING_SHARD, 32, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, PULSATING_SHARD, 32, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, PULSATING_SHARD, 32, ALACRITY_AUGMENT, 16);
		}
	}

	private static class Generic extends MasterworkCost {
		private Generic(String frag, String mat, Masterwork minLevel) {
			super(minLevel);
			put(Masterwork.I, frag, 1, HYPER_ARCHOS_RING, 1);
			put(Masterwork.II, frag, 1, HYPER_ARCHOS_RING, 3);
			put(Masterwork.III, frag, 1, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, frag, 1, PULSATING_DIAMOND, 1, HYPER_ARCHOS_RING, 4);
			put(Masterwork.V, PULSATING_DIAMOND, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, mat, 16, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, mat, 32, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, mat, 32, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, mat, 32, ALACRITY_AUGMENT, 16);
		}
	}

	private static class Gallery extends MasterworkCost {
		private Gallery(String mat) {
			super(Masterwork.III);
			put(Masterwork.III, mat, 10, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, mat, 12, HYPER_ARCHOS_RING, 5);
			put(Masterwork.V, PULSATING_DIAMOND, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, mat, 24, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, mat, 48, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, mat, 48, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, mat, 48, ALACRITY_AUGMENT, 16);
		}
	}

	private static class Zenith extends MasterworkCost {
		private Zenith(String mat) {
			super(Masterwork.IV);
			put(Masterwork.IV, mat, 8, HYPER_ARCHOS_RING, 4);
		}
	}

	private static class Boss extends MasterworkCost {
		private Boss(String mat) {
			super(Masterwork.III);
			put(Masterwork.III, mat, 6, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, mat, 10, HYPER_ARCHOS_RING, 4);
			put(Masterwork.V, PULSATING_DIAMOND, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, mat, 18, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, mat, 32, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, mat, 32, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, mat, 32, ALACRITY_AUGMENT, 16);
		}
	}

	private static class WorldBoss extends MasterworkCost {
		private WorldBoss(String frag) {
			super(Masterwork.IV);
			put(Masterwork.IV, frag, 2, HYPER_ARCHOS_RING, 4);
		}
	}

	private static class Fish extends MasterworkCost {
		private Fish() {
			super(Masterwork.II);
			put(Masterwork.II, FISH_MAT, 4, HYPER_ARCHOS_RING, 3);
			put(Masterwork.III, FISH_MAT, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, FISH_MAT, 6, HYPER_ARCHOS_RING, 5);
			put(Masterwork.V, PULSATING_DIAMOND, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, FISH_MAT, 12, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, FISH_MAT, 20, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, FISH_MAT, 20, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, FISH_MAT, 20, ALACRITY_AUGMENT, 16);
		}
	}

	private static class Exalted extends MasterworkCost {
		private Exalted(String mat, Masterwork minLevel) {
			super(minLevel);
			put(Masterwork.II, mat, 3, HYPER_ARCHOS_RING, 3);
			put(Masterwork.III, mat, 6, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, mat, 6, PULSATING_DIAMOND, 1, HYPER_ARCHOS_RING, 4);
			put(Masterwork.V, PULSATING_DIAMOND, 4, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VI, mat, 16, HYPER_ARCHOS_RING, 4);
			put(Masterwork.VIIA, mat, 32, FORTITUDE_AUGMENT, 16);
			put(Masterwork.VIIB, mat, 32, POTENCY_AUGMENT, 16);
			put(Masterwork.VIIC, mat, 32, ALACRITY_AUGMENT, 16);
		}
	}

	private static class TrueNorth extends MasterworkCost {
		private TrueNorth() {
			super(Masterwork.I);
			put(Masterwork.I, "epic:r3/items/currency/godtree_carving", 4, HYPER_ARCHOS_RING, 1);
			put(Masterwork.II, PULSATING_DIAMOND, 2, HYPER_ARCHOS_RING, 3);
			put(Masterwork.III, "epic:r3/shrine/curse_of_the_dark_soul", 1, HYPER_ARCHOS_RING, 4);
			put(Masterwork.IV, "epic:r3/shrine/gift_of_the_stars", 1, HYPER_ARCHOS_RING, 6);
		}
	}

	private static final Map<Location, MasterworkCost> MASTERWORK_COSTS = new HashMap<>();
	private static final MasterworkCost MISC = new Misc();

	static {
		MASTERWORK_COSTS.put(Location.FOREST, new Generic(FOREST_FRAG, FOREST_MAT, Masterwork.I));
		MASTERWORK_COSTS.put(Location.KEEP, new Generic(KEEP_FRAG, KEEP_MAT, Masterwork.I));
		MASTERWORK_COSTS.put(Location.STARPOINT, new Generic(STAR_FRAG, STAR_MAT, Masterwork.II));

		MASTERWORK_COSTS.put(Location.SILVER, new Generic(SKT_FRAG, SKT_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.BLUE, new Generic(BLUE_FRAG, BLUE_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.BROWN, new Generic(BROWN_FRAG, BROWN_MAT, Masterwork.II));

		MASTERWORK_COSTS.put(Location.SCIENCE, new Generic(PORTAL_FRAG, PORTAL_MAT, Masterwork.III));
		MASTERWORK_COSTS.put(Location.BLUESTRIKE, new Generic(MASK_FRAG, MASK_MAT, Masterwork.III));

		MASTERWORK_COSTS.put(Location.SANGUINEHALLS, new Gallery(GALLERY1_MAT));
		MASTERWORK_COSTS.put(Location.MARINANOIR, new Gallery(GALLERY2_MAT));

		MASTERWORK_COSTS.put(Location.ZENITH, new Zenith(ZENITH_MAT));

		MASTERWORK_COSTS.put(Location.GODSPORE, new Boss(GODSPORE_MAT));

		MASTERWORK_COSTS.put(Location.SIRIUS, new WorldBoss(SIRIUS_FRAG));

		MASTERWORK_COSTS.put(Location.FISHING, new Fish());

		MASTERWORK_COSTS.put(Location.WHITE, new Exalted(WHITE_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.ORANGE, new Exalted(ORANGE_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.MAGENTA, new Exalted(MAGENTA_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.LIGHTBLUE, new Exalted(LIGHTBLUE_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.YELLOW, new Exalted(YELLOW_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.WILLOWS, new Exalted(WILLOWS_MAT, Masterwork.II));
		MASTERWORK_COSTS.put(Location.REVERIE, new Exalted(REVERIE_MAT, Masterwork.II));

		MASTERWORK_COSTS.put(Location.TRUENORTH, new TrueNorth());
	}

	public static MasterworkCost getMasterworkCost(Location location) {
		return MASTERWORK_COSTS.getOrDefault(location, MISC);
	}

	public static MasterworkCost getMasterworkCost(ItemStack item) {
		return getMasterworkCost(ItemStatUtils.getLocation(item));
	}

	private static String itemNameSuffix(Player p, String itemName) {
		if (itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(GALLERY1_MAT)))) ||
			itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(GALLERY2_MAT)))) ||
			itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(GODSPORE_MAT)))) ||
			itemName.equals(ItemUtils.getPlainName(InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(ZENITH_MAT))))) {
			// Torn Canvas or Deathly Piece of Eight or Fungal Remnants or Indigo Blightdust, do nothing
			return "";
		}
		return "s";
	}

	public static boolean isMasterwork(ItemStack item) {
		Masterwork m = ItemStatUtils.getMasterwork(item);
		return !(m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING);
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
			paths.add(basePath + "_m7" + abc.charAt(i));
		}

		//TODO: Replace with next max level
		List<ItemStack> realItems = paths.stream().filter(s -> InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(s)) != null)
			                            .filter(s -> s.substring(s.lastIndexOf('m') + 1).matches("[01234]"))
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

		EntityUtils.fireworkAnimation(player.getLocation(), List.of(Color.GRAY, Color.WHITE, colorChoice), FireworkEffect.Type.BURST, 5);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.f, 1.f), 5);
	}

	public static ItemStack preserveModified(ItemStack base, ItemStack upgrade) {
		ItemStack newUpgrade = ItemUtils.clone(upgrade);

		NBTItem playerItemNbt = new NBTItem(base);
		NBTItem newUpgradeNbt = new NBTItem(newUpgrade);
		NBTCompound playerModified = playerItemNbt.getCompound(ItemStatUtils.MONUMENTA_KEY).getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);

		if (playerModified != null) {
			ItemStatUtils.addPlayerModified(newUpgradeNbt).mergeCompound(playerModified);
			newUpgrade = newUpgradeNbt.getItem();
			ItemUpdateHelper.generateItemStats(newUpgrade);
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
