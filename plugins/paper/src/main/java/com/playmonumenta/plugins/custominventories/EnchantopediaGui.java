package com.playmonumenta.plugins.custominventories;

import com.comphenix.protocol.wrappers.Pair;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantopediaGui extends Gui {
	private static final int INV_SIZE = 54;
	private static final Component BASE_TITLE = Component.text("Enchantopedia");
	private static final String ROOT_PATH = "monumenta:handbook/enchantments/root";

	private final List<Advancement> mCategories;
	private final List<Advancement> mEnchants;
	private final LinkedHashMap<Advancement, Advancement> mCategoryMap;

	private int mRow = 0;
	private int mFilterIndex = -1;
	private @Nullable ItemStack mFilterItem = null;
	private String mFilterTerm = "";
	private boolean mFilterSelected = false;

	public EnchantopediaGui(Player player) {
		super(player, INV_SIZE, BASE_TITLE);

		mCategories = new ArrayList<>();
		mEnchants = new ArrayList<>();
		mCategoryMap = new LinkedHashMap<>();

		Advancement root = Bukkit.getAdvancement(NamespacedKeyUtils.fromString(ROOT_PATH));
		if (root == null) {
			return;
		}

		// Get all custom enchantment advancements from server
		ArrayDeque<Advancement> stack = new ArrayDeque<>();
		root.getChildren().stream()
			.filter(a -> !a.getKey().asString().equals("monumenta:handbook/enchantments/agility"))    // "Agility" isn't a category of advancements
			.forEach(category -> {
				mCategories.add(category);
				stack.addAll(category.getChildren());
				int categorySize = 0;
				while (!stack.isEmpty()) {
					var enchant = stack.pop();
					mEnchants.add(enchant);
					mCategoryMap.put(enchant, category);
					stack.addAll(enchant.getChildren());
					categorySize += 1;
				}
				int spacersNeeded = 9 - categorySize % 9;
				if (categorySize % 9 != 0) {
					spacersNeeded += 9;
				}
				for (int i = 0; i < spacersNeeded; i++) {
					mEnchants.add(null);
				}
			});
	}

	@Override
	protected void setup() {
		int resetFiltersSlot = 0;   // Reset filters button
		int backArrowSlot = 1;      // Back arrow
		int itemFilterSlot = 3;     // Item examine
		int nameSearchSlot = 4;     // Filter by name
		int categoryFilterSlot = 5; // Filter by category
		int nextArrowSlot = 8;      // Next arrow

		int offset = 9;
		List<Advancement> displayedEnchants = mEnchants;

		// Back arrow
		if (mRow > 0) {
			setItem(backArrowSlot,
				new GuiItem(GUIUtils.createBasicItem(
					Material.ARROW,
					"Scroll Up",
					NamedTextColor.WHITE))
					.onClick((evt) -> {
						if (mRow > 0) {
							mRow -= 1;
							update();
						}
					}));
		}

		// Item filter
		addItemFilter(itemFilterSlot);

		ItemStack finalFilter = mFilterItem; // Theoretically isn't necessary, but reviewdog gets cranky without it
		if (finalFilter != null) {
			displayedEnchants = mEnchants.stream().filter(adv -> {
				if (adv == null) {
					return false;
				}
				for (var filter : getEnchantNames(finalFilter)) {
					if (MessagingUtils.plainText(adv.displayName()).contains(filter.getFirst().getName())) {
						return true;
					}
				}
				return false;
			}).toList();
		}


		// Name filter
		setItem(nameSearchSlot, new GuiItem(GUIUtils.createBasicItem(
			Material.OAK_SIGN,
			Component.text("Search By Name", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
			List.of("Click to filter enchantments by name", "Shift-click to reset"), NamedTextColor.GRAY)).onClick((evt) -> {
			if (evt.isShiftClick()) {
				mFilterTerm = "";
				mFilterSelected = false;
			} else {
				openSignMenu((filterTerm) -> {
					mFilterSelected = true;
					mFilterTerm = filterTerm;
					open();
				});
			}
			mRow = 0;
			mFilterItem = null;
			update();
		}));

		if (!mFilterTerm.equals("")) {
			displayedEnchants = mEnchants.stream().filter(adv -> adv != null && MessagingUtils.plainText(adv.displayName()).toUpperCase(Locale.ROOT).contains(mFilterTerm.toUpperCase(Locale.ROOT))).toList();
		}


		// Category filter
		Advancement category = null;
		if (mFilterIndex > -1 && mFilterIndex < mCategories.size()) {
			category = mCategories.get(mFilterIndex);
		}

		addCategoryFilter(categoryFilterSlot, category);

		Advancement finalCategory = category;
		if (category != null) {
			displayedEnchants = mEnchants.stream().filter(adv -> {
				if (adv == null) {
					return false;
				} else {
					Advancement match = mCategoryMap.get(adv);
					if (match == null) {
						return false;
					} else {
						return match.equals(finalCategory);
					}
				}
			}).toList();
		}

		if (!mFilterSelected) {
			displayedEnchants = mCategories;
			offset = 18;
		}

		// Next arrow
		int rowCount = displayedEnchants.size() / 9 - (INV_SIZE - offset) / 9 + 1;
		if (mRow < rowCount) {
			setItem(nextArrowSlot, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Scroll Down", NamedTextColor.WHITE)).onClick((evt) -> {
				if (mRow < rowCount) {
					mRow += 1;
					update();
				}
			}));
		}

		if (!mFilterSelected) {
			addCategories(offset);
		} else {
			// Reset filters button
			setItem(resetFiltersSlot, new GuiItem(GUIUtils.createBasicItem(
				Material.BARRIER,
				Component.text("Reset Filters", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				List.of("Click here to reset all filters"), NamedTextColor.GRAY)).onClick((evt) -> {
				mRow = 0;
				mFilterIndex = -1;
				mFilterItem = null;
				mFilterTerm = "";
				mFilterSelected = false;
				update();
			}));
			addEnchants(offset, displayedEnchants);
		}
	}

	private void addItemFilter(int slot) {
		if (mFilterItem == null) {
			setItem(slot,
				new GuiItem(GUIUtils.createBasicItem(
					Material.SPYGLASS,
					Component.text("View Item Enchantments", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
					List.of("Click an item in your inventory to view its enchantments",
						"Click here to reset"),
					NamedTextColor.GRAY
				)).onClick((evt) -> {
					mFilterItem = null;
					mFilterTerm = "";
					mFilterSelected = true;
					update();
				}));
		} else {
			List<Component> lore = new ArrayList<>();
			for (var enchant : getEnchantNames(mFilterItem)) {
				for (var enchant2 : mEnchants) {
					if (enchant2 == null) {
						continue;
					}
					if (MessagingUtils.plainText(enchant2.displayName()).contains(enchant.getFirst().getName())) {
						lore.add(enchant.getFirst().getDisplay(enchant.getSecond()));
					}
				}
			}
			lore.add(Component.text(""));
			lore.add(Component.text("Click an item in your inventory to view its enchantments", NamedTextColor.GRAY));
			lore.add(Component.text("Click here to reset", NamedTextColor.GRAY));

			setItem(slot, GUIUtils.createBasicItem(
				mFilterItem,
				1,
				ItemUtils.getDisplayName(mFilterItem),
				lore,
				true
			)).onClick((evt) -> {
				mFilterSelected = false;
				mFilterItem = null;
				mFilterTerm = "";
				update();
			});
		}
	}

	private void addCategoryFilter(int slot, @Nullable Advancement category) {
		ItemStack categoryIcon = GUIUtils.createBasicItem(
			Material.ENDER_EYE,
			Component.text("Category: All", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
			List.of("Left or right click to cycle through filters", "Shift-click to reset"),
			NamedTextColor.GRAY);

		if (category != null && category.getDisplay() != null) {
			var name = MessagingUtils.plainText(category.displayName()).replace("[", "").replace("]", "");
			var c = NamedTextColor.WHITE;
			if (name.contains("Curses")) {
				c = NamedTextColor.RED;
			}

			categoryIcon = GUIUtils.createBasicItem(
				category.getDisplay().icon(),
				1,
				Component.text("Category: " + name, c).decoration(TextDecoration.ITALIC, false),
				List.of(Component.text("Left or right click to cycle through filters", NamedTextColor.GRAY), Component.text("Shift-click to reset", NamedTextColor.GRAY)),
				true);
		}

		setItem(slot, categoryIcon).onClick(evt -> {
			mFilterSelected = true;
			if (evt.isShiftClick()) {
				mFilterSelected = false;
				mFilterIndex = -1;
			} else if (evt.isLeftClick()) {
				mFilterIndex += 1;
				if (mFilterIndex >= mCategories.size()) {
					mFilterIndex = -1;
				}
			} else if (evt.isRightClick()) {
				mFilterIndex -= 1;
				if (mFilterIndex < -1) {
					mFilterIndex = mCategories.size() - 1;
				}
			}
			mRow = 0;
			mFilterItem = null;
			mFilterTerm = "";
			update();
		});
	}

	private void addCategories(int offsetStart) {
		int i = 0;
		int offset = offsetStart;
		while (i + offset < INV_SIZE) {
			if (i >= mCategories.size()) {
				break;
			}
			var cat = mCategories.get(i);
			if (cat == null) {
				i += 1;
				offset += 1;
				continue;
			}

			if (offset % 9 == 0 || offset % 9 == 8) {
				offset += 1;
				continue;
			}

			var d = cat.getDisplay();
			if (d == null) {
				MMLog.warning("Null category display in Enchantopedia GUI");
				i += 1;
				continue;
			}

			String name = MessagingUtils.plainText(cat.displayName()).replace("[", "").replace("]", "");

			TextColor nameColor = NamedTextColor.WHITE;
			if (MessagingUtils.plainText(cat.displayName()).contains("Curses")) {
				nameColor = NamedTextColor.RED;
			}

			int finalIndex = i;
			var item = new GuiItem(GUIUtils.createBasicItem(
				d.icon(),
				1,
				Component.text(name, nameColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
				new ArrayList<>(),
				true
			)).onClick(evt -> {
				mFilterSelected = true;
				mFilterIndex = finalIndex;
				update();
			});

			GUIUtils.splitLoreLine(item.getItem(), d.description().appendNewline().appendNewline().append(Component.text("Click to view enchantments").color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY), 45, true);

			setItem(offset, item);
			i += 1;
			offset += 1;
		}

		var viewAll = new GuiItem(GUIUtils.createBasicItem(
			Material.ENDER_EYE,
			Component.text("View All Enchantments")
				.color(NamedTextColor.WHITE)
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false)
		)).onClick(evt -> {
			mFilterSelected = true;
			update();
		});
		GUIUtils.splitLoreLine(viewAll.getItem(), Component.text("A scrollable list of all enchantments, separated by category.", NamedTextColor.GRAY).appendNewline().appendNewline().append(Component.text("Click to view enchantments").color(NamedTextColor.WHITE)), 45, true);
		setItem(offset, viewAll);
	}

	private void addEnchants(int offset, List<Advancement> displayedEnchants) {
		int slot = 0;
		while (slot + offset < INV_SIZE) {
			if (slot + mRow * 9 >= displayedEnchants.size()) {
				return;
			}
			var adv = displayedEnchants.get(slot + mRow * 9);
			if (adv == null) {
				slot += 1;
				continue;
			}

			if (!mPlayer.getAdvancementProgress(adv).isDone()) {
				setItem(slot + offset, GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE, Component.text("Enchantment not discovered!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true), ""));
				slot += 1;
				continue;
			}

			var d = adv.getDisplay();
			if (d == null) {
				MMLog.warning("Null advancement display in Enchantopedia GUI");
				slot += 1;
				continue;
			}

			String name =
				MessagingUtils.plainText(adv.displayName()).replace("[", "").replace("]", "");

			TextColor nameColor = NamedTextColor.WHITE;
			Advancement category = mCategoryMap.get(adv);
			if (category != null && MessagingUtils.plainText(category.displayName()).contains("Curses")) {
				nameColor = NamedTextColor.RED;
			}

			var item = GUIUtils.createBasicItem(
				d.icon(),
				1,
				Component.text(name, nameColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
				new ArrayList<>(),
				true);

			GUIUtils.splitLoreLine(item, d.description().color(NamedTextColor.WHITE), 45, true);
			setItem(slot + offset, item);

			slot += 1;
		}
	}

	private void openSignMenu(Consumer<String> onSuccess) {
		close();
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter a name", "to search for"))
			.response((player, lines) -> {
				// Cancel if no input
				if (lines[0].isEmpty()) {
					onSuccess.accept("");
					return true;
				}

				onSuccess.accept(lines[0]);
				return true;
			})
			.reopenIfFail(false)
			.open(mPlayer);
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (event.getCurrentItem() == null || getEnchantNames(event.getCurrentItem()).isEmpty()) {
			return;
		}
		mFilterSelected = true;
		mRow = 0;
		mFilterIndex = -1;
		mFilterItem = event.getCurrentItem();
		update();
	}

	private List<Pair<EnchantmentType, Integer>> getEnchantNames(ItemStack item) {
		List<Pair<EnchantmentType, Integer>> enchants = new ArrayList<>();
		for (var enc : EnchantmentType.values()) {
			int level = ItemStatUtils.getEnchantmentLevel(item, enc);
			if (level > 0) {
				enchants.add(new Pair<>(enc, level));
			}
		}
		return enchants;
	}
}
