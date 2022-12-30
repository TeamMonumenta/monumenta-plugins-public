package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobRarity;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TowerGuiShowMobs extends CustomInventory {

	private static final int[] VALID_MOBS_SLOT = {
	// 0,   1,  2,  3,  4,  5,  6,  7,    8
	/* 9*/ 10, 11, 12, 13, 14, 15, 16, //17
	/*18*/ 19, 20, 21, 22, 23, 24, 25, //26
	/*27*/ 28, 29, 30, 31, 32, 33, 34, //35
	/*36*/ 37, 38, 39, 40, 41, 42, 43  //44
	//45,  46, 47, 48, 49, 50, 51, 52,   53
	};

	private static final int VALID_MOBS_SLOT_SIZE = VALID_MOBS_SLOT.length;


	public static final List<TowerGuiItem> MOBS_ITEMS = new ArrayList<>();
	private static final List<TowerMobInfo> MOBS_INFO = new ArrayList<>();

	public static void loadGuiItems() {
		for (TowerMobInfo info : TowerFileUtils.getMobsByRarity(TowerMobRarity.COMMON)) {
			MOBS_ITEMS.add(new TowerGuiItem(info.getBuyableItem(), player -> true));
			MOBS_INFO.add(info);
		}
		for (TowerMobInfo info : TowerFileUtils.getMobsByRarity(TowerMobRarity.RARE)) {
			MOBS_ITEMS.add(new TowerGuiItem(info.getBuyableItem(), player -> true));
			MOBS_INFO.add(info);
		}
		for (TowerMobInfo info : TowerFileUtils.getMobsByRarity(TowerMobRarity.EPIC)) {
			MOBS_ITEMS.add(new TowerGuiItem(info.getBuyableItem(), player -> true));
			MOBS_INFO.add(info);
		}
		for (TowerMobInfo info : TowerFileUtils.getMobsByRarity(TowerMobRarity.LEGENDARY)) {
			MOBS_ITEMS.add(new TowerGuiItem(info.getBuyableItem(), player -> true));
			MOBS_INFO.add(info);
		}
	}

	private int mOffset = 0;
	private final int mGuiSize = Math.min(VALID_MOBS_SLOT_SIZE, MOBS_ITEMS.size());


	public TowerGuiShowMobs(Player owner) {
		super(owner, 54, "Blitz units");

		loadInv(owner);
	}

	private void loadInv(Player player) {
		mInventory.clear();

		ItemStack stack;
		for (int i = 0; i < mGuiSize; i++) {
			if (i + mOffset < MOBS_ITEMS.size()) {
				stack = MOBS_ITEMS.get(i).getItem(player);
				mInventory.setItem(VALID_MOBS_SLOT[i], stack);
			}
		}


		//fill white hole

		stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(Component.empty());
		stack.setItemMeta(meta);

		for (int i = 0; i < VALID_MOBS_SLOT_SIZE; i++) {
			if (mInventory.getItem(VALID_MOBS_SLOT[i]) == null) {
				mInventory.setItem(VALID_MOBS_SLOT[i], stack);
			}
		}

		stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		meta = stack.getItemMeta();
		meta.displayName(Component.empty());
		stack.setItemMeta(meta);

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, stack);
			}
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);

		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		Player player = ((Player)event.getWhoClicked());
		int slot = event.getSlot();

		if (validMobSlot(slot)) {
			mInventory.clear();
			player.closeInventory();
			new TowerGuiMobInfo(player, MOBS_INFO.get(getMobIndex(slot))).openInventory(player, Plugin.getInstance());
		}

	}


	private static boolean validMobSlot(int slot) {
		for (int i : VALID_MOBS_SLOT) {
			if (i == slot) {
				return true;
			}
		}
		return false;
	}

	private static int getMobIndex(int slot) {
		int ind = 0;
		for (int i : VALID_MOBS_SLOT) {
			if (i == slot) {
				return ind;
			}
			ind++;
		}
		return -1;
	}

}
