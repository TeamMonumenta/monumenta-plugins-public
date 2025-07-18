package com.playmonumenta.plugins.discoveries;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ItemDiscovery {
	public Marker mMarkerEntity;
	public int mId;
	public ItemDiscoveryTier mTier;
	public NamespacedKey mLootTablePath;
	public @Nullable NamespacedKey mOptionalFunctionPath;

	public ItemDiscovery(Marker markerEntity, int id, ItemDiscoveryTier tier, NamespacedKey lootTablePath, @Nullable NamespacedKey optionalFunctionPath) {
		mMarkerEntity = markerEntity;
		mId = id;
		mTier = tier;
		mLootTablePath = lootTablePath;
		mOptionalFunctionPath = optionalFunctionPath;
	}

	// manager for the visual effects shown by discoveries
	public void runEffect(List<Player> players, boolean collected) {
		int tick = Bukkit.getCurrentTick();
		if (!collected) {
			mTier.mPeriodicEffect.accept(tick, mMarkerEntity.getLocation().clone(), players);
		} else {
			if (tick % 5 == 0) {
				new PartialParticle(Particle.FALLING_DUST, mMarkerEntity.getLocation())
					.delta(0.05f, 0.05f, 0.05f)
					.count(2)
					.data(Material.DEEPSLATE.createBlockData())
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
		}
	}

	// returns whether the event was successful
	public boolean giveLootToPlayer(Player player) {
		LootTable lootTable = Bukkit.getServer().getLootTable(mLootTablePath);
		if (lootTable == null) {
			MMLog.fine("[Discoveries] Invalid loot table on Discovery with id: " + mId + " to player: " + player.getName());
			return false;
		}

		// generate the loot table and stack all similar items together
		Collection<ItemStack> lootItems = lootTable.populateLoot(FastUtils.RANDOM, new LootContext.Builder(player.getLocation()).build());
		if (lootItems.isEmpty()) {
			// exit successfully if there is nothing to give
			return true;
		}

		List<ItemStack> items = new ArrayList<>();
		for (ItemStack item : lootItems) {
			// increase the count of an existing stack if matched
			if (items.stream().anyMatch(stack -> stack.isSimilar(item))) {
				items.stream().filter(stack -> stack.isSimilar(item))
					.findFirst().ifPresent(itemStack -> itemStack.setAmount(itemStack.getAmount() + item.getAmount()));
			} else {
				items.add(item);
			}
		}

		// determine whether the player has enough inventory slots available
		int slotsNeeded = 0;
		for (ItemStack item : items) {
			int amountNeeded = item.getAmount();
			for (ItemStack slot : player.getInventory().getStorageContents()) {
				if (slot != null && slot.isSimilar(item)) {
					amountNeeded -= (slot.getMaxStackSize() - slot.getAmount());
				}
			}
			if (amountNeeded <= 0) {
				continue;
			}

			slotsNeeded = (int)(slotsNeeded + Math.ceil((double)amountNeeded / item.getMaxStackSize()));
		}
		int openSlots = InventoryUtils.numEmptySlots(player.getInventory());
		if (slotsNeeded > openSlots) {
			int neededSlots = slotsNeeded - openSlots;
			player.sendActionBar(Component.text("Open " + neededSlots + " inventory slot" + (neededSlots == 1 ? "" : "s") + " to collect this pickup.", mTier.mDisplayColor));
			return false;
		}

		// action bar message on pickup
		Component giveMessage = Component.text("Collected ", mTier.mDisplayColor);
		int total = 0;
		for (ItemStack item : items) {
			String name = ItemUtils.getPlainName(item);
			if (name.isEmpty()) {
				name = StringUtils.capitalizeWords(item.getType().name().replace("_", " "));
			}

			giveMessage = giveMessage.append(Component.text(item.getAmount() + "x "
				+ name
				+ (total == items.size() - 1 ? "" : ", "), mTier.mDisplayColor));

			total++;
		}
		player.sendActionBar(giveMessage);

		for (ItemStack item : items) {
			InventoryUtils.giveItemWithStacksizeCheck(player, item);
		}
		mTier.mCollectSound.accept(player, mMarkerEntity.getLocation());
		return true;
	}

	// transfer data onto the marker entity
	public void writeDataOnMarker() {
		NBTEntity entity = new NBTEntity(mMarkerEntity);
		NBTCompound container = entity.getPersistentDataContainer().getOrCreateCompound("discovery");
		container.setInteger("id", mId);
		container.setString("tier", mTier.name());
		container.setString("loot", mLootTablePath.getNamespace() + ":" + mLootTablePath.getKey());
		container.setString("function", mOptionalFunctionPath == null ? "" : (mOptionalFunctionPath.getNamespace() + ":" + mOptionalFunctionPath.getKey()));
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("id", mId);
		object.addProperty("tier", mTier.name());
		object.addProperty("loot", mLootTablePath.getNamespace() + ":" + mLootTablePath.getKey());
		object.addProperty("function", mOptionalFunctionPath == null ? "" : (mOptionalFunctionPath.getNamespace() + ":" + mOptionalFunctionPath.getKey()));
		object.addProperty("marker_uuid", mMarkerEntity.getUniqueId().toString());

		JsonObject location = new JsonObject();
		location.addProperty("shard", ServerProperties.getShardName());
		location.addProperty("world", mMarkerEntity.getWorld().getKey().asString());
		location.addProperty("x", mMarkerEntity.getLocation().getX());
		location.addProperty("y", mMarkerEntity.getLocation().getY());
		location.addProperty("z", mMarkerEntity.getLocation().getZ());
		object.add("location", location);

		return object;
	}

	public enum ItemDiscoveryTier {
		COMMON(TextColor.color(192, 237, 230), (ticks, location, players) -> {
			if (ticks % 20 == 0) {
				new PartialParticle(Particle.END_ROD, location)
					.directionalMode(true)
					.delta(0, 1, 0)
					.extra(0.02)
					.count(1)
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
			if (ticks % 2 == 0) {
				new PartialParticle(Particle.REDSTONE, location)
					.delta(0.1f, 0.18f, 0.1f)
					.count(3)
					.data(new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.75f))
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
		}, (player, location) -> {
			player.playSound(location, Sound.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 1.25f, 0.5f);
			player.playSound(location, Sound.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 1.25f, 0.5f);
		}),
		RARE(TextColor.color(177, 129, 219), (ticks, location, players) -> {
			if (ticks % 20 == 0) {
				new PartialParticle(Particle.END_ROD, location)
					.directionalMode(true)
					.delta(0, 1, 0)
					.extra(0.02)
					.count(1)
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
			if (ticks % 2 == 0) {
				new PartialParticle(Particle.REDSTONE, location)
					.delta(0.1f, 0.12f, 0.1f)
					.count(8)
					.data(new Particle.DustOptions(Color.fromRGB(183, 141, 217), 1f))
					.spawnForPlayers(ParticleCategory.FULL, players);
				new PartialParticle(Particle.REDSTONE, location.clone().add(0, 0.5, 0))
					.delta(0.03f, 0.175f, 0.03f)
					.count(4)
					.data(new Particle.DustOptions(Color.fromRGB(192, 120, 191), 0.75f))
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
			double theta = Math.PI * 2 * (((double)ticks % 40) / 40);
			Vector delta = new Vector(FastUtils.cos(theta) * 0.4, 0, FastUtils.sin(theta) * 0.4);
			new PartialParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(delta))
				.count(1)
				.spawnForPlayers(ParticleCategory.FULL, players);
			new PartialParticle(Particle.ENCHANTMENT_TABLE, location.clone().subtract(delta))
				.count(1)
				.spawnForPlayers(ParticleCategory.FULL, players);
		}, (player, location) -> {
			player.playSound(location, Sound.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 1.25f, 0.5f);
			player.playSound(location, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.75f, 2f);
			player.playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.75f, 0.5f);
		}),
		LEGENDARY(TextColor.color(224, 170, 76), (ticks, location, players) -> {
			if (ticks % 10 == 0) {
				new PartialParticle(Particle.END_ROD, location)
					.directionalMode(true)
					.delta(0, 1, 0)
					.extra(0.02)
					.count(1)
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
			if (ticks % 2 == 0) {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, location)
					.delta(0.1f, 0.12f, 0.1f)
					.count(6)
					.data(new Particle.DustTransition(Color.fromRGB(240, 156, 41), Color.fromRGB(242, 181, 92), 1.1f))
					.spawnForPlayers(ParticleCategory.FULL, players);
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, location.clone().add(0, 0.55, 0))
					.delta(0.04f, 0.28f, 0.04f)
					.count(4)
					.data(new Particle.DustTransition(Color.fromRGB(240, 156, 41), Color.fromRGB(242, 181, 92), 0.8f))
					.spawnForPlayers(ParticleCategory.FULL, players);
			}
			double theta1 = Math.PI * 2 * (((double)ticks % 50) / 50);
			double theta2 = Math.PI * 2 * (((double)ticks % 30) / 30);
			Vector delta = new Vector(FastUtils.cos(theta2) * 0.45, FastUtils.cos(theta1) * 0.45, FastUtils.sin(theta2) * 0.45);
			new PartialParticle(Particle.ELECTRIC_SPARK, location.clone().add(delta))
				.directionalMode(true)
				.count(1)
				.delta(0, 1, 0)
				.extra(0.2)
				.spawnForPlayers(ParticleCategory.FULL, players);
			new PartialParticle(Particle.ELECTRIC_SPARK, location.clone().subtract(delta))
				.directionalMode(true)
				.count(1)
				.delta(0, 1, 0)
				.extra(0.2)
				.spawnForPlayers(ParticleCategory.FULL, players);
		}, (player, location) -> {
			player.playSound(location, Sound.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 1.25f, 0.5f);
			player.playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.75f, 1f);
			player.playSound(location, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.75f, 1f);
		});

		public final TextColor mDisplayColor;
		private final TriConsumer<Integer, Location, List<Player>> mPeriodicEffect;
		private final BiConsumer<Player, Location> mCollectSound;

		ItemDiscoveryTier(TextColor displayColor, TriConsumer<Integer, Location, List<Player>> periodicEffect, BiConsumer<Player, Location> collectSound) {
			mDisplayColor = displayColor;
			mPeriodicEffect = periodicEffect;
			mCollectSound = collectSound;
		}
	}
}
