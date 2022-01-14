package com.playmonumenta.plugins.commands.experiencinator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;
import com.playmonumenta.plugins.utils.ItemUtils.ItemTier;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

/**
 * Configuration for Experiencinators, stored in a JSON file for quick & easy changes.
 */
public final class ExperiencinatorConfig {

	private final List<Experiencinator> mExperiencinators = new ArrayList<>();
	private final Map<String, Conversion> mConversions = new LinkedHashMap<>(); // LinkedHashMap to preserve iteration order
	private final GuiConfig mGuiConfig;
	private final ScoreboardConfig mScoreboardConfig;

	public ExperiencinatorConfig(JsonElement element, Location lootTableLocation) throws Exception {
		JsonObject object = element.getAsJsonObject();

		JsonElement experiencinatorsElement = object.get("experiencinators");
		if (experiencinatorsElement != null) {
			JsonArray experiencinatorsArray = experiencinatorsElement.getAsJsonArray();
			for (JsonElement experiencinatorsArrayElement : experiencinatorsArray) {
				mExperiencinators.add(new Experiencinator(experiencinatorsArrayElement));
			}
		} else {
			throw new Exception("Experiencinator config missing experiencinators value!");
		}

		JsonElement conversionsElement = object.get("conversions");
		if (conversionsElement != null) {
			JsonObject conversionObject = conversionsElement.getAsJsonObject();
			for (Entry<String, JsonElement> entry : conversionObject.entrySet()) {
				String conversionName = entry.getKey();
				Conversion conversion = new Conversion(entry.getValue(), lootTableLocation);
				mConversions.put(conversionName, conversion);
			}
		} else {
			throw new Exception("Experiencinator config missing conversions value!");
		}

		JsonElement guiElement = object.get("gui");
		if (guiElement != null) {
			mGuiConfig = new GuiConfig(guiElement);
		} else {
			throw new Exception("Experiencinator config missing gui value!");
		}

		JsonElement scoreboardsElement = object.get("scoreboards");
		if (scoreboardsElement != null) {
			mScoreboardConfig = new ScoreboardConfig(scoreboardsElement);
		} else {
			throw new Exception("Experiencinator config missing scoreboards value!");
		}

		for (Experiencinator experiencinator : mExperiencinators) {
			experiencinator.validate(this);
		}
		for (Conversion conversion : mConversions.values()) {
			conversion.validate(this);
		}
	}

	public @Nullable Experiencinator getExperiencinator(ItemStack item) {
		Material material = item.getType();
		String plainName = ItemUtils.getPlainNameIfExists(item);
		if (plainName == null) {
			return null;
		}
		for (Experiencinator experiencinator : mExperiencinators) {
			if (experiencinator.mMaterial == material
					&& experiencinator.mName.equals(plainName)) {
				return experiencinator;
			}
		}
		return null;
	}

	public List<Experiencinator> getExperiencinators() {
		return List.copyOf(mExperiencinators);
	}

	public Collection<Conversion> getConversions() {
		return mConversions.values();
	}

	public Set<String> getConversionNames() {
		return mConversions.keySet();
	}

	public @Nullable Conversion getConversion(String id) {
		return mConversions.get(id);
	}

	public @Nullable Conversion findConversion(int conversionSettingsId, ItemUtils.ItemRegion itemRegion) {
		for (Conversion conversion : mConversions.values()) {
			if (conversion.getSettingsId() == conversionSettingsId
				    && conversion.getConversionRates(itemRegion) != null) {
				return conversion;
			}
		}
		return null;
	}

	public GuiConfig getGuiConfig() {
		return mGuiConfig;
	}

	public ScoreboardConfig getScoreboardConfig() {
		return mScoreboardConfig;
	}

	public static class Experiencinator {

		private final String mName;
		private final Material mMaterial;
		private final Map<ItemRegion, String> mConversionRates = new EnumMap<>(ItemRegion.class);
		private final @Nullable QuestPrerequisites mPrerequisites;
		private final @Nullable String mPrerequisitesFailureMessage;

