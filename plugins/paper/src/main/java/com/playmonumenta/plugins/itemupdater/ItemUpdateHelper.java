package com.playmonumenta.plugins.itemupdater;

import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
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
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.listeners.QuiverListener;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTType;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class ItemUpdateHelper {
	// Keys allowed to be empty
	private static final List<String> whitelistedKeys = Arrays.asList("AttributeModifiers");
	private static final List<Component> loreToRemove = Collections.singletonList(ItemStatUtils.DUMMY_LORE_TO_REMOVE);
	private static final UUID cachedDummyUUID = new UUID(0, 0);
	private static final AttributeModifier cachedDummyAttributeModifier = new AttributeModifier(cachedDummyUUID, ItemStatUtils.MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME, 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

	private static final Map<String, String> enchantmentConversionMap = Map.ofEntries(
		Map.entry("Frost", "Ice Aspect"),
		Map.entry("Spark", "Thunder Aspect"),
		Map.entry("Flame", "Fire Aspect")
	);
	private static final Map<String, String> effectConversionMap = Map.ofEntries(
		Map.entry("InstantHealth", "InstantHealthPercent")
	);

	public record VanillaEnchantmentType(Enchantment enchant, int level) {}

	public record VanillaAttributeType(Attribute type, double amount, AttributeModifier.Operation operation, @Nullable EquipmentSlot slot) {}

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
				paths = getEmptyKeys(compound, paths, baseKey.isBlank() ? key : baseKey);
			} else {
				NBTType listType = nbt.getListType(key);
				// MMLog.info(key + "-" + listType);
				// if the compound isn't a compound but a list instead
				if (listType == NBTType.NBTTagCompound) {
					ReadableNBTList<ReadWriteNBT> compoundList = nbt.getCompoundList(key);
					if (compoundList == null || compoundList.isEmpty()) {
						paths.add(baseKey);
						break;
					}
				} else if (listType == NBTType.NBTTagEnd) {
					paths.add(baseKey);
					break;
				}
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

		boolean wasDirty = ItemStatUtils.isDirty(item);
		if (wasDirty) {
			ItemStatUtils.removeDirty(item);
		}

		// check if item has NBT data
		boolean hasNBTData = NBT.get(item, nbt -> (boolean) nbt.hasNBTData());
		if (!hasNBTData) {
			return;
		}

		if (wasDirty && ItemStatUtils.isZenithCharm(item)) {
			// Update Depths charms once a week (when not clean) to catch balance changes
			// This calls generateItemStats again once updated. Mark clean to prevent infinite recursion
			ItemStack newCharm = CharmFactory.updateCharm(item);
			if (newCharm != null) {
				item.setItemMeta(newCharm.getItemMeta());
			}
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
			String plainName = ItemUtils.getPlainNameIfExists(nbt);
			if (plainName.equals("Potion Injector") && ItemUtils.isShulkerBox(item.getType())) {
				List<String> plainLore = ItemUtils.getPlainLore(item);
				Component potionName = Objects.requireNonNull(item.lore()).get(1);
				lore.add(Component.text(plainLore.get(0), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				lore.add(potionName);
			} else if (plainName.equals("Totem of Transposing")) {
				int transposingId = nbt.getOrDefault("TransposingID", -1);
				String transposingChannel = transposingId == -1 ? "MISSING CHANNEL" : transposingId + "";
				lore.add(Component.text("Transposing Channel: " + transposingChannel, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			}

			List<Component> tagsLater = new ArrayList<>();

			// ENCHANTMENTS
			ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
			if (enchantments != null && ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_ENCHANTS) == 0) {
				EnumMap<EnchantmentType, Component> enchantmentMap = new EnumMap<>(EnchantmentType.class);
				Set<String> keys = enchantments.getKeys();
				for (String key : keys) {
					EnchantmentType type = EnchantmentType.getEnchantmentType(key);
					if (type == null || type.isHidden()) {
						// invalid EnchantmentType
						continue;
					}
					ReadableNBT enchantmentNBT = enchantments.getCompound(key);
					if (enchantmentNBT == null) {
						// invalid nbt?
						continue;
					}
					Integer level = enchantmentNBT.getInteger(ItemStatUtils.LEVEL_KEY);
					if (level == null || level == 0) {
						continue;
					}
					Component display = type.getDisplay(level);
					if (type.isItemTypeEnchantment()) {
						tagsLater.add(display);
					} else {
						enchantmentMap.put(type, display);
					}
				}
				if (!enchantmentMap.isEmpty()) {
					lore.addAll(enchantmentMap.values());
				}
			}

			// INFUSIONS
			ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
			if (infusions != null) {
				EnumMap<InfusionType, Component> infusionMap = new EnumMap<>(InfusionType.class);
				Set<String> keys = infusions.getKeys();
				for (String key : keys) {
					InfusionType type = InfusionType.getInfusionType(key);
					if (type == null || type.isHidden()) {
						continue;
					}
					ReadableNBT infusion = infusions.getCompound(key);
					if (infusion == null) {
						continue;
					}
					if (type.isStatTrackOption()) {
						if (type == InfusionType.STAT_TRACK_DEATH && ItemUtils.isShulkerBox(item.getType())) {
							// Easter egg: Times Dyed for shulker boxes
							infusionMap.put(type, Component.text("Times Dyed: " + (infusion.getInteger(ItemStatUtils.LEVEL_KEY) - 1), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
						} else {
							infusionMap.put(type, type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY)));
						}
						continue;
					}
					if (!type.getMessage().isEmpty()) {
						String name = infusion.getString(ItemStatUtils.INFUSER_NPC_KEY);
						if (name == null || name.isEmpty()) {
							UUID playerUuid = UUID.fromString(infusion.getString(ItemStatUtils.INFUSER_KEY));
							name = MonumentaRedisSyncIntegration.cachedUuidToNameOrUuid(playerUuid);
						}
						infusionMap.put(type, type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY), name));
						continue;
					}
					Component display = type.getDisplay(infusion.getInteger(ItemStatUtils.LEVEL_KEY));
					if (type.isDelveInfusion()) { // delve infusion symbol addition
						DelveInfusionUtils.DelveInfusionMaterial materialUsed = DelveInfusionUtils.getDelveInfusionMaterial(item);
						if (materialUsed != null) {
							display = display.append(materialUsed.getIcon());
						}
					}
					infusionMap.put(type, display);
				}
				if (!infusionMap.isEmpty()) {
					lore.addAll(infusionMap.values());
				}
			}

			// Add Magic Wand Tag *after* all other stats
			lore.addAll(tagsLater);

			// TIER/MASTERWORK
			if (ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_INFO) == 0) {
				Region region = Region.getRegion(monumenta.getString(Region.KEY));
				Masterwork masterwork = Masterwork.getMasterwork(monumenta.getString(Masterwork.KEY));
				Tier tier = Tier.getTier(monumenta.getString(Tier.KEY));
				if (region != null && region != Region.NONE) {
					// For R3 items, set tier to match masterwork level
					if (region == Region.RING) {
						if (masterwork != null && masterwork != Masterwork.ERROR && masterwork != Masterwork.NONE) {
							final Tier previousTier = tier;
							switch (Objects.requireNonNull(masterwork)) {
								case ZERO, I, II, III -> tier = Tier.RARE;
								case IV, V -> tier = Tier.ARTIFACT;
								case VI -> tier = Tier.EPIC;
								case VIIA, VIIB, VIIC -> tier = Tier.LEGENDARY;
								default -> {
									// No Tier
								}
							}
							if (previousTier != tier && tier != Tier.NONE) {
								String modifyTier = tier.getName();
								NBT.modify(item, wnbt -> {
									ReadWriteNBT wmonumenta = wnbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY);
									wmonumenta.setString(Tier.KEY, modifyTier);
								});
							}
						}
					}
					if (tier != null && tier != Tier.NONE) {
						lore.add(region.getDisplay().append(tier.getDisplay()));
					}
				}

				// Masterwork, Charm Power and Fish Quality are mutually exclusive
				if (masterwork != null && masterwork != Masterwork.NONE) {
					lore.add(Component.text("Masterwork : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(masterwork.getDisplay()));
				} else if (ItemStatUtils.isCharm(tier)) {
					CharmManager.CharmType charmType = ItemStatUtils.getCharmType(item);
					int charmPower = ItemStatUtils.getCharmPower(item);
					if (charmType != null && charmPower > 0) {
						String starString = "★".repeat(charmPower);
						lore.add(Component.text("Charm Power : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, TextColor.fromHexString("#FFFA75")).decoration(TextDecoration.ITALIC, false)).append(Component.text(" - ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)).append(charmType.getLabel(nbt)));
					}
				} else if (ItemStatUtils.isFish(tier)) {
					int fishQuality = ItemStatUtils.getFishQuality(item);
					if (fishQuality > 0) {
						String starString = "★".repeat(fishQuality) + "☆".repeat(5 - fishQuality);
						TextColor color = fishQuality == 5 ? TextColor.fromHexString("#28FACC") : TextColor.fromHexString("#1DCC9A");
						lore.add(Component.text("Fish Quality : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, color)).decoration(TextDecoration.ITALIC, false));
					}
				}

				Location location = Location.getLocation(monumenta.getString(Location.KEY));
				if (location != null) {
					lore.add(location.getDisplay());
				}
			}

			// LORE (Description)
			ReadableNBTList<String> description = monumenta.getStringList(ItemStatUtils.LORE_KEY);
			if (description != null) {
				for (String serializedLine : description) {
					Component lineAdd = Component.text("", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
					lineAdd = lineAdd.append(MessagingUtils.fromMiniMessage(serializedLine));
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
					EffectType effectType = EffectType.fromType(type);
					if (effectType != null) {
						int duration = effect.getInteger(ItemStatUtils.EFFECT_DURATION_KEY);
						double strength = effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY);
						Component comp = EffectType.getComponent(effectType, strength, duration);
						if (!lore.contains(comp)) {
							lore.add(comp);
						}
					}
				}
			}

			// ATTRIBUTES
			ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);
			if (attributes != null && !attributes.isEmpty() && ItemStatUtils.getEnchantmentLevel(enchantments, EnchantmentType.HIDE_ATTRIBUTES) == 0) {
				EnumMap<Slot, EnumMap<AttributeType, List<ReadWriteNBT>>> attributesBySlots = new EnumMap<>(Slot.class);
				for (ReadWriteNBT attribute : attributes) {
					Slot slot = Slot.getSlot(attribute.getString(Slot.KEY));
					AttributeType attributeType = AttributeType.getAttributeType(attribute.getString(ItemStatUtils.ATTRIBUTE_NAME_KEY));
					attributesBySlots.computeIfAbsent(slot, key -> new EnumMap<>(AttributeType.class)).computeIfAbsent(attributeType, key -> new ArrayList<>()).add(attribute);
				}

				// sort by slot
				EnumMap<Slot, List<Component>> slotMap = new EnumMap<>(Slot.class);

				for (Map.Entry<Slot, EnumMap<AttributeType, List<ReadWriteNBT>>> attributeSlotEntry : attributesBySlots.entrySet()) {
					Slot slot = attributeSlotEntry.getKey();
					EnumMap<AttributeType, List<ReadWriteNBT>> attributesBySlot = attributeSlotEntry.getValue();
					if (slot == null || attributesBySlot == null || attributesBySlot.isEmpty()) {
						continue;
					}
					List<Component> attributeList = new ArrayList<>();
					// sort by attribute
					EnumMap<AttributeType, EnumMap<Operation, Component>> attributeMap = new EnumMap<>(AttributeType.class);

					attributeList.add(Component.empty());
					attributeList.add(slot.getDisplay());

					for (Map.Entry<AttributeType, List<ReadWriteNBT>> attributeEntry : attributesBySlot.entrySet()) {
						AttributeType type = attributeEntry.getKey();
						List<ReadWriteNBT> attributesByType = attributeEntry.getValue();
						if (type == null || attributesByType == null) {
							continue;
						}
						EnumMap<Operation, Component> operationMap = new EnumMap<>(Operation.class);
						for (ReadWriteNBT attribute : attributesByType) {
							Operation operation = Operation.getOperation(attribute.getString(Operation.KEY));
							if (operation == null) {
								continue;
							}
							operationMap.put(operation, AttributeType.getDisplay(type, attribute.getDouble(ItemStatUtils.AMOUNT_KEY), slot, operation));
						}
						attributeMap.put(type, operationMap);
					}

					// add results together
					// Mainhand slots have attack and projectile related attributes higher than other attributes
					if (slot == Slot.MAINHAND) {
						boolean attackDamage = attributeMap.containsKey(AttributeType.ATTACK_DAMAGE_ADD);
						boolean attackSpeed = attributeMap.containsKey(AttributeType.ATTACK_SPEED);
						// show default attack speed if an item has attack damage, but no attack speed attribute (wands)
						if (attackDamage && !attackSpeed) {
							EnumMap<Operation, Component> operationMap = new EnumMap<>(Operation.class); // placeholder
							operationMap.put(Operation.ADD, AttributeType.getDisplay(AttributeType.ATTACK_SPEED, 0, slot, Operation.ADD));
							attributeMap.put(AttributeType.ATTACK_SPEED, operationMap);
						} else if (!attackDamage && attackSpeed) {
							EnumMap<Operation, Component> operationMap = new EnumMap<>(Operation.class); // placeholder
							operationMap.put(Operation.ADD, AttributeType.getDisplay(AttributeType.ATTACK_DAMAGE_ADD, 0, slot, Operation.ADD));
							attributeMap.put(AttributeType.ATTACK_DAMAGE_ADD, operationMap);
						}

						List<Component> regularList = new ArrayList<>();
						List<Component> mainhandPriorityList = new ArrayList<>();
						for (Map.Entry<AttributeType, EnumMap<Operation, Component>> entry : attributeMap.entrySet()) {
							if (AttributeType.MAINHAND_ATTRIBUTE_TYPES.contains(entry.getKey())) {
								mainhandPriorityList.addAll(entry.getValue().values());
							} else {
								regularList.addAll(entry.getValue().values());
							}
						}
						attributeList.addAll(mainhandPriorityList);
						attributeList.addAll(regularList);
					} else {
						for (EnumMap<Operation, Component> operationMap : attributeMap.values()) {
							attributeList.addAll(operationMap.values());
						}
					}
					slotMap.put(slot, attributeList);
				}
				// add the final lore to the main lore array
				for (List<Component> finalLore : slotMap.values()) {
					lore.addAll(finalLore);
				}
			}

			// CHARM
			ReadableNBTList<String> charmLore = monumenta.getStringList(ItemStatUtils.CHARM_KEY);
			if (charmLore != null && ItemStatUtils.isCharm(item)) {
				lore.add(Component.empty());
				lore.add(Component.text("When in Charm Slot:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				for (String serializedLine : charmLore) {
					Component lineAdd = MessagingUtils.parseComponent(serializedLine);
					lore.add(lineAdd);
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
			for (String path : unusedKeys) {
				cleanEmptyTags(nbt, path);
			}

			addDummyAttributeIfNeeded(item);

			// return if no NBT
			if (nbt.getKeys().isEmpty()) {
				return;
			}

			// placeholder attributes (for items with default attributes)
			nbt.modifyMeta((nbtr, meta) -> {

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
					if (!PotionUtils.BASE_POTION_ITEM_TYPES.contains(potionMeta.getBasePotionType())) {
						potionMeta.setBasePotionType(PotionType.AWKWARD);
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
					|| type == Material.TIPPED_ARROW) { // tipped arrows
					meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
				} else {
					meta.removeItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
				}
			});

			nbt.removeKey(ItemUtils.DISPLAY_KEY);
			ItemUtils.setDisplayName(nbt, ItemStatUtils.getName(nbt));
			ItemUtils.setDisplayLore(nbt, lore);
			ItemUtils.setPlainComponentLore(nbt, lore);
		});
	}

	public static void addDummyAttributeIfNeeded(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		boolean needsDummyAttribute = true;
		Multimap<Attribute, AttributeModifier> attributeModifiers = meta.getAttributeModifiers();
		if (attributeModifiers != null && !attributeModifiers.isEmpty()) {
			Collection<AttributeModifier> toughnessAttributes = attributeModifiers.get(Attribute.GENERIC_ARMOR_TOUGHNESS);
			boolean hasDummyAttribute = toughnessAttributes.size() == 1 && ItemStatUtils.MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME.equals(toughnessAttributes.iterator().next().getName());
			needsDummyAttribute = !(attributeModifiers.size() >= (hasDummyAttribute ? 2 : 1));
		}
		boolean hasDefaultAttributes = ItemUtils.hasDefaultAttributes(item);
		meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
		if (hasDefaultAttributes && needsDummyAttribute) {
			meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, cachedDummyAttributeModifier);
		}
		item.setItemMeta(meta);
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
			if (monumenta != null) {
				monumenta.removeKey(ItemStatUtils.STOCK_KEY);
			}
			nbt.removeKey("CustomPotionEffects");
			nbt.removeKey("AttributeModifiers");
			nbt.removeKey("Enchantments");
			// readd placeholder attribute if needed
			if (ItemUtils.hasDefaultAttributes(item)) {
				nbt.modifyMeta((nbtr, meta) -> {
						meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, cachedDummyAttributeModifier);
				});
			}
		});
	}

	public static void removeStats(ItemStack item) {
		NBT.modify(item, nbt -> {
			nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			nbt.removeKey("CustomPotionEffects");
			nbt.removeKey("AttributeModifiers");
			nbt.removeKey("Enchantments");
			// readd placeholder attribute if needed
			if (ItemUtils.hasDefaultAttributes(item)) {
				nbt.modifyMeta((nbtr, meta) -> {
						meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, cachedDummyAttributeModifier);
				});
			}
			GUIUtils.setPlaceholder(nbt);
		});
	}

	public static @Nullable String regenerateStats(ItemStack item) {
		List<String> errors = new ArrayList<String>(5);
		NBT.modify(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound("Monumenta");
			if (nbt.hasTag("CustomPotionEffects") && monumenta != null && monumenta.hasTag(ItemStatUtils.STOCK_KEY)) {
				errors.add("Has CustomPotionEffects & Monumenta.Stock tags!");
			} else if (nbt.hasTag("CustomPotionEffects")) {
				errors.add("Has CustomPotionEffects tag!");
			}

			List<VanillaEnchantmentType> nbtEnchantments = new ArrayList<>();
			List<VanillaAttributeType> nbtAttributes = new ArrayList<>();
			boolean isUnbreakable = false;

			// custom enchantment checking
			ReadWriteNBT enchantmentsNBT = ItemStatUtils.getEnchantments(nbt);
			if (enchantmentsNBT != null) {
				Set<String> keys = enchantmentsNBT.getKeys();
				for (String key : keys) {
					ReadWriteNBT enchantmentNBT = enchantmentsNBT.getCompound(key);
					if (enchantmentNBT == null) {
						errors.add("[Enchant] Invalid EnchantmentType: '" + key + "' (EnchantmentType removed)");
						continue;
					}
					EnchantmentType enchantment = EnchantmentType.getEnchantmentType(key);
					Integer nbtLevel = enchantmentNBT.getInteger(ItemStatUtils.LEVEL_KEY);

					// conversion from invalid enchantment to valid enchantment
					if (enchantmentConversionMap.containsKey(key)) {
						String newKey = enchantmentConversionMap.get(key);
						boolean found = false;
						for (String otherKey : keys) {
							if (Objects.equals(newKey, otherKey)) {
								found = true;
								break;
							}
						}
						if (!found) {
							errors.add("[Enchant] Converted EnchantmentType: ('" + key + "' -> '" + newKey + "')");
							enchantmentsNBT.removeKey(key);
							enchantmentsNBT.getOrCreateCompound(newKey).setInteger(ItemStatUtils.LEVEL_KEY, nbtLevel);
							key = newKey;
							enchantment = EnchantmentType.getEnchantmentType(key);
						}
					}

					if (enchantment == null) {
						errors.add("[Enchant] Invalid EnchantmentType: ('" + key + "' " + nbtLevel + ") (EnchantmentType removed)");
						enchantmentsNBT.removeKey(key);
						continue;
					}
					if (nbtLevel <= 0) { // enchant levels shouldn't go below 1
						errors.add("[Enchant] Invalid amount: ('" + enchantment.getName() + "' " + nbtLevel + ") (Level set to 1)");
						enchantmentNBT.setInteger(ItemStatUtils.LEVEL_KEY, 1);
					}

					// override for unbreakable items
					if (enchantment == EnchantmentType.UNBREAKABLE) {
						isUnbreakable = true;
						continue;
					}
					// some custom enchants don't have vanilla counterparts
					@Nullable Enchantment nbtEnchantment = enchantment.getEnchantment();
					if (nbtEnchantment == null) {
						continue;
					}
					nbtEnchantments.add(new VanillaEnchantmentType(nbtEnchantment, nbtLevel));
				}
			}

			ReadWriteNBTCompoundList attributesNBT = ItemStatUtils.getAttributes(nbt);
			if (attributesNBT != null && !attributesNBT.isEmpty()) {
				List<Integer> indexesToRemove = new ArrayList<>();
				int i = -1;
				for (ReadWriteNBT attribute : attributesNBT) {
					i++;
					String name = attribute.getString(ItemStatUtils.ATTRIBUTE_NAME_KEY);
					Double amount = attribute.getDouble(ItemStatUtils.AMOUNT_KEY);
					String operation = attribute.getString(Operation.KEY);
					String slot = attribute.getString(Slot.KEY);
					@Nullable AttributeType attributeType = AttributeType.getAttributeType(name);
					@Nullable Slot slotType = Slot.getSlot(slot);
					@Nullable Operation operationType = Operation.getOperation(operation);

					// validate the custom attribute
					if (attributeType == null || slotType == null || operationType == null) {
						errors.add("[Attribute] Invalid AttributeType: '" + name + "' slot: '" + slot + "' operation: '" + operation + "' amount: '" + amount + "' (AttributeType removed)");
						indexesToRemove.add(i);
						continue;
					}

					if (amount == 0) { // attribute amount can technically go into the negatives...
						errors.add("[Attribute] Invalid AttributeType amount: '" + name + "' slot: '" + slot + "' operation: '" + operation + "' amount: '" + amount + "' (AttributeType removed)");
						indexesToRemove.add(i);
						continue;
					}

					@Nullable Attribute nbtAttribute = attributeType.getAttribute();
					AttributeModifier.Operation nbtOperation = operationType.getAttributeOperation();
					@Nullable EquipmentSlot nbtSlot = slotType.getEquipmentSlot(); // can be null, means any slot
					if (nbtAttribute == null || nbtOperation == null) {
						continue;
					}
					nbtAttributes.add(new VanillaAttributeType(nbtAttribute, amount, nbtOperation, nbtSlot));
				}
				for (Integer index : indexesToRemove) {
					attributesNBT.remove(index);
				}
			}

			ReadWriteNBTCompoundList effectsNBT = ItemStatUtils.getEffects(nbt);
			if (effectsNBT != null && !effectsNBT.isEmpty()) {
				List<Integer> indexesToRemove = new ArrayList<>();
				int i = -1;
				for (ReadWriteNBT effect : effectsNBT) {
					i++;
					String type = effect.getString(ItemStatUtils.EFFECT_TYPE_KEY);
					int duration = effect.getInteger(ItemStatUtils.EFFECT_DURATION_KEY);
					double strength = effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY);

					// conversion from invalid type to valid type
					if (effectConversionMap.containsKey(type)) {
						String newType = effectConversionMap.get(type);
						boolean found = false;
						for (ReadWriteNBT otherEffect : effectsNBT) {
							String otherType = otherEffect.getString(ItemStatUtils.EFFECT_TYPE_KEY);
							if (Objects.equals(newType, otherType)) {
								found = true;
								break;
							}
						}
						if (!found) {
							errors.add("[Effect] Converted EffectType: ('" + type + "' -> '" + newType + "')");
							type = newType;
							effect.setString(ItemStatUtils.EFFECT_TYPE_KEY, type);
						}
					}

					EffectType effectType = EffectType.fromType(type);
					if (effectType == null) {
						errors.add("[Effect] Invalid EffectType: '" + type + "' duration: '" + duration + "' strength: '" + strength + "' (EffectType removed)");
						indexesToRemove.add(i);
						continue;
					}
					if (duration == 0 || duration < -1) {
						errors.add("[Effect] Invalid duration: '" + type + "' duration: '" + duration + "' strength: '" + strength + "' (Duration set to 1)");
						effect.setInteger(ItemStatUtils.EFFECT_DURATION_KEY, 1);
						continue;
					}
					if (strength <= 0) {
						errors.add("[Effect] Invalid strength: '" + type + "' duration: '" + duration + "' strength: '" + strength + "' (No Action)");
						continue;
					}
					// we don't store the actual potion effects on the item so no need to check for CustomPotionEffects validation
				}
				for (Integer index : indexesToRemove) {
					effectsNBT.remove(index);
				}
			}

			// regenerate display name and lore
			Component displayName = ItemUtils.getRawDisplayName(nbt);
			List<Component> lore = legacyGetLore(nbt);
			if (Component.IS_NOT_EMPTY.test(displayName)) {
				nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).setString(ItemStatUtils.NAME_KEY, MessagingUtils.toMiniMessage(displayName));
			}
			if (!lore.isEmpty()) {
				ReadWriteNBTList<String> loreNBT = nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).getStringList(ItemStatUtils.LORE_KEY);
				loreNBT.clear();
				for (Component line : lore) {
					loreNBT.add(MessagingUtils.toMiniMessage(line));
				}
				nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).removeKey(ItemStatUtils.LEGACY_LORE_KEY);
			}

			// regenerate charm lore
			if (monumenta != null && ItemStatUtils.isCharm(item)) {
				List<Component> charmEffects = legacyGetCharmEffects(nbt);
				if (!charmEffects.isEmpty()) {
					// mriaw
					ReadWriteNBTList<String> charmLoreNBT = nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).getStringList(ItemStatUtils.CHARM_KEY);
					charmLoreNBT.clear();
					for (Component effect : charmEffects) {
						charmLoreNBT.add(MessagingUtils.toMiniMessage(effect));
					}
					nbt.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY).removeKey(ItemStatUtils.LEGACY_CHARM_KEY);
				}
			}

			// purge vanilla enchantments and attribute modifiers
			nbt.removeKey("AttributeModifiers");
			nbt.removeKey("Enchantments");
			// readd vanilla enchantments and attribute modifiers
			final boolean unbreakable = isUnbreakable;
			nbt.modifyMeta((nbtr, meta) -> {
				meta.setUnbreakable(unbreakable);
				for (VanillaEnchantmentType type : nbtEnchantments) {
					meta.addEnchant(type.enchant, type.level, true);
				}
				// add placeholder if nbtAttributes is empty
				if (ItemUtils.hasDefaultAttributes(item) && nbtAttributes.size() == 0) {
					meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, cachedDummyAttributeModifier);
				} else {
					for (VanillaAttributeType type : nbtAttributes) {
						UUID uuid = UUID.nameUUIDFromBytes(("" + type.type + type.operation + type.slot).getBytes(StandardCharsets.UTF_8));
						if (type.slot == null) {
							meta.addAttributeModifier(type.type, new AttributeModifier(uuid, "Modifier", type.amount, type.operation));
						} else {
							meta.addAttributeModifier(type.type, new AttributeModifier(uuid, "Modifier", type.amount, type.operation, type.slot));
						}
					}
				}
			});
		});
		Material mat = item.getType();
		ItemMeta meta = item.getItemMeta();
		String plainName = ItemUtils.getPlainNameIfExists(item);
		if (!plainName.isEmpty() && !meta.hasDisplayName()) {
			if (mat != Material.WRITABLE_BOOK && mat != Material.WRITTEN_BOOK) {
				errors.add("Name: '" + plainName + "' (No Display Name)");
			}
		} else if (plainName.isEmpty() && meta.hasDisplayName()) {
			errors.add("Name: '" + meta.displayName() + "' (No Plain Name)");
			plainName = ItemUtils.toPlainTagText(meta.displayName());
		}
		if (!errors.isEmpty()) {
			return "Name: '" + plainName + "'\n" + String.join("\n", errors);
		}
		return null;
	}

	public static List<Component> legacyGetCharmEffects(final ReadableNBT nbt) {
		List<Component> lore = new ArrayList<>();
		ReadableNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
		if (monumenta == null) {
			return lore;
		}
		for (String serializedLine : monumenta.getStringList(ItemStatUtils.LEGACY_CHARM_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}

	public static List<Component> legacyGetLore(final ReadableNBT nbt) {
		List<Component> lore = new ArrayList<>();
		ReadableNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
		if (monumenta == null) {
			return lore;
		}
		for (String serializedLine : monumenta.getStringList(ItemStatUtils.LEGACY_LORE_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}
}
