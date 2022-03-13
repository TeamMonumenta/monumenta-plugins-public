package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TowerGuiShowMobs extends CustomInventory {

	private static final Map<Player, TowerGuiShowMobs> mInstances = new LinkedHashMap<>();
	//used so we keep track of player instance of this class, we can save all item in a static way.


	private static final int[] VALID_MOBS_SLOT = {
	// 0,   1,  2,  3,  4,  5,  6,  7,    8
	/* 9*/ 10, 11, 12, 13, 14, 15, 16, //17
	/*18*/ 19, 20, 21, 22, 23, 24, 25, //26
	/*27*/ 28, 29, 30, 31, 32, 33, 34, //35
	/*36*/ 37, 38, 39, 40, 41, 42, 43  //44
	//45,  46, 47, 48, 49, 50, 51, 52,   53
	};

	private static final int VALID_MOBS_SLOT_SIZE = 28;

	private static final int ARROW_UP_SLOT = 17;
	private static final int ARROW_DOWN_SLOT = 44;

	public static final List<TowerGuiItem> MOBS_ITEMS = new ArrayList<>();

	private static final List<TowerGuiItem> FUNCTIONAL_ITEMS = new ArrayList<>();

	public static void loadGuiItems() {
		for (TowerMobInfo item : TowerFileUtils.TOWER_MOBS_INFO) {
			MOBS_ITEMS.add(new TowerGuiItem(item.getBuyableItem(), player -> true));
		}

		ItemStack arrow = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzNzM0NmQ4YmRhNzhkNTI1ZDE5ZjU0MGE5NWU0ZTc5ZGFlZGE3OTVjYmM1YTEzMjU2MjM2MzEyY2YifX19");
		ItemMeta meta = arrow.getItemMeta();
		meta.displayName(Component.empty());
		arrow.setItemMeta(meta);

		FUNCTIONAL_ITEMS.add(new TowerGuiItem(arrow, (player) -> {
			TowerGuiShowMobs instance = mInstances.get(player);
			return instance.mGuiSize >= instance.mOffset + 7;
		}, (player, clickedSlot) -> {
			mInstances.get(player).mOffset += 7;
			return true;
		}));

		arrow = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM2YTZjMmZiZDJjN2E5YzhlYzZiZTUxNzRmZGRmMWFjMjBmNTVlMzY2MTU2ZmE1ZjcxMmUxMCJ9fX0=");
		meta = arrow.getItemMeta();
		meta.displayName(Component.empty());
		arrow.setItemMeta(meta);

		FUNCTIONAL_ITEMS.add(new TowerGuiItem(arrow, player -> mInstances.get(player).mOffset != 0, (player, clickedSlot) -> {
			mInstances.get(player).mOffset -= 7;
			return true;
		}));

	}

	private int mOffset = 0;
	private final int mGuiSize = Math.min(VALID_MOBS_SLOT_SIZE, MOBS_ITEMS.size());

	private final Map<Integer, TowerGuiItem> mFunctions = new LinkedHashMap<>();


	public TowerGuiShowMobs(Player owner) {
		super(owner, 54, "Shop new Mobs");
		mInstances.put(owner, this);

		loadInv(owner);
	}

	private void loadInv(Player player) {
		mFunctions.clear();
		mInventory.clear();

		ItemStack stack;
		for (int i = 0; i < mGuiSize; i++) {
			if (i + mOffset < MOBS_ITEMS.size()) {
				stack = MOBS_ITEMS.get(i + mOffset).getItem(player);
				mInventory.setItem(VALID_MOBS_SLOT[i], stack);
				if (stack != null) {
					mFunctions.put(VALID_MOBS_SLOT[i], MOBS_ITEMS.get(i + mOffset));
				}
			}
		}

		stack = FUNCTIONAL_ITEMS.get(0).getItem(player);
		mInventory.setItem(ARROW_DOWN_SLOT, stack);
		if (stack != null) {
			mFunctions.put(ARROW_DOWN_SLOT, FUNCTIONAL_ITEMS.get(0));
		}

		stack = FUNCTIONAL_ITEMS.get(1).getItem(player);
		mInventory.setItem(ARROW_UP_SLOT, stack);
		if (stack != null) {
			mFunctions.put(ARROW_UP_SLOT, FUNCTIONAL_ITEMS.get(1));
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

		if (mFunctions.get(slot) != null) {

			mFunctions.get(slot).mPostClick.apply(player, slot);
			loadInv(player);
		}

	}



	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		mInstances.remove(event.getPlayer());
		super.inventoryClose(event);
	}

}
