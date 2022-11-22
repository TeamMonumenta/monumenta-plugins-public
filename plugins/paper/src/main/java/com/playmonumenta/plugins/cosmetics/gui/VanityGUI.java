package com.playmonumenta.plugins.cosmetics.gui;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class VanityGUI extends Gui {

	static final int STRAND_COST_PER_VANITY_UNLOCK = 16;

	private static final ImmutableMap<EquipmentSlot, Integer> VANITY_EQUIPMENT_TITLE_SLOTS = ImmutableMap.of(
		EquipmentSlot.HEAD, 1 * 9 + 2,
		EquipmentSlot.CHEST, 1 * 9 + 3,
		EquipmentSlot.LEGS, 1 * 9 + 4,
		EquipmentSlot.FEET, 1 * 9 + 5,
		EquipmentSlot.OFF_HAND, 1 * 9 + 6
	);
	private static final ImmutableMap<EquipmentSlot, Integer> VANITY_EQUIPMENT_ITEM_SLOTS = ImmutableMap.of(
		EquipmentSlot.HEAD, 2 * 9 + 2,
		EquipmentSlot.CHEST, 2 * 9 + 3,
		EquipmentSlot.LEGS, 2 * 9 + 4,
		EquipmentSlot.FEET, 2 * 9 + 5,
		EquipmentSlot.OFF_HAND, 2 * 9 + 6
	);
	private static final ImmutableMap<EquipmentSlot, Material> VANITY_EQUIPMENT_TITLE_ITEMS = ImmutableMap.of(
		EquipmentSlot.HEAD, Material.GOLDEN_HELMET,
		EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE,
		EquipmentSlot.LEGS, Material.GOLDEN_LEGGINGS,
		EquipmentSlot.FEET, Material.GOLDEN_BOOTS,
		EquipmentSlot.OFF_HAND, Material.SHIELD
	);
	private static final ImmutableMap<EquipmentSlot, String> VANITY_EQUIPMENT_TITLE_NAMES = ImmutableMap.of(
		EquipmentSlot.HEAD, "Helmet",
		EquipmentSlot.CHEST, "Chestplate",
		EquipmentSlot.LEGS, "Leggings",
		EquipmentSlot.FEET, "Boots",
		EquipmentSlot.OFF_HAND, "Offhand"
	);
	private final boolean mPreviousOtherVanityEnabled;

	private final Map<EquipmentSlot, ItemStack> mNewVanity;

	public VanityGUI(Player player) {
		super(player, 6 * 9, Component.text("Vanity Manager", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
		mPreviousOtherVanityEnabled = vanityData.mOtherVanityEnabled;
		mNewVanity = vanityData.getEquipped();
	}

	public VanityGUI(VanityGUI old, Player player) {
		super(player, 6 * 9, Component.text("Vanity Manager", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mPreviousOtherVanityEnabled = old.mPreviousOtherVanityEnabled;
		mNewVanity = old.mNewVanity;
	}

	@Override
	protected void setup() {

		VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(mPlayer);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot == EquipmentSlot.HAND) {
				continue;
			}

			{ // title
				ItemStack titleItem = new ItemStack(VANITY_EQUIPMENT_TITLE_ITEMS.get(slot));
				ItemMeta meta = titleItem.getItemMeta();
				meta.displayName(Component.text("Vanity " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				List<Component> lore = new ArrayList<>(List.of(Component.text("Put an item here to change", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("the appearance of your worn armor.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				if (slot == EquipmentSlot.OFF_HAND) {
					lore.add(Component.text("Shields, food, bows, etc. can only use", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("vanity of the same base item type.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				lore.add(Component.text("There is a one-time cost of " + STRAND_COST_PER_VANITY_UNLOCK + " Twisted Strands", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("per item to unlock that item's skin for vanity use.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("Tier 3+ patrons can use any vanity", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text("item without paying this fee.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				meta.lore(lore);
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
				titleItem.setItemMeta(meta);
				setItem(VANITY_EQUIPMENT_TITLE_SLOTS.get(slot), titleItem);
			}

			// active vanity item or placeholder
			ItemStack vanityItem = mNewVanity.get(slot);
			if (vanityItem == null || vanityItem.getType() == Material.AIR) {
				vanityItem = new ItemStack(Material.ITEM_FRAME);
				ItemMeta meta = vanityItem.getItemMeta();
				meta.displayName(Component.text("Empty Vanity " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				List<Component> lore = new ArrayList<>(List.of(Component.text("Left-click items in your inventory", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("to equip them in the proper vanity slot.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
					Component.text("Right-click to force equip in the offhand slot.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				if (VanityManager.hasFreeAccess(mPlayer)) {
					lore.add(Component.text("Patron perk: click to hide the equipped item.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					if (slot == EquipmentSlot.HEAD) {
						lore.add(Component.text("Patron perk: shift click any item in your", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
						lore.add(Component.text("inventory to equip it as vanity in your head slot.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					}
				}
				meta.lore(lore);
				vanityItem.setItemMeta(meta);
				setItem(VANITY_EQUIPMENT_ITEM_SLOTS.get(slot), vanityItem).onLeftClick(() -> {
					if (VanityManager.hasFreeAccess(mPlayer)) {
						mNewVanity.put(slot, VanityManager.getInvisibleVanityItem(slot));
						update();
					}
				});
			} else {
				vanityItem = ItemUtils.clone(vanityItem);
				ItemMeta meta = vanityItem.getItemMeta();
				List<Component> lore = new ArrayList<>();
				if (VanityManager.isInvisibleVanityItem(vanityItem)) {
					lore.add(Component.text("Hides your equipped " + VANITY_EQUIPMENT_TITLE_NAMES.get(slot).toLowerCase(Locale.ROOT) + ".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					if (slot == EquipmentSlot.OFF_HAND) {
						lore.add(Component.text("Does not hide shields, food, bows, etc.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					}
					lore.add(Component.text("Requires the Resource Pack to be hidden", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("for yourself when 'Self Vanity' is enabled.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Exclusive to T4+ patrons.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("Active vanity skin. Click to remove.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				meta.lore(lore);
				meta.addItemFlags(ItemFlag.values());
				vanityItem.setItemMeta(meta);
				setItem(VANITY_EQUIPMENT_ITEM_SLOTS.get(slot), new GuiItem(vanityItem, false)).onLeftClick(() -> {
					mNewVanity.remove(slot);
					update();
				});
			}
		}

		{
			ItemStack selfVanityToggle = new ItemStack(Material.PLAYER_HEAD, 1);
			ItemMeta meta = selfVanityToggle.getItemMeta();
			((SkullMeta) meta).setOwningPlayer(mPlayer);
			meta.displayName(Component.text("Self Vanity " + (vanityData.mSelfVanityEnabled ? "Enabled" : "Disabled"), vanityData.mSelfVanityEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				                 .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether your own", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("vanity is visible to yourself as well.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			selfVanityToggle.setItemMeta(meta);
			setItem(4, 2, selfVanityToggle).onLeftClick(() -> {
				mPlugin.mVanityManager.toggleSelfVanity(mPlayer);
				update();
			});
		}
		{
			ItemStack otherVanityToggle = new ItemStack(Material.PLAYER_HEAD, 1);
			ItemMeta meta = otherVanityToggle.getItemMeta();
			meta.displayName(Component.text("Others' Vanity " + (vanityData.mOtherVanityEnabled ? "Enabled" : "Disabled"), vanityData.mOtherVanityEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				                 .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether you see", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("other people's vanity equipment.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			otherVanityToggle.setItemMeta(meta);
			setItem(4, 3, otherVanityToggle).onLeftClick(() -> {
				mPlugin.mVanityManager.toggleOtherVanity(mPlayer);
				update();
			});
		}
		{
			ItemStack lockboxVanityToggle = new ItemStack(Material.GRAY_SHULKER_BOX, 1);
			ItemMeta meta = lockboxVanityToggle.getItemMeta();
			meta.displayName(Component.text("Lockbox Vanity Swap " + (vanityData.mLockboxSwapEnabled ? "Enabled" : "Disabled"), vanityData.mLockboxSwapEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				                 .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether Lockboxes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("swap vanity along with equipment.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			lockboxVanityToggle.setItemMeta(meta);
			ItemUtils.setPlainTag(lockboxVanityToggle);
			ItemUtils.setPlainName(lockboxVanityToggle, vanityData.mLockboxSwapEnabled ? "Loadout: Alchemist" : "Loadout: Warrior");
			setItem(4, 5, new GuiItem(lockboxVanityToggle, false)).onLeftClick(() -> {
				mPlugin.mVanityManager.toggleLockboxSwap(mPlayer);
				update();
			});
		}
		{
			ItemStack guiVanityToggle = new ItemStack(vanityData.mGuiVanityEnabled ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME, 1);
			ItemMeta meta = guiVanityToggle.getItemMeta();
			meta.displayName(Component.text("GUI Vanity " + (vanityData.mGuiVanityEnabled ? "Enabled" : "Disabled"), vanityData.mGuiVanityEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
				                 .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Click to toggle whether you see", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("vanity equipment in GUIs.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("(e.g. in the Player Stats GUI)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			guiVanityToggle.setItemMeta(meta);
			setItem(4, 6, guiVanityToggle).onLeftClick(() -> {
				mPlugin.mVanityManager.toggleGuiVanity(mPlayer);
				update();
			});
		}

		if (!mNewVanity.equals(vanityData.getEquipped())) {
			{
				ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
				ItemMeta meta = confirm.getItemMeta();
				meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				meta.lore(List.of(Component.text("Accept vanity changes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				confirm.setItemMeta(meta);
				ItemUtils.setPlainTag(confirm);
				setItem(5, 2, confirm).onLeftClick(() -> {
					vanityData.setEquipped(mNewVanity);
					ProtocolLibrary.getProtocolManager().updateEntity(mPlayer, ProtocolLibrary.getProtocolManager().getEntityTrackers(mPlayer));
					update();
				});
			}
			{
				ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
				ItemMeta meta = cancel.getItemMeta();
				meta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				meta.lore(List.of(Component.text("Discard vanity changes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				cancel.setItemMeta(meta);
				ItemUtils.setPlainTag(cancel);
				setItem(5, 6, cancel).onLeftClick(() -> {
					mNewVanity.clear();
					mNewVanity.putAll(vanityData.getEquipped());
					update();
				});
			}
		} else {
			{
				ItemStack backItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
				ItemMeta meta = backItem.getItemMeta();
				meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				backItem.setItemMeta(meta);
				setItem(5, 4, backItem).onLeftClick(() -> new CosmeticsGUI(mPlugin, mPlayer).openInventory(mPlayer, mPlugin));
			}
		}
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		// equip vanity equipment from inventory
		if (ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SOULBOUND) > 0
			    && !mPlayer.getUniqueId().equals(ItemStatUtils.getInfuser(item, ItemStatUtils.InfusionType.SOULBOUND))) {
			mPlayer.sendMessage(Component.text("This item is soulbound to another player!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
			return;
		}
		item = VanityManager.cleanCopyForDisplay(item);
		item.setAmount(1);
		EquipmentSlot slot = ItemUtils.getEquipmentSlot(item);
		if (slot == EquipmentSlot.HAND || event.getClick().isRightClick()) {
			slot = EquipmentSlot.OFF_HAND;
		}
		if (event.getClick().isShiftClick() && VanityManager.hasFreeAccess(mPlayer)) {
			slot = EquipmentSlot.HEAD;
		}
		if (slot == EquipmentSlot.OFF_HAND && !VanityManager.isValidOffhandVanityItem(item)) {
			mPlayer.sendMessage(Component.text("Cannot use items of this type as offhand vanity!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
			return;
		}
		if (VanityManager.hasVanityUnlocked(mPlayer, item)) {
			mNewVanity.put(slot, item);
			update();
		} else {
			new VanityBuyScreen(mPlayer, item, this).open();
		}
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mPlayer.updateInventory();
			if (mPreviousOtherVanityEnabled != mPlugin.mVanityManager.getData(mPlayer).mOtherVanityEnabled) {
				for (Player otherPlayer : ProtocolLibrary.getProtocolManager().getEntityTrackers(mPlayer)) {
					ProtocolLibrary.getProtocolManager().updateEntity(otherPlayer, List.of(mPlayer));
				}
			}
		}, 2);
	}

}
