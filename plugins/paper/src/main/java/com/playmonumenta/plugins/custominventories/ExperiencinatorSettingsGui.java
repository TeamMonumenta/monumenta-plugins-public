package com.playmonumenta.plugins.custominventories;

import com.google.common.collect.Lists;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig.Conversion;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorSettings;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.ITALIC;
import static org.bukkit.ChatColor.WHITE;

/*
 * Experiencinator Settings default GUI layout:
 *
 * ================================================================
 * | back |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |
 * |   -  |   -  |   1  |   2  |   3  |   4  |   5  |   U  |   -  |
 * |   -  |  KV  |   x  |   x  |   x  |   x  |   x  |   x  |   -  |
 * |   -  |  CI  |   x  |   x  |   x  |   x  |   x  |   x  |   -  |
 * |   -  |  AR  |   x  |   x  |   x  |   x  |   x  |   x  |   -  |
 * |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |
 * ================================================================
 *
 * -: filler
 * back: back to main menu
 * KV/CI/1-5: region/tier banners, inert; U only appears after buying the upgrade for it
 * x: actual setting, clickable to cycle settings
 *
 * If Celsian Isles and/or Architect's Ring are not available for the current Experiencinator, those lines will be missing.
 *
 * The actual settings used may change the layout from this default (e.g. add more tiers).
 */
public final class ExperiencinatorSettingsGui extends CustomInventory {

	// Due to limited size, the GUI only supports up to 3 regions with up to 8 tiers each.
	private static final int MAX_REGIONS = 3; // could fit 5 actually if the first and last lines weren't used as filler
	private static final int MAX_TIERS = 8;

	private final Player mPlayer;
	private final ExperiencinatorConfig.Experiencinator mExperiencinator;
	private final ItemStack mExperiencinatorItem;
	private final ExperiencinatorConfig mConfig;
	private final LinkedHashMap<Region, Set<Tier>> mAvailableOptions; // LinkedHashMap to preserve iteration order
	private final List<Tier> mVisibleTiers;
	private final ExperiencinatorSettings mSettings;

	private ExperiencinatorSettingsGui(Player owner, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem, ExperiencinatorConfig config,
	                                   LinkedHashMap<Region, Set<Tier>> availableOptions, List<Tier> visibleTiers) {
		super(owner, (3 + availableOptions.size()) * 9, experiencinator.getName() + " Settings");

		mPlayer = owner;
		mExperiencinator = experiencinator;
		mExperiencinatorItem = experiencinatorItem;
		mConfig = config;
		mAvailableOptions = availableOptions;
		mVisibleTiers = visibleTiers;
		mSettings = new ExperiencinatorSettings(mConfig.getScoreboardConfig(), owner);

		setupInventory();

	}

	public static void showConfig(Player player, Plugin plugin, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem) {

		ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(player.getLocation());
		if (config == null) {
			return;
		}
		if (!ExperiencinatorUtils.checkExperiencinator(experiencinator, experiencinatorItem, player)) {
			return;
		}

		// calculate which options are visible
		Set<Tier> allUsedTiers = EnumSet.noneOf(Tier.class);
		LinkedHashMap<Region, Set<Tier>> availableTiers = new LinkedHashMap<>();
		for (Region region : config.getGuiConfig().getRegionOrder()) {
			String conversionRateName = experiencinator.getConversionRates().get(region);
			Set<Tier> tiers = EnumSet.noneOf(Tier.class);
			for (Conversion conversion : config.getConversions()) {
				ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
				if (conversionRates == null) {
					continue;
				}
				Map<Tier, Integer> tierRates = conversionRates.getRates(conversionRateName);
				if (tierRates == null) {
					continue;
				}
				for (Tier tier : tierRates.keySet()) {
					if (conversion.conversionAllowed(player, region, tier, experiencinatorItem)) {
						tiers.add(tier);
						allUsedTiers.add(tier);
					}
				}
			}
			if (!tiers.isEmpty()) {
				availableTiers.put(region, tiers);
				if (availableTiers.size() >= MAX_REGIONS) { // there's only so much space in this GUI
					break;
				}
			}
		}
		if (availableTiers.isEmpty()) {
			return;
		}
		List<Tier> visibleTiers = config.getGuiConfig().getTierOrder().stream()
			.filter(allUsedTiers::contains)
			.limit(MAX_TIERS)
			.collect(Collectors.toList());

		// open the GUI
		ExperiencinatorSettingsGui inv = new ExperiencinatorSettingsGui(player, experiencinator, experiencinatorItem, config, availableTiers, visibleTiers);
		inv.openInventory(player, plugin);
	}

