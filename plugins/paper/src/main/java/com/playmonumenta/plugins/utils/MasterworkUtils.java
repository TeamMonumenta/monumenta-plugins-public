package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils.Masterwork;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

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

	public enum MasterworkCost {
		FOREST_ONE("forest_1", FOREST_FRAG, 1, HYPER_ARCHOS_RING, 1),
		FOREST_TWO("forest_2", FOREST_FRAG, 2, HYPER_ARCHOS_RING, 2),
		FOREST_THREE("forest_3", FOREST_FRAG, 3, HYPER_ARCHOS_RING, 3),
		FOREST_FOUR("forest_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		FOREST_FIVE("forest_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		FOREST_SIX("forest_6", FOREST_MAT, 16, HYPER_ARCHOS_RING, 4),
		FOREST_SEVENA("forest_7a", FOREST_MAT, 32, FORTITUDE_AUGMENT, 16),
		FOREST_SEVENB("forest_7b", FOREST_MAT, 32, POTENCY_AUGMENT, 16),
		FOREST_SEVENC("forest_7c", FOREST_MAT, 32, ALACRITY_AUGMENT, 16),

		KEEP_ONE("keep_1", KEEP_FRAG, 1, HYPER_ARCHOS_RING, 1),
		KEEP_TWO("keep_2", KEEP_FRAG, 2, HYPER_ARCHOS_RING, 2),
		KEEP_THREE("keep_3", KEEP_FRAG, 3, HYPER_ARCHOS_RING, 3),
		KEEP_FOUR("keep_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		KEEP_FIVE("keep_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		KEEP_SIX("keep_6", KEEP_MAT, 16, HYPER_ARCHOS_RING, 4),
		KEEP_SEVENA("keep_7a", KEEP_MAT, 32, FORTITUDE_AUGMENT, 16),
		KEEP_SEVENB("keep_7b", KEEP_MAT, 32, POTENCY_AUGMENT, 16),
		KEEP_SEVENC("keep_7c", KEEP_MAT, 32, ALACRITY_AUGMENT, 16),

		SKT_ONE("silver_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		SKT_TWO("silver_2", SKT_FRAG, 2, HYPER_ARCHOS_RING, 2),
		SKT_THREE("silver_3", SKT_FRAG, 3, HYPER_ARCHOS_RING, 3),
		SKT_FOUR("silver_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		SKT_FIVE("silver_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		SKT_SIX("silver_6", SKT_MAT, 16, HYPER_ARCHOS_RING, 4),
		SKT_SEVENA("silver_7a", SKT_MAT, 32, FORTITUDE_AUGMENT, 16),
		SKT_SEVENB("silver_7b", SKT_MAT, 32, POTENCY_AUGMENT, 16),
		SKT_SEVENC("silver_7c", SKT_MAT, 32, ALACRITY_AUGMENT, 16),

		BLUE_ONE("blue_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		BLUE_TWO("blue_2", BLUE_FRAG, 2, HYPER_ARCHOS_RING, 2),
		BLUE_THREE("blue_3", BLUE_FRAG, 3, HYPER_ARCHOS_RING, 3),
		BLUE_FOUR("blue_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		BLUE_FIVE("blue_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		BLUE_SIX("blue_6", BLUE_MAT, 16, HYPER_ARCHOS_RING, 4),
		BLUE_SEVENA("blue_7a", BLUE_MAT, 32, FORTITUDE_AUGMENT, 16),
		BLUE_SEVENB("blue_7b", BLUE_MAT, 32, POTENCY_AUGMENT, 16),
		BLUE_SEVENC("blue_7c", BLUE_MAT, 32, ALACRITY_AUGMENT, 16),

		BROWN_ONE("brown_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		BROWN_TWO("brown_2", BROWN_FRAG, 2, HYPER_ARCHOS_RING, 2),
		BROWN_THREE("brown_3", BROWN_FRAG, 3, HYPER_ARCHOS_RING, 3),
		BROWN_FOUR("brown_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		BROWN_FIVE("brown_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		BROWN_SIX("brown_6", BROWN_MAT, 16, HYPER_ARCHOS_RING, 4),
		BROWN_SEVENA("brown_7a", BROWN_MAT, 32, FORTITUDE_AUGMENT, 16),
		BROWN_SEVENB("brown_7b", BROWN_MAT, 32, POTENCY_AUGMENT, 16),
		BROWN_SEVENC("brown_7c", BROWN_MAT, 32, ALACRITY_AUGMENT, 16),

		PORTAL_ONE("science_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		PORTAL_TWO("science_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		PORTAL_THREE("science_3", PORTAL_FRAG, 3, HYPER_ARCHOS_RING, 3),
		PORTAL_FOUR("science_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		PORTAL_FIVE("science_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		PORTAL_SIX("science_6", PORTAL_MAT, 16, HYPER_ARCHOS_RING, 4),
		PORTAL_SEVENA("science_7a", PORTAL_MAT, 32, FORTITUDE_AUGMENT, 16),
		PORTAL_SEVENB("science_7b", PORTAL_MAT, 32, POTENCY_AUGMENT, 16),
		PORTAL_SEVENC("science_7c", PORTAL_MAT, 32, ALACRITY_AUGMENT, 16),

		MASK_ONE("bluestrike_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		MASK_TWO("bluestrike_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		MASK_THREE("bluestrike_3", MASK_FRAG, 3, HYPER_ARCHOS_RING, 3),
		MASK_FOUR("bluestrike_4", PULSATING_DIAMOND, 6, HYPER_ARCHOS_RING, 4),
		MASK_FIVE("bluestrike_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		MASK_SIX("bluestrike_6", MASK_MAT, 16, HYPER_ARCHOS_RING, 4),
		MASK_SEVENA("bluestrike_7a", MASK_MAT, 32, FORTITUDE_AUGMENT, 16),
		MASK_SEVENB("bluestrike_7b", MASK_MAT, 32, POTENCY_AUGMENT, 16),
		MASK_SEVENC("bluestrike_7c", MASK_MAT, 32, ALACRITY_AUGMENT, 16),

		HALLS_ONE("gallery1_1", INVALID_ITEM, 1, INVALID_ITEM, 1),
		HALLS_TWO("gallery1_2", INVALID_ITEM, 1, INVALID_ITEM, 1),
		HALLS_THREE("gallery1_3", GALLEY_MAT, 12, HYPER_ARCHOS_RING, 3),
		HALLS_FOUR("gallery1_4", GALLEY_MAT, 24, HYPER_ARCHOS_RING, 4),
		HALLS_FIVE("gallery1_5", PULSATING_DIAMOND, 12, HYPER_ARCHOS_RING, 4),
		HALLS_SIX("gallery1_6", GALLEY_MAT, 34, HYPER_ARCHOS_RING, 4),
		HALLS_SEVENA("gallery1_7a", GALLEY_MAT, 64, FORTITUDE_AUGMENT, 16),
		HALLS_SEVENB("gallery1_7b", GALLEY_MAT, 64, POTENCY_AUGMENT, 16),
		HALLS_SEVENC("gallery1_7c", GALLEY_MAT, 64, ALACRITY_AUGMENT, 16),

		MISC_ONE("misc_1", PULSATING_DIAMOND, 1, HYPER_ARCHOS_RING, 1),
		MISC_TWO("misc_2", PULSATING_DIAMOND, 2, HYPER_ARCHOS_RING, 2),
		MISC_THREE("misc_3", PULSATING_SHARD, 6, HYPER_ARCHOS_RING, 3),
		MISC_FOUR("misc_4", PULSATING_SHARD, 12, HYPER_ARCHOS_RING, 4),
		MISC_FIVE("misc_5", PULSATING_SHARD, 18, HYPER_ARCHOS_RING, 4),
		MISC_SIX("misc_6", PULSATING_DIAMOND, 18, HYPER_ARCHOS_RING, 4),
		MISC_SEVENA("misc_7a", PULSATING_SHARD, 32, FORTITUDE_AUGMENT, 16),
		MISC_SEVENB("misc_7b", PULSATING_SHARD, 32, POTENCY_AUGMENT, 16),
		MISC_SEVENC("misc_7c", PULSATING_SHARD, 32, ALACRITY_AUGMENT, 16),

		DEFAULT("default", INVALID_ITEM, 1, INVALID_ITEM, 1);

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
			NamespacedKeyUtils.fromString(GALLEY_MAT))))) {
			//Torn Canvas, do nothing
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
			Plugin.getInstance().getLogger().warning("[Masterwork] Player: " + p.getName() + " upgraded an item while be on creative mode!");
			return;
		}

		PlayerInventory inventory = p.getInventory();
		ItemStack itemA = InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(m.getPathA()));
		ItemStack itemB = InventoryUtils.getItemFromLootTable(p, NamespacedKeyUtils.fromString(m.getPathB()));

		if (isRefund) {
			itemA.setAmount(m.getCostA());
			itemB.setAmount(48);

			InventoryUtils.giveItem(p, itemA);
			InventoryUtils.giveItem(p, itemB);
		} else {
			itemA.setAmount(m.getCostA());
			itemB.setAmount(m.getCostB());

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

	public static String getNextItemPath(ItemStack item) {
		String path = "epic:r3/masterwork";

		Masterwork m = ItemStatUtils.getMasterwork(item);
		if (m == Masterwork.ERROR || m == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING
			|| m == Masterwork.VIIA || m == Masterwork.VIIB || m == Masterwork.VIIC || m == Masterwork.VI) {
			path += "/invalid_masterwork_selection";
		} else {
			Masterwork nextM = switch (Objects.requireNonNull(m)) {
				case ZERO -> Masterwork.I;
				case I -> Masterwork.II;
				case II -> Masterwork.III;
				case III -> Masterwork.IV;
				case IV -> Masterwork.V;
				case V -> Masterwork.VI;
				default -> Masterwork.I;
			};
			path += "/" + toCleanPathName(ItemUtils.getPlainName(item)) + "/"
				+ toCleanPathName(ItemUtils.getPlainName(item)) + "_m" + nextM.getName();
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
		Location loc = player.getLocation();
		Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
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
		fwBuilder.withColor(Color.GRAY, Color.WHITE, colorChoice);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);


		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
				player.playSound(loc, Sound.BLOCK_ANVIL_USE, 1.f, 1.f);
			}
		}.runTaskLater(Plugin.getInstance(), 5);
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
			newUpgrade.setItemMeta((ItemMeta) newResultMeta);
		}

		// Carry over the current arrow of a crossbow if the player item has an arrow but the result item doesn't have one
		if (newUpgrade.getItemMeta() instanceof CrossbowMeta newResultMeta && base.getItemMeta() instanceof CrossbowMeta playerItemMeta
			&& !newResultMeta.hasChargedProjectiles() && playerItemMeta.hasChargedProjectiles()) {
			newResultMeta.setChargedProjectiles(playerItemMeta.getChargedProjectiles());
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
