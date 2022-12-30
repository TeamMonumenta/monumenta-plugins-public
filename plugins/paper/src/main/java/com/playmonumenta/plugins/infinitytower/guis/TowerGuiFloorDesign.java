package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.infinitytower.TowerTeam;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TowerGuiFloorDesign extends CustomInventory {

	private static final ItemStack RED_GLASS = new ItemStack(Material.RED_STAINED_GLASS_PANE);
	private static final ItemStack GREEN_GLASS = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
	private static final ItemStack MAGENTA_GLASS = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
	private static final ItemStack PURPLE_GLASS = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
	private static final ItemStack GRAY_GLASS = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
	private static final ItemStack LEVEL_UP_MOB = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjYmY5ODgzZGQzNTlmZGYyMzg1YzkwYTQ1OWQ3Mzc3NjUzODJlYzQxMTdiMDQ4OTVhYzRkYzRiNjBmYyJ9fX0=");
	private static final ItemStack LEVEL_DOWN_MOB = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzI0MzE5MTFmNDE3OGI0ZDJiNDEzYWE3ZjVjNzhhZTQ0NDdmZTkyNDY5NDNjMzFkZjMxMTYzYzBlMDQzZTBkNiJ9fX0=");

	private static final ItemStack MOVE_UP_LEFT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY1NDI2YTMzZGY1OGI0NjVmMDYwMWRkOGI5YmVjMzY5MGIyMTkzZDFmOTUwM2MyY2FhYjc4ZjZjMjQzOCJ9fX0=");
	private static final ItemStack MOVE_UP = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM2YTZjMmZiZDJjN2E5YzhlYzZiZTUxNzRmZGRmMWFjMjBmNTVlMzY2MTU2ZmE1ZjcxMmUxMCJ9fX0=");
	private static final ItemStack MOVE_UP_RIGHT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTBlMGE0ZDQ4Y2Q4MjlhNmQ1ODY4OTA5ZDY0M2ZhNGFmZmQzOWU4YWU2Y2FhZjZlYzc5NjA5Y2Y3NjQ5YjFjIn19fQ==");
	private static final ItemStack MOVE_RIGHT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19");
	private static final ItemStack MOVE_DOWN_RIGHT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzVjYmRiMjg5OTFhMTZlYjJjNzkzNDc0ZWY3ZDBmNDU4YTVkMTNmZmZjMjgzYzRkNzRkOTI5OTQxYmIxOTg5In19fQ==");
	private static final ItemStack MOVE_DOWN = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzNzM0NmQ4YmRhNzhkNTI1ZDE5ZjU0MGE5NWU0ZTc5ZGFlZGE3OTVjYmM1YTEzMjU2MjM2MzEyY2YifX19");
	private static final ItemStack MOVE_DOWN_LEFT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzU0Y2U4MTU3ZTcxZGNkNWI2YjE2NzRhYzViZDU1NDkwNzAyMDI3YzY3NWU1Y2RjZWFjNTVkMmZiYmQ1YSJ9fX0=");
	private static final ItemStack MOVE_LEFT = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==");


	private static final int[] VALID_FLOOR_SLOT = {
		 0, 1, 2, 3, 4,     // 5,  6,  7,  8,
		 9, 10, 11, 12, 13, //14, 15, 16, 17,
		18, 19, 20, 21, 22, //23, 24, 25, 26,
		27, 28, 29, 30, 31, //32, 33, 34, 35,
		36, 37, 38, 39, 40, //41, 42, 43  44,
		45, 46, 47, 48, 49, //50, 51, 52, 53
	};

	private static final int[] VALID_WALL_SLOT = {5, 14, 23, 32, 41, 50};
	private static final int DOWNGRADE_LVL_ITEM_SLOT = 6;
	private static final int MOB_ITEM_SLOT = 7;
	private static final int UPGRADE_LVL_ITEM_SLOT = 8;
	private static final int MOB_REMOVE_SLOT = 16;




	private TowerTeam mTeam;
	private final int mFloor;
	private final Map<Integer, TowerMob> mFloorMap = new LinkedHashMap<>();
	private final Map<Integer, ItemStack> mItemMap = new LinkedHashMap<>();
	private int mDx = 0;
	private int mDz = 0;

	private @Nullable TowerMob mMobSelected;

	public TowerGuiFloorDesign(Player owner, int floor) {
		super(owner, 54, "Floor N " + (floor + 1));
		mFloor = floor;
		TowerTeam team = TowerFileUtils.getDefaultFloorTeam(floor);
		if (team == null) {
			owner.sendMessage("No team found?");
			team = new TowerTeam("", new ArrayList<>());
		}
		mTeam = team;

		loadItems();
		loadInv();
	}

	private void loadItems() {
		mItemMap.clear();

		boolean found;
		for (int x = 0; x < TowerConstants.FLOOR_SIZE_X / 2; x++) {
			for (int z = 0; z < TowerConstants.FLOOR_SIZE_Z; z++) {
				found = false;
				for (TowerMob mob : mTeam.mMobs) {
					if (mob.getX() == (TowerConstants.FLOOR_SIZE_X - x) && mob.getZ() == z) {
						found = true;
						mItemMap.put(TowerConstants.FLOOR_SIZE_Z * x + z, mob.buildItem());
						mFloorMap.put(TowerConstants.FLOOR_SIZE_Z * x + z, mob);
						break;
					}
				}

				if (!found) {
					if (z == TowerConstants.FLOOR_SIZE_Z / 2 && x == TowerConstants.FLOOR_SIZE_X / 4) {
						mItemMap.put(TowerConstants.FLOOR_SIZE_Z * x + z, PURPLE_GLASS);
					} else if (z == TowerConstants.FLOOR_SIZE_Z / 2) {
						mItemMap.put(TowerConstants.FLOOR_SIZE_Z * x + z, MAGENTA_GLASS);
					} else {
						mItemMap.put(TowerConstants.FLOOR_SIZE_Z * x + z, GREEN_GLASS);
					}
				}

			}
		}
	}

	private void loadInv() {
		mInventory.clear();

		for (int i : VALID_FLOOR_SLOT) {
			if ((mDx < 0 && i < 5) || (mDx >= (TowerConstants.FLOOR_SIZE_X / 2 - 5) && i > 44)) {
				mInventory.setItem(i, RED_GLASS);
				continue;
			}
			if ((mDz < 0 && i % 9 == 0) || (mDz > (TowerConstants.FLOOR_SIZE_Z - 4) && i % 9 == 4)) {
				mInventory.setItem(i, RED_GLASS);
				continue;
			}
			int mod = i % 9;
			int q = i / 9;

			mInventory.setItem(i, mItemMap.get(TowerConstants.FLOOR_SIZE_Z * (q + mDx) + mod + mDz));
		}

		for (int i : VALID_WALL_SLOT) {
			mInventory.setItem(i, new ItemStack(Material.POLISHED_BLACKSTONE_WALL));
		}

		mInventory.setItem(33, MOVE_UP_LEFT.clone());
		mInventory.setItem(34, MOVE_UP.clone());
		mInventory.setItem(35, MOVE_UP_RIGHT.clone());

		mInventory.setItem(42, MOVE_LEFT.clone());
		mInventory.setItem(44, MOVE_RIGHT.clone());

		mInventory.setItem(51, MOVE_DOWN_LEFT.clone());
		mInventory.setItem(52, MOVE_DOWN.clone());
		mInventory.setItem(53, MOVE_DOWN_RIGHT.clone());


		if (mMobSelected != null) {
			mInventory.setItem(DOWNGRADE_LVL_ITEM_SLOT, LEVEL_DOWN_MOB);
			mInventory.setItem(MOB_ITEM_SLOT, mMobSelected.buildItem());
			mInventory.setItem(UPGRADE_LVL_ITEM_SLOT, LEVEL_UP_MOB);
			mInventory.setItem(MOB_REMOVE_SLOT, new ItemStack(Material.ANVIL));
		}

		mInventory.setItem(17, new ItemStack(Material.FEATHER));

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, GRAY_GLASS);
			}
		}

	}


	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);

		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		int slotClicked = event.getSlot();
		Player player = (Player) event.getWhoClicked();

		int mod = slotClicked % 9;
		int q = slotClicked / 9;
		switch (slotClicked) {
			case 33 -> {
				if (mDx >= 0 && mDz >= 0) {
					mDx--;
					mDz--;
				}
			}
			case 34 -> {
				if (mDx >= 0) {
					mDx--;
				}
			}
			case 35 -> {
				if (mDx >= 0 && mDz >= -1) {
					mDx--;
					mDz++;
				}
			}
			case 42 -> {
				if (mDz >= 0) {
					mDz--;
				}
			}
			case 44 -> {
				if (mDz >= -1) {
					mDz++;
				}
			}
			case 51 -> {
				if (mDx >= -1 && mDz >= 0 && mDx < TowerConstants.FLOOR_SIZE_X / 2 - 5) {
					mDx++;
					mDz--;
				}
			}
			case 52 -> {
				if (mDx >= -1 && mDx < TowerConstants.FLOOR_SIZE_X / 2 - 5) {
					mDx++;
				}
			}
			case 53 -> {
				if (mDx >= -1 && mDz >= -1 && mDx < TowerConstants.FLOOR_SIZE_X / 2 - 5) {
					mDx++;
					mDz++;
				}
			}
			case 6 -> {
				if (mMobSelected != null) {
					mMobSelected.mMobLevel--;
					loadItems();
				}
			}
			case 8 -> {
				if (mMobSelected != null) {
					mMobSelected.mMobLevel++;
					loadItems();
				}
			}
			case 16 -> {
				if (mMobSelected != null) {
					mTeam.remove(mMobSelected);
					mMobSelected = null;
					loadItems();
				}
			}
			case 17 -> {
				mTeam.mMobs.clear();
				loadItems();
			}
			default -> {
				if (!validClick(slotClicked)) {
					return;
				}

				TowerMob towerMob = mFloorMap.get(TowerConstants.FLOOR_SIZE_Z * (q + mDx) + mod + mDz);
				if (towerMob != null) {
					mMobSelected = towerMob;
					player.sendMessage(towerMob.toJson().toString());
				} else {
					int x = TowerConstants.FLOOR_SIZE_X - (q + mDx);
					int z = mod + mDz;
					Vector vec = new Vector(x, 0, z);
					player.closeInventory();
					new TowerGuiFloorDesignMob(player, mTeam, mFloor, vec).openInventory(player, TowerManager.mPlugin);
					return;
				}
			}
		}



		loadInv();


	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		super.inventoryClose(event);
		//Save the floor
		TowerFileUtils.saveDefaultTeam(mTeam, mFloor);
	}


	private boolean validClick(int slot) {
		for (int i : VALID_FLOOR_SLOT) {
			if (i == slot) {
				return true;
			}
		}
		return false;
	}
}
