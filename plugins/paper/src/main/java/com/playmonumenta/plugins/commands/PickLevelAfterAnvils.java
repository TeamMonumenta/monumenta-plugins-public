package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.SignUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class PickLevelAfterAnvils extends GenericCommand {
	static final String COMMAND = "picklevelafteranvils";
	static final String FAILURE = "Please enter a number between 1 and your current level.";
	public static final NamespacedKey ANVILTABLE = NamespacedKeyUtils.fromString("epic:r1/items/misc/repair_anvil");

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.picklevelafteranvils");
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run((Player)args[0]);
			})
			.register();
	}

	private static void run(Player target) throws WrapperCommandSyntaxException {
		SignUtils.Menu menu = SignUtils.newMenu(
				new ArrayList<String>(Arrays.asList("", "~~~~~~~~~~~", "Input total", "levels to keep.")))
	            .reopenIfFail(false)
	            .response((player, strings) -> {
					int inputVal = -1;
					try {
						inputVal = Integer.parseInt(strings[0]);
					} catch (Exception e) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage(FAILURE);
							}
						}.runTaskLater(Plugin.getInstance(), 2);
						return true;
					}
					if (inputVal == -1) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage(FAILURE);
							}
						}.runTaskLater(Plugin.getInstance(), 2);
						return true;
					}
					if (inputVal >= 1 && player.getLevel() >= inputVal) {
						final int finalInputVal = inputVal;
						new BukkitRunnable() {
							@Override
							public void run() {
								LootContext context = new LootContext.Builder(player.getLocation()).build();
								LootTable anvilTable = Bukkit.getLootTable(ANVILTABLE);
								Collection<ItemStack> anvilLoot = anvilTable.populateLoot(FastUtils.RANDOM, context);
								int currentXp = ExperienceUtils.getTotalExperience(player);
								int currentAnvilCount = (int) Math.floor((currentXp - ExperienceUtils.getTotalExperience(finalInputVal)) / ExperienceUtils.getTotalExperience(30));
								if (currentAnvilCount <= 0) {
									player.sendMessage("The levels between your current level and the requested level will not be enough to create an anvil.");
									return;
								}
								ExperienceUtils.setTotalExperience(player, currentXp - (currentAnvilCount * ExperienceUtils.getTotalExperience(30)));
								player.sendMessage("You've been given " + String.valueOf(currentAnvilCount) + " anvils.");
								for (ItemStack anvilTableItem : anvilLoot) {
									ItemStack anvil = anvilTableItem;
									anvil.setAmount(64);
									while (currentAnvilCount >= 64) {
										currentAnvilCount -= 64;
										InventoryUtils.giveItem(player, anvil);
									}
									anvil.setAmount(currentAnvilCount);
									InventoryUtils.giveItem(player, anvil);
								}
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					} else {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage(FAILURE);
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					}
					return true;
	            });
		menu.open(target);
	}
}

