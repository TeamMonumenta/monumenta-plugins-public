package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobRarity;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TowerGuiBuyMob extends CustomInventory {

	private static final ItemStack WHITE_BORDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack WHITE_CENTER_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
	private static final ItemStack REFRESH_ITEM = new ItemStack(Material.BONE_MEAL);
	private static final ItemStack FREE_REFRESH_ITEM = new ItemStack(Material.BONE_MEAL);

	static {
		ItemMeta meta = WHITE_BORDER_ITEM.getItemMeta();
		meta.displayName(Component.empty());
		WHITE_BORDER_ITEM.setItemMeta(meta);
		WHITE_CENTER_ITEM.setItemMeta(meta);

		meta = REFRESH_ITEM.getItemMeta();
		meta.displayName(Component.text("Refresh the shop!", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("This will cost " + TowerConstants.COST_REROLL + " coin", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		meta.lore(lore);
		REFRESH_ITEM.setItemMeta(meta);

		meta = FREE_REFRESH_ITEM.getItemMeta();
		meta.displayName(Component.text("Refresh the shop!", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		List<Component> lore2 = new ArrayList<>();
		lore2.add(Component.text("1 free refresh!", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
		lore2.add(Component.empty());
		lore2.add(Component.text("Each round from 1 to 5 have a free shop refresh", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore2);
		FREE_REFRESH_ITEM.setItemMeta(meta);

	}

	private static final int[] VALID_MOBS_SLOT = {
		// 0,     1,     2,  3,    4,    5,  6,     7,    8
		/* 9*/   10, /* 11, 12,*/ 13, /* 14, 15,*/ 16  //17
		/*18*/// 19,    20, 21,   22,   23, 24,    25, //26
	};

	private static final int VALID_MOBS_SIZE = VALID_MOBS_SLOT.length;

	private static final Map<TowerGame, Integer> ROLL_MAP = new LinkedHashMap<>();
	private static final Map<TowerGame, List<TowerMobInfo>> ITEM_MAP = new LinkedHashMap<>();

	private final TowerGame mGame;
	private final Map<Integer, TowerMobInfo> mMapItem = new LinkedHashMap<>();

	public TowerGuiBuyMob(Player owner, TowerGame game) {
		super(owner, 9 * 3, "Buy a new mob");
		mGame = game;
		owner.playSound(owner.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1, 2);

		ITEM_MAP.computeIfAbsent(mGame, game1 -> new ArrayList<>());

		loadInv();
	}


	private List<TowerMobInfo> getItemList() {
		List<TowerMobInfo> mobs = Objects.requireNonNull(ITEM_MAP.get(mGame));
		if (ROLL_MAP.get(mGame) != null && ROLL_MAP.get(mGame) == mGame.mRoll) {
			return mobs;
		}

		int currentLvl = mGame.mPlayerLevel;
		ROLL_MAP.put(mGame, mGame.mRoll);
		mobs.clear();
		double rand;
		for (int i = 0; i < VALID_MOBS_SIZE; i++) {
			rand = FastUtils.RANDOM.nextDouble();
			for (TowerMobRarity rarity : TowerMobRarity.values()) {
				rand -= rarity.getWeight(currentLvl - 1);
				if (rand <= 0) {
					TowerMobInfo info = Objects.requireNonNull(TowerFileUtils.getMobsByRarity(rarity))
						                    .get(FastUtils.RANDOM.nextInt(TowerFileUtils.getMobsByRarity(rarity).size()));
					if (mGame.mCurrentFloor == 0 && info.mLosName.equals("ITBabyMimic")) {
						info = TowerFileUtils.getMobsByRarity(TowerMobRarity.COMMON).get(0);
						//at the first round don't take any BabyMimic
					}
					mobs.add(info);
					break;
				}
			}
		}

		return mobs;
	}

	private void loadInv() {
		mInventory.clear();
		mMapItem.clear();

		List<TowerMobInfo> mobs = getItemList();

		mInventory.setItem(4, TowerGameUtils.getSignLvlItem(mGame));

		int i = 0;
		for (TowerMobInfo info : mobs) {
			mInventory.setItem(VALID_MOBS_SLOT[i], info.getBuyableItem());
			mMapItem.put(VALID_MOBS_SLOT[i], info);
			i++;
		}

		while (i < VALID_MOBS_SIZE) {
			mInventory.setItem(VALID_MOBS_SLOT[i++], WHITE_CENTER_ITEM);
		}

		for (int j = 0; j < mInventory.getSize(); j++) {
			if (mInventory.getItem(j) == null) {
				mInventory.setItem(j, WHITE_BORDER_ITEM);
			}
		}

		mInventory.setItem(21, TowerGameUtils.getXPItem(mGame));
		mInventory.setItem(22, TowerGameUtils.getCoinItem(mGame));
		mInventory.setItem(23, mGame.mFreeRoll ? FREE_REFRESH_ITEM : REFRESH_ITEM);
		mInventory.setItem(25, TowerGameUtils.getWeightItem(mGame));

	}


	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		if (event.isShiftClick()) {
			return;
		}

		Player player = ((Player)event.getWhoClicked());
		int slot = event.getSlot();
		TowerMobInfo info = mMapItem.get(slot);

		if (info != null) {
			if (TowerGameUtils.canBuy(info, player)) {
				if (mGame.canAddWeight(info)) {
					if (mGame.canAddLimit(info)) {
						Objects.requireNonNull(ITEM_MAP.get(mGame)).remove(info);
						TowerGameUtils.pay(info, player);
						mGame.addNewMob(info);
						player.playSound(player.getEyeLocation(), Sound.ENTITY_ARMOR_STAND_HIT, SoundCategory.PLAYERS, 10f, 0.6f);
					} else {
						player.sendMessage(Component.text("[Plunderer's Blitz]", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
							Component.text(" You can't use more then " + info.mMobStats.mLimit + " " + info.mDisplayName + " in the same team!", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false)
						));
						//Limit of the same mob
					}
				} else {
					player.sendMessage(Component.text("[Plunderer's Blitz]", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
											Component.text(" You don't have enough space inside the team", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false)
					));
					//no space
				}
			} else {
				player.sendMessage(Component.text("[Plunderer's Blitz]", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
											Component.text(" You don't have enough money to buy this item", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false)
					));
				//no money
			}
		} else {
			if (slot == 21) {
				if (TowerGameUtils.canBuyXP(mGame, player)) {
					TowerGameUtils.upgradeLvl(mGame, player);
					player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 2f);
				}
			}

			if (slot == 23) {
				//do we have a free roll or we need to pay
				if (mGame.mFreeRoll || TowerGameUtils.canBuy(player, TowerConstants.COST_REROLL)) {
					if (!mGame.mFreeRoll) {
						TowerGameUtils.pay(player, TowerConstants.COST_REROLL);
					}
					player.playSound(player.getEyeLocation(), Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.PLAYERS, 1, 0.9f);
					mGame.mRoll++;
					mGame.mFreeRoll = false;
				}
			}

		}

		loadInv();

	}


	public static void unloadGame(TowerGame game) {
		ITEM_MAP.remove(game);
		ROLL_MAP.remove(game);
	}
}
