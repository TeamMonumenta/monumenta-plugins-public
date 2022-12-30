package com.playmonumenta.plugins.depths;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.abilities.aspects.AxeAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.ScytheAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.SwordAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.WandAspect;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
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

	public static final int LEVELSIX = 0x703663;

	//Text that gets displayed by players getting messages
	public static final String DEPTHS_MESSAGE_PREFIX = ChatColor.DARK_PURPLE + "[Depths Party] " + ChatColor.LIGHT_PURPLE;

	//Material defined as ice
	public static final Material ICE_MATERIAL = Material.ICE;

	//Forbidden blocks for replacing with ice
	private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.END_PORTAL,
		Material.STONE_BUTTON,
		Material.OBSIDIAN,
		Material.LIGHT
		);

	//List of locations where ice is currently active
	public static Map<Location, BlockData> iceActive = new HashMap<>();
	//List of locations where ice is spawned by a barrier
	public static Map<Location, Boolean> iceBarrier = new HashMap<>();

	public static Component getLoreForItem(DepthsTree tree, int rarity) {
		TextComponent loreLine = Component.text("");
		loreLine = loreLine.append(tree.getNameComponent());

		loreLine = loreLine.append(Component.text(" : ", NamedTextColor.DARK_GRAY));
		TextComponent rarityText = Component.text(getRarityColor(rarity) + getRarityText(rarity));
		if (rarity == 6) {
			rarityText = Component.text(ChatColor.MAGIC + getRarityText(rarity), TextColor.color(LEVELSIX));
		}
		loreLine = loreLine.append(rarityText);

		return loreLine;
	}

	public static String getRarityText(int rarity) {
		String[] rarities = {
				"Common",
				"Uncommon",
				"Rare",
				"Epic",
				"Legendary",
				"XXXXXX"
		};
		return rarities[rarity - 1];
	}

	public static ChatColor getRarityColor(int rarity) {
		ChatColor[] colors = {
				ChatColor.GRAY,
				ChatColor.GREEN,
				ChatColor.BLUE,
				ChatColor.LIGHT_PURPLE,
				ChatColor.GOLD,
				ChatColor.DARK_PURPLE
		};
		return colors[rarity - 1];
	}

	public static void spawnIceTerrain(Location l, int ticks, Player p) {
		spawnIceTerrain(l, ticks, p, Boolean.FALSE);
	}

	public static void spawnIceTerrain(Location l, int ticks, Player p, Boolean isBarrier) {
		//Check if the block is valid, or if the location is already active in the system
		if (mIgnoredMats.contains(l.getWorld().getBlockAt(l).getType()) || iceActive.get(l) != null) {
			return;
		}

		//Check for permafrost to increase ice duration
		int playerPermLevel = DepthsManager.getInstance().getPlayerLevelInAbility(Permafrost.ABILITY_NAME, p);
		if (playerPermLevel > 0) {
			ticks += (Permafrost.ICE_BONUS_DURATION_SECONDS[playerPermLevel - 1] * 20);
		}

		Material iceMaterial = playerPermLevel >= 6 ? Permafrost.PERMAFROST_ICE_MATERIAL : ICE_MATERIAL;

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
		return item != null && (ItemUtils.isAxe(item) || ItemUtils.isSword(item) ||
			ItemUtils.isWand(item) || ItemUtils.isHoe(item) || (item.getType() == Material.TRIDENT && ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.RIPTIDE) == 0));
	}

	public static double roundDouble(double num) {
		return Math.round(num * 100.0) / 100.0;
	}

	public static double roundPercent(double num) {
		return Math.round(num * 100.0 * 100.0) / 100.0;
	}

	public static boolean isWeaponAspectAbility(String s) {
		return s.equals(AxeAspect.ABILITY_NAME) || s.equals(WandAspect.ABILITY_NAME) || s.equals(ScytheAspect.ABILITY_NAME) || s.equals(SwordAspect.ABILITY_NAME) || s.equals(BowAspect.ABILITY_NAME);
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
		return event.getType() == DamageType.MELEE && player.getCooledAttackStrength(0) == 1 && isWeaponItem(player.getInventory().getItemInMainHand());
	}

	//Firework effect
	public static void animate(Location loc) {
		Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
		fwBuilder.withColor(Color.RED, Color.GREEN, Color.BLUE);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(Plugin.getInstance(), 5);
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

	public static void iceExposedBlock(Block b, int iceTicks, Player p) {
		//Check above block first and see if it is exposed to air
		if (b.getRelative(BlockFace.UP).isSolid() && !(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isSolid() || b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.WATER)) {
			DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), iceTicks, p);
		} else if (b.isSolid() || b.getType() == Material.WATER) {
			DepthsUtils.spawnIceTerrain(b.getLocation(), iceTicks, p);
		} else if (b.getRelative(BlockFace.DOWN).isSolid() || b.getRelative(BlockFace.DOWN).getType() == Material.WATER) {
			DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.DOWN).getLocation(), iceTicks, p);
		}
	}

	public static void explodeEvent(EntityExplodeEvent event) {
		// Check location of blocks to see if they were ice barrier placed
		if (event.getEntity().isDead() || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof AbstractHorse) {
			return;
		}
		List<Block> blocks = event.blockList();
		for (Block b : blocks) {
			if (Boolean.TRUE.equals(iceBarrier.get(b.getLocation()))) {
				// Apply ice barrier stun passive effect to the mob
				EntityUtils.applyStun(Plugin.getInstance(), 2 * 20, (LivingEntity) event.getEntity());
				return;
			}
		}

	}

	//Store the player ability data to a file, including the name of the player and the room they died in.
	public static void storetoFile(DepthsPlayer dp, String path) {
		try {
			// Name of the file and it's path
			String fileName = path + File.separator + dp.mPlayerId + " - " + java.time.Instant.now().getEpochSecond() + ".json";
			// Player data inside the json
			JsonObject json = new JsonObject();
			JsonObject abilityObjectInJson = new JsonObject();
			for (String ability : dp.mAbilities.keySet()) {
				abilityObjectInJson.addProperty(ability, dp.mAbilities.get(ability));
			}
			JsonArray initialPlayersJsonArray = new JsonArray();
			DepthsParty depthsParty = DepthsManager.getInstance().getPartyFromId(dp);
			if (depthsParty == null || depthsParty.mInitialPlayers == null) {
				return;
			}
			for (String player : depthsParty.mInitialPlayers) {
				initialPlayersJsonArray.add(player);
			}
			json.addProperty("PlayerName", Bukkit.getPlayer(dp.mPlayerId).getName());
			json.addProperty("Room Number", depthsParty.getRoomNumber());
			json.addProperty("Treasure Score", depthsParty.mTreasureScore);
			json.add("Abilities", abilityObjectInJson);
			json.add("Initial Players", initialPlayersJsonArray);
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), new Runnable() { //run async
				@Override
				public void run() {
					try {
						FileUtils.writeFile(fileName, json.toString());
					} catch (Exception e) {
						Plugin.getInstance().getLogger().severe("Caught exception saving file '" + fileName + "': " + e);
						e.printStackTrace();
					}
				}
			});
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}
}
