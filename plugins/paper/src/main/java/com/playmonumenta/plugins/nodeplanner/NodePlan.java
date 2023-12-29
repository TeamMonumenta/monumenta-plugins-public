package com.playmonumenta.plugins.nodeplanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class NodePlan {
	Map<String, NodeStack> mStacks = new HashMap<>();
	Map<UUID, NodeStack> mStacksByEntityUuid = new HashMap<>();

	public NodePlan(Location location, float gigabytesPerBlock, JsonObject nodeMemoryInfoObject) throws WrapperCommandSyntaxException {
		JsonElement nodesElement = nodeMemoryInfoObject.get("nodes");

		if (!(nodesElement instanceof JsonObject nodesObject)) {
			throw CommandAPI.failWithString(
				"Cannot load initial node memory info from json: nodes must be JsonObject");
		}

		Location nodeLocation = location;
		for (Map.Entry<String, JsonElement> nodeEntry : nodesObject.entrySet()) {
			String nodeName = nodeEntry.getKey();
			JsonElement nodeElement = nodeEntry.getValue();

			NodeStack nodeStack = new NodeStack(nodeLocation,
				nodeName,
				gigabytesPerBlock,
				nodeElement);
			mStacks.put(nodeName, nodeStack);
			for (UUID uuid : nodeStack.getEntityUuids()) {
				mStacksByEntityUuid.put(uuid, nodeStack);
			}
			nodeLocation = nodeLocation.clone()
				.add(new Vector(0.0f, 0.0f, 2.0f * NodeStack.COLUMN_OFFSET));
		}
	}

	public Set<UUID> getEntityUuids() {
		Set<UUID> result = new HashSet<>();

		for (NodeStack nodeStack : mStacks.values()) {
			result.addAll(nodeStack.getEntityUuids());
		}

		return result;
	}

	// Discard object after use
	public void killEntities() {
		for (NodeStack nodeStack : mStacks.values()) {
			nodeStack.killEntities();
		}
		mStacks.clear();
	}

	public void onInteract(Player player, Interaction interaction) {
		UUID interactionUuid = interaction.getUniqueId();
		NodeStack nodeStack = mStacksByEntityUuid.get(interactionUuid);
		if (nodeStack == null) {
			player.sendMessage(Component.text(
				"How did we get here? Part of plan, but not in a stack?", NamedTextColor.RED));
			return;
		}

		if (nodeStack.isShardInteraction(interactionUuid)) {
			nodeStack.onShardInteraction(player, interaction);
			return;
		}

		// Selected a header - move the shards!

		// Remove shards from source stacks
		List<Shard> movingShards = new ArrayList<>();
		for (NodeStack iterStack : mStacks.values()) {
			movingShards.addAll(iterStack.removeSelectedShards());
		}

		if (movingShards.isEmpty()) {
			player.sendMessage(Component.text("Relocated shards:", NamedTextColor.GOLD, TextDecoration.BOLD));

			boolean unchanged = true;
			for (NodeStack iterStack : mStacks.values()) {
				if (!iterStack.showModifications(player)) {
					unchanged = false;
				}
			}

			if (unchanged) {
				player.sendMessage(Component.text("- No changes found", NamedTextColor.GOLD, TextDecoration.BOLD));
			}

			player.playSound(interaction, Sound.EVENT_RAID_HORN, SoundCategory.BLOCKS, 0.5f, 1.0f);

			return;
		}

		// Add them to the new stacks
		nodeStack.addShards(movingShards);

		// Update entity UUID -> stack linkage
		for (Shard shard : movingShards) {
			for (UUID uuid : shard.getEntityUuids()) {
				mStacksByEntityUuid.put(uuid, nodeStack);
			}
		}

		// Confirmation effect
		player.playSound(interaction, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 1.0f);
	}
}
