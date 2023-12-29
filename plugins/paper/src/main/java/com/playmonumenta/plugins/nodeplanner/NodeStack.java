package com.playmonumenta.plugins.nodeplanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class NodeStack {
	public static final float COLUMN_OFFSET = 1.0f;
	private static final float STACK_MARKER_OFFSET = -1.0f / 8.0f;
	private static final float STACK_MARKER_SIZE = 7.0f / 8.0f;
	protected static final float BLOCK_WIDTH = 7.0f / 8.0f;
	private static final BlockData NODE_LIMIT_BLOCK = Material.BLACK_STAINED_GLASS.createBlockData();
	protected static final BlockData SHARD_MEMORY_BLOCK = Material.SMOOTH_STONE.createBlockData();
	protected static final BlockData SHARD_HUGE_PAGE_BLOCK = Material.POLISHED_ANDESITE.createBlockData();
	protected static final BlockData SHARD_BLOCK_SELECTED = Material.SEA_LANTERN.createBlockData();


	public final String mName;
	private final Location mNormalMemoryBottomLocation;
	private final Location mHugePagesBottomLocation;
	private final NamedBox mNormalMemoryLimit;
	private final NamedBox mHugePageLimit;
	private final List<Shard> mShards = new ArrayList<>();

	public NodeStack(Location location, String nodeName, float gigabytesPerBlock, JsonElement nodeElement)
		throws WrapperCommandSyntaxException {
		mName = nodeName;

		if (!(nodeElement instanceof JsonObject nodeObject)) {
			throw CommandAPI.failWithString("Node " + nodeName + " entry must be a json object");
		}

		JsonElement nodeMemoryElement = nodeObject.get("max_memory_GB");
		if (!(nodeMemoryElement instanceof JsonPrimitive nodeMemoryPrimitive) || !nodeMemoryPrimitive.isNumber()) {
			throw CommandAPI.failWithString("Node " + nodeName + " max_memory_GB must be a number");
		}
		float normalMemoryLimit = nodeMemoryPrimitive.getAsFloat();

		JsonElement nodeHugePageElement = nodeObject.get("max_hugepage_GB");
		if (!(nodeHugePageElement instanceof JsonPrimitive nodeHugePagePrimitive) || !nodeHugePagePrimitive.isNumber()) {
			throw CommandAPI.failWithString("Node " + nodeName + " max_hugepage_GB must be a number");
		}
		float hugePageLimit = nodeHugePagePrimitive.getAsFloat();

		JsonElement shardsElement = nodeObject.get("shards");
		if (!(shardsElement instanceof JsonArray shardsArray)) {
			throw CommandAPI.failWithString("Node " + nodeName + " shards must be an array of shard info");
		}

		mNormalMemoryBottomLocation = location.clone();
		Location hugePageBottomLocation = mNormalMemoryBottomLocation.clone();
		hugePageBottomLocation.add(0.0, 0.0, COLUMN_OFFSET);
		mHugePagesBottomLocation = hugePageBottomLocation;

		// Reverse the order, so the bottom of the stack matches the last element
		ArrayList<JsonElement> shardsReversed = new ArrayList<>();
		for (JsonElement shardElement : shardsArray) {
			shardsReversed.add(shardElement);
		}
		Collections.reverse(shardsReversed);

		Location shardMemoryLocation = mNormalMemoryBottomLocation.clone();
		Location shardHugePageLocation = mHugePagesBottomLocation.clone();
		int elementIndex = shardsReversed.size();
		for (JsonElement shardElement : shardsReversed) {
			--elementIndex;
			if (!(shardElement instanceof JsonObject shardObject)) {
				throw CommandAPI.failWithString("Node " + nodeName + " shard index " + elementIndex
					+ " must be a json object");
			}

			mShards.add(new Shard(nodeName, gigabytesPerBlock, elementIndex, shardObject, shardMemoryLocation, shardHugePageLocation));
		}

		Location normalMemoryLimitLocation = mNormalMemoryBottomLocation.clone();
		normalMemoryLimitLocation.add(new Vector(STACK_MARKER_OFFSET, normalMemoryLimit / gigabytesPerBlock, 0.0));

		mNormalMemoryLimit = new NamedBox(normalMemoryLimitLocation,
			STACK_MARKER_SIZE,
			STACK_MARKER_SIZE,
			NODE_LIMIT_BLOCK,
			Component.text(nodeName + "\nNormal Memory"));

		Location hugePageLimitLocation = mHugePagesBottomLocation.clone();
		hugePageLimitLocation.add(new Vector(STACK_MARKER_OFFSET, hugePageLimit / gigabytesPerBlock, 0.0));

		mHugePageLimit = new NamedBox(hugePageLimitLocation,
			STACK_MARKER_SIZE,
			STACK_MARKER_SIZE,
			NODE_LIMIT_BLOCK,
			Component.text(nodeName + "\nHuge Pages"));
	}

	public Set<UUID> getEntityUuids() {
		Set<UUID> result = new HashSet<>();

		result.addAll(mNormalMemoryLimit.getEntityUuids());
		result.addAll(mHugePageLimit.getEntityUuids());

		for (Shard shard : mShards) {
			result.addAll(shard.getEntityUuids());
		}

		return result;
	}

	// Discard object after use
	public void killEntities() {
		mNormalMemoryLimit.killEntities();
		mHugePageLimit.killEntities();

		for (Shard shard : mShards) {
			shard.killEntities();
		}
		mShards.clear();
	}

	public boolean isShardInteraction(UUID interactionUuid) {
		return !mNormalMemoryLimit.getInteractionUuid().equals(interactionUuid)
			&& !mHugePageLimit.getInteractionUuid().equals(interactionUuid);
	}

	// Returns true if the limit boxes were selected
	public void onShardInteraction(Player player, Interaction interaction) {
		for (Shard shard : mShards) {
			if (shard.onShardInteraction(player, interaction)) {
				return;
			}
		}
	}

	public List<Shard> removeSelectedShards() {
		List<Shard> removed = new ArrayList<>();

		Location shardMemoryLocation = mNormalMemoryBottomLocation.clone();
		Location shardHugePageLocation = mHugePagesBottomLocation.clone();
		Iterator<Shard> it = mShards.iterator();
		while (it.hasNext()) {
			Shard shard = it.next();
			if (shard.isSelected()) {
				removed.add(shard);
				shard.deselect();
				it.remove();
			} else {
				shard.updateStackLocation(shardMemoryLocation, shardHugePageLocation);
			}
		}

		return removed;
	}

	public void addShards(List<Shard> shards) {
		Location normalMemoryLocation;
		Location hugePageLocation;

		if (mShards.isEmpty()) {
			normalMemoryLocation = mNormalMemoryBottomLocation.clone();
			hugePageLocation = mHugePagesBottomLocation.clone();
		} else {
			Shard lastShard = mShards.get(mShards.size() - 1);
			normalMemoryLocation = lastShard.getNormalMemoryTop().clone();
			hugePageLocation = lastShard.getHugeMemoryTop().clone();
		}

		for (Shard shard : shards) {
			shard.updateStackLocation(normalMemoryLocation, hugePageLocation);
		}
		mShards.addAll(shards);
	}

	public boolean showModifications(Audience audience) {
		Map<String, Set<String>> movedShards = new TreeMap<>();

		for (Shard shard : mShards) {
			if (mName.equals(shard.getOriginalNode())) {
				continue;
			}
			movedShards
				.computeIfAbsent(shard.getName(), k -> new TreeSet<>())
				.add(shard.getNamespace());
		}

		if (movedShards.isEmpty()) {
			return true;
		}

		audience.sendMessage(Component.text("- " + mName + ":", NamedTextColor.GOLD, TextDecoration.BOLD));
		for (Map.Entry<String, Set<String>> shardEntry : movedShards.entrySet()) {
			String shardName = shardEntry.getKey();
			Set<String> namespaces = shardEntry.getValue();
			audience.sendMessage(Component.text("  - " + shardName + ":", NamedTextColor.YELLOW));
			for (String namespace : namespaces) {
				audience.sendMessage(Component.text("    - " + namespace));
			}
		}

		return false;
	}
}
