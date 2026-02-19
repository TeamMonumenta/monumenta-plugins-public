package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Cat;
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
				for (Entity entity : (Collection<Entity>) args.get("entities")) {
					eggify(args.getUnchecked("player"), entity);
				}
			})
			.register();
	}

	private static void eggify(Player player, Entity entity) {
		if (GuildPlotUtils.guildPlotUseEggsBlocked(player)) {
			player.sendMessage(Component.text("You do not have permission to turn entities into eggs on this plot.", NamedTextColor.RED));
			return;
		}

		if (!entity.isValid() || entity.customName() == null) {
			return;
		}
		EntityType entityType = entity.getType();
		String entityName = MessagingUtils.plainText(entity.customName());

		// if mob is a cat get cats type, otherwise null
		Cat.Type catType = (entity instanceof Cat ? ((Cat) entity).getCatType() : null);

		List<ItemStack> spawnEggs = ServerProperties.getEggifySpawnEggs().stream()
			.flatMap(key -> InventoryUtils.getItemsFromLootTable(player.getLocation(), key).stream())
			.filter(Objects::nonNull)
			.toList();
		for (ItemStack spawnEgg : spawnEggs) {
			if (entityType != ItemUtils.getSpawnEggType(spawnEgg)) {
				continue;
			}
			String[] eggData = NBT.get(spawnEgg, nbt -> {
				ReadableNBT entityTag = nbt.getCompound("EntityTag");
				if (entityTag == null) {
					return null;
				}
				return new String[] {entityTag.getString("CustomName"), entityTag.getString("variant")};
			});
			String customNameJson = eggData == null ? null : eggData[0];
			String name = customNameJson == null ? null : MessagingUtils.plainText(MessagingUtils.parseComponent(customNameJson));
			if (!Objects.equals(entityName, name)) {
				continue;
			}

			// cat specific handling
			if (catType != null) {
				String existingCatTypeNamespacedKey = catType.key().asString();
				String eggCatTypeNamespacedKey = eggData[1];
				if (!existingCatTypeNamespacedKey.equals(eggCatTypeNamespacedKey)) {
					continue;
				}
			}

			entity.remove();
			InventoryUtils.giveItem(player, spawnEgg);
			return;
		}

		player.sendMessage(Component.text("This entity cannot be turned into an egg.", NamedTextColor.RED));
	}

}
