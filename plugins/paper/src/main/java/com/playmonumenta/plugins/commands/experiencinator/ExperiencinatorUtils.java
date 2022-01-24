package com.playmonumenta.plugins.commands.experiencinator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig.Experiencinator;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public abstract class ExperiencinatorUtils {

	private static @Nullable File mConfigFile;
	private static long mConfigLastModTimestamp;
	private static @Nullable ExperiencinatorConfig mConfig;

	private ExperiencinatorUtils() {
	}

	private static void reloadConfig(Location lootTableLocation) {
		try {
			if (mConfigFile == null) {
				mConfigFile = new File(Plugin.getInstance().getDataFolder(), "experiencinator_config.json");
			}
			long lastMod = mConfigFile.lastModified();
			if (mConfig == null || lastMod != mConfigLastModTimestamp) {
				try (FileReader reader = new FileReader(mConfigFile)) {
					mConfig = new ExperiencinatorConfig(new Gson().fromJson(reader, JsonObject.class), lootTableLocation);
				}
				mConfigLastModTimestamp = lastMod;
			}
		} catch (Exception e) {
			Plugin.getInstance().getLogger().log(Level.SEVERE, e, () -> "Could not (re)load Experiencinator config!");
		}
	}

	public static @Nullable ExperiencinatorConfig getConfig(@Nullable Location lootTableLocation) {
		if (lootTableLocation != null) {
			reloadConfig(lootTableLocation);
		}
		return mConfig;
	}

	/**
	 * Checks if the player can use the given Experiencinator (not shattered and prerequisites met).
	 * Will send a failure message to the player if this returns false.
	 */
	public static boolean checkExperiencinator(Experiencinator experiencinator, ItemStack experiencinatorItem, Player player) {

		if (ItemStatUtils.isShattered(experiencinatorItem)) {
			player.sendRawMessage(ChatColor.DARK_RED + "Your " + experiencinator.getName() + " is shattered and cannot be used!");
			return false;
		}

		if (!experiencinator.checkPrerequisites(player)) { // sends a failure message directly
			return false;
		}

		return true;

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

		ExperiencinatorSettings settings = new ExperiencinatorSettings(mConfig.getScoreboardConfig(), player);

		Map<Region, String> conversionRateNames = experiencinator.getConversionRates();

		boolean soldSomething = false;
		for (ExperiencinatorConfig.Conversion conversion : mConfig.getConversions()) {

			Map<Region, Integer> totalSellValue = new HashMap<>();

			@Nullable ItemStack[] inventory = player.getInventory().getStorageContents();
			for (int i = 9; i < inventory.length; i++) {
				ItemStack item = inventory[i];
				if (item == null) {
					continue;
				}
				Region region = ItemStatUtils.getRegion(item);
				ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
				if (conversionRates == null) { // cannot convert items of this region with this conversion
					continue;
				}
				Tier tier = ItemStatUtils.getTier(item);
				if (!conversion.conversionAllowed(player, tier)) { // prerequisites for conversion not met
					continue;
				}
				if (settings.getConversion(region, tier) != conversion.getSettingsId()) { // conversion not enabled
					continue;
				}
				String conversionRateName = conversionRateNames.get(region);
				Integer sellValue = conversionRates.getRate(conversionRateName, tier);
				if (sellValue == null) { // cannot convert items of this tier with this conversion
					continue;
				}
				if (!canConvert(item)) { // item is modified by player (infused etc.)
					continue;
				}
				List<ExperiencinatorConfig.ConversionResult> conversionResults = conversion.getConversionResults(region);
				if (conversionResults == null || conversionResults.stream().anyMatch(res -> res.getItem().isSimilar(item))) { // no result, or item is a result item itself
					continue;
				}

				// calculate sell value for the entire item stack and clear the item
				int value = sellValue * item.getAmount();
				totalSellValue.merge(region, value, Integer::sum);
				inventory[i] = null;
			}
			player.getInventory().setStorageContents(inventory);

			// Give currency to player and send a chat message
			// Iterate over regions to get a consistent order
			for (Region region : Region.values()) {
				Integer sellValue = totalSellValue.get(region);
				if (sellValue == null) {
					continue;
				}

				if (sellValue > 0) {
					soldSomething = true;

					giveResults(sellValue, conversion, player, region, true);
				}
			}
		}

		if (!soldSomething) {
			player.sendRawMessage(ChatColor.AQUA + "No items were converted.");
		}

	}

	/**
	 * Checks if the given item is allowed to be converted, i.e. has no player modifications (stat tracking, infusions, etc.)
	 */
	private static boolean canConvert(ItemStack item) {
		for (InfusionType infusion : InfusionType.values()) {
			if (ItemStatUtils.getInfusionLevel(item, infusion) > 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gives result items to the player, including compressing them if enabled,a dn sends a chat message of the total amount given if desired.
	 */
	private static void giveResults(int sellValue, ExperiencinatorConfig.Conversion conversion, Player player, Region region, boolean chatMessage) {
		int totalValue = sellValue;
		int remainingValue = sellValue;
		try {
			List<ExperiencinatorConfig.ConversionResult> conversionResults = conversion.getConversionResults(region);
			assert conversionResults != null : "@AssumeAssertion(nullness): if a sell value is set, a conversion must exist for that value to have been calculated";
			// sort by value ascending
			conversionResults.sort(Comparator.comparingInt(ExperiencinatorConfig.ConversionResult::getValue));

			// If compressing result items is enabled, first remove existing uncompressed result items from the player and add their value to the totalValue counter
			if (conversion.getCompressExistingResults()) {
				@Nullable ItemStack[] inv = player.getInventory().getContents();
				for (int i = 0; i < conversionResults.size() - 1; i++) { // all but the most valuable one - that one cannot be compressed further
					ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
					ItemStack resultItem = conversionResult.getItem();
					for (int j = 0; j < inv.length; j++) {
						ItemStack invItem = inv[j];
						if (invItem != null && resultItem.isSimilar(invItem)) {
							totalValue += invItem.getAmount() * conversionResult.getValue();
							inv[j] = null;
						}
					}
				}
				player.getInventory().setContents(inv);
			}

			// Give result items to the player
			remainingValue = totalValue;
			for (int i = conversionResults.size() - 1; i >= 0; i--) {
				ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
				int given = remainingValue / conversionResult.getValue();
				if (given > 0) {
					remainingValue -= given * conversionResult.getValue();

					ItemStack resultStack = conversionResult.getItem();
					resultStack.setAmount(given);
					InventoryUtils.giveItem(player, resultStack);
				}
			}
			if (remainingValue != 0) {
				// this should never happen, as a result with value 1 must exist (and this is checked when the config is loaded)
				player.sendRawMessage(ChatColor.RED + "Unable to give you " + remainingValue + " remaining items! Please contact a moderator.");
			}

			if (chatMessage) {
				// Build a nice message detailing how much the player got from the conversion and send it to them
				// The loop here is almost the same as the one for giving items, but we start at sellValue instead of totalValue
				List<String> givenTexts = new ArrayList<>();
				remainingValue = sellValue;
				for (int i = conversionResults.size() - 1; i >= 0; i--) {
					ExperiencinatorConfig.ConversionResult conversionResult = conversionResults.get(i);
					int given = remainingValue / conversionResult.getValue();
					if (given > 0) {
						remainingValue -= given * conversionResult.getValue();
						givenTexts.add("" + ChatColor.RESET + given + ChatColor.AQUA + " " + (given == 1 ? conversionResult.getNameSingular() : conversionResult.getName()));
					}
				}
				String message;
				if (givenTexts.size() == 1) { // 1: nothing to do
					message = givenTexts.get(0);
				} else if (givenTexts.size() == 2) { // 2: join with " and "
					message = StringUtils.join(givenTexts, " and ");
				} else { // 3 or more: join with ", ", and the last one with ", and "
					message = StringUtils.join(givenTexts.subList(0, givenTexts.size() - 1), ", ") + ", and " + givenTexts.get(givenTexts.size() - 1);
				}
				player.sendRawMessage(ChatColor.AQUA + "Given " + message + "!");
			}
		} catch (Throwable t) {
			player.sendRawMessage(ChatColor.RED + "Error while giving you " + remainingValue + " remaining items from a total of " + totalValue + "! Please contact a moderator.");
			Plugin.getInstance().getLogger().severe("Error while giving Experiencinator result items. Initial sell value: " + sellValue + ", totalValue: " + totalValue + ", remainingValue: " + remainingValue + "");
			throw t;
		}
	}

	/**
	 * Converts an {@link Item} using the given conversion and rates. Results will be given to the passed player,
	 * and if the ite cannot be converted and giveToPlayerOnFail is true, the item will be given to the palyer directly.
	 * <p>
	 * Does not check any prerequisites.
	 *
	 * @param player             Player to receive results
	 * @param entity             The item entity to convert
	 * @param conversionName     Name of the conversion to use
	 * @param conversionRateName Name of the conversion rates to use
	 * @param giveToPlayerOnFail Whether to give the item to the player if it cannot be converted
	 */
	public static void convertItemEntity(Player player, Entity entity, String conversionName, String conversionRateName, boolean giveToPlayerOnFail) {
		if (!(entity instanceof Item itemEntity)) {
			return;
		}
		reloadConfig(player.getLocation());
		if (mConfig == null) {
			return;
		}
		ExperiencinatorConfig.Conversion conversion = mConfig.getConversion(conversionName);
		if (conversion == null) {
			return;
		}
		if (convertSingleItem(player, itemEntity.getItemStack(), conversion, conversionRateName)) {
			itemEntity.remove();
		} else if (giveToPlayerOnFail) {
			itemEntity.remove();
			InventoryUtils.giveItem(player, itemEntity.getItemStack());
		}
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
	public static boolean convertSingleItem(Player player, ItemStack item, ExperiencinatorConfig.Conversion conversion, String conversionRateName) {
		if (!canConvert(item)) {
			return false;
		}

		Region region = ItemStatUtils.getRegion(item);
		Tier tier = ItemStatUtils.getTier(item);

		ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
		if (conversionRates == null) {
			return false;
		}
		Integer value = conversionRates.getRate(conversionRateName, tier);
		if (value == null) {
			return false;
		}
		List<ExperiencinatorConfig.ConversionResult> conversionResults = conversion.getConversionResults(region);
		if (conversionResults == null || conversionResults.stream().anyMatch(res -> res.getItem().isSimilar(item))) {
			return false;
		}

		giveResults(value * item.getAmount(), conversion, player, region, false);
		return true;
	}

}