		private Experiencinator(JsonElement element) throws Exception {
			JsonObject object = element.getAsJsonObject();

			JsonElement nameElement = object.get("name");
			if (nameElement != null) {
				mName = nameElement.getAsString();
			} else {
				throw new Exception("experiencinator missing name value!");
			}

			JsonElement materialElement = object.get("material");
			if (materialElement != null) {
				Material material = Material.matchMaterial(materialElement.getAsString());
				if (material == null) {
					throw new Exception("Invalid material value '" + materialElement.getAsString() + "'");
				}
				mMaterial = material;
			} else {
				throw new Exception("experiencinator missing material value!");
			}

			JsonElement conversionRatesElement = object.get("conversion_rates");
			if (conversionRatesElement != null) {
				JsonObject conversionRatesObject = conversionRatesElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : conversionRatesObject.entrySet()) {
					ItemRegion itemRegion = parseItemRegion(entry.getKey());
					mConversionRates.put(itemRegion, entry.getValue().getAsString());
				}
			} else {
				throw new Exception("experiencinator missing conversion_rates value!");
			}

			JsonElement prerequisitesElement = object.get("prerequisites");
			if (prerequisitesElement != null) {
				mPrerequisites = new QuestPrerequisites(prerequisitesElement);
			} else {
				mPrerequisites = null;
			}

			JsonElement prerequisitesFailureMessageElement = object.get("prerequisites_failure_message");
			if (prerequisitesFailureMessageElement != null) {
				mPrerequisitesFailureMessage = prerequisitesFailureMessageElement.getAsString();
			} else {
				if (mPrerequisites != null) {
					throw new Exception("experiencinator missing prerequisites_failure_message value!");
				} else {
					mPrerequisitesFailureMessage = null;
				}
			}

		}

		private void validate(ExperiencinatorConfig config) throws Exception {
			for (String conversionName : mConversionRates.values()) {
				boolean conversionExists = config.mConversions.values().stream()
					.flatMap(conversion -> conversion.mRates.values().stream())
					.flatMap(conversionRates -> conversionRates.mRates.keySet().stream())
					.anyMatch(name -> name.equals(conversionName));
				if (!conversionExists) {
					throw new Exception("Conversion '" + conversionName + "' used by experiencinator '" + mName + "' is not defined!");
				}
			}
		}

		public boolean checkPrerequisites(Player player) {
			if (mPrerequisites != null && !mPrerequisites.prerequisiteMet(player, null)) {
				player.sendRawMessage(ChatColor.DARK_RED + mPrerequisitesFailureMessage);
				return false;
			}
			return true;
		}

		public String getName() {
			return mName;
		}

