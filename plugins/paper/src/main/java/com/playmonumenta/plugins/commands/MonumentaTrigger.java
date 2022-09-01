package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class MonumentaTrigger {

	private static final String METADATA_KEY = "MonumentaTrigger";

	private static final AtomicInteger nextId = new AtomicInteger(0);

	public static void register() {
		new CommandAPICommand("monumentatrigger")
			.withArguments(new IntegerArgument("id"))
			.executesPlayer((player, args) -> {
				List<MetadataValue> metadata = player.getMetadata(METADATA_KEY);
				if (!metadata.isEmpty()) {
					Map<Integer, Consumer<Player>> triggers = (Map<Integer, Consumer<Player>>) metadata.get(0).value();
					player.removeMetadata(METADATA_KEY, Plugin.getInstance());
					Consumer<Player> callback = triggers.get((Integer) args[0]);
					if (callback != null) {
						callback.accept(player);
					}
				}
			})
			.register();
	}

	public static String makeTrigger(Player player, Consumer<Player> callback) {
		Map<Integer, Consumer<Player>> triggers;
		List<MetadataValue> metadata = player.getMetadata(METADATA_KEY);
		if (!metadata.isEmpty()) {
			triggers = (Map<Integer, Consumer<Player>>) metadata.get(0).value();
		} else {
			triggers = new HashMap<>();
		}
		int id = nextId.getAndIncrement();
		triggers.put(id, callback);
		player.setMetadata(METADATA_KEY, new FixedMetadataValue(Plugin.getInstance(), triggers));
		return "/monumentatrigger " + id;
	}

}
