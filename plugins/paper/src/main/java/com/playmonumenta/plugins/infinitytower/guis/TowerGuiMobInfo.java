package com.playmonumenta.plugins.infinitytower.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TowerGuiMobInfo extends CustomInventory {


	private static final ItemStack WHITE_BORDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack WHITE_CENTER_ITEM = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
	private static final ItemStack BACK_ITEM = new ItemStack(Material.BARRIER);

	private static final int MOB_ITEM_SLOT = 13;
	private static final int ATK_ITEM_SLOT = 21;
	private static final int CLASS_ITEM_SLOT = 22;
	private static final int HP_ITEM_SLOT = 23;
	private static final int[][] SPELL_ITEMS_SLOT = {{31}, {30, 32}, {29, 31, 33}, {29, 30, 32, 33}, {29, 30, 31, 32, 33}};
	private static final int BACK_ITEM_SLOT = 43;

	static {
		ItemMeta meta = WHITE_BORDER_ITEM.getItemMeta();
		meta.displayName(Component.empty());
		WHITE_BORDER_ITEM.setItemMeta(meta);
		WHITE_CENTER_ITEM.setItemMeta(meta);

		meta = BACK_ITEM.getItemMeta();
		meta.displayName(Component.text("Back!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		BACK_ITEM.setItemMeta(meta);

	}

	private final TowerMobInfo mInfo;

	public TowerGuiMobInfo(Player owner, TowerMobInfo info) {
		super(owner, 54);
		mInfo = info;
		loadInv();
	}

	private void loadInv() {
		mInventory.clear();

		mInventory.setItem(MOB_ITEM_SLOT, mInfo.getBuyableItem());
		mInventory.setItem(ATK_ITEM_SLOT, TowerMobInfo.buildAtkItem(mInfo));
		mInventory.setItem(CLASS_ITEM_SLOT, TowerMobInfo.buildClassItem(mInfo));
		mInventory.setItem(HP_ITEM_SLOT, TowerMobInfo.buildHPItem(mInfo));
		int i = 0;
		if (mInfo.mAbilities.size() > 0) {
			for (int pos : SPELL_ITEMS_SLOT[mInfo.mAbilities.size() - 1]) {
				mInventory.setItem(pos, TowerMobInfo.buildSpellItem(mInfo, i++));
			}
		}
		mInventory.setItem(BACK_ITEM_SLOT, BACK_ITEM);

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


		if (slot == BACK_ITEM_SLOT) {
			mInventory.clear();
			player.closeInventory();
			new TowerGuiShowMobs(player).openInventory(player, Plugin.getInstance());
		}


	}


}
