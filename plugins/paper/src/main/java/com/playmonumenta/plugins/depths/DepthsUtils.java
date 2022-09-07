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
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class DepthsUtils {

	//Tree colors
	public static final int FROSTBORN = 0xa3cbe1;
	public static final int METALLIC = 0x929292;
	public static final int SUNLIGHT = 0xf0b326;
	public static final int EARTHBOUND = 0x6b3d2d;
	public static final int FLAMECALLER = 0xf04e21;
	public static final int WINDWALKER = 0xc0dea9;
	public static final int SHADOWS = 0x7948af;

	public static final int LEVELSIX = 0x703663;

	public static final DepthsTree[] TREES = {
			DepthsTree.SUNLIGHT,
			DepthsTree.EARTHBOUND,
			DepthsTree.FLAMECALLER,
			DepthsTree.FROSTBORN,
			DepthsTree.SHADOWS,
			DepthsTree.METALLIC,
			DepthsTree.WINDWALKER
	};

	public static final String[] TREE_NAMES = {
			"Dawnbringer",
			"Earthbound",
			"Flamecaller",
			"Frostborn",
			"Shadowdancer",
			"Steelsage",
			"Windwalker"
	};

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
		if (tree == DepthsTree.EARTHBOUND) {
			loreLine = loreLine.append(Component.text("Earthbound", TextColor.color(EARTHBOUND)));
		} else if (tree == DepthsTree.SUNLIGHT) {
			loreLine = loreLine.append(Component.text("Dawnbringer", TextColor.color(SUNLIGHT)));
		} else if (tree == DepthsTree.METALLIC) {
			loreLine = loreLine.append(Component.text("Steelsage", TextColor.color(METALLIC)));
		} else if (tree == DepthsTree.WINDWALKER) {
			loreLine = loreLine.append(Component.text("Windwalker", TextColor.color(WINDWALKER)));
		} else if (tree == DepthsTree.FROSTBORN) {
			loreLine = loreLine.append(Component.text("Frostborn", TextColor.color(FROSTBORN)));
		} else if (tree == DepthsTree.SHADOWS) {
			loreLine = loreLine.append(Component.text("Shadowdancer", TextColor.color(SHADOWS)));
		} else if (tree == DepthsTree.FLAMECALLER) {
			loreLine = loreLine.append(Component.text("Flamecaller", TextColor.color(FLAMECALLER)));
		}

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

	public static ItemStack getTreeItem(DepthsTree tree) {
		ItemMeta buildMeta;
		Material itemMat;
		Component name;
		String description;

		if (tree == DepthsTree.EARTHBOUND) {
			itemMat = Material.LEATHER_CHESTPLATE;
			name = Component.text("Earthbound", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Resolute tank with capabilities of taking aggro and granting resistance to self, armed with minor crowd control.";
		} else if (tree == DepthsTree.SUNLIGHT) {
			itemMat = Material.SUNFLOWER;
			name = Component.text("Dawnbringer", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Bestows passive and active buffs to allies including speed, damage, resistance, and healing.";
		} else if (tree == DepthsTree.METALLIC) {
			itemMat = Material.CROSSBOW;
			name = Component.text("Steelsage", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Master of ranged abilities with dual AOE and single target damage capabilities.";
		} else if (tree == DepthsTree.WINDWALKER) {
			itemMat = Material.FEATHER;
			name = Component.text("Windwalker", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "An arsenal of movement abilities and crowd control, allowing precise maneuvers and quick escapes.";
		} else if (tree == DepthsTree.FROSTBORN) {
			itemMat = Material.ICE;
			name = Component.text("Frostborn", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Manipulates the flow of combat by debuffing enemies with ice generating abilities and high damage potential.";
		} else if (tree == DepthsTree.SHADOWS) {
			itemMat = Material.IRON_SWORD;
			name = Component.text("Shadowdancer", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Skilled in single target melee damage, especially against bosses and elites.";
		} else if (tree == DepthsTree.FLAMECALLER) {
			itemMat = Material.FIRE_CHARGE;
			name = Component.text("Flamecaller", getTreeColor(tree))
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Caster of strong burst AOE abilities and potent damage over time.";
		} else {
			itemMat = Material.RED_STAINED_GLASS_PANE;
			name = Component.text("Invalid Tree", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true);
			description = "Please report this item's existence to a moderator.";
		}
		ItemStack buildItem = new ItemStack(itemMat, 1);
		buildMeta = buildItem.getItemMeta();
		buildMeta.displayName(name);
		buildMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		GUIUtils.splitLoreLine(buildMeta, description, 30, ChatColor.GRAY, true);
		buildItem.setItemMeta(buildMeta);
		ItemUtils.setPlainName(buildItem);

		return buildItem;
	}

	public static TextColor getTreeColor(DepthsTree tree) {

		if (tree == DepthsTree.EARTHBOUND) {
			return TextColor.color(EARTHBOUND);
		} else if (tree == DepthsTree.SUNLIGHT) {
			return TextColor.color(SUNLIGHT);
		} else if (tree == DepthsTree.METALLIC) {
			return TextColor.color(METALLIC);
		} else if (tree == DepthsTree.WINDWALKER) {
			return TextColor.color(WINDWALKER);
		} else if (tree == DepthsTree.FROSTBORN) {
			return TextColor.color(FROSTBORN);
		} else if (tree == DepthsTree.SHADOWS) {
			return TextColor.color(SHADOWS);
		} else if (tree == DepthsTree.FLAMECALLER) {
			return TextColor.color(FLAMECALLER);
		}
		return NamedTextColor.WHITE;
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

		if (players == null || players.size() == 0) {
			return null;
		}

		for (Player p : players) {
			DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(p.getUniqueId());
			if (dp != null && DepthsManager.getInstance().getPartyFromId(dp) != null) {
				//Just return the first party we find
				return DepthsManager.getInstance().getPartyFromId(dp);
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
		if (roomType == DepthsRoomType.ABILITY) {
			return DepthsRewardType.ABILITY;
		} else if (roomType == DepthsRoomType.ABILITY_ELITE) {
			return DepthsRewardType.ABILITY_ELITE;
		} else if (roomType == DepthsRoomType.UPGRADE) {
			return DepthsRewardType.UPGRADE;
		} else if (roomType == DepthsRoomType.UPGRADE_ELITE) {
			return DepthsRewardType.UPGRADE_ELITE;
		} else if (roomType == DepthsRoomType.TWISTED) {
			return DepthsRewardType.TWISTED;
		}
		return null;
	}

	public static String rewardString(@Nullable DepthsRoomType roomType) {
		if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.ABILITY_ELITE) {
			return "Ability";
		} else if (roomType == DepthsRoomType.UPGRADE || roomType == DepthsRoomType.UPGRADE_ELITE) {
			return "Upgrade";
		} else if (roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.TREASURE_ELITE) {
			return "Treasure";
		} else if (roomType == DepthsRoomType.TWISTED) {
			return ChatColor.MAGIC + "XXXXXX" + ChatColor.LIGHT_PURPLE;
		}
		return "";
	}

	public static String roomString(DepthsRoomType roomType) {
		String reward = rewardString(roomType);
		if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.UPGRADE || roomType == DepthsRoomType.TREASURE) {
			return reward;
		} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE || roomType == DepthsRoomType.TREASURE_ELITE) {
			return "Elite " + reward;
		} else if (roomType == DepthsRoomType.UTILITY) {
			return "Utility";
		} else if (roomType == DepthsRoomType.BOSS) {
			return "Boss";
		} else if (roomType == DepthsRoomType.TWISTED) {
			return ChatColor.MAGIC + "XXXXXX" + ChatColor.LIGHT_PURPLE;
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
		if (event.getEntity() == null || event.getEntity().isDead() || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof AbstractHorse) {
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

	public static boolean isPlant(Entity entity) {
		if (entity != null) {
			String name = entity.getName();
			if (name != null && name.contains("Dionaea")) {
				List<String> plantNames = new ArrayList<>();
				plantNames.add("Spore Dionaea");
				plantNames.add("Vampiric Dionaea");
				plantNames.add("Poisonous Dionaea");
				plantNames.add("Fertilizer Dionaea");
				for (String plant : plantNames) {
					if (name.contains(plant)) {
						return true;
					}
				}
			}
		}
		return false;
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
			if (DepthsManager.getInstance().getPartyFromId(dp).mInitialPlayers == null) {
		          return;
			}
			for (String player : DepthsManager.getInstance().getPartyFromId(dp).mInitialPlayers) {
				initialPlayersJsonArray.add(player);
			}
			json.addProperty("PlayerName", Bukkit.getPlayer(dp.mPlayerId).getName());
			json.addProperty("Room Number", DepthsManager.getInstance().getPartyFromId(dp).getRoomNumber());
			json.addProperty("Treasure Score", DepthsManager.getInstance().getPartyFromId(dp).mTreasureScore);
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
