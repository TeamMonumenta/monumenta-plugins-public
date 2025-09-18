package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.lib.GuiItem;
import com.playmonumenta.plugins.guis.lib.PagedGui;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.CheckReturnValue;

class PebPage extends PagedGui.Page {
	protected class PebEntryHelper {
		private static final Supplier<GuiItem.Builder> BUILDER = GuiItem.builder()
			.defaultNameColor(NamedTextColor.WHITE)
			.defaultNameColor(TextDecoration.UNDERLINED)
			.defaultLoreColor(NamedTextColor.GRAY)
			::copy;

		private final GuiItem.Builder mBuilder;

		protected PebEntryHelper(Material item, String title, String desc) {
			mBuilder = BUILDER.get().name(title).lore(desc).material(item);
			if (item == Material.PLAYER_HEAD) {
				mBuilder.setHead(getPlayer());
			}
		}

		protected PebEntryHelper disableHead() {
			mBuilder.setHead(null);
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper switchTo(PagedGui.PageType target) {
			mBuilder.lore("");
			mBuilder.lore("-> <blue>" + MessagingUtils.plainText(target.name()));
			mBuilder.onMouseClick(() -> setPage(target));
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper toggle(String prompt, ReactiveValue<Boolean> value, String enabled, String disabled) {
			mBuilder.lore("").lore(prompt + (value.get() ? enabled : disabled)).onMouseClick(() -> value.with(x -> !x));
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper toggle(String prompt, String tag, String enabled, String disabled) {
			return toggle(prompt, ReactiveValue.tag(mGui, tag), enabled, disabled);
		}

		@CheckReturnValue
		protected PebEntryHelper toggle(String prompt, String tag) {
			return toggle(prompt, ReactiveValue.tag(mGui, tag), "<green>enabled", "<red>disabled");
		}

		@CheckReturnValue
		protected PebEntryHelper toggle(String prompt, ReactiveValue<Boolean> tag) {
			return toggle(prompt, tag, "<green>enabled", "<red>disabled");
		}

		@CheckReturnValue
		protected PebEntryHelper invertedToggle(String prompt, ReactiveValue<Boolean> value) {
			return toggle(prompt, value, "<red>disabled", "<green>enabled");
		}

		@CheckReturnValue
		protected PebEntryHelper invertedToggle(String prompt, String tag) {
			return toggle(prompt, ReactiveValue.tag(mGui, tag), "<red>disabled", "<green>enabled");
		}

		@CheckReturnValue
		protected PebEntryHelper onMouseClick(Runnable action) {
			mBuilder.onMouseClick(action);
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper onClick(Consumer<InventoryClickEvent> action) {
			mBuilder.onClick(action);
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper command(String command) {
			mBuilder.onMouseClick(() -> {
				getPlayer().performCommand(command);
				mGui.close();
			});
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper cycle(ReactiveValue<Integer> value, String... options) {
			mBuilder.lore("");

			if (options.length > 2 && options.length < 9) {
				for (int i = 0; i < options.length; i++) {
					if (value.get() == i) {
						mBuilder.lore("[<white><key:%s></white>] <green>%s".formatted(Constants.Keybind.hotbar(i).asKeybind(), options[i]));
					} else {
						mBuilder.lore("[<white><key:%s></white>] %s".formatted(Constants.Keybind.hotbar(i).asKeybind(), options[i]));
					}
				}

				mBuilder.onClick(e -> {
					if (e.getClick() == ClickType.LEFT) {
						value.with(x -> (x + 1) % options.length);
					} else if (e.getClick() == ClickType.RIGHT) {
						value.with(x -> (x - 1 + options.length) % options.length);
					} else if (e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() < options.length) {
						value.set(e.getHotbarButton());
					}
				});
			} else {
				for (int i = 0; i < options.length; i++) {
					if (value.get() == i) {
						mBuilder.lore("- <green>" + options[i]);
					} else {
						mBuilder.lore("- " + options[i]);
					}
				}

				mBuilder.onClick(e -> {
					if (e.getClick() == ClickType.LEFT) {
						value.with(x -> (x + 1) % options.length);
					} else if (e.getClick() == ClickType.RIGHT) {
						value.with(x -> (x - 1 + options.length) % options.length);
					}
				});
			}

			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper cycle(String scoreboard, String... options) {
			return cycle(ReactiveValue.scoreboard(mGui, scoreboard, 0), options);
		}

		@CheckReturnValue
		protected PebEntryHelper lore(String entry) {
			mBuilder.lore(entry);
			return this;
		}

		@CheckReturnValue
		protected PebEntryHelper lore(Component entry) {
			mBuilder.lore(entry);
			return this;
		}

		protected void set(int row, int col) {
			mBuilder.set(mGui, row, col);
		}
	}

	private final GuiItem mTitleItem;

	public PebPage(PebGui gui, Material item, String title, String desc) {
		super(gui);
		final var titleBuilder = GuiItem.builder(item)
			.defaultNameColor(NamedTextColor.GOLD)
			.defaultNameColor(TextDecoration.BOLD)
			.defaultNameColor(TextDecoration.UNDERLINED)
			.defaultLoreColor(NamedTextColor.GRAY)
			.name(title)
			.lore(desc);

		if (item == Material.PLAYER_HEAD) {
			titleBuilder.setHead(getPlayer());
		}

		mTitleItem = titleBuilder.build();
	}

	@Override
	protected void render() {
		if (previousPage() != null) {
			entry(Material.OBSERVER, "Previous page", "Click to return to the previous page")
				.lore("")
				.lore("-> <blue>" + MessagingUtils.plainText(previousPage().name()))
				.onMouseClick(this::goToPreviousPage)
				.set(0, 0);
		}

		entry(
			Material.FLINT_AND_STEEL, "Delete P.E.B.s",
			"Removes P.E.B.s in the inventory"
		).command("clickable peb_delete").set(0, 8);

		mGui.setItem(0, 4, mTitleItem);
	}

	protected PebEntryHelper entry(Material item, String title, String desc) {
		return new PebEntryHelper(item, title, desc);
	}
}
