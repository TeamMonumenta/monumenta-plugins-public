package com.playmonumenta.plugins.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class UnsignBook extends GenericCommand {

	public static void register() {
		registerPlayerCommand("unsignbook", "monumenta.command.unsignbook", UnsignBook::run);
	}

	private static void run(CommandSender sender, Player player) {

		ItemStack item = player.getInventory().getItemInMainHand();

		// validation check
		if (!item.getType().equals(Material.WRITTEN_BOOK)) {
			sender.sendMessage("Held item is not a Written Book");
			return;
		}

		// get the info of the written book
		BookMeta bookmeta = (BookMeta)item.getItemMeta();

		// create new item
		ItemStack newItem = new ItemStack(Material.WRITABLE_BOOK, item.getAmount());
		BookMeta newBookMeta = (BookMeta)newItem.getItemMeta();

		// transfer the infos
		newBookMeta.addPages(bookmeta.pages().toArray(new Component[0]));
		newBookMeta = newBookMeta.title(bookmeta.title());
		newItem.setItemMeta(newBookMeta);

		// set new item in mainhand
		player.getInventory().setItemInMainHand(newItem);
		sender.sendMessage("Book unsigned");

	}

}
