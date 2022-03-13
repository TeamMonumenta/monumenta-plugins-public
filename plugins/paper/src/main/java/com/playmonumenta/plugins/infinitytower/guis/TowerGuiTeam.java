package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TowerGuiTeam extends CustomInventory {

	private static final ItemStack WHITE_BORDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack WHITE_TEAM_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
	private static final ItemStack ARROW_DOWN_ITEM = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzNzM0NmQ4YmRhNzhkNTI1ZDE5ZjU0MGE5NWU0ZTc5ZGFlZGE3OTVjYmM1YTEzMjU2MjM2MzEyY2YifX19");
	private static final ItemStack ARROW_UP_ITEM = TowerFileUtils.getHeadFromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM2YTZjMmZiZDJjN2E5YzhlYzZiZTUxNzRmZGRmMWFjMjBmNTVlMzY2MTU2ZmE1ZjcxMmUxMCJ9fX0=");

	static {
		ItemMeta meta = WHITE_BORDER_ITEM.getItemMeta();
		meta.displayName(Component.empty());
		WHITE_BORDER_ITEM.setItemMeta(meta);
		WHITE_TEAM_ITEM.setItemMeta(meta);
		ARROW_DOWN_ITEM.setItemMeta(meta);
		ARROW_UP_ITEM.setItemMeta(meta);

	}


	private static final int[] VALID_MOBS_SLOT = {
		// 0,   1,  2,  3,  4,  5,    6,  7,  8,
		/* 9*/ 10, 11, 12, 13, 14, //15, 16, 17,
		/*18*/ 19, 20, 21, 22, 23, //24, 25, 26,
		/*27*/ 28, 29, 30, 31, 32, //33, 34, 35,
		/*36*/ 37, 38, 39, 40, 41, //42, 43  44,
		//45,  46, 47, 48, 49, 50,   51, 52, 53
	};


	private int mOffset = 0;
	private final TowerGame mGame;
	private final Map<Integer, TowerMob> mTowerMobMap = new LinkedHashMap<>();

	public TowerGuiTeam(Player owner, TowerGame game) {
		super(owner, 54, "Player Team");

		mGame = game;
		owner.playSound(owner.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1, 2);

		loadInv();
	}

	private void loadInv() {
		mInventory.clear();
		mTowerMobMap.clear();

		int i = 0;

		for (int j = mOffset; j < Math.min(VALID_MOBS_SLOT.length, mGame.mPlayer.mTeam.mMobs.size()); j++) {
			mInventory.setItem(VALID_MOBS_SLOT[i], mGame.mPlayer.mTeam.mMobs.get(j).buildTeamItem(mGame));
			mTowerMobMap.put(VALID_MOBS_SLOT[i], mGame.mPlayer.mTeam.mMobs.get(j));
			i++;
		}

		while (i < VALID_MOBS_SLOT.length) {
			mInventory.setItem(VALID_MOBS_SLOT[i++], WHITE_TEAM_ITEM);
		}

		//functional item.
		if (mGame.mPlayer.mTeam.mMobs.size() > VALID_MOBS_SLOT.length + mOffset) {
			mInventory.setItem(42, ARROW_DOWN_ITEM);
		}

		if (mOffset != 0) {
			mInventory.setItem(15, ARROW_UP_ITEM);

		}
		mInventory.setItem(26, TowerGameUtils.getCoinItem(mGame));
		mInventory.setItem(35, TowerGameUtils.getSignLvlItem(mGame));
		mInventory.setItem(44, TowerGameUtils.getWeightItem(mGame));


		//fill the blank space
		for (i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, WHITE_BORDER_ITEM);
			}
		}


	}



	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);

		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}


		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();


		if (mTowerMobMap.get(slot) != null) {
			mInventory.clear();
			player.closeInventory();
			new TowerGuiMob(player, mGame, mTowerMobMap.get(slot)).openInventory(player, TowerManager.mPlugin);
			return;
		}

		if (slot == 15 && mOffset > 0) {
			mOffset -= 5;
			loadInv();
			return;
		}

		if (slot == 42 && (mGame.mPlayer.mTeam.mMobs.size() > VALID_MOBS_SLOT.length + mOffset)) {
			mOffset += 5;
			loadInv();
		}




	}



}
