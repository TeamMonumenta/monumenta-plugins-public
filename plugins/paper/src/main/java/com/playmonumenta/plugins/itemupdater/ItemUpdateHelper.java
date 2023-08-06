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
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;

public class ItemUpdateHelper {
	public static void generateItemStats(final ItemStack item) {
		 List<Component> lore = NBT.modify(item, nbt -> {
				return generateItemStats(item, nbt);
		 });
		 if (!lore.isEmpty()) {
				postGenerateItemStats(item, lore);
		 }
	}

	private static List<Component> generateItemStats(final ItemStack item, final ReadWriteItemNBT nbt) {
		ReadWriteNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
		if (monumenta == null || monumenta.getKeys().isEmpty()) {
			return new ArrayList<>();
		} else {
			// There is probably a cleaner way to clean up unused NBT, not sure if recursion directly works due to the existence of both NBTCompounds and NBTCompoundLists
			// TODO: clean up other unused things from item (e.g. empty lore, reset hideflags if no NBT)

			Set<String> keys;

			ReadWriteNBT stock = monumenta.getCompound(ItemStatUtils.STOCK_KEY);
			if (stock != null) {
				ReadWriteNBT enchantments = stock.getCompound(EnchantmentType.KEY);
				if (enchantments != null) {
					keys = enchantments.getKeys();
					if (keys == null || keys.isEmpty()) {
						stock.removeKey(EnchantmentType.KEY);
					}
				}

				ReadWriteNBTCompoundList attributes = stock.getCompoundList(AttributeType.KEY);
				if (attributes != null && attributes.isEmpty()) {
					stock.removeKey(AttributeType.KEY);
				}

				ReadWriteNBTCompoundList effects = stock.getCompoundList(EffectType.KEY);
				if (effects != null && effects.isEmpty()) {
					stock.removeKey(EffectType.KEY);
				}

				keys = stock.getKeys();
				if (keys == null || keys.isEmpty()) {
					monumenta.removeKey(ItemStatUtils.STOCK_KEY);
				}
			}

			ReadWriteNBT player = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (player != null) {
				ReadWriteNBT infusions = player.getCompound(InfusionType.KEY);
				if (infusions != null) {
					keys = infusions.getKeys();
					if (keys == null || keys.isEmpty()) {
						player.removeKey(InfusionType.KEY);
					}
				}

				keys = player.getKeys();
				if (keys == null || keys.isEmpty()) {
					monumenta.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
				}
			}

			ReadWriteNBTList<String> lore = monumenta.getStringList(ItemStatUtils.LORE_KEY);
			if (lore != null && lore.isEmpty()) {
				monumenta.removeKey(ItemStatUtils.LORE_KEY);
			}
		}

		List<Component> lore = new ArrayList<>();

		// Checks for PI + Totem of Transposing
		if (ItemUtils.getPlainName(item).equals("Potion Injector") && ItemUtils.isShulkerBox(item.getType())) {
			List<String> plainLore = ItemUtils.getPlainLore(item);
			Component potionName = Objects.requireNonNull(item.lore()).get(1);
			lore.add(Component.text(plainLore.get(0), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			lore.add(potionName);
		} else if (ItemUtils.getPlainName(item).equals("Totem of Transposing")) {
			List<String> plainLore = ItemUtils.getPlainLore(item);
			lore.add(Component.text(plainLore.get(0), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		}

		List<Component> tagsLater = new ArrayList<>();
		List<Component> unbreakingTags = new ArrayList<>();

		if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.HIDE_ENCHANTS) == 0) {
			ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
			if (enchantments != null) {
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
		}

		// Add unbreaking tags
		lore.addAll(unbreakingTags);

		List<Component> statTrackLater = new ArrayList<>();
		List<Component> infusionTagsLater = new ArrayList<>();

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

		// Add infusions with message
		lore.addAll(infusionTagsLater);

		// Add stat tracking lore
		lore.addAll(statTrackLater);

		// Add Magic Wand Tag *after* all other stats,
		lore.addAll(tagsLater);

		if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.HIDE_INFO) == 0) {
			String regionString = monumenta.getString(Region.KEY);
			if (regionString != null) {
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
								}
							}
							monumenta.setString(Tier.KEY, tier.getName());
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
						lore.add(Component.text("Charm Power : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, TextColor.fromHexString("#FFFA75")).decoration(TextDecoration.ITALIC, false))
							.append(Component.text(" - ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)).append(ItemStatUtils.getCharmClassComponent(monumenta.getStringList(ItemStatUtils.CHARM_KEY))));
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

		ReadableNBTList<String> description = monumenta.getStringList(ItemStatUtils.LORE_KEY);
		if (description != null) {
			for (String serializedLine : description) {
				Component lineAdd = Component.text("", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
				lineAdd = lineAdd.append(MessagingUtils.fromGson(serializedLine));
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

		ReadWriteNBTCompoundList effects = ItemStatUtils.getEffects(nbt);
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

		ReadWriteNBTCompoundList attributes = ItemStatUtils.getAttributes(nbt);
		if (attributes != null
			&& ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.HIDE_ATTRIBUTES) == 0) {
			EnumMap<Slot, EnumMap<AttributeType, List<ReadWriteNBT>>> attributesBySlots = new EnumMap<>(Slot.class);
			for (ReadWriteNBT attribute : attributes) {
				Slot slot = Slot.getSlot(attribute.getString(Slot.KEY));
				AttributeType attributeType = AttributeType.getAttributeType(attribute.getString(ItemStatUtils.ATTRIBUTE_NAME_KEY));
				attributesBySlots.computeIfAbsent(slot, key -> new EnumMap<>(AttributeType.class))
					.computeIfAbsent(attributeType, key -> new ArrayList<>())
					.add(attribute);
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

		ReadableNBTList<String> charmLore = monumenta.getStringList(ItemStatUtils.CHARM_KEY);
		if (charmLore != null && ItemStatUtils.isCharm(item)) {
			lore.add(Component.empty());
			lore.add(Component.text("When in Charm Slot:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			for (String serializedLine : charmLore) {
				Component lineAdd = MessagingUtils.fromGson(serializedLine);
				lore.add(lineAdd);
			}
		}

		Set<String> keys = monumenta.getKeys();
		if (keys == null || keys.isEmpty()) {
			nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
		}

		return lore;
	}

	private static void postGenerateItemStats(ItemStack item, List<Component> lore) {
		lore.removeAll(Collections.singletonList(ItemStatUtils.DUMMY_LORE_TO_REMOVE));
		item.lore(lore);

		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_DYE);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		String name = item.getType().name();
		if (name.contains("POTION") || name.contains("PATTERN") || name.contains("SHIELD") || ItemUtils.isArrow(item)) {
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		}

		boolean hasDummyArmorToughnessAttribute = false;
		if (meta.hasAttributeModifiers()) {
			Collection<AttributeModifier> toughnessAttrs = meta.getAttributeModifiers(Attribute.GENERIC_ARMOR_TOUGHNESS);
			hasDummyArmorToughnessAttribute = toughnessAttrs != null && toughnessAttrs.size() == 1 && toughnessAttrs.iterator().next().getName().equals(ItemStatUtils.MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME);
		}

		if (!hasDummyArmorToughnessAttribute) {
			meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
			meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), ItemStatUtils.MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME, 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}

		Enchantment placeholder = ItemUtils.isSomeBow(item) ? Enchantment.WATER_WORKER : Enchantment.ARROW_DAMAGE;
		if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.NO_GLINT) > 0) {
			meta.removeEnchant(placeholder);
		} else {
			meta.addEnchant(placeholder, 1, true);
		}

		if (meta instanceof PotionMeta potionMeta) {
			potionMeta.clearCustomEffects();
			potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			item.setItemMeta(potionMeta);
		} else {
			item.setItemMeta(meta);
		}

		ItemUtils.setPlainLore(item);
	}
}
