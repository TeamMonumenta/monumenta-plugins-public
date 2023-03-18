package com.playmonumenta.plugins.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RenameItemCommand {

	private static final ImmutableMap<String, ImmutableList<String>> ITEM_SKINS =
		ImmutableMap.<String, ImmutableList<String>>builder()
			.put("Loadout Lockbox", ImmutableList.of(
				"None",
				"King's Valley",
				"Celsian Isles",
				"Architect's Ring",
				"Kaul",
				"Speed",
				"Tank",
				"Damage",
				"Healing",
				"Exploration",
				"Charms",
				"Mage",
				"Warrior",
				"Cleric",
				"Rogue",
				"Alchemist",
				"Scout",
				"Warlock"
			))
			.build();


	public static void register() {
		new CommandAPICommand("renameitem")
			.withPermission("monumenta.command.renameitem")
			.withSubcommand(new CommandAPICommand("name")
				                .withArguments(new GreedyStringArgument("name"))
				                .executes((sender, args) -> {
					                Player player = CommandUtils.getPlayerFromSender(sender);
					                checkItem(player);
					                String name = ((String) args[0]).trim();
					                rename(player, name);
				                }))
			.withSubcommand(new CommandAPICommand("sign")
				                .executes((sender, args) -> {
					                Player player = CommandUtils.getPlayerFromSender(sender);
					                checkItem(player);
					                ItemStack itemStack = player.getInventory().getItemInMainHand();
					                String existingName = ItemStatUtils.addPlayerModified(new NBTItem(itemStack))
						                                      .getString(ItemStatUtils.PLAYER_CUSTOM_NAME_KEY);
					                if (existingName == null) {
						                existingName = "";
					                }
					                int spaceIndex1 = existingName.indexOf(' ', existingName.length() / 2);
					                int spaceIndex2 = existingName.lastIndexOf(' ', existingName.length() / 2);
					                int spaceIndex = spaceIndex2 < 0 || (spaceIndex1 >= 0 && Math.abs(spaceIndex1 - existingName.length() / 2) < Math.abs(spaceIndex2 - existingName.length() / 2))
						                                 ? spaceIndex1 : spaceIndex2;
					                if (spaceIndex < 0) {
						                spaceIndex = existingName.length();
					                }
					                SignUtils.newMenu(List.of(existingName.substring(0, spaceIndex).trim(), existingName.substring(spaceIndex).trim(), "~~~~~~~~~~~", "Enter new name")).reopenIfFail(true).response(
						                (p, text) -> {
							                String name = (StringUtils.substring(text[0], 0, 24).trim() + " " + StringUtils.substring(text[1], 0, 24).trim()).trim();
							                rename(p, name);
							                return true;
						                }
					                ).open(player);
				                }))
			.withSubcommand(new CommandAPICommand("skingui")
				                .executes((sender, args) -> {
					                Player player = CommandUtils.getPlayerFromSender(sender);
					                checkItem(player);
					                showSkinGui(player);
				                }))
			.register();
	}

	private static void checkItem(Player player) throws WrapperCommandSyntaxException {
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		if (itemStack.getType().isAir()
			    || !ItemUtils.isShulkerBox(itemStack.getType())) {
			player.sendMessage(Component.text("You must be holding a shulker box!", NamedTextColor.RED));
			throw CommandAPI.failWithString("You must be holding a Shulker box!");
		}
		if (ItemStatUtils.getRegion(itemStack) != ItemStatUtils.Region.SHULKER_BOX
			    && !ShulkerEquipmentListener.isEquipmentBox(itemStack)
			    && !ShulkerEquipmentListener.isCharmBox(itemStack)) {
			player.sendMessage(Component.text("This item cannot be renamed!", NamedTextColor.RED));
			throw CommandAPI.failWithString("This item cannot be renamed!");
		}
	}

	private static void rename(Player player, String name) {
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		NBTCompound playerModified = ItemStatUtils.addPlayerModified(new NBTItem(itemStack, true));
		if (!name.isEmpty()) {
			playerModified.setString(ItemStatUtils.PLAYER_CUSTOM_NAME_KEY, name);
		} else {
			playerModified.removeKey(ItemStatUtils.PLAYER_CUSTOM_NAME_KEY);
		}
		ItemStatUtils.generateItemStats(itemStack);
		player.updateInventory();
		String baseName = ItemUtils.getPlainNameIfExists(itemStack);
		if (!name.isEmpty()) {
			AuditListener.log("Item rename: " + player.getName() + " renamed a " + (baseName.isEmpty() ? "Shulker Box" : baseName) + " to '" + name + "'");
		}
	}

	private static void showSkinGui(Player player) {
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		String plainName = ItemUtils.getPlainNameIfExists(itemStack);
		ImmutableList<String> skins = plainName == null ? null : ITEM_SKINS.get(plainName);
		if (skins == null) {
			player.sendMessage(Component.text("This item doesn't have any skins.", NamedTextColor.RED));
			return;
		}
		new ItemSkinGui(player, itemStack, skins).open();
	}

	private static class ItemSkinGui extends Gui {
		private final ItemStack mItemStack;
		private final ImmutableList<String> mSkins;

		public ItemSkinGui(Player player, ItemStack itemStack, ImmutableList<String> skins) {
			super(player, 3 * 9, Component.text("Choose item skin"));
			mItemStack = itemStack;
			mSkins = skins;
		}

		@Override
		protected void setup() {
			String plainName = ItemUtils.getPlainName(mItemStack);

			ItemStack resetIcon = new ItemStack(mItemStack.getType());
			ItemUtils.setPlainName(resetIcon, plainName);
			ItemUtils.modifyMeta(resetIcon, meta -> {
				meta.displayName(Component.text("Reset to default", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			});
			setItem(0, new GuiItem(resetIcon, false))
				.onLeftClick(() -> {
					ItemStatUtils.addPlayerModified(new NBTItem(mItemStack, true)).removeKey(ItemStatUtils.CUSTOM_SKIN_KEY);
					mPlayer.updateInventory();
				});

			for (int i = 0; i < mSkins.size(); i++) {
				String skin = mSkins.get(i);
				ItemStack icon = new ItemStack(mItemStack.getType());
				ItemUtils.setPlainName(icon, plainName);
				ItemUtils.modifyMeta(icon, meta -> {
					meta.displayName(Component.text(skin, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				});
				ItemStatUtils.addPlayerModified(new NBTItem(icon, true)).setString(ItemStatUtils.CUSTOM_SKIN_KEY, skin);
				setItem(i + 1, new GuiItem(icon, false))
					.onLeftClick(() -> {
						ItemStatUtils.addPlayerModified(new NBTItem(mItemStack, true)).setString(ItemStatUtils.CUSTOM_SKIN_KEY, skin);
						mPlayer.updateInventory();
					});
			}
		}
	}

}
