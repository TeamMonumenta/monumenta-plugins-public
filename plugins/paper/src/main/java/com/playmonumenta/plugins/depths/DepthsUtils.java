package com.playmonumenta.plugins.depths;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.depths.abilities.frostborn.Avalanche;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class DepthsUtils {

	//Tree colors
	public static final int DAWNBRINGER = 0xf0b326;
	public static final int EARTHBOUND = 0x6b3d2d;
	public static final int FLAMECALLER = 0xf04e21;
	public static final int FROSTBORN = 0xa3cbe1;
	public static final int STEELSAGE = 0x929292;
	public static final int SHADOWDANCER = 0x7948af;
	public static final int WINDWALKER = 0xc0dea9;
	public static final int PRISMATIC = 0xf4f9ff; // TODO This is only used in DiscoBall - not sure what we should do there

	//Material defined as ice
	public static final Material ICE_MATERIAL = Material.ICE;
	public static final Material SNOW_MATERIAL = Material.SNOW;

	private static @Nullable BukkitRunnable FROZEN_TERRAIN_REMOVAL = null;

	//Forbidden blocks for replacing with ice
	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.STRUCTURE_BLOCK,
		Material.JIGSAW,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.LECTERN,
		Material.PLAYER_HEAD,
		Material.BARREL,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME,
		Material.END_GATEWAY,
		Material.STONE_BUTTON,
		Material.OBSIDIAN,
		Material.PISTON,
		Material.STICKY_PISTON,
		Material.MOVING_PISTON,
		Material.BREWING_STAND,
		Material.ANVIL,
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.BLACK_SHULKER_BOX,
		Material.COBWEB
	);

	//List of locations where ice or snow is currently active
	public static Map<Location, FrozenTerrainData> frostActive = new HashMap<>();

	public static class FrozenTerrainData {

		public final @Nullable BlockData mBlockData;
		public final boolean mIceBarrier;
		public final boolean mIsVirtual;
		public int mRemainingTicks;

		/**
		 * @param blockData the blockData of the replaced block
		 * @param duration the duration of the frost
		 * @param iceBarrier whether the frost was placed by ice barrier
		 * @param isVirtual whether there is real ice/snow placed or if it only acts the part for abilities
		 */
		public FrozenTerrainData(@Nullable BlockData blockData, int duration, boolean iceBarrier, boolean isVirtual) {
			mBlockData = blockData;
			mIceBarrier = iceBarrier;
			mIsVirtual = isVirtual;
			mRemainingTicks = duration;
		}

	}

	// depths content type for the shard
	// depths skills api changes this temporarily to get descriptions for both depths and zenith
	private static @Nullable DepthsContent depthsContentOverride = null;

	public static Component getLoreForItem(DepthsTree tree, int rarity, int oldRarity, int preIncreaseRarity) {
		Component extraComponent = Component.empty();
		if (oldRarity != 0 && oldRarity != rarity) {
			extraComponent = Component.empty().append(getRarityComponent(oldRarity)).append(Component.text(" → ", NamedTextColor.DARK_GRAY));
		} else if (preIncreaseRarity != 0 && preIncreaseRarity != rarity) {
			extraComponent = Component.empty()
				.append(getRarityComponent(preIncreaseRarity).decorate(TextDecoration.STRIKETHROUGH))
				.append(Component.text(" "));
		}

		return tree.getNameComponent()
			.append(Component.text(" : ", NamedTextColor.DARK_GRAY))
			.append(extraComponent)
			.append(getRarityComponent(rarity))
			.decoration(TextDecoration.ITALIC, false);
	}

	public static DepthsRarity getRarity(int rarity) {
		return DepthsRarity.values()[rarity - 1];
	}

	public static Component getRarityComponent(int rarity) {
		return getRarity(rarity).getDisplay();
	}

	public static TextColor getRarityColor(int rarity) {
		return getRarity(rarity).getColor();
	}

	public static TextColor getRarityTextColor(int rarity) {
		TextColor[] colors = {
			TextColor.fromHexString("#9f929c"),
			TextColor.fromHexString("#70bc6d"),
			TextColor.fromHexString("#705eca"),
			TextColor.fromHexString("#cd5eca"),
			TextColor.fromHexString("#e49b20"),
			NamedTextColor.DARK_PURPLE
		};
		return colors[rarity - 1];
	}

	public static NamedTextColor getRarityNamedTextColor(int rarity) {
		NamedTextColor[] colors = {
			NamedTextColor.DARK_GRAY,
			NamedTextColor.GREEN,
			NamedTextColor.BLUE,
			NamedTextColor.LIGHT_PURPLE,
			NamedTextColor.GOLD,
			NamedTextColor.DARK_PURPLE
		};
		return colors[rarity - 1];
	}

	public static boolean freezeTerrain(Block block, int ticks, boolean isBarrier, boolean useSnow) {
		Location l = block.getLocation();

		//Check if the location is already active in the system
		if (frostActive.get(l) != null) {

			frostActive.get(l).mRemainingTicks = ticks;
			if (!useSnow && l.getBlock().getType() == SNOW_MATERIAL) {
				l.getBlock().setType(ICE_MATERIAL);
			}
			if (!useSnow && frostActive.get(l).mIsVirtual && canFreeze(l.getBlock(), false)) {
				BlockData bd = l.getWorld().getBlockAt(l).getBlockData();
				frostActive.put(l, new FrozenTerrainData(bd, ticks, isBarrier, false));
				l.getBlock().setType(ICE_MATERIAL);
			}
			return false; // return whether we placed ice or not
		}

		//Whether to place down actual snow/ice or just consider it frozen
		if (canFreeze(l.getBlock(), useSnow)) {
			BlockData bd = l.getWorld().getBlockAt(l).getBlockData();

			//Move light blocks before replacing it to not affect the general brightness of the affected area too much
			if (l.getBlock().getType() == Material.LIGHT) {
				BlockUtils.tryMoveLight(l.getWorld().getBlockState(l));
			}

			//Edge case to turn path and farmland into grass to make it work nicely with snow
			Block relativeDown = l.getBlock().getRelative(BlockFace.DOWN);
			if (useSnow && (relativeDown.getType() == Material.DIRT_PATH || relativeDown.getType() == Material.FARMLAND)) {
				frostActive.put(relativeDown.getLocation(), new FrozenTerrainData(relativeDown.getBlockData().clone(), ticks, false, false));
				relativeDown.setType(Material.GRASS_BLOCK);
			}

			l.getBlock().setType(useSnow ? SNOW_MATERIAL : ICE_MATERIAL);
			frostActive.put(l, new FrozenTerrainData(bd, ticks, isBarrier, false));
		} else {
			frostActive.put(l, new FrozenTerrainData(null, ticks, isBarrier, true));
		}

		scheduleFrozenTerrainRemoval();
		return true;
	}

	private static void scheduleFrozenTerrainRemoval() {
		if (FROZEN_TERRAIN_REMOVAL != null && !FROZEN_TERRAIN_REMOVAL.isCancelled()) {
			return;
		}

		FROZEN_TERRAIN_REMOVAL = new BukkitRunnable() {
			@Override
			public void run() {

				List<Location> sortedKeys = frostActive
					.keySet()
					.stream()
					.sorted(Comparator.comparingDouble(Location::getY).reversed())
					.toList();

				for (Location key : sortedKeys) {
					if (!frostActive.containsKey(key)) {
						continue;
					}

					frostActive.get(key).mRemainingTicks -= 1;
					if (frostActive.get(key).mRemainingTicks <= 0) {

						if (!frostActive.get(key).mIsVirtual && key.isChunkLoaded()) {
							Block b = key.getBlock();
							b.setBlockData(frostActive.get(key).mBlockData);
							new PartialParticle(Particle.REDSTONE, key.clone().add(0, 0.1, 0), 1, 0.2, 0.3, 0.2, Avalanche.ICE_PARTICLE_COLOR).spawnFull();
						}
						frostActive.remove(key);
					}
				}

				if (frostActive.isEmpty() || frostActive.keySet().stream().noneMatch(Location::isChunkLoaded)) {
					this.cancel();
				}
			}
		};

		FROZEN_TERRAIN_REMOVAL.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static void unfreezeGround(Location l) {
		if (!frostActive.containsKey(l)) {
			return;
		}
		if (!frostActive.get(l).mIsVirtual) {
			l.getBlock().setBlockData(frostActive.get(l).mBlockData);
		}
		frostActive.remove(l);
	}

	public static boolean isFrozen(Material material) {
		return material == ICE_MATERIAL || material == SNOW_MATERIAL;
	}

	/**
	 * Used for ability run checks for casting, rough estimate at whether they are holding a viable weapon
	 *
	 * @param item Item to check for
	 * @return if the item is an axe, sword, scythe, wand, or trident
	 */
	public static boolean isWeaponItem(@Nullable ItemStack item) {
		return item != null
			&& (ItemUtils.isAxe(item)
			|| ItemUtils.isSword(item)
			|| ItemUtils.isWand(item)
			|| ItemUtils.isHoe(item)
			|| (item.getType() == Material.TRIDENT && ItemStatUtils.getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) != 0));
	}

	public static boolean isWeaponAspectAbility(String s) {
		return DepthsManager.getWeaponAspects().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList().contains(s);
	}

	public static boolean isPrismaticAbility(String s) {
		return DepthsManager.getPrismaticAbilities().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList().contains(s);
	}

	public static boolean isCurseAbility(String s) {
		return DepthsManager.getCurseAbilities().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList().contains(s);
	}

	public static boolean isGiftAbility(String s) {
		return DepthsManager.getGiftAbilities().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList().contains(s);
	}

	/**
	 * Returns the party of nearby players, if applicable
	 *
	 * @return nearby party, otherwise null if none exists
	 */
	public static @Nullable DepthsParty getPartyFromNearbyPlayers(Location l) {

		List<Player> players = PlayerUtils.playersInRange(l, 30.0, true);

		if (players.isEmpty()) {
			return null;
		}

		DepthsManager dm = DepthsManager.getInstance();
		for (Player p : players) {
			DepthsParty party = dm.getDepthsParty(p);
			//Return the first party found
			if (party != null) {
				return party;
			}
		}

		return null;
	}

	public static boolean isValidComboAttack(DamageEvent event, Player player) {
		return event.getType() == DamageType.MELEE && player.getCooledAttackStrength(0) == 1;
	}

	public static @Nullable DepthsRewardType rewardFromRoom(@Nullable DepthsRoomType roomType) {
		if (roomType != null) {
			return roomType.getRewardType();
		}
		return null;
	}

	public static Component rewardComponent(@Nullable DepthsRoomType roomType) {
		if (roomType != null) {
			return roomType.getRewardComponent();
		}
		return Component.text("");
	}

	private static boolean canFreeze(Block b, boolean snow) {
		return snow ? canConvertToSnow(b) : !IGNORED_MATS.contains(b.getType());
	}

	//Blocks considered solid for frostborn snow
	private static boolean isSolidGround(Material material) {
		return material.isSolid() && material != Material.ICE && material != Material.SPAWNER && material != Material.SCULK_VEIN;
	}

	private static boolean canConvertToSnow(Block b) {
		if (!isSolidGround(b.getRelative(BlockFace.DOWN).getType())) {
			return false;
		}

		//Check if the top face of a block is a full 1x1 square
		List<BoundingBox> bbs = b.getRelative(BlockFace.DOWN).getCollisionShape()
			.getBoundingBoxes()
			.stream()
			.filter(a -> a.getMaxY() > 0.85).toList();
		boolean minX = bbs.stream().map(BoundingBox::getMinX).toList().contains(0.0);
		boolean maxX = bbs.stream().map(BoundingBox::getMaxX).toList().contains(1.0);
		boolean minZ = bbs.stream().map(BoundingBox::getMinZ).toList().contains(0.0);
		boolean maxZ = bbs.stream().map(BoundingBox::getMaxZ).toList().contains(1.0);

		if (!minX || !maxX || !minZ || !maxZ) {
			return false;
		}
		if (BlockUtils.isLiquid(b)) {
			return false;
		}
		return canConvertToSnow(b.getType());
	}

	private static boolean canConvertToSnow(Material mat) {

		if (IGNORED_MATS.contains(mat)) {
			return false;
		}
		if (mat == Material.WATER || mat == Material.LAVA) {
			return false;
		}
		return switch (mat) {
			case
				// carpets
				RED_CARPET, BLACK_CARPET, BLUE_CARPET, BROWN_CARPET, CYAN_CARPET, GRAY_CARPET, GREEN_CARPET,
					LIGHT_BLUE_CARPET,
					LIGHT_GRAY_CARPET, LIME_CARPET, MAGENTA_CARPET, ORANGE_CARPET, PINK_CARPET, PURPLE_CARPET, WHITE_CARPET,
					YELLOW_CARPET, MOSS_CARPET,
					//Misc that's also considered collidable
					LIGHT, SCULK_VEIN, SNOW
				-> true;
			default -> !mat.isCollidable();
		};
	}

	public static boolean freezeExposedBlock(Block b, int iceTicks) {
		// Try the block above and below the desired block for a block that is near the surface
		for (Block block : new Block[] {b.getRelative(BlockFace.UP), b, b.getRelative(BlockFace.DOWN)}) {

			//Turn surface level water in to ice even when using snow placing abilities
			Block relativeUp = block.getRelative(BlockFace.UP);
			if (BlockUtils.isLiquid(block) && block.getType() != Material.LAVA
				&& !BlockUtils.isLiquid(relativeUp) && !relativeUp.isSolid()) {
				return freezeTerrain(block, iceTicks, false, false);
			}

			//The check that decides whether a block can be frozen. Does NOT decide whether actual snow/ice is placed
			if (isSolidGround(block.getRelative(BlockFace.DOWN).getType()) && canConvertToSnow(block.getType())) {
				return freezeTerrain(block, iceTicks, false, true);
			}
		}
		return false;
	}

	public static boolean isOnFrozenGround(Entity entity) {
		Block b = entity.getLocation().getBlock();
		return frostActive.containsKey(b.getRelative(BlockFace.DOWN).getLocation()) || frostActive.containsKey(b.getLocation());
	}

	public static void explodeEvent(EntityExplodeEvent event) {
		// Check location of blocks to see if they were ice barrier placed
		if (event.getEntity().isDead() || !(event.getEntity() instanceof LivingEntity le) || event.getEntity() instanceof AbstractHorse || ScoreboardUtils.checkTag(event.getEntity(), AbilityUtils.IGNORE_TAG)) {
			return;
		}
		List<Block> blocks = event.blockList();
		for (Block b : blocks) {
			FrozenTerrainData barrier = frostActive.get(b.getLocation());
			if (barrier != null && barrier.mIceBarrier) {
				// Apply ice barrier stun passive effect to the mob
				EntityUtils.applyStun(Plugin.getInstance(), 2 * 20, le);
				return;
			}
		}

	}

	public static void sendFormattedMessage(Player player, DepthsContent content, String message) {
		sendFormattedMessage(player, content, Component.text(message));
	}

	public static void sendFormattedMessage(Player player, DepthsContent content, Component message) {
		if (content == null) {
			MMLog.warning("Player " + player.getName() + " has null DepthsContent!");
			content = DepthsContent.DARKEST_DEPTHS;
		}
		player.sendMessage(Component.text(content.getPrefix(), NamedTextColor.DARK_PURPLE).append(Component.text(" ")).append(message.colorIfAbsent(NamedTextColor.LIGHT_PURPLE)));
	}

	public static DepthsContent getDepthsContent() {
		//TODO revisit after zenith release and split up dev shards for testing
		if (depthsContentOverride != null) {
			return depthsContentOverride;
		} else if (ServerProperties.getShardName().contains("zenith") || ServerProperties.getShardName().startsWith("dev")) {
			return DepthsContent.CELESTIAL_ZENITH;
		} else if (ServerProperties.getShardName().contains("depths")) {
			return DepthsContent.DARKEST_DEPTHS;
		} else {
			return DepthsContent.CELESTIAL_ZENITH;
		}
	}

	public static void setDepthsContentOverride(@Nullable DepthsContent content) {
		depthsContentOverride = content;
	}

	public static double getDamageMultiplier() {
		return getDepthsContent().getDamageMultiplier();
	}

	public static boolean isDepthsGrave(Entity entity) {
		return entity instanceof ArmorStand && entity.getScoreboardTags().contains(DepthsListener.GRAVE_TAG);
	}

	public static double getAdaptiveDamageMultiplier(ItemStatManager.PlayerItemStats playerItemStats, DamageType damageType) {
		AttributeType correctAttributeType = switch (damageType) {
			case MELEE_SKILL -> AttributeType.ATTACK_DAMAGE_MULTIPLY;
			case PROJECTILE_SKILL -> AttributeType.PROJECTILE_DAMAGE_MULTIPLY;
			case MAGIC -> AttributeType.MAGIC_DAMAGE_MULTIPLY;
			default -> null;
		};
		if (correctAttributeType == null) {
			return 0;
		}
		Map<AttributeType, Double> damageMultipliers = ItemStatManager.getDamageMultipliers(playerItemStats);
		Map<AttributeType, Double> adjustedMultipliers = new HashMap<>();
		damageMultipliers.forEach((type, mult) -> adjustedMultipliers.put(type, 1 + ((mult - 1) * (type == correctAttributeType ? 1 : 0.5))));
		return adjustedMultipliers.values().stream().mapToDouble(a -> a).max().orElse(0);
	}

	// Stores run stats of the given DepthsPlayer to a json file.
	public static void storeRunStatsToFile(Player player, DepthsPlayer dp, DepthsParty depthsParty, String path, boolean victory) {
		if (depthsParty.mInitialPlayers == null) {
			return;
		}

		String playerId = dp.mPlayerId.toString();
		long timestamp = Instant.now().getEpochSecond();
		String fileName = path + File.separator + playerId + " - " + timestamp + ".json";
		// Player data inside the json
		JsonObject json = new JsonObject();
		JsonObject abilityObjectInJson = new JsonObject();
		for (String ability : dp.mAbilities.keySet()) {
			abilityObjectInJson.addProperty(ability, dp.mAbilities.get(ability));
		}
		JsonArray removedAbilitiesJsonArray = new JsonArray();
		for (String ability : dp.mRemovedAbilities) {
			removedAbilitiesJsonArray.add(ability);
		}
		JsonArray initialPlayersJsonArray = new JsonArray();
		for (String partyPlayer : depthsParty.mInitialPlayers) {
			initialPlayersJsonArray.add(partyPlayer);
		}

		json.addProperty("player_name", player.getName());
		json.addProperty("player_uuid", playerId);
		json.addProperty("timestamp", timestamp);
		json.addProperty("room_number", depthsParty.getRoomNumber());
		json.addProperty("treasure_score", depthsParty.mTreasureScore);
		json.addProperty("content_type", dp.getContent().name());
		json.addProperty("victory", victory);
		json.addProperty("start_timestamp", depthsParty.mStartTimestamp);
		json.addProperty("f1_timestamp", depthsParty.mFloor1Timestamp);
		json.addProperty("f2_timestamp", depthsParty.mFloor2Timestamp);
		json.addProperty("f3_timestamp", depthsParty.mFloor3Timestamp);
		json.add("abilities", abilityObjectInJson);
		json.add("removed_abilities", removedAbilitiesJsonArray);
		json.add("initial_players", initialPlayersJsonArray);

		if (depthsParty.getContent() == DepthsContent.DARKEST_DEPTHS) {
			json.addProperty("endless", depthsParty.mEndlessMode);
		}
		if (depthsParty.getContent() == DepthsContent.CELESTIAL_ZENITH) {
			json.addProperty("ascension", depthsParty.getAscension());
		}
		if (depthsParty.mCurrentRoom != null) {
			json.addProperty("current_room", depthsParty.mCurrentRoom.mLoadPath);
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				FileUtils.writeFile(Path.of(fileName), json.toString());
			} catch (Exception e) {
				MMLog.severe("Caught exception saving file '" + fileName + "': " + e);
				e.printStackTrace();
			}
		});
	}
}
