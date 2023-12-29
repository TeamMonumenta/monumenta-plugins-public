package com.playmonumenta.plugins.depths;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.io.File;
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
import org.bukkit.Color;
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
		Material.BARREL,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME,
		Material.END_GATEWAY,
		Material.STONE_BUTTON,
		Material.OBSIDIAN,
		Material.PISTON,
		Material.STICKY_PISTON,
		Material.MOVING_PISTON,
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
		Material.BLACK_SHULKER_BOX
	);

	//List of locations where ice is currently active
	public static Map<Location, BlockData> iceActive = new HashMap<>();
	//List of locations where ice is spawned by a barrier
	public static Map<Location, Boolean> iceBarrier = new HashMap<>();

	public static Component getLoreForItem(DepthsTree tree, int rarity, int oldRarity) {
		Component oldRarityComponent = Component.empty();
		if (oldRarity != 0 && oldRarity != rarity) {
			oldRarityComponent = Component.empty().append(getRarityComponent(oldRarity)).append(Component.text(" → ", NamedTextColor.DARK_GRAY));
		}

		return tree.getNameComponent()
			       .append(Component.text(" : ", NamedTextColor.DARK_GRAY))
			       .append(oldRarityComponent)
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

	public static void spawnIceTerrain(Location l, int ticks, Player p) {
		spawnIceTerrain(l, ticks, p, Boolean.FALSE, true);
	}

	public static void spawnIceTerrain(Location l, int ticks, Player p, Boolean isBarrier) {
		spawnIceTerrain(l, ticks, p, isBarrier, true);
	}

	public static void spawnIceTerrain(Location l, int ticks, Player p, Boolean isBarrier, boolean withParticles) {

		// when placing ice on top of existing ice, deal % damage to mobs
		if (iceActive.get(l) != null && !isBarrier) {
			Location aboveLoc = l.clone().add(0.5, 1, 0.5);

			double hpPercentDamage = 0.15;
			for (LivingEntity mob : EntityUtils.getNearbyMobs(aboveLoc, 1.5, 5.0, 1.5)) {
				if (!(EntityUtils.isBoss(mob) || ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG))) {
					DamageUtils.damage(p, mob, DamageType.TRUE, EntityUtils.getMaxHealth(mob) * hpPercentDamage);
				}
			}

			if (withParticles) {
				new PartialParticle(Particle.REDSTONE, aboveLoc.clone().add(0, 0.5, 0), 24, 0.2, 0.7, 0.2, new Particle.DustOptions(Color.fromRGB(200, 225, 255), 1.0f)).spawnAsPlayerActive(p);
			}

			return;
		}

		//Check if the block is valid, or if the location is already active in the system
		if (IGNORED_MATS.contains(l.getWorld().getBlockAt(l).getType()) || iceActive.get(l) != null) {
			return;
		}

		Material iceMaterial = ICE_MATERIAL;

		//Check for permafrost to increase ice duration
		Permafrost permafrost = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(p, Permafrost.class);
		if (permafrost != null) {
			ticks += permafrost.getBonusIceDuration();
			if (permafrost.getAbilityScore() == 6) {
				iceMaterial = Permafrost.PERMAFROST_ICE_MATERIAL;
			}
		}

		BlockData bd = l.getWorld().getBlockAt(l).getBlockData();
		l.getBlock().setType(iceMaterial);
		iceActive.put(l, bd);
		iceBarrier.put(l, isBarrier);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (iceActive.containsKey(l)) {
					if (l.isChunkLoaded()) {
						Block b = l.getBlock();
						if (isIce(b.getType())) {
							b.setBlockData(bd);
						}
					}
					iceActive.remove(l);
					iceBarrier.remove(l);
				}
			}
		}.runTaskLater(Plugin.getInstance(), ticks);
	}

	public static boolean isIce(Material material) {
		return material == ICE_MATERIAL || material == Permafrost.PERMAFROST_ICE_MATERIAL;
	}

	/**
	 * Used for ability run checks for casting, rough estimate at whether they are holding a viable weapon
	 * @param item Item to check for
	 * @return if the item is an axe, sword, scythe, wand, or trident
	 */
	public static boolean isWeaponItem(@Nullable ItemStack item) {
		return item != null && (ItemUtils.isAxe(item) || ItemUtils.isSword(item) || ItemUtils.isWand(item) || ItemUtils.isHoe(item));
	}

	private static final List<String> WEAPON_ASPECT_NAMES = DepthsManager.getWeaponAspects().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList();

	public static boolean isWeaponAspectAbility(String s) {
		return WEAPON_ASPECT_NAMES.contains(s);
	}

	public static boolean isPrismaticAbility(String s) {
		return DepthsManager.getPrismaticAbilities().stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList().contains(s);
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

	public static String rewardString(@Nullable DepthsRoomType roomType) {
		if (roomType != null) {
			return roomType.getRewardString();
		}
		return "";
	}

	private static boolean canConvertToIce(Block b) {
		return canConvertToIce(b.getType());
	}

	/**
	 * Checks if a block of the given type can be converted to ice.
	 * All blocks with collision (plus water) can be converted, except if they are special blocks like spawners or chests, or if they only have very small collision like carpets.
	 */
	private static boolean canConvertToIce(Material mat) {
		if (mat == Material.WATER) {
			return true;
		}
		if (!mat.isBlock() || !mat.isCollidable() || IGNORED_MATS.contains(mat)) {
			return false;
		}
		return switch (mat) {
			case // carpets
				RED_CARPET, BLACK_CARPET, BLUE_CARPET, BROWN_CARPET, CYAN_CARPET, GRAY_CARPET, GREEN_CARPET, LIGHT_BLUE_CARPET,
					LIGHT_GRAY_CARPET, LIME_CARPET, MAGENTA_CARPET, ORANGE_CARPET, PINK_CARPET, PURPLE_CARPET, WHITE_CARPET, YELLOW_CARPET,
					MOSS_CARPET,
					// trapdoors
					BIRCH_TRAPDOOR, ACACIA_TRAPDOOR, CRIMSON_TRAPDOOR, DARK_OAK_TRAPDOOR, IRON_TRAPDOOR, JUNGLE_TRAPDOOR, MANGROVE_TRAPDOOR,
					OAK_TRAPDOOR, SPRUCE_TRAPDOOR, WARPED_TRAPDOOR, BAMBOO_TRAPDOOR, CHERRY_TRAPDOOR,
					// candles
					CANDLE, CYAN_CANDLE, BLACK_CANDLE, BLUE_CANDLE, BROWN_CANDLE, GRAY_CANDLE, GREEN_CANDLE, LIME_CANDLE, MAGENTA_CANDLE,
					ORANGE_CANDLE, PINK_CANDLE, PURPLE_CANDLE, RED_CANDLE, WHITE_CANDLE, YELLOW_CANDLE, LIGHT_BLUE_CANDLE, LIGHT_GRAY_CANDLE,
					// rails
					RAIL, ACTIVATOR_RAIL, DETECTOR_RAIL, POWERED_RAIL -> false;
			default -> true;
		};
	}

	public static void iceExposedBlock(Block b, int iceTicks, Player p) {
		iceExposedBlock(b, iceTicks, p, true);
	}

	public static void iceExposedBlock(Block b, int iceTicks, Player p, boolean withParticles) {
		// Try the block above and below the desired block for a block that is near the surface
		Block converted;
		if (canConvertToIce(b.getRelative(BlockFace.UP)) && !canConvertToIce(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP))) {
			converted = b.getRelative(BlockFace.UP);
		} else if (canConvertToIce(b)) {
			converted = b;
		} else if (canConvertToIce(b.getRelative(BlockFace.DOWN))) {
			converted = b.getRelative(BlockFace.DOWN);
		} else {
			return;
		}
		DepthsUtils.spawnIceTerrain(converted.getLocation(), iceTicks, p, Boolean.FALSE, withParticles);
	}

	public static void explodeEvent(EntityExplodeEvent event) {
		// Check location of blocks to see if they were ice barrier placed
		if (event.getEntity().isDead() || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof AbstractHorse || ScoreboardUtils.checkTag(event.getEntity(), AbilityUtils.IGNORE_TAG)) {
			return;
		}
		List<Block> blocks = event.blockList();
		for (Block b : blocks) {
			if (Boolean.TRUE.equals(iceBarrier.get(b.getLocation())) && !ScoreboardUtils.checkTag(event.getEntity(), MetalmancyBoss.identityTag)) {
				// Apply ice barrier stun passive effect to the mob
				EntityUtils.applyStun(Plugin.getInstance(), 2 * 20, (LivingEntity) event.getEntity());
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

		if (ServerProperties.getShardName().contains("zenith") || ServerProperties.getShardName().startsWith("dev")) {
			return DepthsContent.CELESTIAL_ZENITH;
		} else if (ServerProperties.getShardName().contains("depths")) {
			return DepthsContent.DARKEST_DEPTHS;
		}
		return DepthsContent.CELESTIAL_ZENITH;
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
	public static void storeRunStatsToFile(DepthsPlayer dp, String path, boolean victory) {
		Player player = dp.getPlayer();
		if (player == null) {
			return;
		}

		DepthsParty depthsParty = DepthsManager.getInstance().getPartyFromId(dp);
		if (depthsParty == null || depthsParty.mInitialPlayers == null) {
			return;
		}

		String playerId = dp.mPlayerId.toString();
		long timestamp = java.time.Instant.now().getEpochSecond();
		String fileName = path + File.separator + playerId + " - " + timestamp + ".json";
		// Player data inside the json
		JsonObject json = new JsonObject();
		JsonObject abilityObjectInJson = new JsonObject();
		for (String ability : dp.mAbilities.keySet()) {
			abilityObjectInJson.addProperty(ability, dp.mAbilities.get(ability));
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
		json.add("abilities", abilityObjectInJson);
		json.add("initial_players", initialPlayersJsonArray);

		if (depthsParty.getContent() == DepthsContent.DARKEST_DEPTHS) {
			json.addProperty("endless", depthsParty.mEndlessMode);
		}
		if (depthsParty.getContent() == DepthsContent.CELESTIAL_ZENITH) {
			json.addProperty("ascension", depthsParty.getAscension());
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				FileUtils.writeFile(fileName, json.toString());
			} catch (Exception e) {
				MMLog.severe("Caught exception saving file '" + fileName + "': " + e);
				e.printStackTrace();
			}
		});
	}
}
