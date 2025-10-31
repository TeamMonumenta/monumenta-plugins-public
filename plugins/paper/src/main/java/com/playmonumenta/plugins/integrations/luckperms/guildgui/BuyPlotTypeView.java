package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildFlag;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotType;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.inventories.Wallet;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BuyPlotTypeView extends View {
	public BuyPlotTypeView(GuildGui gui) {
		super(gui);
	}

	@Override
	public void setup() {
		final Group guild = mGui.mGuildGroup;
		if (guild == null) {
			return;
		}

		if (GuildFlag.OWNS_PLOT.hasFlag(guild)) {
			mGui.setView(new SettingsView(mGui));
			return;
		}

		List<Component> plotOwnershipLore = getPlotOwnershipLore();

		for (GuildPlotType plotType : GuildPlotType.values()) {
			ItemStack plotIcon = plotType.getIcon();
			ItemMeta plotMeta = plotIcon.getItemMeta();
			List<Component> lore = plotMeta.lore();
			if (lore == null) {
				lore = new ArrayList<>();
			} else {
				lore = new ArrayList<>(lore);
				lore.add(Component.empty());
			}
			lore.addAll(plotOwnershipLore);
			plotMeta.lore(lore);
			plotIcon.setItemMeta(plotMeta);

			mGui.setItem(3, 2 + 2 * plotType.mScore, plotIcon)
				.onClick(getOnClick(guild, plotType));
		}
	}

	public List<Component> getPlotOwnershipLore() {
		List<Component> plotOwnershipLore = new ArrayList<>();
		plotOwnershipLore.add(Component.text("You can buy a guild plot for:", NamedTextColor.GOLD)
			.decoration(TextDecoration.ITALIC, false));

		Wallet wallet = WalletManager.getWallet(mGui.mPlayer);
		int numButtons = Integer.min(9, GuildPlotUtils.PLOT_COSTS.size());
		for (int hotbarButton = 0; hotbarButton < numButtons; hotbarButton++) {
			Map<ItemStack, Integer> costMap = GuildPlotUtils.PLOT_COSTS.get(hotbarButton);
			Constants.Keybind keybind = Constants.Keybind.hotbar(hotbarButton);

			plotOwnershipLore.add(
				Component.text("Option " + (hotbarButton + 1) + ": Press ", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(keybind))
					.append(Component.text(" to purchase"))
			);

			for (Map.Entry<ItemStack, Integer> costEntry : costMap.entrySet()) {
				ItemStack currency = costEntry.getKey();
				int amount = costEntry.getValue();
				WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(
					currency, amount, mGui.mPlayer.getInventory().getContents(), wallet, false);
				plotOwnershipLore.add(Component.text(amount + " ", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false)
					.append(ItemUtils.getDisplayName(currency))
					.append(Component.space())
					.append(Component.text(
						debt.mMeetsRequirement() ? "✓" : "✗",
						debt.mMeetsRequirement() ? NamedTextColor.GREEN : NamedTextColor.RED
					))
					.append(Component.text(
						debt.mWalletDebt() > 0 ? " (" + debt.mNumInWallet() + " in wallet)" : "",
						NamedTextColor.GRAY
					))
				);
			}
		}

		if (mGui.mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
			plotOwnershipLore.add(Component.empty());
			plotOwnershipLore.add(
				Component.text("Mods can toggle this for free with ", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.SWAP_OFFHAND))
			);
		}

		return plotOwnershipLore;
	}

	public Consumer<InventoryClickEvent> getOnClick(Group guild, GuildPlotType plotType) {
		return (InventoryClickEvent event) -> {
			switch (event.getClick()) {
				case SWAP_OFFHAND -> {
					if (mGui.mPlayer.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
						boolean modifiedFlag = !GuildFlag.OWNS_PLOT.hasFlag(guild);
						if (modifiedFlag) {
							AuditListener.log(mGui.mPlayer.getName() + " unlocked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild));
						} else {
							AuditListener.log(mGui.mPlayer.getName() + " locked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild));
						}

						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							try {
								LuckPermsIntegration.setPlotType(guild, plotType).join();
								GuildFlag.OWNS_PLOT.setFlag(guild, true).join();
							} catch (Exception ex) {
								mGui.mPlayer.sendMessage(Component.text("An error occurred toggling guild ownership:", NamedTextColor.RED));
								MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
							}
							Bukkit.getScheduler().runTask(Plugin.getInstance(), mGui::refresh);
						});
					}
				}
				case NUMBER_KEY -> {
					if (GuildFlag.OWNS_PLOT.hasFlag(guild)) {
						mGui.setView(new SettingsView(mGui));
						return;
					}

					int hotbarButton = event.getHotbarButton();
					if (hotbarButton < 0 || hotbarButton >= GuildPlotUtils.PLOT_COSTS.size()) {
						return;
					}
					Map<ItemStack, Integer> costMap = GuildPlotUtils.PLOT_COSTS.get(hotbarButton);

					Wallet wallet = WalletManager.getWallet(mGui.mPlayer);

					// Verify the player has enough
					List<WalletUtils.Debt> debts = new ArrayList<>();
					List<String> notEnough = new ArrayList<>();
					for (Map.Entry<ItemStack, Integer> costEntry : costMap.entrySet()) {
						ItemStack currency = costEntry.getKey();
						int required = costEntry.getValue();
						WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(
							currency, required, mGui.mPlayer.getInventory().getContents(), wallet, false);
						debts.add(debt);
						if (!debt.mMeetsRequirement()) {
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

					for (WalletUtils.Debt debt : debts) {
						WalletUtils.payDebt(debt, mGui.mPlayer, true);
					}

					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						try {
							LuckPermsIntegration.setPlotType(guild, plotType).join();
							GuildFlag.OWNS_PLOT.setFlag(guild, true).join();

							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								AuditListener.logPlayer(mGui.mPlayer.getName() + " unlocked the guild plot for " + LuckPermsIntegration.getNonNullGuildName(guild) + " using option " + (hotbarButton + 1));

								mGui.refresh();
							});
						} catch (Exception ex) {
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								mGui.mPlayer.sendMessage(Component.text("An error occurred toggling guild ownership:", NamedTextColor.RED));
								MessagingUtils.sendStackTrace(mGui.mPlayer, ex);

								for (WalletUtils.Debt debt : debts) {
									WalletUtils.refundDebt(debt, mGui.mPlayer, true);
								}

								mGui.refresh();
							});
						}
					});

				}
				default -> {
				}
			}
		};
	}
}
