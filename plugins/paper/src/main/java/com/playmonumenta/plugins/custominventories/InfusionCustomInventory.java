package com.playmonumenta.plugins.custominventories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils.InfusionSelection;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;


public class InfusionCustomInventory extends CustomInventory {


	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final int ROW = 6;
	private static final int COLUMNS = 9;
	private static final int MAX_LORE_LENGHT = 30;
	private static final Material JUNK_ITEM = Material.GRAY_STAINED_GLASS_PANE;

	private static final Map<InfusionSelection, List<ItemStack>> mInfusionPannelsMap = new HashMap<>();
	private static final List<ItemStack> mPannelList = new ArrayList<>();

	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mMaxLevelReachedItem = new ItemStack(Material.CAKE);

	private Map<Integer, ItemClicked> mMapFunction;


	static {
		//Vitality generic pannel.
		ItemStack vitalityPannel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
		ItemMeta vitalityMeta = vitalityPannel.getItemMeta();
		vitalityMeta.displayName(Component.text("Vitality", TextColor.fromCSSHexString("#FF8C00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(vitalityMeta, "Each level of Vitality gives you 1% bonus health.", MAX_LORE_LENGHT, ChatColor.GRAY);
		vitalityPannel.setItemMeta(vitalityMeta);
		mPannelList.add(vitalityPannel);

		//Vigor generic pannel.
		ItemStack vigorPannel = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
		ItemMeta vigorMeta = vigorPannel.getItemMeta();
		vigorMeta.displayName(Component.text("Vigor", TextColor.fromCSSHexString("#ff0000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(vigorMeta, "Each level of Vigor gives you 1% bonus melee damage.", MAX_LORE_LENGHT, ChatColor.GRAY);
		vigorPannel.setItemMeta(vigorMeta);
		mPannelList.add(vigorPannel);

		//tenacity generic pannel.
		ItemStack tenacityPannel = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		ItemMeta tenacityMeta = tenacityPannel.getItemMeta();
		tenacityMeta.displayName(Component.text("Tenacity", TextColor.fromCSSHexString("#A9A9A9")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(tenacityMeta, "Each level of Tenacity gives you 0.5% damage reduction.", MAX_LORE_LENGHT, ChatColor.GRAY);
		tenacityPannel.setItemMeta(tenacityMeta);
		mPannelList.add(tenacityPannel);

		//perspicacity generic pannel.
		ItemStack perspicacityPannel = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1);
		ItemMeta perspicacityMeta = perspicacityPannel.getItemMeta();
		perspicacityMeta.displayName(Component.text("Perspicacity", TextColor.fromCSSHexString("#6666ff")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(perspicacityMeta, "Each level of Perspicacity gives you 1% bonus ability damage.", MAX_LORE_LENGHT, ChatColor.GRAY);
		perspicacityPannel.setItemMeta(perspicacityMeta);
		mPannelList.add(perspicacityPannel);

		//focus generic pannel.
		ItemStack focusPannel = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1);
		ItemMeta focusMeta = focusPannel.getItemMeta();
		focusMeta.displayName(Component.text("Focus", TextColor.fromCSSHexString("#FFFF00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(focusMeta, "Each level of Focus gives you 1% bonus ranged damage.", MAX_LORE_LENGHT, ChatColor.GRAY);
		focusPannel.setItemMeta(focusMeta);
		mPannelList.add(focusPannel);

		//acumen generic pannel.
		ItemStack acumenPannel = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
		ItemMeta acumenMeta = acumenPannel.getItemMeta();
		acumenMeta.displayName(Component.text("Acumen", TextColor.fromCSSHexString("#32CD32")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(acumenMeta, "Each level of Acumen gives you 1% bonus experience.", MAX_LORE_LENGHT, ChatColor.GRAY);
		acumenPannel.setItemMeta(acumenMeta);
		mPannelList.add(acumenPannel);

		//---------------------------------------------
		//pannels showed for each infusion on item
		//--------------------------------------------
		List<ItemStack> focusPannels = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Focus level " + (i + 1), TextColor.fromCSSHexString("#FFFF00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1) + "% bonus ranged damage", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			focusPannels.add(pannel);
		}
		mInfusionPannelsMap.put(InfusionSelection.FOCUS, focusPannels);

		List<ItemStack> acumenPannels = new ArrayList<>();


		for (int i = 0; i < 4; i++) {
			ItemStack pannels = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannels.getItemMeta();
			meta.displayName(Component.text("Acumen level " + (i + 1), TextColor.fromCSSHexString("#32CD32")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1) + "% bonus experience.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannels.setItemMeta(meta);
			acumenPannels.add(pannels);
		}
		mInfusionPannelsMap.put(InfusionSelection.ACUMEN, acumenPannels);

		List<ItemStack> perspicacityPannels = new ArrayList<>();


		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Perspicacity level " + (i + 1), TextColor.fromCSSHexString("#6666ff")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1) + "% bonus ability damage.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			perspicacityPannels.add(pannel);
		}
		mInfusionPannelsMap.put(InfusionSelection.PERSPICACITY, perspicacityPannels);



		List<ItemStack> tenacityPannels = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Tenacity level " + (i + 1), TextColor.fromCSSHexString("#A9A9A9")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1)*0.5 + "% damage reduction.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			tenacityPannels.add(pannel);
		}
		mInfusionPannelsMap.put(InfusionSelection.TENACITY, tenacityPannels);


		List<ItemStack> vigorPannels = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Vigor level " + (i + 1), TextColor.fromCSSHexString("#ff0000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1) + "% bonus melee damage.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			vigorPannels.add(pannel);
		}
		mInfusionPannelsMap.put(InfusionSelection.VIGOR, vigorPannels);


		List<ItemStack> vitalityPannels = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Vitality level " + (i + 1), TextColor.fromCSSHexString("#FF8C00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You are currently receiving: " + (i + 1) + "% bonus health.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			vitalityPannels.add(pannel);
		}
		mInfusionPannelsMap.put(InfusionSelection.VITALITY, vitalityPannels);


		//INVALIDS ITEM.
		//placeholder when an item can't be infused.

		ItemStack invalidItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta meta = invalidItem.getItemMeta();
		meta.displayName(Component.text("Invalid item", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));

		List<Component> itemLore = new ArrayList<Component>();
		itemLore.add(Component.text("Your helmet can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);

		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your chestplate can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your leggings can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your boots can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your main hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your off hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());

		//Refund item
		ItemMeta refundMeta = mRefundItem.getItemMeta();
		refundMeta.displayName(Component.text("Click to refund this item's infusions", NamedTextColor.DARK_GRAY)
							.decoration(TextDecoration.ITALIC, false)
							.decoration(TextDecoration.BOLD, true));
		splitLoreLine(refundMeta, "You will receive 50% of the experience, but all of the materials back.", MAX_LORE_LENGHT, ChatColor.GRAY);
		mRefundItem.setItemMeta(refundMeta);

		//Cake for max level reached
		ItemMeta maxMeta = mMaxLevelReachedItem.getItemMeta();
		maxMeta.displayName(Component.text("Congratulations!", NamedTextColor.DARK_AQUA)
						.decoration(TextDecoration.BOLD, true)
						.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(maxMeta, "You've reached the max Infusion level on this item.", MAX_LORE_LENGHT, ChatColor.DARK_AQUA);
		mMaxLevelReachedItem.setItemMeta(maxMeta);
	}


	private static void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (currentString != defaultColor + "") {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}

	public InfusionCustomInventory(Player owner) {
		super(owner, ROW * COLUMNS, "Infusions");
		mMapFunction = new HashMap<>();
		loadInv(owner);
	}

	public void loadInv(Player player) {
		_inventory.clear();
		mMapFunction.clear();
		PlayerInventory pi = player.getInventory();
		List<ItemStack> items = new ArrayList<>();
		items.addAll(Arrays.asList(pi.getArmorContents()));
		Collections.reverse(items);
		items.add(pi.getItemInMainHand());
		items.add(pi.getItemInOffHand());
		loadInfusionPage(items);
		fillWithJunk();
	}

	private void loadInfusionPage(List<ItemStack> items) {
		int row = 0;
		for (ItemStack is : items) {
			if (is != null) {
				if (InfusionUtils.isInfusionable(is)) {
					loadRowNormalInfusionItem(is, row);
					final int mRow = row;
					//we need to delay the item change so the skins are loaded
					new BukkitRunnable() {
						@Override
						public void run() {
							ItemStack itemStack = new ItemStack(is.getType());
							ItemMeta meta = itemStack.getItemMeta();
							meta.displayName(Component.text("Placeholder", TextColor.fromCSSHexString("000000"))
											.decoration(TextDecoration.BOLD, true)
											.decoration(TextDecoration.ITALIC, false));
							meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
							itemStack.setItemMeta(meta);
							ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(is));
							_inventory.setItem((mRow*9) + 1, itemStack);
						}
					}.runTaskLater(Plugin.getInstance(), 2);

				} else {
					ItemStack invalidItem = mInvalidItems.get(row);
					_inventory.setItem((row*9) + 1, invalidItem);
				}
			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				_inventory.setItem((row*9) + 1, invalidItem);
			}
			row++;
		}
	}



	private void fillWithJunk() {
		ItemStack junk = new ItemStack(JUNK_ITEM, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);

		for (int i = 0; i < (ROW * COLUMNS); i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, junk);
			}
		}
	}

	private void loadRowNormalInfusionItem(ItemStack item, int row) {

		InfusionSelection infunsion = InfusionUtils.getCurrentInfusion(item);
		int infunsionLvl = InfusionUtils.getInfuseLevel(item);

		List<ItemStack> pannelsInfusions = mInfusionPannelsMap.get(infunsion);
		//notes: if pannelsInfusions == null mean that this item has no infusion.

		//check if the item has an infusion infusion or not
		if (infunsionLvl > 0) {
			//set the refound item
			_inventory.setItem((row*9), mRefundItem);
			mMapFunction.put((row*9), (p, inventory, slot) -> {
				try {
					InfusionUtils.refundInfusion(item, p);
				} catch (WrapperCommandSyntaxException e) {
					p.sendMessage(Component.text("Error refunding infusion. Please contact a mod: " + e.getMessage()));
				}
			});

			//set the pannels to show the current infusion and level
			for (int index = 0; index < infunsionLvl; index++) {
				_inventory.setItem((row*9) + 2 + index, pannelsInfusions.get(index));
			}

			if (infunsionLvl < 4) {
				int slot = (row * 9) + 2 + infunsionLvl;
				//creating item and setting the meta
				ItemStack infuseItem = new ItemStack(Material.ENCHANTED_BOOK, 1);
				ItemMeta infuseMeta = infuseItem.getItemMeta();
				infuseMeta.displayName(Component.text("Click to infuse to level " + (infunsionLvl + 1), NamedTextColor.DARK_AQUA)
								.decoration(TextDecoration.ITALIC, false)
								.decoration(TextDecoration.BOLD, true));
				List<Component> itemLore = new ArrayList<>();
				itemLore.add(Component.text("You need " + InfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false));
				int currency = -1;
				try {
					currency = InfusionUtils.calcInfuseCost(item);
				} catch (WrapperCommandSyntaxException e) {
					currency = -1;
				}

				if (currency > 0) {
					ItemRegion region = ItemUtils.getItemRegion(item);
					if (region == ItemRegion.CELSIAN_ISLES) {
						itemLore.add(Component.text("and " + currency + " Pulsating Emeralds", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
					}

					if (region == ItemRegion.KINGS_VALLEY) {
						itemLore.add(Component.text("and " + currency + " Pulsating Gold", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
					}
				}
				infuseMeta.lore(itemLore);
				infuseItem.setItemMeta(infuseMeta);
				_inventory.setItem(slot, infuseItem);

				mMapFunction.put(slot, (p, inventory, itemSlot) -> {
					if (InfusionUtils.canPayInfusion(p, item)) {
						if (InfusionUtils.payInfusion(p, item)) {
							InfusionUtils.animate(p);
							InfusionUtils.infuseItem(item, infunsion);
						} else {
							p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
						}
					} else {
						p.sendMessage("You don't have enough currency and/or experience for this infusion.");
					}
				});
			} else {
				int slot = (row * 9) + 2 + infunsionLvl;
				_inventory.setItem(slot, mMaxLevelReachedItem);
			}
		} else {
			ItemStack mInfuseStack = new ItemStack(Material.ENCHANTED_BOOK, 1);
			ItemMeta meta = mInfuseStack.getItemMeta();
			meta.displayName(Component.text("Click to select an Infusion to infuse this item with", NamedTextColor.DARK_GRAY)
								.decoration(TextDecoration.ITALIC, false)
								.decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("The first infusion costs only " + InfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			mInfuseStack.setItemMeta(meta);
			_inventory.setItem((row*9), mInfuseStack);

			//set the function when the item is clicked

			_inventory.setItem((row*9) + 2, mPannelList.get(0));
			mMapFunction.put((row*9) + 2, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					InfusionUtils.payInfusion(p, item);
					InfusionUtils.animate(p);
					InfusionUtils.infuseItem(item, InfusionSelection.VITALITY);
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});

			_inventory.setItem((row*9) + 3, mPannelList.get(1));
			mMapFunction.put((row*9) + 3, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					if (InfusionUtils.payInfusion(p, item)) {
						InfusionUtils.animate(p);
						InfusionUtils.infuseItem(item, InfusionSelection.VIGOR);
					} else {
						p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
					}
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});

			_inventory.setItem((row*9) + 4, mPannelList.get(2));
			mMapFunction.put((row*9) + 4, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					if (InfusionUtils.payInfusion(p, item)) {
						InfusionUtils.animate(p);
						InfusionUtils.infuseItem(item, InfusionSelection.TENACITY);
					} else {
						p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
					}
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});

			_inventory.setItem((row*9) + 5, mPannelList.get(3));
			mMapFunction.put((row*9) + 5, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					if (InfusionUtils.payInfusion(p, item)) {
						InfusionUtils.animate(p);
						InfusionUtils.infuseItem(item, InfusionSelection.PERSPICACITY);
					} else {
						p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
					}
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});

			_inventory.setItem((row*9) + 6, mPannelList.get(4));
			mMapFunction.put((row*9) + 6, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					if (InfusionUtils.payInfusion(p, item)) {
						InfusionUtils.animate(p);
						InfusionUtils.infuseItem(item, InfusionSelection.FOCUS);
					} else {
						p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
					}
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});

			_inventory.setItem((row*9) + 7, mPannelList.get(5));
			mMapFunction.put((row*9) + 7, (p, inventory, slot) -> {
				if (InfusionUtils.canPayExp(p, item)) {
					if (InfusionUtils.payInfusion(p, item)) {
						InfusionUtils.animate(p);
						InfusionUtils.infuseItem(item, InfusionSelection.ACUMEN);
					} else {
						p.sendMessage("If you see this message please contact a mod! (Error payInfusion)");
					}
				} else {
					p.sendMessage("You don't have enough currency and/or experience for this infusion.");
				}
			});
		}
	}

	@Override
	public void inventoryClick(InventoryClickEvent event) {
		Inventory clickedInventory = event.getClickedInventory();
		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		event.setCancelled(true);

		if (event.isShiftClick()) {
			return;
		}

		if (!_inventory.equals(clickedInventory)) {
			return;
		}


		if (!mMapFunction.keySet().contains(slot)) {
			return;
		}
		mMapFunction.get(slot).run(player, clickedInventory, slot);
		loadInv(player);
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player) event.getPlayer();
			PlayerTracking.getInstance().updateItemSlotProperties(player, player.getInventory().getHeldItemSlot());
			PlayerTracking.getInstance().updateItemSlotProperties(player, 36);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 37);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 38);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 39);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 40);
		}
	}
}
