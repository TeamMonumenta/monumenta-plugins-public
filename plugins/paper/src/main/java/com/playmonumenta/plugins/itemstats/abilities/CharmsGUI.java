package com.playmonumenta.plugins.itemstats.abilities;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CharmsGUI extends CustomInventory {
	private static final int START_OF_CHARMS = 45;
	private static final int EXIT_BUTTON_LOC = 53;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material RED_FILLER = Material.RED_STAINED_GLASS_PANE;
	private static final Material YELLOW_FILLER = Material.YELLOW_STAINED_GLASS_PANE;



	public CharmsGUI(Player player) {
		this(player, player);
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer) {
		super(requestingPlayer, 54, "" + targetPlayer.getDisplayName() + "\'s " + "Charms");
		setCharms(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory) {
			//Attempt to load charm if clicked in inventory
			ItemStack item = event.getCurrentItem();
			Player p = (Player) event.getWhoClicked();
			if (item != null && item.getType() != Material.AIR) {
				if (CharmManager.getInstance().validateCharm(p, item)) {
					if (CharmManager.getInstance().addCharm(p, item)) {
						setCharms(p);
						p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1f);
						//Remove one charm from the player inventory, not all ones with this name!
						ItemStack[] items = p.getInventory().getContents();
						for (int i = 0; i < items.length; i++) {
							if (items[i] != null) {
								if (items[i].hasItemMeta() && items[i].getItemMeta().getDisplayName().equals(item.getItemMeta().getDisplayName())) {
									if (items[i].getAmount() > 1) {
										items[i].setAmount(items[i].getAmount() - 1);
									} else {
										items[i].setAmount(0);
									}
									return;
								}
							}
						}
					} else {
						p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
					}
				} else {
					p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
				}
			}
		} else {
			Player p = (Player) event.getWhoClicked();
			// Check for exit gui option
			if (event.getSlot() == 53) {
				p.closeInventory();
			}
			// It's in the charms gui, check clicked charms to remove them
			if (event.getCurrentItem().getType() != Material.RED_STAINED_GLASS && event.getSlot() >= START_OF_CHARMS && event.getSlot() < 52) {

				ItemStack item = mInventory.getItem(event.getSlot());
				if (item == null || item.getType() == Material.AIR) {
					return;
				}
				if (CharmManager.getInstance().removeCharm(p, item)) {
					p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1f);
					setCharms(p);
					InventoryUtils.giveItem(p, item);
				} else {
					p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
				}
			}
		}

	}

	public Boolean setCharms(Player targetPlayer) {
		List<ItemStack> items = CharmManager.getInstance().mPlayerCharms.get(targetPlayer.getUniqueId());
		Optional<Integer> optionalBudget = ScoreboardUtils.getScoreboardValue(targetPlayer, "CharmPower");
		int totalBudget = 0;
		if (optionalBudget.isPresent()) {
			totalBudget = optionalBudget.get();
		}
		if (totalBudget == 0) {
			totalBudget = 15;
		}


		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		for (int i = START_OF_CHARMS; i < 52; i++) {
			mInventory.setItem(i, new ItemStack(RED_FILLER, 1));
		}

		for (int i = 0; i < totalBudget; i++) {
			int slot = 0;
			if (i > 9) {
				slot = 29 + (i - 10);
			} else if (i > 4) {
				slot = 20 + (i - 5);
			} else {
				slot = 11 + i;
			}
			mInventory.setItem(slot, new ItemStack(YELLOW_FILLER, 1));
		}


		//Display active charms
		if (items != null) {
			for (int i = 0; i < items.size(); i++) {
				if (items.get(i) == null || items.get(i).getType() == Material.AIR) {
					continue;
				}
				mInventory.setItem(i + START_OF_CHARMS, items.get(i));
			}

			//Fill out yellow stained glass for visual display of charm budget
			List<ItemStack> indexedCharms = new ArrayList<>();
			for (int i = 0; i < items.size(); i++) {
				if (items.get(i) == null || items.get(i).getType() == Material.AIR) {
					continue;
				}
				for (int j = 0; j < ItemStatUtils.getCharmPower(items.get(i)); j++) {
					indexedCharms.add(items.get(i));
				}
			}

			for (int i = 0; i < indexedCharms.size(); i++) {
				int slot = 0;
				if (i > 9) {
					slot = 29 + (i - 10);
				} else if (i > 4) {
					slot = 20 + (i - 5);
				} else {
					slot = 11 + i;
				}
				mInventory.setItem(slot, indexedCharms.get(i));
			}
		}
		//Charm power indicator
		int charmPower = CharmManager.getInstance().getCharmPower(targetPlayer);
		ItemStack item = new ItemStack(Material.GLOWSTONE_DUST, Math.max(1, charmPower));

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("" + charmPower + " Charm Power Used", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(String.format("%d Total Charm Power", totalBudget), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);
		mInventory.setItem(0, item);

		//Charm effect indicator

		item = new ItemStack(Material.BOOK, 1);

		meta = item.getItemMeta();
		meta.displayName(Component.text("Charm Effect Summary", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


		lore = CharmManager.getInstance().getSummaryOfAllAttributesAsComponents(targetPlayer);
		meta.lore(lore);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);
		mInventory.setItem(9, item);

		//Escape gui button

		item = new ItemStack(Material.BARRIER, 1);

		meta = item.getItemMeta();
		meta.displayName(Component.text("Save and Exit GUI", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);
		mInventory.setItem(EXIT_BUTTON_LOC, item);

		return true;
	}
}