		public Map<ItemRegion, String> getConversionRates() {
			return mConversionRates;
		}

	}

	public static class Conversion {

		private final String mName;
		private final int mSettingsId;
		private final Map<ItemRegion, ConversionRates> mRates = new EnumMap<>(ItemRegion.class);
		private final Map<ItemRegion, List<ConversionResult>> mResults = new EnumMap<>(ItemRegion.class);
		private final boolean mCompressExistingResults;
		private final @Nullable QuestPrerequisites mPrerequisites;
		private final Map<ItemTier, QuestPrerequisites> mTierPrerequisites = new EnumMap<>(ItemTier.class);

		private Conversion(JsonElement element, Location lootTableLocation) throws Exception {
			JsonObject object = element.getAsJsonObject();

			JsonElement nameElement = object.get("name");
			if (nameElement != null) {
				mName = nameElement.getAsString();
			} else {
				throw new Exception("conversion missing name value!");
			}

			JsonElement settingsIdElement = object.get("settings_id");
			if (settingsIdElement != null) {
				mSettingsId = settingsIdElement.getAsInt();
			} else {
				throw new Exception("conversion missing settings_id value!");
			}

			JsonElement ratesElement = object.get("rates");
			if (ratesElement != null) {
				JsonObject ratesObject = ratesElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : ratesObject.entrySet()) {
					ItemRegion itemRegion = parseItemRegion(entry.getKey());
					ConversionRates rates = new ConversionRates(entry.getValue());
					mRates.put(itemRegion, rates);
				}
			} else {
				throw new Exception("conversion missing rates value!");
			}

			JsonElement resultsElement = object.get("results");
			if (resultsElement != null) {
				JsonObject ratesObject = resultsElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : ratesObject.entrySet()) {
					ItemRegion itemRegion = parseItemRegion(entry.getKey());
					JsonArray resultsArray = entry.getValue().getAsJsonArray();
					List<ConversionResult> conversionResults = new ArrayList<>();
					for (JsonElement resultElement : resultsArray) {
						conversionResults.add(new ConversionResult(resultElement, lootTableLocation));
					}
					mResults.put(itemRegion, conversionResults);
				}
			} else {
				throw new Exception("conversion missing rates value!");
			}

			JsonElement compressExistingResultsElement = object.get("compress_existing_results");
			if (compressExistingResultsElement != null) {
				mCompressExistingResults = compressExistingResultsElement.getAsBoolean();
			} else {
				mCompressExistingResults = false;
			}

			JsonElement prerequisitesElement = object.get("prerequisites");
			if (prerequisitesElement != null) {
				mPrerequisites = new QuestPrerequisites(prerequisitesElement);
			} else {
				mPrerequisites = null;
			}

			JsonElement tierPrerequisitesElement = object.get("tier_prerequisites");
			if (tierPrerequisitesElement != null) {
				JsonObject prerequisitesObject = tierPrerequisitesElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : prerequisitesObject.entrySet()) {
					ItemTier itemTier = parseItemTier(entry.getKey());
					QuestPrerequisites tierPrerequisites = new QuestPrerequisites(entry.getValue());
					mTierPrerequisites.put(itemTier, tierPrerequisites);
				}
			}

		}

		public String getName() {
			return mName;
		}

		public int getSettingsId() {
			return mSettingsId;
		}

		public @Nullable ConversionRates getConversionRates(ItemRegion itemRegion) {
			return mRates.get(itemRegion);
		}

		public Set<String> getConversionRateNames() {
			return mRates.values().stream().flatMap(rates -> rates.mRates.keySet().stream()).collect(Collectors.toSet());
		}

		public @Nullable List<ConversionResult> getConversionResults(ItemRegion itemRegion) {
			return mResults.get(itemRegion);
		}

		public @Nullable List<ConversionResult> getAnyConversionResult() {
			return mResults.values().stream().findFirst().orElse(null);
		}

		public boolean getCompressExistingResults() {
			return mCompressExistingResults;
		}

		public boolean conversionAllowed(Player player, ItemTier itemTier) {
			if (!conversionAllowedInGeneral(player)) {
				return false;
			}
			QuestPrerequisites tierPrerequisites = mTierPrerequisites.get(itemTier);
			return tierPrerequisites == null || tierPrerequisites.prerequisiteMet(player, null);
		}

		/**
		 * @return Whether the player has access to this conversion. Does not check any tier prerequisites.
		 */
		public boolean conversionAllowedInGeneral(Player player) {
			return mPrerequisites == null || mPrerequisites.prerequisiteMet(player, null);
		}

		private void validate(ExperiencinatorConfig config) throws Exception {
			if (!mRates.keySet().equals(mResults.keySet())) {
				throw new Exception("missing rates or result mapping in experiencinator conversions!");
			}
			if (mSettingsId <= 0 || mSettingsId > 9) {
				throw new Exception("settingsId must be a single-digit number not equal to zero!");
			}
			if (config.getConversions().stream().anyMatch(c -> c != this
				                                                   && c.mSettingsId == mSettingsId
				                                                   && c.mRates.keySet().stream().anyMatch(mRates::containsKey))) {
				throw new Exception("settingsId must be unique per region!");
			}
			if (mResults.values().stream().anyMatch(results -> results.stream().noneMatch(result -> result.mValue == 1))) {
				throw new Exception("Conversion " + mName + " does not have a result with value 1");
			}
		}

	}

	public static class ConversionRates {

		private final Map<String, Map<ItemTier, Integer>> mRates = new HashMap<>();

		private ConversionRates(JsonElement element) throws IllegalStateException {
			JsonObject object = element.getAsJsonObject();
			for (Entry<String, JsonElement> entry : object.entrySet()) {
				String conversionName = entry.getKey();
				Map<ItemTier, Integer> rates = new EnumMap<>(ItemTier.class);
				mRates.put(conversionName, rates);
				JsonObject conversionRatesObject = entry.getValue().getAsJsonObject();
				for (Entry<String, JsonElement> tierEntry : conversionRatesObject.entrySet()) {
					ItemTier itemTier = parseItemTier(tierEntry.getKey());
					int value = tierEntry.getValue().getAsInt();
					rates.put(itemTier, value);
				}
			}
		}

		public @Nullable Integer getRate(@Nullable String conversionRateName, ItemTier itemTier) {
			if (conversionRateName == null) {
				return null;
			}
			Map<ItemTier, Integer> tierMap = mRates.get(conversionRateName);
			if (tierMap == null) {
				return null;
			}
			return tierMap.get(itemTier);
		}

		public @Nullable Map<ItemTier, Integer> getRates(@Nullable String conversionRateName) {
			if (conversionRateName == null) {
				return null;
			}
			return mRates.get(conversionRateName);
		}

	}

	public static class ConversionResult {

		private final int mValue;
		private final String mName;
		private final String mNameSingular;
		private final ItemStack mItem;

		private ConversionResult(JsonElement element, Location lootTableLocation) throws Exception {
			JsonObject object = element.getAsJsonObject();

			JsonElement valueElement = object.get("value");
			if (valueElement != null) {
				mValue = valueElement.getAsInt();
			} else {
				throw new Exception("conversion result missing value value!");
			}

			JsonElement nameElement = object.get("name");
			if (nameElement != null) {
				mName = nameElement.getAsString();
			} else {
				throw new Exception("conversion result missing name value!");
			}

			JsonElement nameSingularElement = object.get("name_singular");
			if (nameSingularElement != null) {
				mNameSingular = nameSingularElement.getAsString();
			} else {
				mNameSingular = mName;
			}

			JsonElement lootTableElement = object.get("loot_table");
			if (lootTableElement != null) {
				String lootTable = lootTableElement.getAsString();
				ItemStack item = InventoryUtils.getItemFromLootTable(lootTableLocation, NamespacedKeyUtils.fromString(lootTable));
				if (item == null) {
					throw new Exception("No items in loot table '" + lootTable + "'");
				}
				mItem = item;
			} else {
				throw new Exception("conversion result missing loot_table value!");
			}

		}

		public int getValue() {
			return mValue;
		}

		public String getName() {
			return mName;
		}

		public String getNameSingular() {
			return mNameSingular;
		}

		public ItemStack getItem() {
			return ItemUtils.clone(mItem);
		}

	}

	public static class GuiConfig {
		private final Map<ItemRegion, ItemStack> mRegionIcons = new EnumMap<>(ItemRegion.class);
		private final Map<ItemTier, ItemStack> mTierIcons = new EnumMap<>(ItemTier.class);
		private final List<ItemRegion> mRegionOrder = new ArrayList<>();
		private final List<ItemTier> mTierOrder = new ArrayList<>();

		public GuiConfig(JsonElement element) throws Exception {
			JsonObject object = element.getAsJsonObject();

			JsonElement regionIconsElement = object.get("region_icons");
			if (regionIconsElement != null) {
				for (Entry<String, JsonElement> entry : regionIconsElement.getAsJsonObject().entrySet()) {
					mRegionIcons.put(parseItemRegion(entry.getKey()), ItemUtils.parseItemStack(entry.getValue().getAsString()));
				}
			} else {
				throw new Exception("settings missing region_icons value!");
			}

			JsonElement tierIconsElement = object.get("tier_icons");
			if (tierIconsElement != null) {
				for (Entry<String, JsonElement> entry : tierIconsElement.getAsJsonObject().entrySet()) {
					mTierIcons.put(parseItemTier(entry.getKey()), ItemUtils.parseItemStack(entry.getValue().getAsString()));
				}
			} else {
				throw new Exception("settings missing tier_icons value!");
			}

			JsonElement regionOrderElement = object.get("region_order");
			if (regionOrderElement != null) {
				for (JsonElement jsonElement : regionOrderElement.getAsJsonArray()) {
					mRegionOrder.add(parseItemRegion(jsonElement.getAsString()));
				}
			} else {
				throw new Exception("settings missing region_order value!");
			}

			JsonElement tierOrderElement = object.get("tier_order");
			if (tierOrderElement != null) {
				for (JsonElement jsonElement : tierOrderElement.getAsJsonArray()) {
					mTierOrder.add(parseItemTier(jsonElement.getAsString()));
				}
			} else {
				throw new Exception("settings missing tier_order value!");
			}

		}

		public @Nullable ItemStack getRegionIcon(ItemRegion itemRegion) {
			return ItemUtils.clone(mRegionIcons.get(itemRegion));
		}

		public @Nullable ItemStack getTierIcon(ItemTier itemTier) {
			return ItemUtils.clone(mTierIcons.get(itemTier));
		}

		public List<ItemRegion> getRegionOrder() {
			return mRegionOrder;
		}

		public List<ItemTier> getTierOrder() {
			return mTierOrder;
		}

	}

	public static class ScoreboardConfig {
		private final List<ItemTier> mTierOrder = new ArrayList<>();
		private final Map<ItemRegion, String> mObjectives = new EnumMap<>(ItemRegion.class);

		public ScoreboardConfig(JsonElement element) throws Exception {
			JsonObject object = element.getAsJsonObject();

			JsonElement tierOrderElement = object.get("tier_order");
			if (tierOrderElement != null) {
				for (JsonElement jsonElement : tierOrderElement.getAsJsonArray()) {
					mTierOrder.add(parseItemTier(jsonElement.getAsString()));
				}
			} else {
				throw new Exception("settings missing tier_order value!");
			}

			JsonElement objectivesElement = object.get("objectives");
			if (objectivesElement != null) {
				for (Entry<String, JsonElement> entry : objectivesElement.getAsJsonObject().entrySet()) {
					mObjectives.put(parseItemRegion(entry.getKey()), entry.getValue().getAsString());
				}
			} else {
				throw new Exception("settings missing objectives value!");
			}
		}

		public List<ItemTier> getTierOrder() {
			return mTierOrder;
		}

		public Map<ItemRegion, String> getObjectives() {
			return mObjectives;
		}
	}

	private static ItemRegion parseItemRegion(String region) throws IllegalArgumentException {
		return ItemRegion.valueOf(region.toUpperCase(Locale.ROOT));
	}

	private static ItemTier parseItemTier(String tier) throws IllegalArgumentException {
		tier = tier.toUpperCase(Locale.ROOT);
		switch (tier) {
		case "TIER_1":
			return ItemTier.ONE;
		case "TIER_2":
			return ItemTier.TWO;
		case "TIER_3":
			return ItemTier.THREE;
		case "TIER_4":
			return ItemTier.FOUR;
		case "TIER_5":
			return ItemTier.FIVE;
		default:
			return ItemTier.valueOf(tier);
		}
	}

}
