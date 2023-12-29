package com.playmonumenta.plugins.nodeplanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.nodeplanner.NodeStack.BLOCK_WIDTH;
import static com.playmonumenta.plugins.nodeplanner.NodeStack.SHARD_BLOCK_SELECTED;
import static com.playmonumenta.plugins.nodeplanner.NodeStack.SHARD_HUGE_PAGE_BLOCK;
import static com.playmonumenta.plugins.nodeplanner.NodeStack.SHARD_MEMORY_BLOCK;

public class Shard {
	private final String mOriginalNode;
	private final String mNamespace;
	private final String mName;
	private final NamedBox mNormalMemoryBox;
	private final NamedBox mHugePageMemoryBox;

	protected Shard(
		String nodeName,
		float gigabytesPerBlock,
		int elementIndex,
		JsonObject shardObject,
		Location shardMemoryLocation,
		Location shardHugePageLocation
	) throws WrapperCommandSyntaxException {
		mOriginalNode = nodeName;

		JsonElement namespaceElement = shardObject.get("namespace");
		if (!(namespaceElement instanceof JsonPrimitive namespacePrimitive) || !namespacePrimitive.isString()) {
			throw CommandAPI.failWithString("Node " + nodeName + " shard index " + elementIndex
				+ " namespace must be a string");
		}
		mNamespace = namespacePrimitive.getAsString();

		JsonElement idElement = shardObject.get("shard");
		if (!(idElement instanceof JsonPrimitive idPrimitive) || !idPrimitive.isString()) {
			throw CommandAPI.failWithString("Node " + nodeName + " shard index " + elementIndex
				+ " shard ID must be a string");
		}
		mName = idPrimitive.getAsString();

		JsonElement memoryElement = shardObject.get("normal_GB");
		if (!(memoryElement instanceof JsonPrimitive memoryPrimitive) || !memoryPrimitive.isNumber()) {
			throw CommandAPI.failWithString("Node " + nodeName + " shard index " + elementIndex
				+ " normal_GB must be a number in GB");
		}
		float shardMemory = memoryPrimitive.getAsFloat();

		JsonElement hugePageElement = shardObject.get("hugepage_GB");
		if (!(hugePageElement instanceof JsonPrimitive hugePagePrimitive) || !hugePagePrimitive.isNumber()) {
			throw CommandAPI.failWithString("Node " + nodeName + " shard index " + elementIndex
				+ " hugepage_GB must be a number in GB");
		}
		float shardHugePage = hugePagePrimitive.getAsFloat();

		Component shardLabel = Component.text(mNamespace + " - " + mName);

		mNormalMemoryBox = new NamedBox(shardMemoryLocation,
			BLOCK_WIDTH,
			shardMemory / gigabytesPerBlock,
			SHARD_MEMORY_BLOCK,
			shardLabel);

		mHugePageMemoryBox = new NamedBox(shardHugePageLocation,
			BLOCK_WIDTH,
			shardHugePage / gigabytesPerBlock,
			SHARD_HUGE_PAGE_BLOCK,
			shardLabel);

		updateStackLocation(shardMemoryLocation, shardHugePageLocation);
	}

	public String getNamespace() {
		return mNamespace;
	}

	public String getName() {
		return mName;
	}

	public String getOriginalNode() {
		return mOriginalNode;
	}

	public void select() {
		mNormalMemoryBox.setBlockData(SHARD_BLOCK_SELECTED);
		mHugePageMemoryBox.setBlockData(SHARD_BLOCK_SELECTED);
	}

	public void deselect() {
		mNormalMemoryBox.setBlockData(SHARD_MEMORY_BLOCK);
		mHugePageMemoryBox.setBlockData(SHARD_HUGE_PAGE_BLOCK);
	}

	public void updateStackLocation(Location shardMemoryLocation, Location shardHugePageLocation) {
		Location tempLoc;

		mNormalMemoryBox.setBottomPos(shardMemoryLocation);
		tempLoc = mNormalMemoryBox.getTopPos();
		shardMemoryLocation.set(tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());

		mHugePageMemoryBox.setBottomPos(shardHugePageLocation);
		tempLoc = mHugePageMemoryBox.getTopPos();
		shardHugePageLocation.set(tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
	}

	public Location getNormalMemoryTop() {
		return mNormalMemoryBox.getTopPos();
	}

	public Location getHugeMemoryTop() {
		return mHugePageMemoryBox.getTopPos();
	}

	public Set<UUID> getEntityUuids() {
		Set<UUID> result = new HashSet<>();

		result.addAll(mNormalMemoryBox.getEntityUuids());
		result.addAll(mHugePageMemoryBox.getEntityUuids());

		return result;
	}

	public Set<UUID> getInteractionUuids() {
		return Set.of(
			mNormalMemoryBox.getInteractionUuid(),
			mHugePageMemoryBox.getInteractionUuid()
		);
	}

	public void killEntities() {
		mNormalMemoryBox.killEntities();
		mHugePageMemoryBox.killEntities();
	}

	public boolean isSelected() {
		return mNormalMemoryBox.getBlockData().equals(SHARD_BLOCK_SELECTED);
	}

	public boolean onShardInteraction(Player player, Interaction interaction) {
		if (!getInteractionUuids().contains(interaction.getUniqueId())) {
			return false;
		}

		// Toggle selection
		if (isSelected()) {
			deselect();
			player.playSound(interaction,
				Sound.BLOCK_BEACON_DEACTIVATE,
				SoundCategory.BLOCKS,
				1.0f,
				1.0f);
		} else {
			select();
			player.playSound(interaction,
				Sound.BLOCK_BEACON_ACTIVATE,
				SoundCategory.BLOCKS,
				1.0f,
				1.0f);
		}

		return true;
	}
}
