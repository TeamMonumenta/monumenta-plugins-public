package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.integrations.monumentanetworkrelay.BroadcastedEvents;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	public enum ShrineType {
		SPEED("Speed", "D1Finished", NamedTextColor.AQUA),
		RESISTANCE("Resistance", "D3Finished", NamedTextColor.GRAY),
		STRENGTH("Strength", "D4Finished", NamedTextColor.DARK_RED),
		INTUITIVE("Intuitive", "D5Finished", NamedTextColor.GOLD),
		THRIFT("Thrift", "D6Finished", NamedTextColor.LIGHT_PURPLE),
		HARVESTER("Harvester", "D7Finished", NamedTextColor.DARK_GREEN),
		;

		public final String mName;
		public final String mObjective;
		public final TextColor mColor;

		ShrineType(String name, String objective, TextColor color) {
			mName = name;
			mObjective = objective;
			mColor = color;
		}

		public int getRemainingTime() {
			return ScoreboardUtils.getScoreboardValue("$PatreonShrine", mObjective).orElse(0);
		}
	}

	private static final Pattern SHARD_NUMBER_PATTERN = Pattern.compile("(?:-|^dev)(\\d+)$");

	private volatile List<ShrineType> mActiveShrines = new ArrayList<>();

	protected BukkitTask mSystemTask;

	private final Plugin mPlugin;

	public PlaceholderAPIIntegration(Plugin plugin) {
		super();
		plugin.getLogger().info("Enabling PlaceholderAPI integration");
		mPlugin = plugin;
		mSystemTask = new BukkitRunnable() {

			@Override
			public void run() {
				mActiveShrines = new ArrayList<>(Arrays.stream(ShrineType.values()).filter(shrine -> shrine.getRemainingTime() > 1).toList());
			}
		}.runTaskTimer(mPlugin, 0, 15);
	}

	@Override
	public String getIdentifier() {
		return "monumenta";
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

		// -------------------------  player-independent placeholders -------------------------

		if (identifier.startsWith("loot_table:")) {
			String lootTable = identifier.substring("loot_table:".length());
			ItemStack item = InventoryUtils.getItemFromLootTable(Bukkit.getWorlds().get(0).getSpawnLocation(), NamespacedKeyUtils.fromString(lootTable));
			if (item == null) {
				return "";
			} else {
				return MiniMessage.miniMessage().serialize(ItemUtils.getDisplayName(item).hoverEvent(item.asHoverEvent()));
			}
		}

		if (identifier.equalsIgnoreCase("shard")) {
			return ServerProperties.getShardName();
		}

		if (identifier.equalsIgnoreCase("shard_number")) {
			String fullShardName = PlaceholderAPI.setPlaceholders(null, "%network-relay_shard%");
			// actual shard numbers except for dev shards (for testing)
			Matcher matcher = SHARD_NUMBER_PATTERN.matcher(fullShardName);
			if (matcher.find()) {
				return matcher.group(1);
			}
			return "1";
		}

		if (identifier.startsWith("shrine_active_count")) {
			return Integer.toString(mActiveShrines.size());
		}

		if (identifier.startsWith("shrineicon")) {
			if (identifier.contains("simplified")) {
				int index = identifier.substring("shrineicon_simplified_".length()).isEmpty() ? 0 :
					            Integer.parseInt(identifier.substring("shrineicon_simplified_".length()));
				if (index < mActiveShrines.size()) {
					ShrineType currentShrine = mActiveShrines.get(index);
					if (ScoreboardUtils.getScoreboardValue("$PatreonShrine", currentShrine.mObjective).orElse(0) > 1) {
						return "active/" + currentShrine.mName;
					} else {
						return "inactive/" + currentShrine.mName;
					}
				}
			} else {
				String shrineType = identifier.substring("shrineicon_".length());
				for (ShrineType shrineName : ShrineType.values()) {
					if (shrineName.mName.equalsIgnoreCase(shrineType)) {
						if (shrineName.getRemainingTime() > 1) {
							return "active";
						} else {
							return "inactive";
						}
					}
				}
				return "inactive";
			}
		}

		if (identifier.startsWith("shrine_")) {
			int remainingTime;
			if (identifier.contains("simplified")) {
				int index = identifier.substring("shrine_simplified_".length()).isEmpty() ? 0 :
					            Integer.parseInt(identifier.substring("shrine_simplified_".length()));
				if (index < mActiveShrines.size()) {
					ShrineType currentShrine = mActiveShrines.get(index);
					remainingTime = currentShrine.getRemainingTime();
					if (remainingTime >= 1) {
						remainingTime = (int) Math.floor(remainingTime / 60.0);
						return MessagingUtils.legacyFromComponent(Component.text(currentShrine.mName + ": ", currentShrine.mColor).append(Component.text(remainingTime + "m", NamedTextColor.WHITE)));
					} else {
						mActiveShrines.remove(index);
						return "";
					}
				} else {
					return "";
				}

			} else {
				String shrineType = identifier.substring("shrine_".length());
				for (ShrineType shrineName : ShrineType.values()) {
					if (shrineName.mName.equalsIgnoreCase(shrineType)) {
						remainingTime = shrineName.getRemainingTime();
						if (remainingTime >= 1) {
							remainingTime = (int) Math.floor(remainingTime / 60.0);
							return MessagingUtils.legacyFromComponent(Component.text(shrineName.mName + ": ", shrineName.mColor).append(Component.text(remainingTime + "m", NamedTextColor.WHITE)));
						} else {
							return MessagingUtils.legacyFromComponent(Component.text(shrineName.mName, shrineName.mColor).append(Component.text(" is not active", NamedTextColor.WHITE)));
						}
					}
				}
			}
		}

		// -------------------------  player-dependent placeholders -------------------------

		if (player == null) {
			return "";
		}

		// %monumenta_class%
		if (identifier.equalsIgnoreCase("class")) {
			String cls = AbilityUtils.getClass(player);
			if (ServerProperties.getClassSpecializationsEnabled(player)) {
				String spec = AbilityUtils.getSpec(player);
				if (!spec.equals("No Spec")) {
					cls = cls + " (" + spec + ")";
				}
			}
			return cls;
		}

		// %monumenta_level%
		if (identifier.equalsIgnoreCase("level")) {
			int charmPower = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.CHARM_POWER).orElse(0);
			charmPower = (charmPower > 0) ? (charmPower / 3) - 2 : 0;
			return Integer.toString(AbilityUtils.getEffectiveTotalSkillPoints(player) +
					AbilityUtils.getEffectiveTotalSpecPoints(player) +
					ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0) +
					charmPower);
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

		//%monumenta_boss_details_<number>%
		if (identifier.startsWith("boss_details_")) {
			int index = Integer.parseInt(identifier.substring("boss_details_".length())) - 1;

			List<BroadcastedEvents.Event> events = BroadcastedEvents.getPerceptibleEvents(player);
			if (index >= 0 && events.size() > index) {
				BroadcastedEvents.Event event = events.get(index);

				String display = event.getDisplay();

				//funny mirrored stuff that only happen when both sides are filled.
				if ((index + 1) % 2 == 1 && events.size() > (index + 1)) {
					//Current is on the left
					BroadcastedEvents.Event mirroredEvent = events.get(index + 1);
					String mirroredDisplay = mirroredEvent.getDisplay();

					int lengthDiff = mirroredDisplay.length() - display.length();
					if (lengthDiff > 0) {
						//mirrored display is bigger
						return " ".repeat(lengthDiff) + display;
					}
				} else if ((index + 1) % 2 == 0) {
					//Current is on the right
					BroadcastedEvents.Event mirroredEvent = events.get(index - 1);
					String mirroredDisplay = mirroredEvent.getDisplay();

					int lengthDiff = mirroredDisplay.length() - display.length();
					if (lengthDiff > 0) {
						//mirrored display is bigger
						return display + " ".repeat(lengthDiff);
					}
				}

				return event.getDisplay();
			} else if ((index + 1) % 2 == 0 && (events.size() > (index - 1) && !events.isEmpty())) {
				//Allows for centering footer when first element exists but second doesn't.
				BroadcastedEvents.Event event = events.get(index - 1);
				return " ".repeat(event.getDisplay().length());
			} else {
				return "";
			}
		}

		if (identifier.startsWith("effect_")) {
			List<String> effectDisplays = DisplayableEffect.getSortedEffectDisplays(mPlugin, player);

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
					if (index >= 0 && effectDisplays.size() > index) {
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
