package com.playmonumenta.plugins.guis.lib;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javax.annotation.concurrent.Immutable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * An item in a {@link Gui}.
 */
@Immutable
public final class GuiItem {
	public static final class Builder {
		@Nullable
		private Material mMaterial;
		private int mCount = 1;
		@Nullable
		private Component mName;
		private boolean mSetPlainTag = true;
		private final List<Component> mLore = new ArrayList<>();
		private int mMaxLoreLength = 30;
		private Style mDefaultLoreStyle = Style.style(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
		private Style mDefaultNameStyle = Style.style(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
		@Nullable
		private OfflinePlayer mHeadOwner;
		private final List<Consumer<InventoryClickEvent>> mHandlers = new ArrayList<>();

		public Builder material(Material m) {
			mMaterial = m;
			return this;
		}

		/**
		 * Sets the number of items in the stack.
		 *
		 * @param count Must be at least 1
		 * @return This builder for chaining
		 */
		public Builder count(int count) {
			mCount = count;
			return this;
		}

		/**
		 * Sets the display name using MiniMessage formatting.
		 *
		 * @param miniMessage The name in MiniMessage format
		 * @return This builder for chaining
		 */
		public Builder name(String miniMessage) {
			return name(MessagingUtils.MINIMESSAGE_ALL.deserialize(miniMessage));
		}

		/**
		 * Sets the display name directly with a component.
		 *
		 * @param component The name component to use
		 * @return This builder for chaining
		 */
		@ApiStatus.Obsolete
		public Builder name(Component component) {
			mName = component;
			return this;
		}

		/**
		 * Adds lore lines using MiniMessage formatting.
		 *
		 * @param args Lore lines in MiniMessage format
		 * @return This builder for chaining
		 */
		public Builder lore(String... args) {
			return lore(Arrays.stream(args).map(MessagingUtils.MINIMESSAGE_ALL::deserialize).toList());
		}

		/**
		 * Adds pre-formatted lore components.
		 *
		 * @param args Lore components to add
		 * @return This builder for chaining
		 */
		public Builder lore(Component... args) {
			return lore(List.of(args));
		}

		/**
		 * Adds a collection of lore components.
		 *
		 * @param args Collection of lore components to add
		 * @return This builder for chaining
		 */
		public Builder lore(Collection<Component> args) {
			mLore.addAll(args);
			return this;
		}

		/**
		 * Prevents adding plain text tags to the item's NBT data.
		 *
		 * @return This builder for chaining
		 */
		public Builder noPlainTag() {
			return setPlainTag(false);
		}

		public Builder setPlainTag(boolean flag) {
			mSetPlainTag = flag;
			return this;
		}

		/**
		 * Sets the maximum length for lore lines before wrapping.
		 *
		 * @param len Maximum characters per lore line (≥0)
		 * @return This builder for chaining
		 */
		public Builder maxLoreLength(int len) {
			mMaxLoreLength = len;
			return this;
		}

		public Builder defaultNameColor(TextColor color) {
			return withDefaultNameStyle(x -> x.color(color));
		}

		public Builder defaultNameColor(TextDecoration... decorations) {
			return withDefaultNameStyle(x -> x.decorate(decorations));
		}

		public Builder withDefaultNameStyle(UnaryOperator<Style> op) {
			mDefaultNameStyle = op.apply(mDefaultNameStyle);
			return this;
		}

		public Builder defaultNameStyle(Style op) {
			mDefaultNameStyle = op;
			return this;
		}

		public Builder defaultLoreColor(TextColor color) {
			return withDefaultLoreStyle(x -> x.color(color));
		}

		public Builder defaultLoreStyle(TextDecoration... decorations) {
			return withDefaultLoreStyle(x -> x.decorate(decorations));
		}

		public Builder defaultLoreStyle(Style op) {
			mDefaultLoreStyle = op;
			return this;
		}

		public Builder withDefaultLoreStyle(UnaryOperator<Style> op) {
			mDefaultLoreStyle = op.apply(mDefaultLoreStyle);
			return this;
		}

		public Builder onLeftClick(Runnable onClick) {
			return onClick(event -> {
				if (event.getClick() == ClickType.LEFT) {
					onClick.run();
				}
			});
		}

		public Builder onRightClick(Runnable onClick) {
			return onClick(event -> {
				if (event.getClick() == ClickType.RIGHT) {
					onClick.run();
				}
			});
		}

		public Builder runCommand(String command, boolean closeAfter, ClickType type, ClickType... types) {
			final var val = EnumSet.of(type, types);

			return onClick(event -> {
				if (!val.contains(event.getClick())) {
					return;
				}

				if (closeAfter) {
					event.getWhoClicked().closeInventory();
				}
				if (event.getWhoClicked() instanceof Player sender) {
					sender.performCommand(command);
				}
			});
		}

		public Builder onClick(Consumer<InventoryClickEvent> onClick) {
			mHandlers.add(onClick);
			return this;
		}

		public Builder onMouseClick(Runnable onClick) {
			return onClick(event -> {
				if (event.getClick().isMouseClick()) {
					onClick.run();
				}
			});
		}

		public Builder setHead(@Nullable OfflinePlayer player) {
			mHeadOwner = player;
			return this;
		}

		public Builder copy() {
			final var builder = new Builder()
				.count(mCount)
				.setPlainTag(mSetPlainTag)
				.lore(mLore)
				.maxLoreLength(mMaxLoreLength)
				.setHead(mHeadOwner)
				.defaultNameStyle(mDefaultNameStyle)
				.defaultLoreStyle(mDefaultLoreStyle);

			if (mMaterial != null) {
				builder.material(mMaterial);
			}

			if (mName != null) {
				builder.name(mName);
			}

			builder.mHandlers.addAll(mHandlers);
			return builder;
		}

		public Builder apply(Consumer<Builder> consumer) {
			consumer.accept(this);
			return this;
		}

		public GuiItem build() {
			Preconditions.checkState(mMaterial != null, "illegal material");
			Preconditions.checkState(mCount > 0, "count must be greater than one");
			Preconditions.checkState(mMaxLoreLength >= 0, "max lore length cannot be negative");

			final var stack = new ItemStack(mMaterial, mCount);
			final var meta = stack.getItemMeta();

			Preconditions.checkState(meta != null, "illegal material used for gui item");

			if (mName != null) {
				meta.displayName(Component.empty().style(mDefaultNameStyle).append(mName));
			}

			meta.addItemFlags(ItemFlag.values());

			meta.lore(
				mLore.stream()
					.map(Component.empty().style(mDefaultLoreStyle)::append)
					.flatMap(x -> GUIUtils.splitLoreLine(x, mMaxLoreLength).stream())
					.toList()
			);

			if (stack.getType() == Material.PLAYER_HEAD && mHeadOwner != null && meta instanceof SkullMeta skullMeta) {
				UUID ownerUuid = mHeadOwner.getUniqueId();
				String ownerName = mHeadOwner.getName();
				PlayerProfile playerProfile;

				if (ownerName == null) {
					ownerName = MonumentaRedisSyncAPI.cachedUuidToName(ownerUuid);
				}

				if (ownerName == null) {
					playerProfile = Bukkit.createProfile(ownerUuid);
				} else {
					playerProfile = Bukkit.createProfile(ownerUuid, ownerName);
				}

				skullMeta.setPlayerProfile(playerProfile);
			}

			stack.setItemMeta(meta);

			if (mSetPlainTag) {
				ItemUtils.setPlainTag(stack);
			}

			return new GuiItem(stack, mHandlers);
		}

		public void set(Gui gui, int index) {
			gui.setItem(index, build());
		}

		public void set(Gui gui, int row, int col) {
			gui.setItem(row, col, build());
		}
	}

	private final ItemStack mItem;
	private final List<Consumer<InventoryClickEvent>> mClickListeners;

	/**
	 * @param item     The base ItemStack to use
	 * @param handlers Initial click handlers to register
	 */
	private GuiItem(ItemStack item, List<Consumer<InventoryClickEvent>> handlers) {
		mItem = item;
		mClickListeners = Collections.unmodifiableList(handlers);
	}

	/**
	 * Creates a new builder for constructing GUI items.
	 *
	 * @param material The base material to use
	 * @return New builder instance
	 */
	public static Builder builder(Material material) {
		return new Builder().material(material);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Getter for the actual ItemStack.
	 *
	 * @return The underlying ItemStack.
	 */
	public ItemStack getItem() {
		return mItem;
	}

	/**
	 * @param event The click event to process
	 */
	void handleClicked(InventoryClickEvent event) {
		for (Consumer<InventoryClickEvent> clickListener : mClickListeners) {
			clickListener.accept(event);
		}
	}
}