	private void setupInventory() {

		// back button
		ItemStack backButton = new ItemStack(Material.OBSERVER);
		ItemMeta meta = backButton.getItemMeta();
		meta.setDisplayName(GRAY + "" + BOLD + "Back");
		meta.setLore(List.of(GRAY + "Return to the main menu"));
		backButton.setItemMeta(meta);
		mInventory.setItem(0, backButton);

		// setup tier icons
		int tierOffset = getRegionStartIndex(mAvailableOptions.keySet().iterator().next()) - 8;
		for (int i = 0; i < mVisibleTiers.size() && i < MAX_TIERS; i++) {
			mInventory.setItem(tierOffset + i, mConfig.getGuiConfig().getTierIcon(mVisibleTiers.get(i)));
		}

		// setup region icons + region options
		for (Map.Entry<Region, Set<Tier>> entry : mAvailableOptions.entrySet()) {
			setupRegionOptions(entry.getKey(), entry.getValue());
		}

		// fill empty slots with filler
		GUIUtils.fillWithFiller(mInventory, Material.GRAY_STAINED_GLASS_PANE);
	}

	private void setupRegionOptions(Region region, Set<Tier> availableTiers) {
		int offset = getRegionStartIndex(region);

		// region icon
		mInventory.setItem(offset, mConfig.getGuiConfig().getRegionIcon(region));

		// options
		for (int i = 0; i < mVisibleTiers.size() && i < MAX_TIERS; i++) {
			Tier tier = mVisibleTiers.get(i);
			if (!availableTiers.contains(tier)) {
				continue;
			}
			int conversionId = mSettings.getConversion(region, tier);
			if (conversionId == 0) {
				ItemStack item = new ItemStack(Material.BARRIER, 1);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(GRAY + "Disabled");
				meta.setLore(List.of(AQUA + "Will not convert ",
					WHITE + "" + region.getPlainDisplay() + tier.getPlainDisplay() + AQUA + " items."));
				item.setItemMeta(meta);
				mInventory.setItem(offset + 1 + i, item);
			} else {
				Conversion conversion = mConfig.findConversion(conversionId, region);
				if (conversion == null) {
					continue;
				}
				List<ExperiencinatorConfig.ConversionResult> conversionResults = conversion.getConversionResults(region);
				if (conversionResults == null || conversionResults.isEmpty()) {
					continue;
				}
				ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
				if (conversionRates == null) {
					continue;
				}
				String conversionRateName = mExperiencinator.getConversionRates().get(region);
				Integer rawAmount = conversionRates.getRate(conversionRateName, tier);
				if (rawAmount == null) { // current Experiencinator does not support this option: display it with an unspecified amount
					rawAmount = -1;
				}

				ItemStack item;
				ExperiencinatorConfig.ConversionResult conversionResult;
				if (rawAmount > 0
					    && conversionResults.size() >= 2
					    && rawAmount % conversionResults.get(1).getValue() == 0) {
					conversionResult = conversionResults.get(1);
					item = new ItemStack(conversionResult.getItem().getType(), rawAmount / conversionResult.getValue());
				} else {
					conversionResult = conversionResults.get(0);
					item = new ItemStack(conversionResult.getItem().getType(), rawAmount < 0 ? 1 : rawAmount);
				}
				ItemUtils.setPlainName(item, ItemUtils.getPlainNameIfExists(conversionResult.getItem()));

				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(WHITE + "Convert to " + GOLD + conversion.getName());
				List<String> lore = new ArrayList<>();
				lore.add(AQUA + "Will convert " + WHITE + region.getPlainDisplay() + tier.getPlainDisplay());
				lore.add(AQUA + "items to " + GOLD + (rawAmount < 0 ? "" : item.getAmount() + " ") + (rawAmount > 0 && item.getAmount() == 1 ? conversionResult.getNameSingular() : conversionResult.getName()) + AQUA + ".");
				if (rawAmount < 0) {
					lore.add("" + GRAY + ITALIC + "This conversion is not available");
					lore.add("" + GRAY + ITALIC + "for the " + mExperiencinator.getName() + ",");
					lore.add("" + GRAY + ITALIC + "thus it will not convert items of this type.");
				}
				meta.setLore(lore);
				meta.addItemFlags(ItemFlag.values());
				item.setItemMeta(meta);
				ItemUtils.setPlainLore(item, List.of("This is a placeholder item."));

				mInventory.setItem(offset + 1 + i, item);
			}
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory) {
			return;
		}
		if (event.getClick() != ClickType.RIGHT
			    && event.getClick() != ClickType.LEFT) {
			return;
		}

		if (event.getSlot() == 0) {
			close();
			ExperiencinatorMainGui.show(mPlayer, getPlugin(), mExperiencinator, mExperiencinatorItem);
			return;
		}

		for (Map.Entry<Region, Set<Tier>> entry : mAvailableOptions.entrySet()) {
			Region region = entry.getKey();
			int tierIndex = event.getSlot() - getRegionStartIndex(region) - 1;
			if (tierIndex >= 0 && tierIndex < mVisibleTiers.size()) {
				Tier tier = mVisibleTiers.get(tierIndex);
				cycleOption(region, tier, event.getClick() != ClickType.RIGHT);
				setupInventory();
				return;
			}
		}

	}

