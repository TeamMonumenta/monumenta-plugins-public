package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.QuiverListener;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class ItemStatUtils {

	public static final String MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME = "MMDummy";
	public static final String MONUMENTA_KEY = "Monumenta";
	public static final String LORE_KEY = "Lore";
	public static final String STOCK_KEY = "Stock";
	public static final String PLAYER_MODIFIED_KEY = "PlayerModified";
	public static final String LEVEL_KEY = "Level";
	public static final String INFUSER_KEY = "Infuser";
	public static final String ATTRIBUTE_NAME_KEY = "AttributeName";
	public static final String CHARM_KEY = "CharmText";
	public static final String CHARM_POWER_KEY = "CharmPower";
	public static final String FISH_QUALITY_KEY = "FishQuality";
	public static final String AMOUNT_KEY = "Amount";
	public static final String EFFECT_TYPE_KEY = "EffectType";
	public static final String EFFECT_DURATION_KEY = "EffectDuration";
	public static final String EFFECT_STRENGTH_KEY = "EffectStrength";
	public static final String EFFECT_SOURCE_KEY = "EffectSource";
	public static final String DIRTY_KEY = "Dirty";
	public static final String SHULKER_SLOTS_KEY = "ShulkerSlots";
	public static final String CUSTOM_INVENTORY_TYPES_LIMIT_KEY = "CustomInventoryTypesLimit";
	public static final String CUSTOM_INVENTORY_TOTAL_ITEMS_LIMIT_KEY = "CustomInventoryTotalItemsLimit";
	public static final String CUSTOM_INVENTORY_ITEMS_PER_TYPE_LIMIT_KEY = "CustomInventoryItemsPerTypeLimit";
	public static final String IS_QUIVER_KEY = "IsQuiver";
	public static final String QUIVER_ARROW_TRANSFORM_MODE_KEY = "ArrowTransformMode";
	public static final String CHARGES_KEY = "Charges";
	public static final String ITEMS_KEY = "Items";
	public static final String VANITY_ITEMS_KEY = "VanityItems";
	public static final String PLAYER_CUSTOM_NAME_KEY = "PlayerCustomName";
	public static final String CUSTOM_SKIN_KEY = "CustomSkin";

	public static final Component DUMMY_LORE_TO_REMOVE = Component.text("DUMMY LORE TO REMOVE", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);

	public static void applyCustomEffects(Plugin plugin, Player player, ItemStack item) {
		applyCustomEffects(plugin, player, item, true);
	}

	public static void applyCustomEffects(Plugin plugin, Player player, ItemStack item, boolean applySickness) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		if (player.isDead() || !player.isValid()) {
			return;
		}

		// Ensure other effects don't apply
		if (item.getItemMeta() instanceof PotionMeta potionMeta) {
			if (ItemStatUtils.hasConsumeEffect(item)) {
				// If it's a custom potion, remove all effects
				potionMeta.clearCustomEffects();
				potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			} else {
				// If it's a vanilla potion, remove positive effects (Ensure legacies stay unusable)
				PotionUtils.removePositiveEffects(potionMeta);
				if (PotionUtils.hasPositiveEffects(potionMeta.getBasePotionData().getType().getEffectType())) {
					// If base potion is vanilla positive potion, set to AWKWARD, otherwise keep (ensures negative effects remain)
					potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
				}
			}
			item.setItemMeta(potionMeta);
		}

		NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return;
			}

			double quenchScale = Quench.getDurationScaling(plugin, player);

			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);
				int duration = effect.getInteger(EFFECT_DURATION_KEY);
				double strength = effect.getDouble(EFFECT_STRENGTH_KEY);

				int modifiedDuration = (int) (duration * quenchScale);

				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					if (effectType == EffectType.ABSORPTION) {
						double sicknessPenalty = 0;
						NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "AbsorptionSickness");
						if (sicks != null) {
							Effect sick = sicks.last();
							sicknessPenalty = sick.getMagnitude();
						}
						EffectType.applyEffect(effectType, player, modifiedDuration, strength * (1 - sicknessPenalty), null, applySickness);
					} else if (effectType == EffectType.INSTANT_HEALTH) {
						double sicknessPenalty = 0;
						NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "HealingSickness");
						if (sicks != null) {
							Effect sick = sicks.last();
							sicknessPenalty = sick.getMagnitude();
						}
						EffectType.applyEffect(effectType, player, modifiedDuration, strength * (1 - sicknessPenalty), null, applySickness);
					} else {
						EffectType.applyEffect(effectType, player, modifiedDuration, strength, null, applySickness);
					}
				}
			}
		});
	}

	public static void changeEffectsDuration(Player player, ItemStack item, int durationChange) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return;
			}

			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);
				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					int duration = effect.getInteger(EFFECT_DURATION_KEY);
					double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
					EffectType.applyEffect(effectType, player, duration + durationChange, strength, null, false);
				}
			}
		});
	}

	public static void changeDurationAndStrengths(Player player, ItemStack item, int durationChange, ImmutableMap<String, Double> strengthChanges) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return;
			}


			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);
				int duration = effect.getInteger(EFFECT_DURATION_KEY);
				double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
				double strengthChange = strengthChanges.getOrDefault(type, 0.0);

				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					EffectType.applyEffect(effectType, player, duration + durationChange, strength + strengthChange, null, false);
				}
			}
		});
	}

	public static void changeEffectsDurationSplash(Player player, ItemStack item, double scale) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return;
			}

			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);
				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					int duration = effect.getInteger(EFFECT_DURATION_KEY);
					double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
					EffectType.applyEffect(effectType, player, (int) (duration * scale), strength, null, false);
				}
			}
		});
	}

	public static boolean hasConsumeEffect(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return false;
			}

			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);

				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					return true;
				}
			}

			return false;
		});
	}

	public static boolean hasNegativeEffect(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);

			if (effects == null || effects.isEmpty()) {
				return false;
			}

			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);

				EffectType effectType = EffectType.fromType(type);
				if (effectType != null && !effectType.isPositive() && effectType != EffectType.CLUCKING) {
					return true;
				}
			}

			return false;
		});
	}

	public static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.###");
	public static final DecimalFormat NUMBER_CHANGE_FORMATTER = new DecimalFormat("+0.###;-0.###");
	public static final DecimalFormat PERCENT_CHANGE_FORMATTER = new DecimalFormat("+0.###%;-0.###%");

	public static void editItemInfo(final ItemStack item, final Region region, final Tier tier, final Masterwork masterwork, final Location location) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getOrCreateCompound(MONUMENTA_KEY);
			if (region == Region.NONE) {
				monumenta.removeKey(Region.KEY);
			} else {
				monumenta.setString(Region.KEY, region.getName());
			}

			if (tier == Tier.NONE) {
				monumenta.removeKey(Tier.KEY);
			} else {
				monumenta.setString(Tier.KEY, tier.getName());
			}

			if (masterwork == Masterwork.NONE) {
				monumenta.removeKey(Masterwork.KEY);
			} else {
				monumenta.setString(Masterwork.KEY, masterwork.getName());
			}

			if (location == Location.NONE) {
				monumenta.removeKey(Location.KEY);
			} else {
				monumenta.setString(Location.KEY, location.getName());
			}

		});
	}

	public static void addLore(final ItemStack item, final int index, final Component line) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBTList<String> lore = nbt.getOrCreateCompound(MONUMENTA_KEY).getStringList(LORE_KEY);
			String serializedLine = MessagingUtils.toGson(line).toString();
			if (index < lore.size()) {
				lore.add(index, serializedLine);
			} else {
				lore.add(serializedLine);
			}
		});
	}

	public static void removeLore(final ItemStack item, final int index) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}
			ReadWriteNBTList<String> lore = monumenta.getStringList(LORE_KEY);
			if (!lore.isEmpty()) {
				if (index < lore.size()) {
					lore.remove(index);
				} else {
					lore.remove(lore.size() - 1);
				}
			}
			if (lore.isEmpty()) {
				monumenta.removeKey(LORE_KEY);
				item.lore(null);
			}
		});
	}

	public static void clearLore(final ItemStack item) {
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}
			monumenta.removeKey(LORE_KEY);
		});
		item.lore(null);
	}

	public static List<Component> getLore(final ItemStack item) {
		return NBT.get(item, nbt -> {
			return getLore(nbt);
		});
	}

	public static List<Component> getLore(final ReadableNBT nbt) {
		List<Component> lore = new ArrayList<>();
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return lore;
		}
		for (String serializedLine : monumenta.getStringList(LORE_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}

	public static List<String> getPlainLore(final ReadableNBT nbt) {
		List<String> plainLore = new ArrayList<>();
		for (Component line : getLore(nbt)) {
			plainLore.add(MessagingUtils.plainText(line));
		}
		return plainLore;
	}

	public static void addCharmEffect(final ItemStack item, final int index, final Component line) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBTList<String> charmLore = nbt.getOrCreateCompound(MONUMENTA_KEY).getStringList(CHARM_KEY);
			String serializedLine = MessagingUtils.toGson(line).toString();
			if (index < charmLore.size()) {
				charmLore.add(index, serializedLine);
			} else {
				charmLore.add(serializedLine);
			}
		});
	}

	public static void removeCharmEffect(final ItemStack item, final int index) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBTList<String> lore = nbt.getOrCreateCompound(MONUMENTA_KEY).getStringList(CHARM_KEY);
			if (lore.size() > 0 && index < lore.size()) {
				lore.remove(index);
			} else if (lore.size() > 0) {
				lore.remove(lore.size() - 1);
			}
		});
	}

	public static List<Component> getCharmEffects(final ItemStack item) {
		return NBT.get(item, nbt -> {
			return getCharmEffects(nbt);
		});
	}

	public static List<Component> getCharmEffects(final ReadableNBT nbt) {
		List<Component> lore = new ArrayList<>();
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return lore;
		}
		for (String serializedLine : monumenta.getStringList(CHARM_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}

	public static List<String> getPlainCharmLore(final ReadableNBT nbt) {
		List<String> plainLore = new ArrayList<>();
		for (Component line : getCharmEffects(nbt)) {
			plainLore.add(MessagingUtils.plainText(line));
		}
		return plainLore;
	}

	public static void setCharmPower(final ItemStack item, final int level) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT charmPower = nbt.getOrCreateCompound(MONUMENTA_KEY);
			charmPower.setInteger(CHARM_POWER_KEY, level);
		});
	}

	public static void removeCharmPower(final ItemStack item) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT charmPower = nbt.getCompound(CHARM_POWER_KEY);
			if (charmPower == null) {
				return;
			}
			charmPower.removeKey(CHARM_POWER_KEY);
		});
	}

	public static void setFishQuality(final ItemStack item, final int level) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
				ReadWriteNBT fishQuality = nbt.getOrCreateCompound(MONUMENTA_KEY);
				fishQuality.setInteger(FISH_QUALITY_KEY, level);
		});
	}

	public static void removeFishQuality(final ItemStack item) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}
			monumenta.removeKey(FISH_QUALITY_KEY);
		});
	}

	public static int getCharmPower(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return NBT.get(item, nbt -> {
			return getCharmPower(nbt);
		});
	}

	public static int getCharmPower(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return 0;
		}
		return monumenta.getInteger(CHARM_POWER_KEY);
	}

	public static int getFishQuality(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return NBT.get(item, nbt -> {
			return getFishQuality(nbt);
		});
	}

	public static int getFishQuality(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return 0;
		}
		return monumenta.getInteger(FISH_QUALITY_KEY);
	}

	public static @Nullable PlayerClass getCharmClass(@Nullable ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}
		if (isZenithCharm(itemStack)) {
			return null;
		}
		return NBT.get(itemStack, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return null;
			}
			if (!monumenta.hasTag(CHARM_KEY)) {
				return null;
			}
			return getCharmClass(monumenta.getStringList(CHARM_KEY));
		});
	}

	public static @Nullable PlayerClass getCharmClass(@Nullable ReadableNBTList<String> charmLore) {
		if (charmLore == null) {
			return null;
		}
		List<PlayerClass> classes = new MonumentaClasses().getClasses();

		for (String line : charmLore) {
			for (PlayerClass playerClass : classes) {
				List<AbilityInfo<?>> abilities = new ArrayList<>();
				abilities.addAll(playerClass.mAbilities);
				abilities.addAll(playerClass.mSpecOne.mAbilities);
				abilities.addAll(playerClass.mSpecTwo.mAbilities);

				List<String> abilityNames = new ArrayList<>();
				abilityNames.add(playerClass.mClassPassiveName);
				abilities.forEach(a -> abilityNames.add(a.getDisplayName()));

				for (String name : abilityNames) {
					if (line.contains(name)) {
						return playerClass;
					}
				}
				if (line.contains(playerClass.mClassPassiveName)) {
					return playerClass;
				}
			}
			// The real ability name is "Alchemist Potions", but charms don't use the "s"
			if (line.contains("Alchemist Potion")) {
				return new Alchemist();
			}
			if (line.contains("Fire Elemental Spirit") || line.contains("Ice Elemental Spirit")) {
				return new Mage();
			}
		}
		return null;
	}

	public static Component getCharmClassComponent(ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta != null) {
			ReadableNBTList<String> charmLore = monumenta.getStringList(CHARM_KEY);
			PlayerClass playerClass = getCharmClass(charmLore);
			if (playerClass != null) {
				return Component.text(playerClass.mClassName, playerClass.mClassColor).decoration(TextDecoration.ITALIC, false);
			}
		}
		return Component.text("Generalist", TextColor.fromHexString("#9F8F91"));
	}

	public static void addConsumeEffect(final ItemStack item, final EffectType type, final double strength, final int duration) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBT.modify(item, nbt -> {
			ReadWriteNBTCompoundList effects = nbt.getOrCreateCompound(MONUMENTA_KEY).getOrCreateCompound(STOCK_KEY).getCompoundList(EffectType.KEY);
			ReadWriteNBT effect = effects.addCompound();
			effect.setString(EFFECT_TYPE_KEY, type.getType());
			effect.setDouble(EFFECT_STRENGTH_KEY, strength);
			effect.setInteger(EFFECT_DURATION_KEY, duration);
		});

		ItemUpdateHelper.generateItemStats(item);
	}

	public static void removeConsumeEffect(final ItemStack item, final EffectType type) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		Boolean success = NBT.modify(item, nbt -> {
			ReadWriteNBTCompoundList effects = getEffects(nbt);
			if (effects == null || effects.isEmpty()) {
				return false;
			}
			int i = 0;
			for (ReadWriteNBT effect : effects) {
				if (type.getType().equals(effect.getString(EFFECT_TYPE_KEY))) {
					effects.remove(i);
					break;
				}
				i++;
			}
			return true;
		});

		if (!success) {
			return;
		}

		ItemUpdateHelper.generateItemStats(item);
	}


	public static void removeConsumeEffect(final ItemStack item, final int i) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		Boolean success = NBT.modify(item, nbt -> {
			ReadWriteNBTCompoundList effects = getEffects(nbt);
			if (effects == null || effects.isEmpty()) {
				return false;
			}
			effects.remove(i);
			return true;
		});

		if (!success) {
			return;
		}
		ItemUpdateHelper.generateItemStats(item);
	}

	public static boolean isConsumable(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> effects = getEffects(nbt);
			if (effects == null || effects.isEmpty()) {
				return false;
			}
			return true;
		});
	}

	public static @Nullable ReadWriteNBTCompoundList getEffects(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadWriteNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(EffectType.KEY);
	}

	public static @Nullable ReadableNBTList<ReadWriteNBT> getEffects(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadableNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(EffectType.KEY);
	}

	public static @Nullable ReadWriteNBT getEnchantments(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadWriteNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompound(EnchantmentType.KEY);
	}

	public static @Nullable ReadableNBT getEnchantments(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadableNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompound(EnchantmentType.KEY);
	}

	public static int getEnchantmentLevel(final @Nullable ItemStack item, final EnchantmentType type) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
			return getEnchantmentLevel(enchantments, type);
		});
	}

	public static int getEnchantmentLevel(final @Nullable ReadableNBT enchantments, final EnchantmentType type) {
		if (enchantments == null) {
			return 0;
		}

		ReadableNBT enchantment = enchantments.getCompound(type.getName());
		if (enchantment == null) {
			return 0;
		}

		return enchantment.getInteger(LEVEL_KEY);
	}

	public static void addEnchantment(final @Nullable ItemStack item, final EnchantmentType type, final int level) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT enchantment = nbt.getOrCreateCompound(MONUMENTA_KEY).getOrCreateCompound(STOCK_KEY).getOrCreateCompound(EnchantmentType.KEY).getOrCreateCompound(type.getName());
			enchantment.setInteger(LEVEL_KEY, level);
		});

		if (type.getEnchantment() != null || type == EnchantmentType.UNBREAKABLE) {
			ItemUpdateHelper.regenerateStats(item);
		}
	}

	public static void removeEnchantment(final @Nullable ItemStack item, final EnchantmentType type) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT enchantments = getEnchantments(nbt);
			if (enchantments == null) {
				return;
			}

			enchantments.removeKey(type.getName());
		});

		if (type.getEnchantment() != null || type == EnchantmentType.UNBREAKABLE) {
			ItemUpdateHelper.regenerateStats(item);
		}
	}

	public static boolean hasEnchantment(@Nullable ItemStack item, EnchantmentType type) {
		return getEnchantmentLevel(item, type) > 0;
	}

	public static boolean hasPlayerModified(final @Nullable ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return false;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return false;
			}
			return monumenta.hasTag(PLAYER_MODIFIED_KEY);
		});
	}

	public static @Nullable ReadWriteNBT getPlayerModified(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}
		return monumenta.getCompound(PLAYER_MODIFIED_KEY);
	}

	public static @Nullable ReadableNBT getPlayerModified(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}
		return monumenta.getCompound(PLAYER_MODIFIED_KEY);
	}

	public static void removePlayerModified(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return;
		}
		monumenta.removeKey(PLAYER_MODIFIED_KEY);
		if (monumenta.getKeys().isEmpty()) {
			nbt.removeKey(MONUMENTA_KEY);
		}
	}

	public static ReadWriteNBT addPlayerModified(final ReadWriteNBT nbt) {
		return nbt.getOrCreateCompound(MONUMENTA_KEY).getOrCreateCompound(PLAYER_MODIFIED_KEY);
	}

	public static @Nullable ItemStack copyPlayerModified(final @Nullable ItemStack item, @Nullable ItemStack newItem) {
		if (ItemUtils.isNullOrAir(item) || newItem == null || newItem.getType() == Material.AIR) {
			return null;
		}

		ReadableNBT itemNBT = NBT.readNbt(item);
		ReadableNBT playerModified = getPlayerModified(itemNBT);
		if (playerModified == null) {
			return newItem;
		}

		NBT.modify(newItem, nbt -> {
			addPlayerModified(nbt).mergeCompound(playerModified);
		});

		ItemUpdateHelper.generateItemStats(newItem);
		return newItem;
	}

	public static @Nullable ReadWriteNBT getInfusions(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadWriteNBT modified = monumenta.getCompound(PLAYER_MODIFIED_KEY);
		if (modified == null) {
			return null;
		}

		return modified.getCompound(InfusionType.KEY);
	}

	public static @Nullable ReadableNBT getInfusions(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadableNBT modified = monumenta.getCompound(PLAYER_MODIFIED_KEY);
		if (modified == null) {
			return null;
		}

		return modified.getCompound(InfusionType.KEY);
	}

	public static int getInfusionLevel(final @Nullable ItemStack item, final @Nullable InfusionType type) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
			return getInfusionLevel(infusions, type);
		});
	}

	public static int getInfusionLevel(final @Nullable ReadableNBT infusions, final @Nullable InfusionType type) {
		if (type == null) {
			return 0;
		}
		if (infusions == null || type.getName() == null) {
			return 0;
		}

		ReadableNBT infusion = infusions.getCompound(type.getName());
		if (infusion == null) {
			return 0;
		}

		return infusion.getInteger(LEVEL_KEY);
	}

	public static @Nullable UUID getInfuser(final @Nullable ItemStack item, final @Nullable InfusionType type) {
		if (ItemUtils.isNullOrAir(item) || type == null || type.getName() == null) {
			return null;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT infusions = getInfusions(nbt);
			if (infusions == null) {
				return null;
			}
			ReadableNBT infusion = infusions.getCompound(type.getName());
			if (infusion == null) {
				return null;
			}
			String uuid = infusion.getString(INFUSER_KEY);
			if (uuid == null || uuid.isEmpty()) {
				return null;
			}
			try {
				return UUID.fromString(uuid);
			} catch (IllegalArgumentException e) { // bad item format
				return null;
			}
		});
	}

	public static void addInfusion(final @Nullable ItemStack item, final InfusionType type, final int level, final UUID infuser) {
		addInfusion(item, type, level, infuser, true);
	}

	public static void addInfusion(final @Nullable ItemStack item, final InfusionType type, final int level, final UUID infuser, boolean updateItem) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT infusion = nbt.getOrCreateCompound(MONUMENTA_KEY).getOrCreateCompound(PLAYER_MODIFIED_KEY).getOrCreateCompound(InfusionType.KEY).getOrCreateCompound(type.getName());
			infusion.setInteger(LEVEL_KEY, level);
			infusion.setString(INFUSER_KEY, infuser.toString());
		});
		if (updateItem) {
			ItemUpdateHelper.generateItemStats(item);
		}
	}

	public static void removeInfusion(final ItemStack item, final InfusionType type) {
		removeInfusion(item, type, true);
	}

	public static void removeInfusion(final ItemStack item, final InfusionType type, boolean updateItem) {
		if (item.getType() == Material.AIR || type == null) {
			return;
		}
		boolean success = NBT.modify(item, nbt -> {
			ReadWriteNBT infusions = getInfusions(nbt);
			if (infusions == null) {
				return false;
			}
			infusions.removeKey(type.getName());
			return true;
		});
		if (success && updateItem) {
			ItemUpdateHelper.generateItemStats(item);
		}
	}

	public static boolean hasInfusion(@Nullable ItemStack item, InfusionType type) {
		return getInfusionLevel(item, type) > 0;
	}

	public static @Nullable ReadWriteNBTCompoundList getAttributes(final ReadWriteNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadWriteNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(AttributeType.KEY);
	}

	public static @Nullable ReadableNBTList<ReadWriteNBT> getAttributes(final ReadableNBT nbt) {
		ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		ReadableNBT stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(AttributeType.KEY);
	}

	public static boolean hasAttributeInSlot(final @Nullable ItemStack item, final Slot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> attributes = getAttributes(nbt);
			if (attributes == null) {
				return false;
			}
			for (ReadableNBT attribute : attributes) {
				if (attribute.getString(Slot.KEY).equals(slot.getName())) {
					return true;
				}
			}
			return false;
		});
	}

	public static double getAttributeAmount(final @Nullable ReadableNBTList<ReadWriteNBT> attributes, final AttributeType type, final Operation operation, final Slot slot) {
		if (attributes == null) {
			return 0;
		}

		for (ReadableNBT attribute : attributes) {
			if (attribute.getString(ATTRIBUTE_NAME_KEY).equals(type.getName()) && attribute.getString(Operation.KEY).equals(operation.getName()) && attribute.getString(Slot.KEY).equals(slot.getName())) {
				return attribute.getDouble(AMOUNT_KEY);
			}
		}

		return 0;
	}

	public static double getAttributeAmount(final @Nullable ItemStack item, final AttributeType type, final Operation operation, final Slot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> compound = getAttributes(nbt);

			return getAttributeAmount(compound, type, operation, slot);
		});
	}

	public static void addAttribute(final ItemStack item, final AttributeType type, final double amount, final Operation operation, final Slot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBT.modify(item, nbt -> {
			ReadWriteNBTCompoundList attributes = nbt.getOrCreateCompound(MONUMENTA_KEY).getOrCreateCompound(STOCK_KEY).getCompoundList(AttributeType.KEY);
			// remove previous attribute before adding
			attributes.removeIf((attribute) ->
				attribute.getString(ATTRIBUTE_NAME_KEY).equals(type.getName()) && attribute.getString(Operation.KEY).equals(operation.getName()) && attribute.getString(Slot.KEY).equals(slot.getName()));
			ReadWriteNBT attribute = attributes.addCompound();
			attribute.setString(ATTRIBUTE_NAME_KEY, type.getName());
			attribute.setString(Operation.KEY, operation.getName());
			attribute.setDouble(AMOUNT_KEY, amount);
			attribute.setString(Slot.KEY, slot.getName());
		});

		EquipmentSlot equipmentSlot = slot.getEquipmentSlot();
		if (type.getAttribute() != null && equipmentSlot != null) {
			ItemUpdateHelper.regenerateStats(item);
		}
	}

	public static void removeAttribute(final ItemStack item, final AttributeType type, final Operation operation, final Slot slot) {
		if (item.getType() == Material.AIR) {
			return;
		}
		boolean success = NBT.modify(item, nbt -> {
			ReadWriteNBTCompoundList attributes = getAttributes(nbt);

			if (attributes == null) {
				return false;
			}

			attributes.removeIf((attribute) ->
				attribute.getString(ATTRIBUTE_NAME_KEY).equals(type.getName()) && attribute.getString(Operation.KEY).equals(operation.getName()) && attribute.getString(Slot.KEY).equals(slot.getName()));
			return true;
		});

		if (!success) {
			return;
		}

		EquipmentSlot equipmentSlot = slot.getEquipmentSlot();
		if (type.getAttribute() != null && equipmentSlot != null) {
			ItemUpdateHelper.regenerateStats(item);
		}
	}

	public static Region getRegion(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Region.NONE;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return ItemUtils.isShulkerBox(item.getType()) ? Region.SHULKER_BOX : Region.NONE;
			}

			String regionString = monumenta.getString(Region.KEY);
			if (regionString != null && !regionString.isEmpty()) {
				return Region.getRegion(regionString);
			}

			if (ItemUtils.isShulkerBox(item.getType())) {
				return Region.SHULKER_BOX;
			}

			return Region.NONE;
		});
	}

	public static Tier getTier(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Tier.NONE;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return ItemUtils.isShulkerBox(item.getType()) ? Tier.SHULKER_BOX : Tier.NONE;
			}

			String tierString = monumenta.getString(Tier.KEY);
			if (tierString != null && !tierString.isEmpty()) {
				return Tier.getTier(tierString);
			}

			if (ItemUtils.isShulkerBox(item.getType())) {
				return Tier.SHULKER_BOX;
			}

			return Tier.NONE;
		});
	}

	public static Masterwork getMasterwork(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Masterwork.NONE;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return Masterwork.NONE;
			}

			if (getRegion(item) != Region.RING) {
				return Masterwork.NONE;
			}

			String tierString = monumenta.getString(Masterwork.KEY);
			if (tierString != null && !tierString.isEmpty()) {
				return Masterwork.getMasterwork(tierString);
			}

			return Masterwork.NONE;
		});
	}

	public static Location getLocation(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Location.NONE;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return Location.NONE;
			}

			String locationString = monumenta.getString(Location.KEY);
			if (locationString != null && !locationString.isEmpty()) {
				return Location.getLocation(locationString);
			}

			return Location.NONE;
		});
	}

	public static boolean isDirty(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return false;
			}

			return monumenta.hasTag(DIRTY_KEY);
		});
	}

	public static void removeDirty(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}

			monumenta.removeKey(DIRTY_KEY);
			if (monumenta.getKeys().isEmpty()) {
				nbt.removeKey(MONUMENTA_KEY);
			}
		});
	}

	public static void cleanIfNecessary(final @Nullable ItemStack item) {
		if (item != null && isDirty(item)) {
			ItemUpdateHelper.generateItemStats(item);
			removeDirty(item);
		}
	}

	public static int getShulkerSlots(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return nbt.resolveOrDefault(MONUMENTA_KEY + "." + STOCK_KEY + "." + SHULKER_SLOTS_KEY, 27);
		});
	}

	public static int getCustomInventoryItemTypesLimit(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return nbt.resolveOrDefault(MONUMENTA_KEY + "." + STOCK_KEY + "." + CUSTOM_INVENTORY_TYPES_LIMIT_KEY, 0);
		});
	}

	public static int getCustomInventoryItemsPerTypeLimit(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return nbt.resolveOrDefault(MONUMENTA_KEY + "." + STOCK_KEY + "." + CUSTOM_INVENTORY_ITEMS_PER_TYPE_LIMIT_KEY, 0);
		});
	}

	public static int getCustomInventoryTotalItemsLimit(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return nbt.resolveOrDefault(MONUMENTA_KEY + "." + STOCK_KEY + "." + CUSTOM_INVENTORY_TOTAL_ITEMS_LIMIT_KEY, 0);
		});
	}

	/**
	 * Checks if an item is a quiver, i.e. is a tipped arrow with the tag Monumenta.Stock.IsQuiver set to true.
	 */
	public static boolean isQuiver(@Nullable ItemStack item) {
		if (item == null || item.getType() != Material.TIPPED_ARROW) {
			return false;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(MONUMENTA_KEY);
			if (monumenta == null) {
				return false;
			}
			ReadableNBT stock = monumenta.getCompound(STOCK_KEY);
			if (stock == null) {
				return false;
			}
			return stock.getBoolean(IS_QUIVER_KEY);
		});
	}

	public static boolean isArrowTransformingQuiver(@Nullable ItemStack item) {
		return isQuiver(item) && "Shaman's Quiver".equals(ItemUtils.getPlainNameIfExists(item));
	}

	public static void setArrowTransformMode(@Nullable ItemStack item, QuiverListener.ArrowTransformMode arrowTransformMode) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			addPlayerModified(nbt).setString(QUIVER_ARROW_TRANSFORM_MODE_KEY, arrowTransformMode.name().toUpperCase(Locale.ROOT));
		});
	}

	public static QuiverListener.ArrowTransformMode getArrowTransformMode(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return (QuiverListener.ArrowTransformMode) nbt.resolveOrDefault(MONUMENTA_KEY + "." + PLAYER_MODIFIED_KEY + "." + QUIVER_ARROW_TRANSFORM_MODE_KEY, QuiverListener.ArrowTransformMode.NONE);
		});
	}

	public static boolean isUpgradedLimeTesseract(@Nullable ItemStack item) {
		return item != null
			&& item.getType() == Material.LIME_STAINED_GLASS
			&& "Tesseract of Knowledge (u)".equals(ItemUtils.getPlainNameIfExists(item));
	}

	public static int getCharges(@Nullable ItemStack item) {
		return NBT.get(item, nbt -> {
			return nbt.resolveOrDefault(MONUMENTA_KEY + "." + PLAYER_MODIFIED_KEY + "." + CHARGES_KEY, 0);
		});
	}

	public static void setCharges(@Nullable ItemStack item, int charges) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBT.modify(item, nbt -> {
			addPlayerModified(nbt).setInteger(CHARGES_KEY, charges);
		});
	}

	public static boolean isMaterial(@Nullable ItemStack item) {
		return item != null && getEnchantmentLevel(item, EnchantmentType.MATERIAL) > 0;
	}

	public static @Nullable CharmManager.CharmType getCharmType(@Nullable ItemStack item) {
		if (item == null) {
			return null;
		}
		for (CharmManager.CharmType charmType : CharmManager.CharmType.values()) {
			if (charmType.isCharm(item)) {
				return charmType;
			}
		}
		return null;
	}

	public static boolean isCharm(@Nullable ItemStack item) {
		return getCharmType(item) != null;
	}

	public static boolean isNormalCharm(@Nullable ItemStack item) {
		Tier tier = getTier(item);
		return tier == Tier.CHARM || tier == Tier.RARE_CHARM || tier == Tier.EPIC_CHARM;
	}

	public static boolean isZenithCharm(@Nullable ItemStack item) {
		Tier tier = getTier(item);
		return tier == Tier.ZENITH_CHARM;
	}

	public static boolean isFish(@Nullable ItemStack item) {
		return getTier(item) == Tier.FISH;
	}

	// Returns true if the item has mainhand attack damage OR doesn't have mainhand projectile damage (i.e. any ranged weapon that is not also a melee weapon)
	public static boolean isNotExclusivelyRanged(@Nullable ItemStack item) {
		return item != null && !ItemUtils.isArrow(item) && !ItemUtils.isAlchemistItem(item) && (getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) > 0 || getAttributeAmount(item, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) == 0);
	}

	// get item list (read/write)
	public static ReadWriteNBTCompoundList getItemList(ReadWriteNBT nbt) {
		return ItemStatUtils.addPlayerModified(nbt).getCompoundList(ItemStatUtils.ITEMS_KEY);
	}

	// get item list (read only)
	public static @Nullable ReadableNBTList<ReadWriteNBT> getItemList(ReadableNBT nbt) {
		ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
		if (playerModified == null) {
			return null;
		}
		return playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
	}
}
