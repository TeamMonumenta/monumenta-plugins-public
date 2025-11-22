package com.playmonumenta.plugins.guis.lib;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class PagedGui extends Gui {
	public abstract static class Page {
		protected final PagedGui mGui;

		protected Page(PagedGui gui) {
			mGui = gui;
		}

		protected abstract void render();

		protected final void setPage(PageType type) {
			mGui.setPage(type);
		}

		@Nullable
		@Contract(pure = true)
		protected final PageType previousPage() {
			return mGui.previousPage();
		}

		protected final void goToPreviousPage() {
			mGui.goToPreviousPage();
		}

		protected final Player getPlayer() {
			return mGui.mPlayer;
		}

		protected boolean onGuiClick(InventoryClickEvent event) {
			return true;
		}

		protected void onOutsideInventoryClick(InventoryClickEvent event) {
		}

		protected void onPlayerInventoryClick(InventoryClickEvent event) {
		}

		protected void onInventoryDrag(InventoryDragEvent event) {
		}
	}

	public record PageType(Component name, int size) {
		public PageType(String name, int size) {
			this(MessagingUtils.fromMiniMessage(name), size);
		}
	}

	private final ReactiveValue<PageType> mCurrPageType;
	private boolean mShouldRecreatePage = true;
	@Nullable
	private Page mCurrPage;
	private final IdentityHashMap<PageType, Supplier<Page>> mMap = new IdentityHashMap<>();
	// used as a stack
	private final ArrayDeque<PageType> mPageQueue = new ArrayDeque<>();

	/**
	 * Constructs a new GUI instance.
	 *
	 * @param player The player who will interact with this GUI
	 * @param filler The item used to fill empty inventory slots
	 */
	protected PagedGui(Player player, ItemStack filler, PageType defaultPage) {
		super(player, filler, defaultPage.name(), defaultPage.size());
		mCurrPageType = ReactiveValue.of(this, defaultPage);
	}

	protected void registerPage(PageType type, Supplier<Page> pageSupplier) {
		mMap.put(type, pageSupplier);
	}

	private Page createPage() {
		return Objects.requireNonNull(mMap.get(mCurrPageType.get())).get();
	}

	private void setPage0(PageType type) {
		mCurrPageType.set(type);
		mShouldRecreatePage = true;
	}

	protected void setPage(PageType type) {
		Preconditions.checkArgument(type != null, "type != null");
		Preconditions.checkArgument(mMap.containsKey(type), "attempting to set page to unregistered page");
		mPageQueue.push(mCurrPageType.get());

		if (mPageQueue.size() > 10) {
			mPageQueue.pollLast();
		}

		setPage0(type);
	}

	@Nullable
	protected PageType previousPage() {
		return mPageQueue.peek();
	}

	protected void goToPreviousPage() {
		Preconditions.checkState(!mPageQueue.isEmpty(), "no previous page to return to");
		setPage0(mPageQueue.pop());
	}

	@Override
	protected final void render() {
		if (mShouldRecreatePage || mCurrPage == null) {
			mCurrPage = createPage();
		}

		mCurrPage.render();
	}

	@Override
	protected final boolean onGuiClick(InventoryClickEvent event) {
		if (mCurrPage == null) {
			mCurrPage = createPage();
		}

		return mCurrPage.onGuiClick(event);
	}

	@Override
	protected final void onOutsideInventoryClick(InventoryClickEvent event) {
		if (mCurrPage == null) {
			mCurrPage = createPage();
		}

		mCurrPage.onOutsideInventoryClick(event);
	}

	@Override
	protected final void onPlayerInventoryClick(InventoryClickEvent event) {
		if (mCurrPage == null) {
			mCurrPage = createPage();
		}

		mCurrPage.onPlayerInventoryClick(event);
	}

	@Override
	protected final void onInventoryDrag(InventoryDragEvent event) {
		if (mCurrPage == null) {
			mCurrPage = createPage();
		}

		mCurrPage.onInventoryDrag(event);
	}

	@Override
	protected int getSize() {
		return mCurrPageType.get().size();
	}

	@Override
	protected Component getTitle() {
		return mCurrPageType.get().name();
	}
}
