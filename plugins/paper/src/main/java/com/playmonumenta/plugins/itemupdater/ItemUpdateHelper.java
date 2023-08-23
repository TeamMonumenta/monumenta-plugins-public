package com.playmonumenta.plugins.itemupdater;

import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.enums.*;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.listeners.QuiverListener;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTType;
import de.tr7zw.nbtapi.iface.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class ItemUpdateHelper {
	// Keys allowed to be empty
	private static final List<String> whitelistedKeys = Arrays.asList("AttributeModifiers");
	private static final ReadWriteNBT emptyAttributes = NBT.parseNBT("{AttributeModifiers:[]}");
	private static final List<Component> loreToRemove = Collections.singletonList(ItemStatUtils.DUMMY_LORE_TO_REMOVE);

	public static List<String> getEmptyKeys(ReadableNBT nbt, List<String> paths, String baseKey) {
		Set<String> keys = nbt.getKeys();
		if (keys.isEmpty()) {
			if (!baseKey.isBlank()) {
				paths.add(baseKey);
			}
			return paths;
		}
		for (String key : keys) {
			if (whitelistedKeys.contains(key)) {
				continue;
			}
			NBTType type = nbt.getType(key);
			// MMLog.info(key + "-" + type);

			if (type == NBTType.NBTTagCompound) {
				ReadableNBT compound = nbt.getCompound(key);
				if (compound == null) {
					paths.add(baseKey);
					break;
				}
				Set<String> nestedKeys = compound.getKeys();
				if (nestedKeys.isEmpty()) {
					paths.add(baseKey);
					break;
				}
				if (baseKey.isBlank()) {
					baseKey = key;
				}
				paths = getEmptyKeys(compound, paths, baseKey);
				break;
			}

			NBTType listType = nbt.getListType(key);
			// MMLog.info(key + "-" + listType);
			// if the compound isn't a compound but a list instead
			if (listType == NBTType.NBTTagCompound) {
				ReadableNBTList<ReadWriteNBT> compoundList = nbt.getCompoundList(key);
				if (compoundList == null || compoundList.isEmpty()) {
					paths.add(baseKey);
				}
				break;
			} else if (listType == NBTType.NBTTagEnd) {
				paths.add(baseKey);
				break;
			}
		}
		return paths;
	}

	public static void cleanEmptyTags(ReadWriteNBT parent, String checkTag) {
		if (parent == null) {
			return;
		}
		ReadWriteNBT nbt = parent.getCompound(checkTag);
		if (nbt == null) {
			return;
		}
		for (String key : nbt.getKeys()) {
			switch (nbt.getType(key)) {
				case NBTTagCompound: {
					cleanEmptyTags(nbt, key);
					break;
				}
				case NBTTagList: {
					NBTType type = nbt.getListType(key);
					if (type == NBTType.NBTTagEnd) {
						nbt.removeKey(key);
					}
					break;
				}
				default: {
					break;
				}
			}
		}
		if (nbt.getKeys().size() == 0) {
			parent.removeKey(checkTag);
		}
	}

	public static void purgeUnusedKeys(final ItemStack item) {
		List<String> paths = NBT.get(item, nbt -> {
			return getEmptyKeys(nbt, new ArrayList<>(), "");
		});
		if (paths.isEmpty()) {
			return;
		}
		NBT.modify(item, nbt -> {
			for (String path : paths) {
				cleanEmptyTags(nbt, path);
			}
		});
	}

	public static void generateItemStats(final ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		// There is probably a cleaner way to clean up unused NBT, not sure if recursion
		// directly works due to the existence of both NBTCompounds and NBTCompoundLists
		// TODO: clean up other unused things from item (e.g. empty lore, reset hideflags if no NBT)
		// ! We also may want to move this outside, as purging unused keys may be useful outside of Monumenta items

		List<Component> lore = new ArrayList<>();

		// batch everything in a GET call
		NBT.get(item, nbt -> {
			// If Monumenta is null, there is probably no information to gleam from the item
			ReadableNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}

			// Checks for PI + Totem of Transposing
			String plainName = ItemUtils.getPlainName(item);
			if (ItemUtils.isShulkerBox(item.getType()) && plainName.equals("Potion Injector")) {
				List<String> plainLore = ItemUtils.getPlainLore(item);
				Component potionName = Objects.requireNonNull(item.lore()).get(1);
				lore.add(Component.text(plainLore.get(0), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				lore.add(potionName);
			} else if (plainName.equals("Totem of Transposing")) {
				List<String> plainLore = ItemUtils.getPlainLore(item);
				lore.add(Component.text(plainLore.get(0), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			}

			List<Component> tagsLater = new ArrayList<>();
			List<Component> unbreakingTags = new ArrayList<>();
			List<Component> statTrackLater = new ArrayList<>();
			List<Component> infusionTagsLater = new ArrayList<>();

			// ENCHANTMENTS
			ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
			if (enchantments != null && ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_ENCHANTS) == 0) {
				for (EnchantmentType type : EnchantmentType.values()) {
					if (type.isHidden()) {
						continue;
					}
					ReadableNBT enchantment = enchantments.getCompound(type.getName());
					if (enchantment != null) {
						Component display = type.getDisplay(enchantment.getInteger(ItemStatUtils.LEVEL_KEY));
						if (type.isItemTypeEnchantment()) {
							tagsLater.add(display);
						} else if (type.getName().equals("Mending") || type.getName().equals("Unbreaking") || type.getName().equals("Unbreakable")) {
							unbreakingTags.add(display);
						} else {
							lore.add(display);
						}
					}
				}
			}

			// INFUSIONS
			ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
			if (infusions != null) {
				for (InfusionType type : InfusionType.values()) {
					if (type.isHidden()) {
						continue;
					}
					ReadableNBT infusion = infusions.getCompound(type.getName());
					if (infusion != null) {
						if (type == InfusionType.STAT_TRACK) {
							statTrackLater.add(0, type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY), MonumentaRedisSyncIntegration.cachedUuidToNameOrUuid(UUID.fromString(infusion.getString(ItemStatUtils.INFUSER_KEY)))));
						} else if (type.isStatTrackOption()) {
							if (type == InfusionType.STAT_TRACK_DEATH && ItemUtils.isShulkerBox(item.getType())) {
								// Easter egg: Times Dyed for shulker boxes
								statTrackLater.add(Component.text("Times Dyed: " + (infusion.getInteger(ItemStatUtils.LEVEL_KEY) - 1), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
							} else {
								statTrackLater.add(type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY)));
							}
						} else if (!type.getMessage().isEmpty()) {
							infusionTagsLater.add(type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY), MonumentaRedisSyncIntegration.cachedUuidToNameOrUuid(UUID.fromString(infusion.getString(ItemStatUtils.INFUSER_KEY)))));
						} else {
							lore.add(type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY)));
						}
					}
				}
			}

			// Add unbreaking tags
			lore.addAll(unbreakingTags);
			// Add infusions with message
			lore.addAll(infusionTagsLater);
			// Add stat tracking lore
			lore.addAll(statTrackLater);
			// Add Magic Wand Tag *after* all other stats,
			lore.addAll(tagsLater);

			// TIER/MASTERWORK
			if (monumenta != null && ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_INFO) == 0) {
				String regionString = monumenta.getString(Region.KEY);
				if (regionString != null && !regionString.isEmpty()) {
					Region region = Region.getRegion(regionString);
					Masterwork masterwork = Masterwork.getMasterwork(monumenta.getString(Masterwork.KEY));
					Tier tier = Tier.getTier(monumenta.getString(Tier.KEY));
					if (region != null) {
						// For R3 items, set tier to match masterwork level
						if (region == Region.RING) {
							if (masterwork != null && masterwork != Masterwork.ERROR && masterwork != Masterwork.NONE) {
								switch (Objects.requireNonNull(masterwork)) {
									case ZERO, I, II, III -> tier = Tier.RARE;
									case IV, V -> tier = Tier.ARTIFACT;
									case VI -> tier = Tier.EPIC;
									case VIIA, VIIB, VIIC -> tier = Tier.LEGENDARY;
									default -> {
										// No Tier
									}
								}
								String modifyTier = tier.getName();
								NBT.modify(item, wnbt -> {
									ReadWriteNBT wmonumenta = wnbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY);
									wmonumenta.setString(Tier.KEY, modifyTier);
								});
							}
						}
						if (tier != null && tier != Tier.NONE) {
							lore.add(region.getDisplay().append(tier.getDisplay()));
						}
					}

					if (ItemStatUtils.isCharm(item)) {
						int charmPower = ItemStatUtils.getCharmPower(item);
						if (charmPower > 0) {
							String starString = "★".repeat(charmPower);
							lore.add(Component.text("Charm Power : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, TextColor.fromHexString("#FFFA75")).decoration(TextDecoration.ITALIC, false)).append(Component.text(" - ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)).append(ItemStatUtils.getCharmClassComponent(monumenta.getStringList(ItemStatUtils.CHARM_KEY))));
						}
					}

					if (ItemStatUtils.isFish(item)) {
						int fishQuality = ItemStatUtils.getFishQuality(item);
						if (fishQuality > 0) {
							String starString = "★".repeat(fishQuality) + "☆".repeat(5 - fishQuality);
							TextColor color = fishQuality == 5 ? TextColor.fromHexString("#28FACC") : TextColor.fromHexString("#1DCC9A");
							lore.add(Component.text("Fish Quality : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, color)).decoration(TextDecoration.ITALIC, false));
						}
					}
					if (masterwork != null && masterwork != Masterwork.NONE) {
						lore.add(Component.text("Masterwork : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(masterwork.getDisplay()));
					}
					Location location = Location.getLocation(monumenta.getString(Location.KEY));
					if (location != null) {
						lore.add(location.getDisplay());
					}
				}
			}

			// LORE (Description)
			if (monumenta != null) {
				ReadableNBTList<String> description = monumenta.getStringList(ItemStatUtils.LORE_KEY);
				if (description != null) {
					for (String serializedLine : description) {
						Component lineAdd = Component.text("", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
						lineAdd = lineAdd.append(MessagingUtils.parseComponent(serializedLine));
						lore.add(lineAdd);
					}
				}
				if (ItemStatUtils.isArrowTransformingQuiver(item)) {
					QuiverListener.ArrowTransformMode transformMode = ItemStatUtils.getArrowTransformMode(item);
					if (transformMode == QuiverListener.ArrowTransformMode.NONE) {
						lore.add(Component.text("Arrow transformation ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
							.append(Component.text("disabled", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
					} else {
						lore.add(Component.text("Transforms arrows to ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
							.append(Component.text(transformMode.getArrowName(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
					}
				}

				CustomContainerItemManager.generateDescription(item, monumenta, lore::add);

				if (ItemStatUtils.isUpgradedLimeTesseract(item)) {
					lore.add(Component.text("Stored anvils: ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
						.append(Component.text(ItemStatUtils.getCharges(item), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
				}

				int shatterLevel = ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED);
				if (shatterLevel > 0) {
					TextColor color = TextColor.color(155 + (int) (100.0 * (shatterLevel - 1) / (Shattered.MAX_LEVEL - 1)), 0, 0);
					lore.add(Component.text("* SHATTERED - " + StringUtils.toRoman(shatterLevel) + " *", color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Return to your grave to remove one level", color).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("of Shattered, or use an anvil on this item.", color).decoration(TextDecoration.ITALIC, false));
				}

				ReadableNBTList<ReadWriteNBT> effects = ItemStatUtils.getEffects(nbt);
				if (effects != null && !effects.isEmpty()) {
					lore.add(Component.empty());
					lore.add(Component.text("When Consumed:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

					for (ReadWriteNBT effect : effects) {
						String type = effect.getString(ItemStatUtils.EFFECT_TYPE_KEY);
						int duration = effect.getInteger(ItemStatUtils.EFFECT_DURATION_KEY);
						double strength = effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY);
						EffectType effectType = EffectType.fromType(type);
						if (effectType != null) {
							Component comp = EffectType.getComponent(effectType, strength, duration);
							if (!lore.contains(comp)) {
								lore.add(comp);
							}
						}
					}
				}
			}

			// ATTRIBUTES
			ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);
			if (attributes != null && ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_ATTRIBUTES) == 0) {
				EnumMap<Slot, EnumMap<AttributeType, List<ReadWriteNBT>>> attributesBySlots = new EnumMap<>(Slot.class);
				for (ReadWriteNBT attribute : attributes) {
					Slot slot = Slot.getSlot(attribute.getString(Slot.KEY));
					AttributeType attributeType = AttributeType.getAttributeType(attribute.getString(ItemStatUtils.ATTRIBUTE_NAME_KEY));
					attributesBySlots.computeIfAbsent(slot, key -> new EnumMap<>(AttributeType.class)).computeIfAbsent(attributeType, key -> new ArrayList<>()).add(attribute);
				}

				for (Slot slot : Slot.values()) {
					EnumMap<AttributeType, List<ReadWriteNBT>> attributesBySlot = attributesBySlots.get(slot);
					if (attributesBySlot == null || attributesBySlot.isEmpty()) {
						continue;
					}

					lore.add(Component.empty());
					lore.add(slot.getDisplay());

					// If mainhand, display certain attributes differently (attack and projectile related ones), and also show them before other attributes
					if (slot == Slot.MAINHAND) {
						boolean needsAttackSpeed = false;
						for (AttributeType attributeType : AttributeType.MAINHAND_ATTRIBUTE_TYPES) {
							List<ReadWriteNBT> attributesByType = attributesBySlot.get(attributeType);
							if (attributesByType != null) {
								for (ReadWriteNBT attribute : attributesByType) {
									Operation operation = Operation.getOperation(attribute.getString(Operation.KEY));
									if (operation == null
										|| (operation != Operation.ADD && attributeType != AttributeType.PROJECTILE_SPEED)) {
										continue;
									}
									lore.add(AttributeType.getDisplay(attributeType, attribute.getDouble(ItemStatUtils.AMOUNT_KEY), slot, operation));
									if (attributeType == AttributeType.ATTACK_DAMAGE_ADD) {
										needsAttackSpeed = true;
									} else if (attributeType == AttributeType.ATTACK_SPEED) {
										needsAttackSpeed = false;
									}
								}
							}
							// show default attack speed if an item has attack damage, but no attack speed attribute
							if (needsAttackSpeed && attributeType == AttributeType.ATTACK_SPEED) {
								lore.add(AttributeType.getDisplay(AttributeType.ATTACK_SPEED, 0, slot, Operation.ADD));
							}
						}
					}

					for (AttributeType type : AttributeType.values()) {
						List<ReadWriteNBT> attributesByType = attributesBySlot.get(type);
						if (attributesByType == null) {
							continue;
						}
						for (Operation operation : Operation.values()) {
							if (slot == Slot.MAINHAND && AttributeType.MAINHAND_ATTRIBUTE_TYPES.contains(type) && (operation == Operation.ADD || type == AttributeType.PROJECTILE_SPEED)) {
								continue; // handled above
							}
							for (ReadWriteNBT attribute : attributesByType) {
								if (Operation.getOperation(attribute.getString(Operation.KEY)) == operation) {
									lore.add(AttributeType.getDisplay(type, attribute.getDouble(ItemStatUtils.AMOUNT_KEY), slot, operation));
									break;
								}
							}
						}
					}
				}
			}

			// CHARM
			if (monumenta != null) {
				ReadableNBTList<String> charmLore = monumenta.getStringList(ItemStatUtils.CHARM_KEY);
				if (charmLore != null && ItemStatUtils.isCharm(item)) {
					lore.add(Component.empty());
					lore.add(Component.text("When in Charm Slot:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					for (String serializedLine : charmLore) {
						Component lineAdd = MessagingUtils.parseComponent(serializedLine);
						lore.add(lineAdd);
					}
				}
			}
		});

		List<String> unusedKeys = NBT.get(item, nbt -> {
			return getEmptyKeys(nbt, new ArrayList<>(), "");
		});

		// Now modify the LORE on the item
		lore.removeAll(loreToRemove);
		NBT.modify(item, nbt -> {
			// remove unused keys
			if (!unusedKeys.isEmpty()) {
				for (String path : unusedKeys) {
					cleanEmptyTags(nbt, path);
				}
			}
			nbt.modifyMeta((nbtr, meta) -> {
				// placeholder attribute (can be removed once loottables are removed)
				boolean hasDummyArmorToughnessAttribute = false;
				if (meta.hasAttributeModifiers()) {
					Collection<AttributeModifier> toughnessAttrs = meta.getAttributeModifiers(Attribute.GENERIC_ARMOR_TOUGHNESS);
					hasDummyArmorToughnessAttribute = toughnessAttrs != null && toughnessAttrs.size() == 1 && toughnessAttrs.iterator().next().getName().equals(ItemStatUtils.MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME);
				}
				if (hasDummyArmorToughnessAttribute) {
					meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
				}

				// placeholder enchantment
				Enchantment placeholder = ItemUtils.isSomeBow(item) ? Enchantment.WATER_WORKER : Enchantment.ARROW_DAMAGE;
				if (meta.getEnchants().size() > 1 || !nbtr.hasTag("Monumenta") || ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.NO_GLINT) > 0) {
					meta.removeEnchant(placeholder);
				} else if (!meta.hasEnchants()) {
					meta.addEnchant(placeholder, 1, true);
				}

				// clear the potion meta
				if (meta instanceof PotionMeta potionMeta) {
					// TODO: Don't clear custom potion effects until we know what it is supposed to do
					// potionMeta.clearCustomEffects();
					if (!PotionUtils.BASE_POTION_ITEM_TYPES.contains(potionMeta.getBasePotionData().getType())) {
						potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
					}
				}

				// itemflags (add them if necessary)
				if (meta.hasEnchants()) {
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
				}
				if (meta.hasAttributeModifiers()) {
					meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				}
				if (meta.isUnbreakable()) {
					meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
				}
				// "display.color" is only used for dyed gear
				ReadableNBT displayCompound = nbtr.getCompound("display");
				if (displayCompound != null && displayCompound.hasTag("color")) {
					meta.addItemFlags(ItemFlag.HIDE_DYE);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_DYE);
				}
				// ? this may not work as intended
				Material type = item.getType();
				String name = type.name();
				if (name.contains("POTION") // potions
					|| type.isRecord() // music disc
					|| name.contains("PATTERN") // banners
					|| name.contains("BANNER") // banners
					|| name.contains("SHIELD") // shield
					|| ItemUtils.isArrow(item)) { // arrows
					meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
				}
			});
			if (ItemUtils.hasDefaultAttributes(item) && !nbt.hasTag("AttributeModifiers")) {
				// create a new empty list
				nbt.mergeCompound(emptyAttributes);
			}
			ItemUtils.setDisplayLore(nbt, lore);
			ItemUtils.setPlainComponentLore(nbt, lore);
		});
	}

	public static void fixLegacies(ItemStack item) {
		boolean purgeLegacy = NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta == null) {
				return false;
			}
			boolean legacy = "legacy".equals(monumenta.getString(Tier.KEY));
			if (!legacy) {
				return false;
			}
			return nbt.hasTag("CustomPotionEffects")
				|| monumenta.hasTag(ItemStatUtils.STOCK_KEY)
				|| nbt.hasTag("AttributeModifiers")
				|| nbt.hasTag("Enchantments");
		});
		if (!purgeLegacy) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}
			monumenta.removeKey(ItemStatUtils.STOCK_KEY);
			nbt.removeKey("CustomPotionEffects");
			nbt.removeKey("AttributeModifiers");
			nbt.removeKey("Enchantments");
		});
	}

	public record VanillaEnchantmentType(Enchantment enchant, int level) {}

	public record VanillaAttributeType(Attribute type, double amount, AttributeModifier.Operation operation, @Nullable EquipmentSlot slot) {}

	public static String checkForErrors(ItemStack item) {
		String errorFound = NBT.get(item, nbt -> {
			List<String> errors = new ArrayList<String>(5);
			ReadableNBT monumenta = nbt.getCompound("Monumenta");
			if (monumenta != null) {
				if (monumenta.getString(Tier.KEY).equals("legacy")) {
					if (monumenta.hasTag(ItemStatUtils.STOCK_KEY)) {
						errors.add("Legacy contains Monumenta.Stock tag!");
					}
				}
				List<VanillaEnchantmentType> nbtEnchantments = new ArrayList<>();
				List<VanillaAttributeType> nbtAttributes = new ArrayList<>();
				ItemMeta meta = item.getItemMeta();

				// custom enchantment checking
				ReadableNBT enchantmentsNBT = ItemStatUtils.getEnchantments(nbt);
				if (enchantmentsNBT != null) {
					Set<String> keys = enchantmentsNBT.getKeys();
					for (String key : keys) {
						EnchantmentType enchantment = EnchantmentType.getEnchantmentType(key);
						if (enchantment == null) {
							errors.add("[Enchant] Invalid EnchantmentType: '" + key + "'");
							continue;
						}
						Integer nbtLevel = enchantmentsNBT.resolveOrDefault(key + "." + ItemStatUtils.LEVEL_KEY, 0);
						if (nbtLevel <= 0) { // enchant levels shouldn't go below 1
							errors.add("[Enchant] Invalid amount: ('" + enchantment.getName() + "' " + nbtLevel + ")");
						}
						Enchantment nbtEnchantment = enchantment.getEnchantment();
						if (nbtEnchantment == null) {
							continue;
						}
						nbtEnchantments.add(new VanillaEnchantmentType(nbtEnchantment, nbtLevel));
						// validation
						if (!meta.hasEnchant(nbtEnchantment)) {
							errors.add("[Enchant] Missing Vanilla Enchant: ('" + nbtEnchantment.getKey().asString() + "' " + nbtLevel + ")");
							continue;
						}
						int vanillaLevel = meta.getEnchantLevel(nbtEnchantment);
						if (vanillaLevel != nbtLevel) {
							errors.add("[Enchant] Mismatched level: ('" + nbtEnchantment.getKey().asString() + "' Custom: " + nbtLevel + " | Vanilla: " + vanillaLevel + ")");
						}
					}
				}
				// vanilla enchantment checking
				if (meta.hasEnchants()) {
					Set<Map.Entry<Enchantment, Integer>> vanillaEnchantments = meta.getEnchants().entrySet();
					if (vanillaEnchantments != null && !vanillaEnchantments.isEmpty()) {
						Enchantment placeholderEnchantment = ItemUtils.isSomeBow(item) ? Enchantment.WATER_WORKER : Enchantment.ARROW_DAMAGE;
						for (Map.Entry<Enchantment, Integer> vanillaCopy : vanillaEnchantments) {
							Enchantment vanillaEnchantment = vanillaCopy.getKey();
							Integer vanillaLevel = meta.getEnchantLevel(vanillaEnchantment);
							// ignore placeholder enchant
							if (vanillaEnchantment.equals(placeholderEnchantment) && vanillaLevel == 1) {
								continue;
							}
							// find custom enchant that matches placeholder enchant
							if (!nbtEnchantments.contains(new VanillaEnchantmentType(vanillaEnchantment, vanillaLevel))) {
								errors.add("[Enchant] Extra Vanilla Enchant: ('" + vanillaEnchantment.getKey().asString() + "' " + vanillaLevel + ")");
								continue;
							}
						}
					}
				}

				ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
				if (infusions != null) {
					Set<String> keys = infusions.getKeys();
					for (String key : keys) {
						InfusionType infusion = InfusionType.getInfusionType(key);
						if (infusion == null) {
							errors.add("[Infusion] Invalid InfusionType: '" + key + "'");
							continue;
						}
					}
				}

				ReadableNBTList<ReadWriteNBT> attributesNBT = ItemStatUtils.getAttributes(nbt);
				if (attributesNBT != null && !attributesNBT.isEmpty()) {
					for (ReadWriteNBT attribute : attributesNBT) {
						String name = attribute.getString(ItemStatUtils.ATTRIBUTE_NAME_KEY);
						Double amount = attribute.getDouble(ItemStatUtils.AMOUNT_KEY);
						String operation = attribute.getString(Operation.KEY);
						String slot = attribute.getString(Slot.KEY);
						AttributeType attributeType = AttributeType.getAttributeType(name);
						Slot slotType = Slot.getSlot(slot);
						Operation operationType = Operation.getOperation(operation);
						// validate the custom attribute
						if (attributeType == null || slotType == null || operationType == null) {
							errors.add("[Attribute] Invalid AttributeType: '" + name + "' slot: '" + slot + "' operation: '" + operation + "' amount: '" + amount + "'");
							continue;
						}
						if (amount == 0) { // attribute amount can technically go into the negatives...
							errors.add("[Attribute] Invalid AttributeType amount: '" + name + "' slot: '" + slot + "' operation: '" + operation + "' amount: '" + amount + "'");
							continue;
						}
						Attribute nbtAttribute = attributeType.getAttribute();
						AttributeModifier.Operation nbtOperation = operationType.getAttributeOperation();
						EquipmentSlot nbtSlot = slotType.getEquipmentSlot();
						if (nbtAttribute == null || nbtOperation == null) {
							continue;
						}
						nbtAttributes.add(new VanillaAttributeType(nbtAttribute, amount, nbtOperation, nbtSlot));

						// try to find the vanilla attribute that matches this custom attribute
						Collection<AttributeModifier> vanillaAttributeModifiers = meta.getAttributeModifiers(nbtAttribute);
						if (vanillaAttributeModifiers == null) {
							errors.add("[Attribute] Missing Vanilla Attribute: '" + nbtAttribute.getKey().asString() + "' slot: '" + nbtSlot + "' operation: '" + nbtOperation + "' amount: '" + amount + "' (0 Vanilla Attributes)");
							continue;
						}
						boolean found = false;
						int counter = 0;
						for (AttributeModifier vanillaAttributeModifier : vanillaAttributeModifiers) {
							if (vanillaAttributeModifier.getAmount() == amount &&
								nbtOperation.equals(vanillaAttributeModifier.getOperation()) &&
								(nbtSlot == null || nbtSlot.equals(vanillaAttributeModifier.getSlot()))) {
								counter++;
								if (found == true) {
									errors.add("[Attribute] Duplicate Vanilla Attributes: '" + nbtAttribute.getKey().asString() + "' slot: '" + nbtSlot + "' operation: '" + nbtOperation + "' amount: '" + amount + "' (Count: " + counter + ")");
								}
								found = true;
							}
						}
						if (!found) {
							errors.add("[Attribute] Missing Vanilla Attribute: '" + nbtAttribute.getKey().asString() + "' slot: '" + nbtSlot + "' operation: '" + nbtOperation + "' amount: '" + amount + "' (1+ Vanilla Attributes)");
							continue;
						}
					}
				}
				if (meta.hasAttributeModifiers()) {
					Collection<Map.Entry<Attribute, AttributeModifier>> vanillaAttributes = meta.getAttributeModifiers().entries();
					if (vanillaAttributes != null && !vanillaAttributes.isEmpty()) {
						for (Map.Entry<Attribute, AttributeModifier> copy : vanillaAttributes) {
							Attribute vanillaAttribute = copy.getKey();
							AttributeModifier vanillaModifier = copy.getValue();
							double vanillaAmount = vanillaModifier.getAmount();
							AttributeModifier.Operation vanillaOperation = vanillaModifier.getOperation();
							EquipmentSlot vanillaSlot = vanillaModifier.getSlot();
							// find a custom attribute that matches this vanilla attribute
							if (!nbtAttributes.contains(new VanillaAttributeType(vanillaAttribute, vanillaAmount, vanillaOperation, vanillaSlot))) {
								errors.add("[Attribute] Extra Vanilla Attribute: '" + vanillaAttribute.getKey().asString() + "' slot: '" + vanillaSlot + "' operation: '" + vanillaOperation + "' amount: '" + vanillaAmount + "' friendlyName: '" + vanillaModifier.getName() + "'  (No Custom Attribute Found)");
								continue;
							}
						}
					}
				}

				ReadableNBTList<ReadWriteNBT> effects = ItemStatUtils.getEffects(nbt);
				if (effects != null && !effects.isEmpty()) {
					for (ReadWriteNBT effect : effects) {
						String type = effect.getString(ItemStatUtils.EFFECT_TYPE_KEY);
						int duration = effect.getInteger(ItemStatUtils.EFFECT_DURATION_KEY);
						double strength = effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY);
						EffectType effectType = EffectType.fromType(type);
						if (effectType == null) {
							errors.add("[Effect] Invalid EffectType: '" + type + "' duration: '" + duration + "' strength: '" + strength + "'");
							continue;
						}
						if (duration <= 0) {
							errors.add("[Effect] Invalid duration: '" + type + "' duration: '" + duration + "' strength: '" + strength + "'");
							continue;
						}
						if (strength <= 0) {
							errors.add("[Effect] Invalid strength: '" + type + "' duration: '" + duration + "' strength: '" + strength + "'");
							continue;
						}
						// we don't store the actual potion effects on the item so no need to check for effectType validation
					}
				}

			}
			if (nbt.hasTag("CustomPotionEffects")) {
				errors.add("Has CustomPotionEffects tag!");
			}
			boolean plain = nbt.hasTag("plain");
			boolean display = nbt.hasTag("display");
			if (!plain && display) {
				errors.add("Has display but missing plain.display tag");
			}
			if (!errors.isEmpty()) {
				return "Name: '" + ItemUtils.toPlainTagText(item.displayName()) + "\'\n" + String.join("\n", errors);
			}
			return null;
		});
		return errorFound;
	}
}
