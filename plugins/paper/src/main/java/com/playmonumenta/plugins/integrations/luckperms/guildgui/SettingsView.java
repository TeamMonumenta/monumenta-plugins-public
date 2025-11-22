package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants.Keybind;
import com.playmonumenta.plugins.integrations.luckperms.GuildFlag;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SettingsView extends View {
	public SettingsView(GuildGui gui) {
		super(gui);
	}

	@Override
	public void setup() {
		// This will need to be expanded as we go
		final Group guild = mGui.mGuildGroup;
		if (guild == null) {
			mGui.setView(new AllGuildsView(mGui, GuildOrder.DEFAULT));
			return;
		}

		boolean hasFlag = GuildFlag.OWNS_PLOT.hasFlag(guild);
		List<Component> plotOwnershipLore = new ArrayList<>();
		if (hasFlag) {
			plotOwnershipLore.add(Component.text("Talk to Orin to visit your guild plot!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			plotOwnershipLore.add(Component.text("Click for plot and purchasing options.", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		}

		if (mGui.mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
			plotOwnershipLore.add(Component.empty());
			plotOwnershipLore.add(
				Component.text("Mods can toggle access for free with ", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Keybind.SWAP_OFFHAND))
					.append(Component.text(","))
			);
			plotOwnershipLore.add(
				Component.text("or left click to grant other plot types.", NamedTextColor.RED)
			);
		}

		mGui.setItem(
				3,
				4,
				GUIUtils.createBasicItem(
					hasFlag ? Material.LIME_CONCRETE : Material.PINK_CONCRETE,
					1,
					GuildFlag.OWNS_PLOT.description(guild)
						.decoration(TextDecoration.ITALIC, false),
					plotOwnershipLore,
					true
				)
			)
			.onClick((InventoryClickEvent event) -> {
				switch (event.getClick()) {
					case SWAP_OFFHAND -> {
						if (mGui.mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
							boolean modifiedFlag = !GuildFlag.OWNS_PLOT.hasFlag(guild);
							if (modifiedFlag) {
								AuditListener.log(mGui.mPlayer.getName() + " unlocked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild));
							} else {
								AuditListener.log(mGui.mPlayer.getName() + " locked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild));
							}
							GuildFlag.OWNS_PLOT.setFlag(guild, modifiedFlag);
							mGui.refresh();
						}
					}
					case LEFT, RIGHT -> mGui.setView(new BuyPlotTypeView(mGui));
					default -> {
					}
				}
			});
	}
}
