package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Eggify {

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("eggify")
			.withPermission("monumenta.command.eggify")
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				new EntitySelectorArgument.ManyEntities("entities"))
			.executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[1]) {
					eggify((Player) args[0], entity);
				}
			})
			.register();
	}

	private static void eggify(Player player, Entity entity) {
		if (!entity.isValid()) {
			return;
		}
		EntityType entityType = entity.getType();
		String entityName = ChatColor.stripColor(entity.getCustomName());
		List<ItemStack> spawnEggs = ServerProperties.getEggifySpawnEggs().stream()
			.flatMap(key -> InventoryUtils.getItemsFromLootTable(player.getLocation(), key).stream())
			.filter(Objects::nonNull)
			.toList();
		for (ItemStack spawnEgg : spawnEggs) {
			if (entityType != ItemUtils.getSpawnEggType(spawnEgg)) {
				continue;
			}
			NBTItem nbtItem = new NBTItem(spawnEgg);
			NBTCompound entityTag = nbtItem.getCompound("EntityTag");
			String customNameJson = entityTag == null ? null : entityTag.getString("CustomName");
			String name = customNameJson == null ? null : ChatColor.stripColor(MessagingUtils.plainText(MessagingUtils.parseComponent(customNameJson)));
			if (!Objects.equals(entityName, name)) {
				continue;
			}
			entity.remove();
			InventoryUtils.giveItem(player, spawnEgg);
			return;
		}

		player.sendMessage(ChatColor.RED + "This entity cannot be turned into an egg.");
	}

}
