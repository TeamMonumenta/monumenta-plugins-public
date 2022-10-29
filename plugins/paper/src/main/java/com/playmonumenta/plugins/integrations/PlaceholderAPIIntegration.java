package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	Plugin mPlugin;
	boolean mIsPlay;

	public PlaceholderAPIIntegration(Plugin plugin) {
		super();
		plugin.getLogger().info("Enabling PlaceholderAPI integration");
		mPlugin = plugin;
		mIsPlay = Plugin.IS_PLAY_SERVER;
	}

	@Override
	public String getIdentifier() {
		return "monumenta";
	}

	@Override
	public @Nullable String getPlugin() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Team Epic";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player player, String identifier) {

		if (identifier.startsWith("loot_table:")) {
			String lootTable = identifier.substring("loot_table:".length());
			ItemStack item = InventoryUtils.getItemFromLootTable(Bukkit.getWorlds().get(0).getSpawnLocation(), NamespacedKeyUtils.fromString(lootTable));
			if (item == null) {
				return "";
			} else {
				return MiniMessage.miniMessage().serialize(ItemUtils.getDisplayName(item).hoverEvent(item.asHoverEvent()));
			}
		}

		if (player == null) {
			return "";
		}

		// %monumenta_class%
		if (identifier.equalsIgnoreCase("class")) {
			// TODO: This really should use the standard thing in Plugin.java... but it's
			// currently a pile of crap and this is actually less awful
			switch (ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0)) {
			case 0:
				return "No class";
			case 1:
				return "Mage";
			case 2:
				return "Warrior";
			case 3:
				return "Cleric";
			case 4:
				return "Rogue";
			case 5:
				return "Alchemist";
			case 6:
				return "Scout";
			case 7:
				return "Warlock";
			default:
				return "Unknown class";

			}
		}

		// %monumenta_level%
		if (identifier.equalsIgnoreCase("level")) {
			int charmPower = ScoreboardUtils.getScoreboardValue(player, "CharmPower").orElse(0);
			charmPower = (charmPower > 0) ? (charmPower / 3) - 2 : 0;
			return Integer.toString(ScoreboardUtils.getScoreboardValue(player, "TotalLevel").orElse(0) +
				                        ScoreboardUtils.getScoreboardValue(player, "TotalSpec").orElse(0) +
				                        ScoreboardUtils.getScoreboardValue(player, "TotalEnhance").orElse(0) +
				                        charmPower);
		}

		if (identifier.equalsIgnoreCase("shard")) {
			String shard = ServerProperties.getShardName();

			String worldName = player.getWorld().getName();
			String mask = "Project_Epic-";
			if (worldName.startsWith(mask)) {
				return worldName.substring(mask.length());
			}
			return shard;
		}

		//Player equipped title
		if (identifier.equalsIgnoreCase("title")) {
			Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
			if (title != null) {
				return title.getName() + " ";
			} else {
				return "";
			}
		}

		if (identifier.startsWith("shrineicon")) {
			String shrineType = identifier.substring("shrineicon_".length());
			if ((shrineType.equalsIgnoreCase("Speed") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D1Finished").orElse(0) > 1) ||
				(shrineType.equalsIgnoreCase("Resistance") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D3Finished").orElse(0) > 1) ||
				(shrineType.equalsIgnoreCase("Strength") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D4Finished").orElse(0) > 1) ||
				(shrineType.equalsIgnoreCase("Intuitive") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D5Finished").orElse(0) > 1) ||
				(shrineType.equalsIgnoreCase("Thrift") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D6Finished").orElse(0) > 1) ||
				(shrineType.equalsIgnoreCase("Harvester") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D7Finished").orElse(0) > 1)) {
				return "active";
			}
			return "inactive";
		}

		if (identifier.startsWith("shrine")) {
			String shrineType = identifier.substring("shrine_".length());
			int remainingTime;
			if (shrineType.equalsIgnoreCase("Speed")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D1Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.AQUA + "Speed: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.AQUA + "Speed" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Resistance")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D3Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.GRAY + "Resistance: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.GRAY + "Resistance" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Strength")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D4Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.DARK_RED + "Strength: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.DARK_RED + "Strength" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Intuitive")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D5Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.GOLD + "Intuitive: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.GOLD + "Intuitive" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Thrift")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D6Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.LIGHT_PURPLE + "Thrift: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.LIGHT_PURPLE + "Thrift" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Harvester")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D7Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.DARK_GREEN + "Harvester: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.DARK_GREEN + "Harvester" + ChatColor.WHITE + " is not active.";
				}
			}
		}

		if (identifier.startsWith("effect_")) {
			List<String> effectDisplays = new ArrayList<>();
			List<Effect> effects = new ArrayList<>(mPlugin.mEffectManager.getPriorityEffects(player).values());
			effects.sort(new EffectManager.SortEffectsByDuration());

			for (Effect effect : effects) {
				String display = effect.getDisplay();
				if (display != null) {
					effectDisplays.add(display);
				}
			}

			if (identifier.startsWith("effect_more")) {
				int extra = effectDisplays.size() - 10;
				if (extra == 1) {
					//Show 11th if there are exactly 11
					return effectDisplays.get(10);
				} else if (extra > 0) {
					return ChatColor.GRAY + "... and " + extra + " more effects";
				} else {
					return "";
				}
			} else {
				try {
					int index = Integer.parseInt(identifier.substring("effect_".length())) - 1;
					if (effectDisplays.size() > index) {
						return effectDisplays.get(index);
					} else {
						return "";
					}
				} catch (NumberFormatException numberFormatException) {
					MMLog.warning("Failed to find integer after 'effect_' on tab list");
				}
			}
		}

		return null;
	}
}