	private int getRegionStartIndex(Region region) {
		int regionIndex = List.copyOf(mAvailableOptions.keySet()).indexOf(region);
		// The first region starts at the third row (+18), and each region occupies 1 row (+9)
		return 18 + regionIndex * 9
			       // Regions are horizontally centered (as far as possible)
			       + (4 - (mVisibleTiers.size() + 1) / 2);
	}

	/**
	 * Cycles through currently valid conversion settings for a given item region and tier.
	 */
	private void cycleOption(Region region, Tier tier, boolean forwards) {
		int currentConversion = mSettings.getConversion(region, tier);
		List<Conversion> conversions = List.copyOf(mConfig.getConversions());
		if (!forwards) {
			conversions = Lists.reverse(conversions);
		}

		// go through the list of conversions and find the first valid one after the currently active one
		boolean currentFound = currentConversion == 0; // If currently disabled, switch to the first one
		for (Conversion conversion : conversions) {
			if (!isValidConversion(conversion, region, tier)) {
				continue;
			}
			if (conversion.getSettingsId() == currentConversion) {
				currentFound = true;
			} else if (currentFound) {
				mSettings.setConversion(region, tier, conversion.getSettingsId());
				return;
			}
		}

		// If we're at the end of the list (current is last or was not found, e.g. because it is not currently valid), switch back to disabled
		mSettings.setConversion(region, tier, 0);
	}

	private boolean isValidConversion(Conversion conversion, Region region, Tier tier) {
		if (!conversion.conversionAllowed(mPlayer, region, tier, mExperiencinatorItem)) {
			return false;
		}
		ExperiencinatorConfig.ConversionRates conversionRates = conversion.getConversionRates(region);
		if (conversionRates == null) {
			return false;
		}
		String conversionRateName = mExperiencinator.getConversionRates().get(region);
		return conversionRates.getRate(conversionRateName, tier) != null;
	}

}
