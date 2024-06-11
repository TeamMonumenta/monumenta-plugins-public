package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission.GuildPermissionResult;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class PermissionView extends View {
	protected static final int TARGET_Y = 2;
	protected static final int TARGET_X = 4;
	protected static final int PERMISSION_ROW = 4;

	protected final PermissionHolder mTarget;
	protected final Supplier<CompletableFuture<ItemStack>> mRefreshTargetIcon;
	protected final @Nullable Consumer<InventoryClickEvent> mOnTargetClick;
	protected final View mPreviousView;
	protected ItemStack mTargetIcon;

	public PermissionView(GuildGui gui,
	                      PermissionHolder target,
						  ItemStack targetIcon,
	                      Supplier<CompletableFuture<ItemStack>> refreshTargetIcon,
	                      @Nullable Consumer<InventoryClickEvent> onTargetClick) {
		super(gui);
		mTarget = target;
		mTargetIcon = targetIcon;
		mRefreshTargetIcon = refreshTargetIcon;
		mOnTargetClick = onTargetClick;
		if (mGui.mView instanceof PermissionView lastPermissionView) {
			mPreviousView = lastPermissionView.mPreviousView;
		} else {
			mPreviousView = mGui.mView;
		}
	}

	@Override
	public void setup() {
		setBackIcon();

		Component targetName = mTargetIcon.getItemMeta().displayName();
		GuiItem targetIcon = mGui.setItem(TARGET_Y, TARGET_X, mTargetIcon);
		if (mOnTargetClick != null) {
			targetIcon.onClick(mOnTargetClick);
		}

		setPermissionIcon(PERMISSION_ROW, 3, GuildPermission.CHAT, targetName);
		setPermissionIcon(PERMISSION_ROW, 5, GuildPermission.VISIT, targetName);
	}

	@Override
	public void refresh() {
		super.refresh();

		Bukkit.getScheduler().runTaskAsynchronously(mGui.mMainPlugin, () -> {
			ItemStack refreshedTargetIcon = mRefreshTargetIcon.get().join();

			Bukkit.getScheduler().runTask(mGui.mMainPlugin, () -> {
				mTargetIcon = refreshedTargetIcon;
				mGui.update();
			});
		});
	}

	protected void setPermissionIcon(
		int row,
		int column,
		GuildPermission guildPermission,
		@Nullable Component targetName
	) {
		if (mGui.mGuildGroup == null) {
			ItemStack item = LuckPermsIntegration.getErrorQuestionMarkPlayerHead();
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Guild not found", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("How did we get here?", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)));
			item.setItemMeta(meta);
			mGui.setItem(row, column, item);
			return;
		}

		GuildPermissionResult permissionResult = guildPermission.checkAccess(mGui.mGuildGroup, mTarget);
		PermissionHolder causingHolder = permissionResult.mCausingHolder;
		boolean isPermitted = permissionResult.mResult;
		boolean isExplicitlySet = mTarget == causingHolder;

		Material material;
		TextColor textColor = isPermitted ? NamedTextColor.GREEN : NamedTextColor.RED;
		if (isExplicitlySet) {
			material = isPermitted ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
		} else {
			material = isPermitted ? Material.LIME_CONCRETE : Material.PINK_CONCRETE;
		}

		if (targetName == null) {
			targetName = Component.text(mTarget.getFriendlyName(), NamedTextColor.RED);
		}

		ItemStack item = GUIUtils.createBasicItem(material, guildPermission.mLabel, textColor, guildPermission.mDescription);
		ItemMeta meta = item.getItemMeta();

		List<Component> lore = meta.lore();
		Component baseLoreFormatting = Component.text("", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false);
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.add(Component.empty());
		lore.add(getResultComponent(isPermitted, isExplicitlySet, causingHolder));

		if (mGui.mayManagePermissions(false)) {
			lore.add(Component.empty());
			lore.add(baseLoreFormatting
				.append(Component.keybind(Constants.Keybind.HOTBAR_1))
				.append(Component.text(": Enable ", NamedTextColor.GREEN))
				.append(Component.text(guildPermission.mLabel + " permission for "))
				.append(targetName));
			lore.add(baseLoreFormatting
				.append(Component.keybind(Constants.Keybind.HOTBAR_2))
				.append(Component.text(": Unset (use default) ", NamedTextColor.YELLOW))
				.append(Component.text(guildPermission.mLabel + " permission for "))
				.append(targetName));
			lore.add(baseLoreFormatting
				.append(Component.keybind(Constants.Keybind.HOTBAR_3))
				.append(Component.text(": Disable ", NamedTextColor.RED))
				.append(Component.text(guildPermission.mLabel + " permission for "))
				.append(targetName));
			meta.lore(lore);
		}

		item.setItemMeta(meta);
		mGui.setItem(row, column, item).onClick((InventoryClickEvent event) -> {
			if (mGui.mGuildGroup == null) {
				return;
			}

			if (!mGui.mayManagePermissions(true)) {
				return;
			}

			switch (event.getHotbarButton()) {
				case 0 -> {
					guildPermission.setExplicitPermission(mGui.mGuildGroup, mTarget, true);
					mGui.mPlayer.playSound(mGui.mPlayer,
						Sound.ENTITY_VILLAGER_YES,
						SoundCategory.PLAYERS,
						1.0f,
						1.0f);
					mGui.refresh();
				}
				case 1 -> {
					guildPermission.setExplicitPermission(mGui.mGuildGroup, mTarget, null);
					mGui.mPlayer.playSound(mGui.mPlayer,
						Sound.ENTITY_VILLAGER_TRADE,
						SoundCategory.PLAYERS,
						1.0f,
						1.0f);
					mGui.refresh();
				}
				case 2 -> {
					guildPermission.setExplicitPermission(mGui.mGuildGroup, mTarget, false);
					mGui.mPlayer.playSound(mGui.mPlayer,
						Sound.ENTITY_VILLAGER_NO,
						SoundCategory.PLAYERS,
						1.0f,
						1.0f);
					mGui.refresh();
				}
				default -> {
				}
			}
		});
	}

	protected static Component getResultComponent(boolean isPermitted, boolean isExplicitlySet, @Nullable PermissionHolder causingHolder) {
		Component resultComponent = Component.empty()
			.color(NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false);
		if (isPermitted) {
			resultComponent = resultComponent
				.append(Component.text("Allowed", NamedTextColor.GREEN));
		} else {
			resultComponent = resultComponent
				.append(Component.text("Disallowed", NamedTextColor.RED));
		}
		if (isExplicitlySet) {
			resultComponent = resultComponent
				.append(Component.text(" explicitly"));
		} else if (causingHolder instanceof Group causingGroup) {
			resultComponent = resultComponent
				.append(Component.text(" by "))
				.append(LuckPermsIntegration.getGroupDescription(causingGroup));
		} else if (causingHolder != null) {
			resultComponent = resultComponent
				.append(Component.text(" by " + causingHolder.getFriendlyName()));
		} else {
			resultComponent = resultComponent
				.append(Component.text(" by default"));
		}
		return resultComponent;
	}

	protected void setBackIcon() {
		ItemStack backIcon = GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.WHITE, true);
		mGui.setItem(GuildGui.HEADER_Y, 0, backIcon)
			.onClick((InventoryClickEvent event) -> mGui.setView(mPreviousView));
	}
}
