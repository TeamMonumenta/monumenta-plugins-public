package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
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

public class TowerGuiMob extends CustomInventory {

	private static final ItemStack WHITE_BORDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack WHITE_CENTER_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
	private static final ItemStack MOVE_ITEM = new ItemStack(Material.SADDLE);

	private static final int MOB_ITEM_SLOT = 13;
	private static final int ATK_ITEM_SLOT = 21;
	private static final int CLASS_ITEM_SLOT = 22;
	private static final int HP_ITEM_SLOT = 23;
	private static final int[][] SPELL_ITEMS_SLOT = {{31}, {30, 32}, {29, 31, 33}, {29, 30, 32, 33}, {29, 30, 31, 32, 33}};
	private static final int SELL_SLOT = 43;
	private static final int MOVE_SLOT = 37;
	private static final int LEVEL_SLOT = 40;
	private static final int COIN_SLOT = 49;

	static {
		ItemMeta meta = WHITE_BORDER_ITEM.getItemMeta();
		meta.displayName(Component.empty());
		WHITE_BORDER_ITEM.setItemMeta(meta);
		WHITE_CENTER_ITEM.setItemMeta(meta);

		meta.displayName(Component.text("Move this unit here!", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		MOVE_ITEM.setItemMeta(meta);
	}

	//   0,   1,  2,  3,  4,  5,  6,  7,  8,
	/*   9,  10, 11, 12, 13, 14, 15, 16, 17,
	/*  18,  19, 20, 21, 22, 23, 24, 25, 26,
	/*  27,  28, 29, 30, 31, 32, 33, 34, 35,
	/*  36,  37, 38, 39, 40, 41, 42, 43  44,
	//  45,  46, 47, 48, 49, 50, 51, 52, 53 */

	private final TowerMob mMob;
	private final TowerGame mGame;

	public TowerGuiMob(Player owner, TowerGame game, TowerMob mob) {
		super(owner, 54, Objects.requireNonNullElse(mob.mInfo.mDisplayName, "Unknown Blitz Mob"));
		mMob = mob;
		mGame = game;
		owner.playSound(owner.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1, 2);

		loadInv();
	}

	private void loadInv() {
		mInventory.clear();

		mInventory.setItem(MOB_ITEM_SLOT, mMob.buildTeamItem(mGame));
		mInventory.setItem(ATK_ITEM_SLOT, mMob.buildAtkItem(mGame));
		mInventory.setItem(CLASS_ITEM_SLOT, mMob.buildClassItem(mGame));
		mInventory.setItem(HP_ITEM_SLOT, mMob.buildHPItem(mGame));
		int i = 0;
		if (mMob.mAbilities.size() > 0) {
			for (int pos : SPELL_ITEMS_SLOT[mMob.mAbilities.size() - 1]) {
				mInventory.setItem(pos, mMob.buildSpellItem(mGame, i++));
			}
		}
		mInventory.setItem(SELL_SLOT, TowerGameUtils.getSellMobItem(mMob));
		mInventory.setItem(MOVE_SLOT, MOVE_ITEM);
		mInventory.setItem(LEVEL_SLOT, mMob.buildLevelItem(mGame));
		mInventory.setItem(COIN_SLOT, TowerGameUtils.getCoinItem(mGame));

		for (int j = 0; j < 54; j++) {
			if (mInventory.getItem(j) == null) {
				if (j % 9 == 8 || j % 9 == 0 || j < 9 || j > 45) {
					mInventory.setItem(j, WHITE_BORDER_ITEM);
				} else {
					mInventory.setItem(j, WHITE_CENTER_ITEM);
				}
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


		if (slot == SELL_SLOT) {
			TowerGameUtils.sellMob(mGame, mMob);
			player.sendMessage(Component.text("[Plunderer's Blitz]", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
				Component.text(" Unit removed from the team", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false)
			));
			player.playSound(player.getEyeLocation(), Sound.ENTITY_CHICKEN_HURT, SoundCategory.MASTER, 1, 1.2f);
			mInventory.clear();
			player.closeInventory();
			new TowerGuiTeam(player, mGame).openInventory(player, TowerManager.mPlugin);
			return;
		}

		if (slot == MOVE_SLOT) {
			TowerGameUtils.moveMob(mGame, mMob);
			player.playSound(player.getEyeLocation(), Sound.ENTITY_ARMOR_STAND_HIT, SoundCategory.MASTER, 10f, 0.6f);
			TowerGameUtils.sendMessage(player, "Unit moved");
			player.playSound(player.getEyeLocation(), Sound.ENTITY_ARMOR_STAND_HIT, SoundCategory.MASTER, 10, 0.6f);
		}

		if (slot == LEVEL_SLOT && mMob.mMobLevel < TowerConstants.MAX_MOB_LEVEL) {
			if (TowerGameUtils.canBuy(player, TowerGameUtils.getNextLevelCost(mMob))) {
				TowerGameUtils.pay(player, TowerGameUtils.getNextLevelCost(mMob));
				mMob.mMobLevel++;

				player.playSound(player.getEyeLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1.2f);
			} else {
				player.sendMessage(Component.text("[Plunderer's Blitz]", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
					Component.text(" You don't have enough money to buy this item", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false)
				));
			}
		}

		loadInv();

	}

}
