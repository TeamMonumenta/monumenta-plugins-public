package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants.Keybind;
import com.playmonumenta.plugins.integrations.luckperms.GuildFlag;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.inventories.Wallet;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.plugins.utils.WalletUtils.Debt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SettingsView extends View {
	public SettingsView(GuildGui gui) {
		super(gui);
	}

	@Override
	public void setup() {
		// This will need to be expanded as we go
		final Group guild = mGui.mGuildGroup;
		if (guild == null) {
			return;
		}

		boolean hasFlag = GuildFlag.OWNS_PLOT.hasFlag(guild);
		List<Component> plotOwnershipLore = new ArrayList<>();
		if (hasFlag) {
			plotOwnershipLore.add(Component.text("Talk to Orin to visit your guild plot!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			plotOwnershipLore.add(Component.text("You can buy a guild plot for:", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));

			Wallet wallet = WalletManager.getWallet(mGui.mPlayer);
			for (int hotbarButton = 0; hotbarButton < 9; hotbarButton++) {
				if (hotbarButton >= GuildPlotUtils.PLOT_COSTS.size()) {
					break;
				}

				Map<ItemStack, Integer> costMap = GuildPlotUtils.PLOT_COSTS.get(hotbarButton);
				Keybind keybind = Keybind.hotbar(hotbarButton);

				plotOwnershipLore.add(
					Component.text("Option " + (hotbarButton + 1) + ": Press ", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false)
						.append(Component.keybind(keybind))
						.append(Component.text(" to purchase"))
				);

				for (Map.Entry<ItemStack, Integer> costEntry : costMap.entrySet()) {
					ItemStack currency = costEntry.getKey();
					int amount = costEntry.getValue();
					Debt debt = WalletUtils.calculateInventoryAndWalletDebt(
						currency, amount, mGui.mPlayer.getInventory().getContents(), wallet, false);
					plotOwnershipLore.add(Component.text(amount + " ", NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false)
						.append(ItemUtils.getDisplayName(currency))
						.append(Component.space())
						.append(Component.text(
							debt.mMeetsRequirement ? "✓" : "✗",
							debt.mMeetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED
						))
						.append(Component.text(
							debt.mWalletDebt > 0 ? " (" + debt.mNumInWallet + " in wallet)" : "",
							NamedTextColor.GRAY
						))
					);
				}
			}
		}

		if (mGui.mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
			plotOwnershipLore.add(Component.empty());
			plotOwnershipLore.add(
				Component.text("Mods can toggle this for free with ", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Keybind.SWAP_OFFHAND))
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
					case NUMBER_KEY -> {
						int hotbarButton = event.getHotbarButton();
						if (hotbarButton < 0 || hotbarButton >= GuildPlotUtils.PLOT_COSTS.size()) {
							return;
						}
						Map<ItemStack, Integer> costMap = GuildPlotUtils.PLOT_COSTS.get(hotbarButton);

						Wallet wallet = WalletManager.getWallet(mGui.mPlayer);

						// Verify the player has enough
						List<Debt> debts = new ArrayList<>();
						List<String> notEnough = new ArrayList<>();
						for (Map.Entry<ItemStack, Integer> costEntry : costMap.entrySet()) {
							ItemStack currency = costEntry.getKey();
							int required = costEntry.getValue();
							Debt debt = WalletUtils.calculateInventoryAndWalletDebt(
								currency, required, mGui.mPlayer.getInventory().getContents(), wallet, false);
							debts.add(debt);
							if (!debt.mMeetsRequirement) {
								notEnough.add(ItemUtils.getPlainName(currency));
							}
						}

						if (!notEnough.isEmpty()) {
							mGui.mPlayer.sendMessage(Component.text(
								"You do not have enough "
									+ String.join(", ", notEnough)
									+ " to buy a guild plot",
								NamedTextColor.RED
							));
							return;
						}

						for (Debt debt : debts) {
							WalletUtils.payDebt(debt, mGui.mPlayer, true);
						}

						GuildFlag.OWNS_PLOT.setFlag(guild, true);
						mGui.refresh();

						AuditListener.logPlayer(mGui.mPlayer.getName() + " unlocked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild) + " using option " + (hotbarButton + 1));
					}
					default -> {
					}
				}
			});
	}
}
