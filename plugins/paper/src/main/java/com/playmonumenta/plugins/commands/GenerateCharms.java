package com.playmonumenta.plugins.commands;

import com.opencsv.CSVReader;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GenerateCharms extends GenericCommand {
	public static void register() {
		registerPlayerCommand("generatecharms", "monumenta.command.generatecharms", GenerateCharms::run);
	}

	public static void run(CommandSender sender, Player player) {
		if (((Player) sender).getGameMode() != GameMode.CREATIVE) {
			return;
		}
		List<List<String>> records = new ArrayList<List<String>>();

		String path = Plugin.getInstance().getDataFolder() + File.separator + "charms.csv";
		try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8))) {
			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				records.add(Arrays.asList(values));
			}
		} catch (Exception ex) {
			MessagingUtils.sendStackTrace(sender, ex);
			ex.printStackTrace();
		}
		int itemCounter = 0;
		int chestCounter = 0;
		for (List<String> row : records) {
			for (int i = 12; i < row.size(); i++) {
				if (row.get(i).equals("") || row.get(i) == null) {
					row.set(i, "0");
				}
			}
			// Item Params
			String itemName = row.get(0);
			String baseItem = row.get(1);
			if (baseItem == null || baseItem.equals("")) {
				baseItem = "minecraft:stone";
			}
			baseItem = baseItem.split(":", 2)[1].toUpperCase();
			String nameColor = row.get(2);
			if (nameColor == null || nameColor.equals("")) {
				nameColor = "white";
			}
			String location = row.get(3);
			if (location == null || location.equals("")) {
				location = "none";
			}
			int power = Integer.parseInt(row.get(4));
			String[] loreText = new String[6];
			loreText[0] = row.get(5);
			loreText[1] = row.get(6);
			loreText[2] = row.get(7);
			loreText[3] = row.get(8);
			loreText[4] = row.get(9);
			loreText[5] = row.get(10);
			for (int i = 0; i < loreText.length; i++) {
				String line = loreText[i];
				if (line == null || line.equals("")) {
					continue;
				}
				if (Character.isDigit(line.charAt(0))) {
					loreText[i] = "+" + line;
				}
			}

			// Logic for item creation
			ItemStack item = new ItemStack(Material.valueOf(baseItem));
			player.getEquipment().setItemInMainHand(item, true);
			player.updateInventory();

			player.chat("/editname " + nameColor + " " + "true" + " false " + itemName);

			// Logic for commands to run
			// Item Info
			player.chat("/editinfo ring charm " + location + " none");
			player.chat("/editcharm power " + Integer.toString(power));
			// Charm Stats
			for (int i = 0; i < loreText.length; i++) {
				String line = loreText[i];
				if (line != null && !line.equals("") && !line.equals("-")) {
					player.chat("/editcharm" + " add " + Integer.toString(i) + " " + line);
				}
			}

			// Logic for inventory manipulation and chest creation
			ItemStack itemToInventory = player.getEquipment().getItemInMainHand();
			player.getInventory().remove(itemToInventory);
			player.getInventory().setItem(9 + itemCounter, itemToInventory);
			player.updateInventory();
			itemCounter++;
			if (itemCounter == 27) {
				itemCounter = 0;
				chestCounter++;
				player.getLocation().add(chestCounter, 0, 0).getBlock().setType(Material.CHEST);
				Chest chest = (Chest) player.getLocation().add(chestCounter, 0, 0).getBlock().getState();
				for (int i = 9; i < 36; i++) {
					chest.getInventory().setItem(i - 9, player.getInventory().getItem(i));
				}
			}
		}
	}
}
