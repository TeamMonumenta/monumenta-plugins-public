package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.abilities.aspects.AxeAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.ScytheAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.SwordAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.WandAspect;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DepthsUtils {

	//Tree colors
	public static final int FROSTBORN = 0xa3cbe1;
	public static final int METALLIC = 0x929292;
	public static final int SUNLIGHT = 0xf0b326;
	public static final int EARTHBOUND = 0x6b3d2d;
	public static final int FLAMECALLER = 0xf04e21;
	public static final int WINDWALKER = 0xc0dea9;
	public static final int SHADOWS = 0x7948af;

	//Text that gets displayed by players getting messages
	public static final String DEPTHS_MESSAGE_PREFIX = ChatColor.DARK_PURPLE + "[Depths Party] " + ChatColor.LIGHT_PURPLE;

	//Material defined as ice
	public static final Material ICE_MATERIAL = Material.PACKED_ICE;

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
		Material.OBSIDIAN
		);

	//List of locations where ice is currently active
	public static Map<Location, BlockData> iceActive = new HashMap<>();

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
		loreLine = loreLine.append(Component.text(getRarityColor(rarity) + getRarityText(rarity)));

		return loreLine;
	}

	public static String getRarityText(int rarity) {
		String[] rarities = {
				"Common",
				"Uncommon",
				"Rare",
				"Epic",
				"Legendary"
		};
		return rarities[rarity - 1];
	}

	public static ChatColor getRarityColor(int rarity) {
		ChatColor[] colors = {
				ChatColor.GRAY,
				ChatColor.GREEN,
				ChatColor.BLUE,
				ChatColor.LIGHT_PURPLE,
				ChatColor.GOLD
		};
		return colors[rarity - 1];
	}

	public static ItemStack getTreeItem(DepthsTree tree) {
		ItemStack buildItem = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
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
		buildItem = new ItemStack(itemMat, 1);
		buildMeta = buildItem.getItemMeta();
		buildMeta.displayName(name);
		buildMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		splitLoreLine(buildMeta, description, 30, ChatColor.GRAY);
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

	public static void spawnIceTerrain(Location l, int ticks) {
		//Check if the block is valid, or if the location is already active in the system
		if (mIgnoredMats.contains(l.getWorld().getBlockAt(l).getType()) || iceActive.get(l) != null) {
			return;
		}

		BlockData bd = l.getWorld().getBlockAt(l).getBlockData();
		l.getWorld().getBlockAt(l).setType(ICE_MATERIAL);
		iceActive.put(l, bd);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (iceActive.containsKey(l)) {
					Block b = l.getWorld().getBlockAt(l);
					if (b.getType() == ICE_MATERIAL) {
						b.setBlockData(bd);
					}
					iceActive.remove(l);
				}
			}
		}.runTaskLater(Plugin.getInstance(), ticks);
	}

	public static void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		if (meta.getLore() != null && meta.getLore().size() > 0) {
			for (String line : meta.getLore()) {
				finalLines.add(line);
			}
		}

		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (currentString != defaultColor + "") {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}

	/**
	 * Used for ability run checks for casting, rough estimate at whether they are holding a viable weapon
	 * @param item Item to check for
	 * @return if the item is an axe, sword, scythe or wand
	 */
	public static boolean isWeaponItem(ItemStack item) {
		return (InventoryUtils.isAxeItem(item) || InventoryUtils.isSwordItem(item) ||
				InventoryUtils.isWandItem(item) || InventoryUtils.isScytheItem(item));
	}

	public static double roundDouble(double num) {
		return Math.round(num * 100.0) / 100.0;
	}

	public static double roundPercent(double num) {
		return Math.round(num * 100.0 * 100.0) / 100.0;
	}

	public static boolean isWeaponAspectAbility(String s) {
		return s.equals(AxeAspect.ABILITY_NAME) || s.equals(WandAspect.ABILITY_NAME) || s.equals(ScytheAspect.ABILITY_NAME) || s.equals(SwordAspect.ABILITY_NAME);
	}

	/**
	 * Returns the party of nearby players, if applicable
	 * @return nearby party, otherwise null if none exists
	 */
	public static DepthsParty getPartyFromNearbyPlayers(Location l) {

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

	public static boolean isValidComboAttack(EntityDamageByEntityEvent event, Player player) {
		return event.getCause() == DamageCause.ENTITY_ATTACK && player.getCooledAttackStrength(0) == 1 && isWeaponItem(player.getInventory().getItemInMainHand());
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

	public static DepthsRewardType rewardFromRoom(DepthsRoomType roomType) {
		if (roomType == DepthsRoomType.ABILITY) {
			return DepthsRewardType.ABILITY;
		} else if (roomType == DepthsRoomType.ABILITY_ELITE) {
			return DepthsRewardType.ABILITY_ELITE;
		} else if (roomType == DepthsRoomType.UPGRADE) {
			return DepthsRewardType.UPGRADE;
		} else if (roomType == DepthsRoomType.UPGRADE_ELITE) {
			return DepthsRewardType.UPGRADE_ELITE;
		}
		return null;
	}

	public static String rewardString(DepthsRoomType roomType) {
		if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.ABILITY_ELITE) {
			return "Ability";
		} else if (roomType == DepthsRoomType.UPGRADE || roomType == DepthsRoomType.UPGRADE_ELITE) {
			return "Upgrade";
		} else if (roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.TREASURE_ELITE) {
			return "Treasure";
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
		}
		return "";
	}

	public static void iceExposedBlock(Block b, int iceTicks) {
		//Check above block first and see if it is exposed to air
		if (b.getRelative(BlockFace.UP).isSolid() && !(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isSolid() || b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.WATER)) {
			DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), iceTicks);
		} else if (b.isSolid() || b.getType() == Material.WATER) {
			DepthsUtils.spawnIceTerrain(b.getLocation(), iceTicks);
		} else if (b.getRelative(BlockFace.DOWN).isSolid() || b.getRelative(BlockFace.DOWN).getType() == Material.WATER) {
			DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.DOWN).getLocation(), iceTicks);
		}
	}
}
