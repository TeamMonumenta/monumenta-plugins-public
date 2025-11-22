package com.playmonumenta.plugins.commands.experiencinator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for Experiencinators, stored in a JSON file for quick & easy changes.
 */
public final class ExperiencinatorConfig {

	public static final String DEFAULT_RESULT = "default";

	private final List<Experiencinator> mExperiencinators = new ArrayList<>();
	private final Map<String, Conversion> mConversions = new LinkedHashMap<>(); // LinkedHashMap to preserve iteration order
	private final GuiConfig mGuiConfig;
	private final ScoreboardConfig mScoreboardConfig;

	ExperiencinatorConfig() {
		mGuiConfig = new GuiConfig();
		mScoreboardConfig = new ScoreboardConfig();
	}

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
				String conversionId = entry.getKey();
				Conversion conversion = new Conversion(conversionId, entry.getValue(), lootTableLocation);
				mConversions.put(conversionId, conversion);
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

	public @Nullable Conversion findConversion(int conversionSettingsId, Region region) {
		if (conversionSettingsId <= 0) {
			return null;
		}
		for (Conversion conversion : mConversions.values()) {
			if (conversion.getSettingsId() == conversionSettingsId
				&& conversion.getConversionRates(region) != null) {
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
		private final Map<Region, String> mConversionRates = new EnumMap<>(Region.class);
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
					Region region = parseItemRegion(entry.getKey());
					mConversionRates.put(region, entry.getValue().getAsString());
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

		public boolean checkPrerequisites(Player player, ItemStack experiencinatorItem) {
			if (mPrerequisites != null && !new QuestContext(Plugin.getInstance(), player, null, false, mPrerequisites, experiencinatorItem).prerequisitesMet()) {
				player.sendMessage(Component.text(Objects.requireNonNull(mPrerequisitesFailureMessage), NamedTextColor.RED)); // message is not null if mPrerequisites is not null
				return false;
			}
			return true;
		}

		public String getName() {
			return mName;
		}

		public Map<Region, String> getConversionRates() {
			return mConversionRates;
		}

	}

	public static class Conversion {

		private final String mId;
		private final String mName;
		private final @Nullable String mCombinedName;
		private final int mSettingsId;
		private final Map<Region, ConversionRates> mRates = new EnumMap<>(Region.class);
		private final Map<Region, Map<String, List<ConversionResult>>> mResults = new EnumMap<>(Region.class);
		private final boolean mCompressExistingResults;
		private final @Nullable QuestPrerequisites mPrerequisites;
		private final Map<Region, QuestPrerequisites> mRegionPrerequisites = new EnumMap<>(Region.class);
		private final Map<Tier, QuestPrerequisites> mTierPrerequisites = new EnumMap<>(Tier.class);

		private Conversion(String id, JsonElement element, Location lootTableLocation) throws Exception {
			mId = id;

			JsonObject object = element.getAsJsonObject();

			JsonElement nameElement = object.get("name");
			if (nameElement != null) {
				mName = nameElement.getAsString();
			} else {
				throw new Exception("conversion missing name value!");
			}

			mCombinedName = JsonUtils.getString(object, "combined_name", null);

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
					Region region = parseItemRegion(entry.getKey());
					ConversionRates rates = new ConversionRates(entry.getValue());
					mRates.put(region, rates);
				}
			} else {
				throw new Exception("conversion missing rates value!");
			}

			JsonElement resultsElement = object.get("results");
			if (resultsElement != null) {
				JsonObject ratesObject = resultsElement.getAsJsonObject();
				for (Entry<String, JsonElement> ratesEntry : ratesObject.entrySet()) {
					Region region = parseItemRegion(ratesEntry.getKey());
					if (ratesEntry.getValue().isJsonArray()) {
						JsonArray resultsArray = ratesEntry.getValue().getAsJsonArray();
						List<ConversionResult> conversionResults = new ArrayList<>();
						for (JsonElement resultElement : resultsArray) {
							conversionResults.add(new ConversionResult(resultElement, lootTableLocation));
						}
						mResults.put(region, Map.of(DEFAULT_RESULT, conversionResults));
					} else {
						JsonObject resultsObject = ratesEntry.getValue().getAsJsonObject();
						Map<String, List<ConversionResult>> results = new HashMap<>();
						for (Entry<String, JsonElement> resultEntry : resultsObject.entrySet()) {
							String tag = resultEntry.getKey();
							JsonArray resultsArray = resultEntry.getValue().getAsJsonArray();
							List<ConversionResult> conversionResults = new ArrayList<>();
							for (JsonElement resultElement : resultsArray) {
								conversionResults.add(new ConversionResult(resultElement, lootTableLocation));
							}
							results.put(tag, conversionResults);
						}
						mResults.put(region, results);
					}
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

			JsonElement regionPrerequisitesElement = object.get("region_prerequisites");
			if (regionPrerequisitesElement != null) {
				JsonObject prerequisitesObject = regionPrerequisitesElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : prerequisitesObject.entrySet()) {
					Region region = parseItemRegion(entry.getKey());
					QuestPrerequisites regionPrerequisites = new QuestPrerequisites(entry.getValue());
					mRegionPrerequisites.put(region, regionPrerequisites);
				}
			}

			JsonElement tierPrerequisitesElement = object.get("tier_prerequisites");
			if (tierPrerequisitesElement != null) {
				JsonObject prerequisitesObject = tierPrerequisitesElement.getAsJsonObject();
				for (Entry<String, JsonElement> entry : prerequisitesObject.entrySet()) {
					Tier tier = parseItemTier(entry.getKey());
					QuestPrerequisites tierPrerequisites = new QuestPrerequisites(entry.getValue());
					mTierPrerequisites.put(tier, tierPrerequisites);
				}
			}

		}

		public String getId() {
			return mId;
		}

		public String getName() {
			return mName;
		}

		public @Nullable String getCombinedName() {
			return mCombinedName;
		}

		public int getSettingsId() {
			return mSettingsId;
		}

		public @Nullable ConversionRates getConversionRates(Region region) {
			return mRates.get(region);
		}

		public Set<String> getConversionRateNames() {
			return mRates.values().stream().flatMap(rates -> rates.mRates.keySet().stream()).collect(Collectors.toSet());
		}

		public @Nullable List<ConversionResult> getConversionResults(Region region, String resultId) {
			Map<String, List<ConversionResult>> conversionResults = mResults.get(region);
			return conversionResults == null ? null : conversionResults.get(resultId);
		}

		public @Nullable List<ConversionResult> getAnyConversionResult() {
			return mResults.values().stream().findFirst().flatMap(m -> m.values().stream().findFirst()).orElse(null);
		}

		public boolean getCompressExistingResults() {
			return mCompressExistingResults;
		}

		public boolean conversionAllowed(Player player, Region region, Tier tier, ItemStack experiencinatorItem) {
			if (!conversionAllowedInGeneral(player, experiencinatorItem)) {
				return false;
			}
			QuestPrerequisites regionPrerequisites = mRegionPrerequisites.get(region);
			QuestPrerequisites tierPrerequisites = mTierPrerequisites.get(tier);
			return (regionPrerequisites == null || new QuestContext(Plugin.getInstance(), player, null, false, regionPrerequisites, experiencinatorItem).prerequisitesMet())
				&& (tierPrerequisites == null || new QuestContext(Plugin.getInstance(), player, null, false, tierPrerequisites, experiencinatorItem).prerequisitesMet());
		}

		/**
		 * Whether the player has access to this conversion. Does not check any tier prerequisites.
		 */
		public boolean conversionAllowedInGeneral(Player player, ItemStack experiencinatorItem) {
			return mPrerequisites == null || new QuestContext(Plugin.getInstance(), player, null, false, mPrerequisites, experiencinatorItem).prerequisitesMet();
		}

		private void validate(ExperiencinatorConfig config) throws Exception {
			if (!mRates.keySet().equals(mResults.keySet())) {
				throw new Exception("missing rates or result mapping in experiencinator conversions!");
			}
			if (!(mSettingsId == -1 || (1 <= mSettingsId && mSettingsId <= 9))) {
				throw new Exception("settingsId must be a single-digit number greater than zero, or -1!");
			}
			if (mSettingsId > 0 && config.getConversions().stream().anyMatch(c -> c != this
				&& c.mSettingsId == mSettingsId
				&& c.mRates.keySet().stream().anyMatch(mRates::containsKey))) {
				throw new Exception("settingsId must be unique per region!");
			}
			if (mResults.values().stream().anyMatch(results -> results.values().stream().anyMatch(r -> r.stream().noneMatch(result -> result.mValue == 1)))) {
				throw new Exception("Conversion " + mName + " does not have a result with value 1");
			}
		}

	}

	public static class ConversionRates {

		private final Map<String, Map<Tier, ConversionRate>> mRates = new HashMap<>();

		private ConversionRates(JsonElement element) throws Exception {
			JsonObject object = element.getAsJsonObject();
			for (Entry<String, JsonElement> entry : object.entrySet()) {
				String conversionName = entry.getKey();
				Map<Tier, ConversionRate> rates = new EnumMap<>(Tier.class);
				mRates.put(conversionName, rates);
				JsonObject conversionRatesObject = entry.getValue().getAsJsonObject();
				for (Entry<String, JsonElement> tierEntry : conversionRatesObject.entrySet()) {
					Tier tier = parseItemTier(tierEntry.getKey());
					ConversionRate value;
					if (tierEntry.getValue().isJsonPrimitive()) {
						value = new StaticConversionRate(tierEntry.getValue().getAsInt());
					} else {
						Entry<String, JsonElement> rate = tierEntry.getValue().getAsJsonObject().entrySet().iterator().next();
						value = new DynamicConversionRate(rate.getKey(), rate.getValue().getAsJsonObject());
					}
					rates.put(tier, value);
				}
			}
		}

		public @Nullable ConversionRate getRate(@Nullable String conversionRateName, Tier tier) {
			if (conversionRateName == null) {
				return null;
			}
			Map<Tier, ConversionRate> tierMap = mRates.get(conversionRateName);
			if (tierMap == null) {
				return null;
			}
			return tierMap.get(tier);
		}

		public @Nullable Map<Tier, ConversionRate> getRates(@Nullable String conversionRateName) {
			if (conversionRateName == null) {
				return null;
			}
			return mRates.get(conversionRateName);
		}

	}

	public interface ConversionRate {
		String getDescription();

		ConversionRateResult getValue(ItemStack item);
	}

	public static class StaticConversionRate implements ConversionRate {
		public final int mRate;

		private StaticConversionRate(int rate) {
			mRate = rate;
		}

		@Override
		public String getDescription() {
			return "" + mRate;
		}

		@Override
		public ConversionRateResult getValue(ItemStack item) {
			return new ConversionRateResult(mRate, DEFAULT_RESULT);
		}
	}

	public static class DynamicConversionRate implements ConversionRate {
		private final String[] mTagPath;
		private final Map<String, ConversionRateResult> mRates = new HashMap<>();

		private DynamicConversionRate(String tagPath, JsonObject json) throws Exception {
			mTagPath = tagPath.split("\\.");
			for (Entry<String, JsonElement> e : json.entrySet()) {
				mRates.put(e.getKey(), new ConversionRateResult(e.getValue()));
			}
		}

		@Override
		public String getDescription() {
			return mRates.values().stream().mapToInt(i -> i.mCount).min().orElse(0) + " to " + mRates.values().stream().mapToInt(i -> i.mCount).max().orElse(0);
		}

		@Override
		public ConversionRateResult getValue(ItemStack item) {
			NBTCompound nbt = new NBTItem(item);
			for (int i = 0; i < mTagPath.length - 1; i++) {
				String tag = mTagPath[i];
				nbt = nbt.getCompound(tag);
				if (nbt == null) {
					return ConversionRateResult.EMPTY;
				}
			}
			String leaf = mTagPath[mTagPath.length - 1];
			NBTType type = nbt.getType(leaf);
			if (type == null) {
				return ConversionRateResult.EMPTY;
			}
			String tagValue = type == NBTType.NBTTagString ? nbt.getString(leaf) : "" + nbt.getInteger(leaf);
			return mRates.getOrDefault(tagValue, ConversionRateResult.EMPTY);
		}
	}

	public static class ConversionRateResult {

		public static final ConversionRateResult EMPTY = new ConversionRateResult(0, DEFAULT_RESULT);

		public final int mCount;
		public final String mResult;

		private ConversionRateResult(JsonElement element) throws Exception {
			if (element.isJsonPrimitive()) {
				mCount = element.getAsInt();
				mResult = DEFAULT_RESULT;
			} else {
				mCount = JsonUtils.getInt((JsonObject) element, "count");
				mResult = Objects.requireNonNull(JsonUtils.getString((JsonObject) element, "result", DEFAULT_RESULT));
			}
		}

		private ConversionRateResult(int count, String result) {
			mCount = count;
			mResult = result;
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

			JsonElement lootTableElement = object.get("loot_table");
			if (lootTableElement != null) {
				String lootTable = lootTableElement.getAsString();
				ItemStack item = InventoryUtils.getItemFromLootTable(lootTableLocation, NamespacedKeyUtils.fromString(lootTable));
				if (item == null || item.getType() == Material.AIR) {
					throw new Exception("No items in loot table '" + lootTable + "'");
				}
				mItem = item;
			} else {
				throw new Exception("conversion result missing loot_table value!");
			}

			String itemName = ItemUtils.getPlainNameIfExists(mItem);

			JsonElement nameElement = object.get("name");
			if (nameElement != null) {
				mName = nameElement.getAsString();
			} else if (itemName != null) {
				mName = itemName + "s";
			} else {
				throw new Exception("conversion result missing name value!");
			}

			JsonElement nameSingularElement = object.get("name_singular");
			if (nameSingularElement != null) {
				mNameSingular = nameSingularElement.getAsString();
			} else if (nameElement == null) {
				mNameSingular = itemName;
			} else {
				mNameSingular = mName;
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
		private final Map<Region, ItemStack> mRegionIcons = new EnumMap<>(Region.class);
		private final Map<Tier, ItemStack> mTierIcons = new EnumMap<>(Tier.class);
		private final List<Region> mRegionOrder = new ArrayList<>();
		private final List<Tier> mTierOrder = new ArrayList<>();

		private GuiConfig() {
		}

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

		public @Nullable ItemStack getRegionIcon(Region region) {
			return ItemUtils.clone(mRegionIcons.get(region));
		}

		public @Nullable ItemStack getTierIcon(Tier tier) {
			return ItemUtils.clone(mTierIcons.get(tier));
		}

		public List<Region> getRegionOrder() {
			return mRegionOrder;
		}

		public List<Tier> getTierOrder() {
			return mTierOrder;
		}

	}

	public static class ScoreboardConfig {
		private final List<Tier> mTierOrder = new ArrayList<>();
		private final Map<Region, String> mObjectives = new EnumMap<>(Region.class);

		private ScoreboardConfig() {
		}

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

		public List<Tier> getTierOrder() {
			return mTierOrder;
		}

		public Map<Region, String> getObjectives() {
			return mObjectives;
		}
	}

	private static Region parseItemRegion(String region) throws IllegalArgumentException {
		return Region.valueOf(region.toUpperCase(Locale.ROOT));
	}

	private static Tier parseItemTier(String tier) throws IllegalArgumentException {
		tier = tier.toUpperCase(Locale.ROOT);
		return switch (tier) {
			case "TIER_1" -> Tier.I;
			case "TIER_2" -> Tier.II;
			case "TIER_3" -> Tier.III;
			case "TIER_4" -> Tier.IV;
			case "TIER_5" -> Tier.V;
			default -> Tier.valueOf(tier);
		};
	}

}
