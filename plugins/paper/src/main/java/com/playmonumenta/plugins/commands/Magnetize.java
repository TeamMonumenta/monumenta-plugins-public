package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Magnetize extends GenericCommand {

	private static final String COMMAND = "magnetize";
	private static final String PERMISSION = "monumenta.command.magnetize";

	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new DoubleArgument("pickup_range"),
				new DoubleArgument("mob_range"),
				new BooleanArgument("consume")
			).executes((sender, args) -> {
				Collection<Player> players = (Collection<Player>) args[0];
				double pickupRange = (Double) args[1];
				double mobRange = (Double) args[2];
				boolean consume = (Boolean) args[3];

				int successes = 0;
				for (Player player : players) {
					successes += run(player, consume, pickupRange, mobRange) ? 1 : 0;
				}
				return successes;
			})
			.register();

	}

	private static boolean run(Player player, boolean consume, double pickupRange, double mobRange) {

		Collection<Item> nearbyItems = player.getLocation().getNearbyEntitiesByType(Item.class, pickupRange);

		if (nearbyItems.isEmpty()) {
			player.sendMessage(Component.text("No nearby death pile items found!", NamedTextColor.RED));
			return false;
		}

		//Get nearby mobs to the player to check for near items
		List<LivingEntity> nearbyMobs = mobRange > 0 ? EntityUtils.getNearbyMobs(player.getLocation(), pickupRange + mobRange) : Collections.emptyList();
		//Total count of items restored
		int restored = 0;
		boolean blockedByMob = false;

		//Loop through nearby items and send them to player if they match their death pile
		for (Item item : nearbyItems) {

			ItemStack stack = item.getItemStack();

			if (stack.getType().isAir()
				    || !GraveManager.isGraveItem(item)
				    || !player.getUniqueId().equals(item.getOwner())) {
				continue;
			}

			//Check to see if any of the mobs around the player are too close to the item
			if (nearbyMobs.stream().anyMatch(mob -> mob.getLocation().distanceSquared(item.getLocation()) < mobRange * mobRange)) {
				blockedByMob = true;
				continue;
			}

			//Restore item to player
			item.teleport(player.getLocation());
			restored++;
		}

		if (restored > 0) {
			//If we restored items, consume item in mainhand if enabled
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			if (consume && mainhand.getAmount() > 0) {
				mainhand.setAmount(mainhand.getAmount() - 1);
			}
			player.sendMessage(Component.text("Retrieved ", NamedTextColor.GREEN)
				.append(Component.text("" + restored, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
				.append(Component.text(" death pile items!", NamedTextColor.GREEN)));
			if (blockedByMob) {
				player.sendMessage(Component.text("Some item(s) were not retrieved because they were too close to mobs.", NamedTextColor.YELLOW));
			}
			return true;
		} else {
			if (blockedByMob) {
				player.sendMessage(Component.text("All nearby death pile items are too close to mobs!", NamedTextColor.RED));
			} else {
				player.sendMessage(Component.text("No nearby death pile items found!", NamedTextColor.RED));
			}
			return false;
		}
	}
}
