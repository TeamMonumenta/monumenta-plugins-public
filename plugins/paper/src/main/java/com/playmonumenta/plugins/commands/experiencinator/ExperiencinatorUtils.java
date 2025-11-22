package com.playmonumenta.plugins.commands.experiencinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.MonumentaTrigger;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig.Experiencinator;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public abstract class ExperiencinatorUtils {

	private static @Nullable File mConfigFile;
	private static long mConfigLastModTimestamp;
	private static @Nullable ExperiencinatorConfig mConfig;

	private ExperiencinatorUtils() {
	}

	private static ExperiencinatorConfig reloadConfig(Location lootTableLocation) {
		try {
			if (mConfigFile == null) {
				mConfigFile = new File(Plugin.getInstance().getDataFolder(), "experiencinator_config.json");
			}
			long lastMod = mConfigFile.lastModified();
			if (mConfig == null || lastMod != mConfigLastModTimestamp) {
				try (Reader reader = Files.newBufferedReader(mConfigFile.toPath(), StandardCharsets.UTF_8)) {
					mConfig = new ExperiencinatorConfig(new Gson().fromJson(reader, JsonObject.class), lootTableLocation);
				}
				mConfigLastModTimestamp = lastMod;
			}
		} catch (Exception e) {
			MMLog.severe("Could not (re)load Experiencinator config!", e);
		}
		if (mConfig == null) { // load blank config on failure if this is the first load, otherwise keep old config
			mConfig = new ExperiencinatorConfig();
		}
		return mConfig;
	}

	@Contract("!null -> !null")
	public static @Nullable ExperiencinatorConfig getConfig(@Nullable Location lootTableLocation) {
		return getConfig(lootTableLocation, true);
	}

	@Contract("!null, _ -> !null")
	public static @Nullable ExperiencinatorConfig getConfig(@Nullable Location lootTableLocation, boolean reload) {
		if (lootTableLocation != null && (mConfig == null || reload)) {
			return reloadConfig(lootTableLocation);
		}
		return mConfig;
	}

	/**
	 * Checks if the player can use the given Experiencinator (prerequisites met).
	 * Will send a failure message to the player if this returns false.
	 */
	public static boolean checkExperiencinator(Experiencinator experiencinator, ItemStack experiencinatorItem, Player player) {

		// sends a failure message directly
		return experiencinator.checkPrerequisites(player, experiencinatorItem);

	}

	/**
	 * Uses an Experiencinator to convert all items in the player's inventory according to the player's settings.
	 */
	public static void useExperiencinator(Experiencinator experiencinator, ItemStack experiencinatorItem, Player player) {

		if (mConfig == null) { // should not happen as the experiencinator is taken from the config
			return;
		}

		if (!checkExperiencinator(experiencinator, experiencinatorItem, player)) {
			return;
		}

		// only sell from inventory (not equipment) excluding hotbar
		List<ItemStack> items = Arrays.asList(player.getInventory().getStorageContents());
		items = items.subList(9, items.size());

		ConversionValues conversionValues = calculateOrConvert(items, player, experiencinator, experiencinatorItem, true);

		int soldItems = giveResults(player, conversionValues, Component.text("Items converted:"));

		if (soldItems > 0) {
			StatTrackManager.getInstance().incrementStatImmediately(experiencinatorItem, player, InfusionType.STAT_TRACK_CONVERT, soldItems);
			player.updateInventory();
		}

	}

	/**
	 * Checks if the given item is allowed to be converted, i.e. has no player modifications (stat tracking, infusions, masterwork levels, etc.)
	 */
	private static boolean canConvert(ItemStack item, Player player) {
		// check for any player infusions that aren't Shattered
		for (InfusionType infusion : InfusionType.values()) {
			if (infusion == InfusionType.SHATTERED) {
				continue;
			}
			if (ItemStatUtils.getInfusionLevel(item, infusion) > 0) {
				return false;
			}
		}
		// check if the item is not at base masterwork level
		Masterwork masterwork = ItemStatUtils.getMasterwork(item);
		return masterwork == Masterwork.ERROR || masterwork == Masterwork.NONE || ItemStatUtils.getRegion(item) != Region.RING
			|| masterwork == ItemStatUtils.getMasterwork(MasterworkUtils.getBaseMasterwork(item, player));
		// if we get here, the item has no player modifications or other reasons to prevent it from being converted
	}

	/**
	 * Gives the calculated results to the player.
	 *
	 * @return The total number of items sold
	 */
	public static int giveResults(Player player, ConversionValues conversionValues, @Nullable Component hoverTextTitle) {
		int soldItems = 0;
		for (Map.Entry<String, Map<Region, ConversionValue>> conversionSellValue : conversionValues.mValues.entrySet()) {
			ExperiencinatorConfig.Conversion conversion = Objects.requireNonNull(getConfig(player.getLocation(), false).getConversion(conversionSellValue.getKey()));
			// Give currency to player and send a chat message
			// Iterate over regions to get a consistent order
			for (Region region : Region.values()) {
				ConversionValue conversionValue = conversionSellValue.getValue().get(region);
				if (conversionValue == null) {
					continue;
				}
				for (Map.Entry<String, Integer> e : conversionValue.mTotalSellValue.entrySet()) {
					String result = e.getKey();
					Integer sellValue = e.getValue();

					if (sellValue > 0) {
						giveResults(sellValue, conversion, player, region, result);
					}
				}
				soldItems += conversionValue.mItemNames.values().stream().reduce(0, Integer::sum);
			}

			if (hoverTextTitle != null) {
				List<Component> messages = makeSellValueMessages(conversionValues, conversion, hoverTextTitle);
				for (Component message : messages) {
					HoverEvent<?> hoverEvent = message.hoverEvent();
					if (hoverEvent != null) {
						message = message.hoverEvent(null);
					}
					Component finalMessage = Component.text("Given ", NamedTextColor.AQUA).append(message).append(Component.text("!", NamedTextColor.AQUA)).hoverEvent(hoverEvent);
					player.sendMessage(finalMessage);
				}
			}
		}
		if (hoverTextTitle != null && soldItems == 0) {
			player.sendMessage(Component.text("No items were converted.", NamedTextColor.AQUA));
		}
		return soldItems;
	}

	/**
	 * Gives result items to the player, including compressing them if enabled, and sends a chat message of the total amount given if desired.
	 */
	private static void giveResults(int sellValue, ExperiencinatorConfig.Conversion conversion, Player player, Region region, String result) {
		int totalValue = sellValue;
		int remainingValue = sellValue;
		try {
			List<ExperiencinatorConfig.ConversionResult> conversionResults = new ArrayList<>(Objects.requireNonNull(conversion.getConversionResults(region, result)));
			// sort by value ascending
			conversionResults.sort(Comparator.comparingInt(ExperiencinatorConfig.ConversionResult::getValue));

			// If compressing result items is enabled, first remove existing uncompressed result items from the player and add their value to the totalValue counter
			List<Integer> existingSlots = new ArrayList<>();
			if (conversion.getCompressExistingResults()) {
				ItemStack[] inv = player.getInventory().getContents();
				for (int i = 0; i < conversionResults.size() - 1; i++) { // all but the most valuable one - that one cannot be compressed further
					existingSlots.add(-1);
					ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
					ItemStack resultItem = conversionResult.getItem();
					for (int j = 0; j < inv.length; j++) {
						ItemStack invItem = inv[j];
						if (invItem != null && resultItem.isSimilar(invItem)) {
							totalValue += invItem.getAmount() * conversionResult.getValue();
							invItem.setAmount(0);
							existingSlots.set(i, j);
						}
					}
				}
			}

			// Give result items to the player
			remainingValue = totalValue;
			for (int i = conversionResults.size() - 1; i >= 0; i--) {
				ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
				int given = remainingValue / conversionResult.getValue();
				if (given > 0) {
					remainingValue -= given * conversionResult.getValue();

					ItemStack resultStack = conversionResult.getItem();
					ItemStack circusTicket = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r1/items/currency/circus_ticket")));
					ItemStack carnivalToken = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r2/items/currency/carnival_token")));
					if (resultStack.displayName().equals(circusTicket.displayName())) {
						for (int j = 0; j < given; j++) {
							PlayerUtils.executeCommandOnPlayer(player, "function monumenta:advancements/general/igor/got_a_ticket_scores");
						}
					}
					if (resultStack.displayName().equals(carnivalToken.displayName())) {
						for (int j = 0; j < given; j++) {
							PlayerUtils.executeCommandOnPlayer(player, "function monumenta:mechanisms/carnival/got_a_token_scores");
						}
					}

					resultStack.setAmount(given);
					if (existingSlots.size() > i && existingSlots.get(i) >= 0 && ItemUtils.isNullOrAir(player.getInventory().getItem(existingSlots.get(i)))) {
						player.getInventory().setItem(existingSlots.get(i), resultStack);
					} else {
						InventoryUtils.giveItem(player, resultStack);
					}
				}
			}
			if (remainingValue != 0) {
				// this should never happen, as a result with value 1 must exist (and this is checked when the config is loaded)
				player.sendMessage(Component.text("Unable to give you " + remainingValue + " remaining items! Please contact a moderator.", NamedTextColor.RED));
				Plugin.getInstance().getLogger().severe("Unable to give " + remainingValue + " remaining Experiencinator result items to " + player.getName() + ".");
			}

		} catch (Throwable t) {
			player.sendMessage(Component.text("Error while giving you " + remainingValue + " remaining items from a total of " + totalValue + "! Please contact a moderator.", NamedTextColor.RED));
			Plugin.getInstance().getLogger().severe("Error while giving Experiencinator result items to " + player.getName() + ". Initial sell value: " + sellValue + ", totalValue: " + totalValue + ", remainingValue: " + remainingValue);
			throw t;
		}
	}

	/**
	 * Converts an {@link Item} using the given conversion and rates. Results will be given to the passed player.
	 * <p>
	 * Does not check any prerequisites.
	 *
	 * @param player             Player to receive results
	 * @param entity             The item entity to convert
	 * @param conversionName     Name of the conversion to use
	 * @param conversionRateName Name of the conversion rates to use
	 */
	public static boolean convertItemEntity(Player player, Entity entity, String conversionName, String conversionRateName) {
		if (!(entity instanceof Item itemEntity)) {
			return false;
		}
		ExperiencinatorConfig.Conversion conversion = getConfig(player.getLocation()).getConversion(conversionName);
		if (conversion == null) {
			return false;
		}
		if (convertSingleItem(itemEntity.getItemStack(), player, conversion, conversionRateName)) {
			itemEntity.remove();
			return true;
		}
		return false;
	}

	public static class ConversionValues {
		// map of conversion -> region -> ConversionValue
		public final Map<String, Map<Region, ConversionValue>> mValues = new HashMap<>();
	}

	public static class ConversionValue {
		// map of result name -> count (of result)
		public final Map<String, Integer> mTotalSellValue = new LinkedHashMap<>();
		// map of item name -> count (of sold items)
		public final Map<String, Integer> mItemNames = new TreeMap<>();
	}


	public static ConversionValues calculateOrConvert(List<ItemStack> items, Player player, Experiencinator experiencinator, ItemStack experiencinatorItem, boolean convert) {

		ExperiencinatorConfig config = getConfig(player.getLocation(), false);
		ExperiencinatorSettings settings = new ExperiencinatorSettings(config.getScoreboardConfig(), player);

		Map<Region, String> conversionRateNames = experiencinator.getConversionRates();

		ConversionValues conversionValues = new ConversionValues();

		for (ExperiencinatorConfig.Conversion conversion : config.getConversions()) {
			for (ItemStack item : items) {
				if (item == null) {
					continue;
				}
				Region region = ItemStatUtils.getRegion(item);
				Tier tier = ItemStatUtils.getTier(item);
				if (!conversion.conversionAllowed(player, region, tier, experiencinatorItem)) { // prerequisites for conversion not met
					continue;
				}
				if (settings.getConversion(region, tier) != conversion.getSettingsId()
					&& conversion.getSettingsId() > 0) { // conversion not enabled
					continue;
				}
				String conversionRateName = conversionRateNames.get(region);
				if (conversionRateName == null) {
					continue;
				}

				calculateOrSell(item, player, conversion, conversionRateName, conversionValues, convert);
			}

		}

		return conversionValues;
	}

	public static ConversionValues calculateOrConvert(List<ItemStack> items, Player player, String conversionName, String conversionRateName, boolean convert) {

		ExperiencinatorConfig.Conversion conversion = getConfig(player.getLocation()).getConversion(conversionName);
		if (conversion == null) {
			return new ConversionValues();
		}

		ConversionValues conversionValues = new ConversionValues();

		for (ItemStack item : items) {
			calculateOrSell(item, player, conversion, conversionRateName, conversionValues, convert);
		}

		return conversionValues;
	}

	/**
	 * Converts a single item with the given conversion settings. Will do nothing if anything is invalid.
	 * <p>
	 * <b>NOTE: This does not check any prerequisites!</b>
	 *
	 * @param player             Player that will receive the conversion results
	 * @param item               The item to convert
	 * @param conversion         The conversion to use
	 * @param conversionRateName Name of the conversion rate to use
	 * @return If the item was converted
	 */
	public static boolean convertSingleItem(ItemStack item, Player player, ExperiencinatorConfig.Conversion conversion, String conversionRateName) {
		ConversionValues conversionValues = new ConversionValues();
		calculateOrSell(item, player, conversion, conversionRateName, conversionValues, true);
		return giveResults(player, conversionValues, null) != 0;
	}

	public static void calculateOrSell(ItemStack item, Player player, ExperiencinatorConfig.Conversion conversion, String conversionRateName, ConversionValues conversionValues, boolean convert) {
		if (item == null) {
			return;
		}
		if (!canConvert(item, player)) {
			return;
		}
		Region region = ItemStatUtils.getRegion(item);
		ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
		if (conversionRates == null) { // cannot convert items of this region with this conversion
			return;
		}
		Tier tier = ItemStatUtils.getTier(item);
		ExperiencinatorConfig.ConversionRate sellRate = conversionRates.getRate(conversionRateName, tier);
		if (sellRate == null) { // cannot convert items of this tier with this conversion
			return;
		}
		ExperiencinatorConfig.ConversionRateResult result = sellRate.getValue(item);
		if (result.mCount <= 0) {
			return;
		}
		List<ExperiencinatorConfig.ConversionResult> conversionResults = conversion.getConversionResults(region, result.mResult);
		if (conversionResults == null || conversionResults.stream().anyMatch(res -> res.getItem().isSimilar(item))) { // no result, or item is a result item itself
			return;
		}

		int value = result.mCount * item.getAmount();

		ConversionValue conversionValue = conversionValues.mValues.computeIfAbsent(conversion.getId(), c -> new HashMap<>()).computeIfAbsent(region, r -> new ConversionValue());
		conversionValue.mTotalSellValue.merge(result.mResult, value, Integer::sum);
		conversionValue.mItemNames.merge(ItemUtils.getPlainNameOrDefault(item), item.getAmount(), Integer::sum);

		if (convert) {
			item.setAmount(0);
		}
	}

	/**
	 * Builds a nice message detailing how much the player got from the conversion
	 *
	 * @return A list of messages, one for each region
	 */
	private static List<Component> makeSellValueMessages(ConversionValues conversionValues, ExperiencinatorConfig.Conversion conversion, @Nullable Component hoverTextTitle) {
		Map<Region, ConversionValue> conversionValueMap = conversionValues.mValues.get(conversion.getId());
		if (conversionValueMap == null) {
			return List.of();
		}
		List<Component> messages = new ArrayList<>();
		for (Region region : Region.values()) {
			ConversionValue conversionValue = conversionValueMap.get(region);
			if (conversionValue == null) {
				continue;
			}
			for (Map.Entry<String, Integer> resultSellValue : conversionValue.mTotalSellValue.entrySet()) {
				List<Component> givenTexts = new ArrayList<>();
				int remainingValue = resultSellValue.getValue();
				List<ExperiencinatorConfig.ConversionResult> conversionResults = Objects.requireNonNull(conversion.getConversionResults(region, resultSellValue.getKey())); // cannot be null if we could convert items with it
				for (int i = conversionResults.size() - 1; i >= 0; i--) {
					ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
					int given = remainingValue / conversionResult.getValue();
					if (given > 0) {
						remainingValue -= given * conversionResult.getValue();
						givenTexts.add(Component.text(given + " ", NamedTextColor.WHITE).append(Component.text(given == 1 ? conversionResult.getNameSingular() : conversionResult.getName(), NamedTextColor.AQUA)));
					}
				}
				Component message;
				if (givenTexts.size() == 1) { // 1: nothing to do
					message = givenTexts.get(0);
				} else if (givenTexts.size() == 2) { // 2: join with " and "
					message = givenTexts.get(0).append(Component.text(" and ", NamedTextColor.AQUA).append(givenTexts.get(1)));
				} else { // 3 or more: join with ", ", and the last one with ", and "
					message = givenTexts.get(0);
					for (Component addmsg : givenTexts.subList(1, givenTexts.size() - 1)) {
						message = message.append(Component.text(", ", NamedTextColor.AQUA).append(addmsg));
					}
					message = message.append(Component.text(", and ", NamedTextColor.AQUA).append(givenTexts.get(givenTexts.size() - 1)));
				}
				if (!conversionValue.mItemNames.isEmpty()) {
					Component hover = Component.empty();
					for (Map.Entry<String, Integer> e : conversionValue.mItemNames.entrySet()) {
						if (!hover.equals(Component.empty())) {
							hover = hover.append(Component.newline());
						}
						hover = hover.append(Component.text(e.getValue() + " " + e.getKey(), NamedTextColor.WHITE));
					}
					if (hoverTextTitle != null) {
						hover = hoverTextTitle.append(Component.newline()).append(hover);
					}
					message = message.hoverEvent(hover);
				}
				messages.add(message);
			}
		}

		return messages;
	}

	public static void convertItemEntitiesWithConfirmation(Player player, Collection<Entity> entities, String conversionName, String conversionRateName, boolean confirmed) {

		ExperiencinatorConfig.Conversion conversion = getConfig(player.getLocation()).getConversion(conversionName);
		if (conversion == null) {
			return;
		}

		List<ItemStack> items = entities.stream()
			.filter(Entity::isValid)
			.map(e -> e instanceof Item i ? i.getItemStack() : null)
			.filter(Objects::nonNull)
			.toList();

		ConversionValues conversionValues = calculateOrConvert(items, player, conversionName, conversionRateName, confirmed);

		if (confirmed) {
			for (Entity entity : entities) {
				if (entity instanceof Item item
					&& item.isValid()
					&& item.getItemStack().getAmount() == 0) {
					item.remove();
				}
			}
			giveResults(player, conversionValues, Component.text("Items sold:"));
		} else {
			List<Component> messages = makeSellValueMessages(conversionValues, conversion, Component.text("Items to be sold:"));

			if (messages.isEmpty()) {
				String message = ServerProperties.getRegion(player) != Region.RING
					? "No items! Place Tier I through Tier V or Uncommon items on the pedestal in front of you to sell them."
					: "No items! Place Charms or Tier I through Tier III items on the pedestal in front of you to sell them.";
				player.sendMessage(Component.text(message, NamedTextColor.AQUA));
				return;
			}

			if (messages.size() == 1) {
				Component message = messages.get(0);
				HoverEvent<?> hoverEvent = message.hoverEvent();
				if (hoverEvent != null) {
					message = message.hoverEvent(null);
				}
				player.sendMessage(Component.text("Selling these items will give ", NamedTextColor.AQUA).append(message).append(Component.text(".", NamedTextColor.AQUA)).hoverEvent(hoverEvent));
			} else {
				player.sendMessage(Component.text("Selling these items will give:", NamedTextColor.AQUA));
				for (Component message : messages) {
					HoverEvent<?> hoverEvent = message.hoverEvent();
					if (hoverEvent != null) {
						message = message.hoverEvent(null);
					}
					player.sendMessage(Component.text(" - ", NamedTextColor.AQUA).append(message).hoverEvent(hoverEvent));
				}
			}
			Location originalLocation = player.getLocation();
			String sellCommand = MonumentaTrigger.makeTrigger(player, true, p -> {
				if (p.getLocation().getWorld() != originalLocation.getWorld() || p.getLocation().distanceSquared(originalLocation) > 4) {
					return;
				}
				convertItemEntitiesWithConfirmation(p, entities, conversionName, conversionRateName, true);
			});
			player.sendMessage(Component.text("[Sell Items]", NamedTextColor.LIGHT_PURPLE).clickEvent(ClickEvent.runCommand(sellCommand)));
		}
	}

}
