package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.overrides.LimeTesseractOverride;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PickLevelAfterAnvils extends GenericCommand {

	public static final NamespacedKey ANVIL_TABLE = NamespacedKeyUtils.fromString("epic:r1/items/misc/repair_anvil");
	public static int LEVELS_PER_ANVIL = 25;
	public static int XP_PER_ANVIL = ExperienceUtils.getTotalExperience(LEVELS_PER_ANVIL);

	private static final String COMMAND = "picklevelafteranvils";
	private static final String FAILURE = "Please enter a number between 1 and your current level.";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.picklevelafteranvils");
		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run((Player)args[0]);
			})
			.register();
	}

	public static void run(Player target) {
		SignUtils.Menu menu = SignUtils.newMenu(
				Arrays.asList("", "~~~~~~~~~~~", "Input total", "levels to keep."))
			.reopenIfFail(false)
			.response((player, strings) -> {
				int inputVal;
				try {
					inputVal = Integer.parseInt(strings[0]);
				} catch (Exception e) {
					player.sendMessage(Component.text(FAILURE, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
					return true;
				}
				if (inputVal >= 1 && player.getLevel() >= inputVal) {
					int currentXp = ExperienceUtils.getTotalExperience(player);
					int anvilsCreated = (currentXp - ExperienceUtils.getTotalExperience(inputVal)) / XP_PER_ANVIL;
					if (anvilsCreated <= 0) {
						player.sendMessage(Component.text("The levels between your current level and the requested level will not be enough to create an anvil.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
						return true;
					}
					ExperienceUtils.setTotalExperience(player, currentXp - (anvilsCreated * XP_PER_ANVIL));
					ItemStack mainHand = player.getInventory().getItemInMainHand();
					if (ItemStatUtils.isUpgradedLimeTesseract(mainHand)) {
						player.sendMessage(Component.text("The tesseract pulls from your intellect and gains " + anvilsCreated + " anvil charges.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
						ItemStatUtils.setCharges(mainHand, ItemStatUtils.getCharges(mainHand) + anvilsCreated);
						ItemStatUtils.generateItemStats(mainHand);
					} else {
						if (LimeTesseractOverride.isAnyLimeTesseract(mainHand)) {
							player.sendMessage(Component.text("The tesseract pulls from your intellect, and gives you " + anvilsCreated + " anvils.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
						} else {
							player.sendMessage(Component.text("You have been given " + anvilsCreated + " anvils.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
						}
						InventoryUtils.giveItemFromLootTable(player, ANVIL_TABLE, anvilsCreated);
					}
				} else {
					player.sendMessage(Component.text(FAILURE, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				}
				return true;
			});
		menu.open(target);
	}
}

