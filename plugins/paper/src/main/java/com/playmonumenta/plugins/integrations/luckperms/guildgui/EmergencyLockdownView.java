package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;

public class EmergencyLockdownView extends View {
	protected static final int PAGE_START_X = 0;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 9;
	public static final Permission ACTIVATE_LOCKDOWN = new Permission("monumenta.guild.activate_lockdown");

	public EmergencyLockdownView(GuildGui gui) {
		super(gui);
		mGui.mPlayer.playSound(mGui.mPlayer, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.7f, Constants.Note.C4.mPitch);
	}

	@Override
	public void setup() {
		mGui.setTitle(Component.text("Emergency Lockdown"));

		ItemMeta meta;

		ItemStack borderA = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		meta = borderA.getItemMeta();
		meta.displayName(Component.empty());
		borderA.setItemMeta(meta);
		GUIUtils.createFiller(borderA);

		ItemStack borderB = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
		meta = borderB.getItemMeta();
		meta.displayName(Component.empty());
		borderB.setItemMeta(meta);
		GUIUtils.createFiller(borderB);

		ItemStack fill = new ItemStack(Material.IRON_BARS);
		meta = fill.getItemMeta();
		meta.displayName(Component.empty());
		fill.setItemMeta(meta);
		GUIUtils.createFiller(fill);

		int minX = PAGE_START_X;
		int minY = PAGE_START_Y;
		int maxX = PAGE_START_X + PAGE_WIDTH - 1;
		int maxY = PAGE_START_Y + GuildGui.PAGE_HEIGHT - 1;

		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;

		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				if (y == centerY && x == centerX) {
					setLockdownButton(y, x);
				} else if (x == minX || x == maxX || y == minY || y == maxY) {
					if ((x + y) % 2 == 0) {
						mGui.setItem(y, x, borderA);
					} else {
						mGui.setItem(y, x, borderB);
					}
				} else {
					mGui.setItem(y, x, fill);
				}
			}
		}
	}

	private void setLockdownButton(int y, int x) {
		boolean isLocked = mGui.mGuildGroup != null && LuckPermsIntegration.isLocked(mGui.mGuildGroup);
		boolean hasLockdownAccess = hasLockdownAccess();

		ItemStack confirmButton = new ItemStack(Material.BEDROCK);
		ItemMeta meta = confirmButton.getItemMeta();
		if (!isLocked) {
			if (!hasLockdownAccess) {
				meta.displayName(Component.text("Confirm Lockdown (No Access)", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(
					Component.text("You need to be a guild manager, guild founder,", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text(" or server operator to lock down the guild.", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false)
				));
			} else {
				meta.displayName(Component.text("Confirm Lockdown", NamedTextColor.DARK_RED, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(
					Component.text("Read the tab description first!", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("You can cancel by clicking another tab", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text(" or closing this interface.", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false)
				));
			}
		} else {
			if (!mGui.mPlayer.isOp()) {
				meta.displayName(Component.text("End Lockdown (No Access)", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(
					Component.text("Only a moderator may end a guild's lockdown.", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Please contact a moderator.", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false)
				));
			} else {
				meta.displayName(Component.text("End Lockdown", NamedTextColor.DARK_RED, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(
					Component.text("As an operator, you may", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text(" end a guild's lockdown.", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Make sure to deal with the", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false),
					Component.text(" reason for the lockdown first!", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false)
				));
			}
		}
		confirmButton.setItemMeta(meta);

		GuiItem guiItem = mGui.setItem(y, x, confirmButton);

		if (!isLocked && hasLockdownAccess) {
			guiItem
				.onClick((InventoryClickEvent event) -> {
					if (!hasLockdownAccess()) {
						mGui.mPlayer.sendMessage(
							Component.text("Sorry! You lost access before hitting confirm.",
								NamedTextColor.RED, TextDecoration.BOLD));
						mGui.mPlayer.sendMessage(
							Component.text("If needed, a moderator can still fix this.",
								NamedTextColor.RED, TextDecoration.BOLD));
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						Group guild = mGui.mGuildGroup;
						if (guild == null) {
							Bukkit.getScheduler().runTask(Plugin.getInstance(), ()
								-> mGui.mPlayer.sendMessage(Component.text(
								"The selected guild is no longer available.",
								NamedTextColor.DARK_GRAY)));
							return;
						}

						try {
							boolean isNowLocked = LuckPermsIntegration.setLocked(guild, true).join();
							String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								if (isNowLocked) {
									if (!mGui.mPlayer.hasPermission("group." + guild.getName())) {
										// Skip playing the sound if this player is in the guild;
										// they'll hear it with their guild mates instead
										mGui.mPlayer.playSound(mGui.mPlayer,
											Sound.BLOCK_IRON_DOOR_CLOSE,
											SoundCategory.PLAYERS,
											0.7f,
											Constants.Note.FS3.mPitch);
									}
									mGui.mPlayer.sendMessage(Component.text(
										guildName + " has been locked.", NamedTextColor.DARK_GRAY));
									String guildTag = LuckPermsIntegration.getGuildPlainTag(guild);
									if (guildTag == null) {
										guildTag = "";
									} else {
										guildTag = "[" + guildTag + "] ";
									}
									MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(
										"Guild Emergency Lockdown triggered by " + mGui.mPlayer.getName()
											+ " for the guild " + guildTag + guildName);
								} else {
									mGui.mPlayer.sendMessage(Component.text(
										guildName + " has been unlocked.", NamedTextColor.GREEN));
								}
								mGui.update();
							});
						} catch (Exception ex) {
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								mGui.mPlayer.sendMessage(Component.text("Guild could not be locked.",
									NamedTextColor.RED));
								MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
								mGui.update();
							});
						}
					});
				});
		}

		if (isLocked && mGui.mPlayer.isOp()) {
			guiItem
				.onClick((InventoryClickEvent event) -> {
					if (!mGui.mPlayer.isOp()) {
						mGui.mPlayer.sendMessage(
							Component.text("Sorry! You lost access before hitting confirm.",
								NamedTextColor.RED, TextDecoration.BOLD));
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						Group guild = mGui.mGuildGroup;
						if (guild == null) {
							Bukkit.getScheduler().runTask(Plugin.getInstance(), ()
								-> mGui.mPlayer.sendMessage(Component.text(
								"The selected guild is no longer available.",
								NamedTextColor.DARK_GRAY)));
							return;
						}

						try {
							boolean isNowLocked = LuckPermsIntegration.setLocked(guild, false).join();
							String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								if (isNowLocked) {
									mGui.mPlayer.sendMessage(Component.text(
										guildName + " has been locked.", NamedTextColor.DARK_GRAY));
								} else {
									if (!mGui.mPlayer.hasPermission("group." + guild.getName())) {
										// Skip playing the sound if this player is in the guild;
										// they'll hear it with their guild mates instead
										mGui.mPlayer.playSound(mGui.mPlayer,
											Sound.ITEM_TOTEM_USE,
											SoundCategory.PLAYERS,
											0.7f,
											Constants.Note.FS4.mPitch);
									}
									mGui.mPlayer.sendMessage(Component.text(
										guildName + " has been unlocked.", NamedTextColor.GREEN));
								}
								mGui.update();
							});
						} catch (Exception ex) {
							Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
								mGui.mPlayer.sendMessage(Component.text("Guild could not be unlocked.",
									NamedTextColor.RED));
								MessagingUtils.sendStackTrace(mGui.mPlayer, ex);
								mGui.update();
							});
						}
					});
				});
		}
	}

	private boolean hasLockdownAccess() {
		Group guildAccessGroup = LuckPermsIntegration.getGuild(mGui.mTargetUser);
		return mGui.mPlayer.isOp() || (
			guildAccessGroup != null
				&& Objects.equals(LuckPermsIntegration.getGuildRoot(mGui.mGuildGroup), LuckPermsIntegration.getGuildRoot(guildAccessGroup))
				&& GuildAccessLevel.byGroup(guildAccessGroup).compareTo(GuildAccessLevel.MANAGER) <= 0
				&& mGui.mPlayer.hasPermission(ACTIVATE_LOCKDOWN)
		);
	}
}
