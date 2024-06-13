package com.playmonumenta.plugins.commands;

import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.MobNBT;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.Attribute;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.AttributeContainer;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.AttributeType;
import com.goncalomb.bukkit.nbteditor.nbt.variables.EffectsVariable;
import com.goncalomb.bukkit.nbteditor.nbt.variables.FloatVariable;
import com.goncalomb.bukkit.nbteditor.nbt.variables.ItemsVariable;
import com.goncalomb.bukkit.nbteditor.nbt.variables.NBTVariable;
import com.google.common.collect.Multimap;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public class ScanMobsCommand {
	public static Iterator<Soul> ARMOR_SOULS = Collections.emptyIterator();
	public static Iterator<Soul> EQUIPMENT_SOULS = Collections.emptyIterator();
	public static Iterator<Soul> HEALTH_SOULS = Collections.emptyIterator();
	public static Iterator<Soul> EFFECT_SOULS = Collections.emptyIterator();

	public static EnumSet<EntityType> BASE_ARMOR_TYPES = EnumSet.of(
		EntityType.ZOMBIE,
		EntityType.ZOMBIFIED_PIGLIN,
		EntityType.ZOMBIE_VILLAGER,
		EntityType.HUSK,
		EntityType.DROWNED,
		EntityType.MAGMA_CUBE
	);

	public static Set<Enchantment> BAD_ENCHANTMENTS = Set.of(
		Enchantment.PROTECTION_ENVIRONMENTAL,
		Enchantment.PROTECTION_FIRE,
		Enchantment.PROTECTION_FALL,
		Enchantment.PROTECTION_EXPLOSIONS,
		Enchantment.PROTECTION_PROJECTILE,
		Enchantment.DAMAGE_ALL
	);

	private static final IntegerArgument countArg = new IntegerArgument("count");

	public static void register() {
		if (ServerProperties.getShardName().contains("build")) {
			return;
		}
		registerSubCommand("armor", ScanMobsCommand::refreshArmor, ScanMobsCommand::getArmor);
		registerSubCommand("equipment", ScanMobsCommand::refreshEquipment, ScanMobsCommand::getEquipment);
		registerSubCommand("health", ScanMobsCommand::refreshHealth, ScanMobsCommand::getHealth);
		registerSubCommand("effects", ScanMobsCommand::refreshEffects, ScanMobsCommand::getEffects);
	}

	private static void registerSubCommand(String command, Consumer<Player> refresh, BiConsumer<Player, Integer> getter) {
		new CommandAPICommand("scanmobs")
			.withPermission("monumenta.command.scanmobs")
			.withArguments(new LiteralArgument(command), new LiteralArgument("refresh"))
			.executesPlayer((player, args) -> {
				refresh.accept(player);
			}).register();

		new CommandAPICommand("scanmobs")
			.withPermission("monumenta.command.scanmobs")
			.withArguments(new LiteralArgument(command), new LiteralArgument("get"))
			.withOptionalArguments(countArg)
			.executesPlayer((player, args) -> {
				getter.accept(player, args.getByArgumentOrDefault(countArg, 1));
			}).register();
	}

	private static void getArmor(Player player, int count) {
		int i = 0;
		while (ARMOR_SOULS.hasNext() && i < count) {
			Soul soul = ARMOR_SOULS.next();
			i++;
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			if (!(entityNBT instanceof MobNBT mobNBT)) {
				return;
			}
			AttributeContainer container = mobNBT.getAttributes();
			Attribute armor = container.getAttribute(AttributeType.ARMOR);
			boolean updateManually = armor != null && armor.getBase() > 2;

			String mobName = MessagingUtils.plainText(soul.getDisplayName());

			container.setAttribute(new Attribute(AttributeType.ARMOR, 0));
			mobNBT.setAttributes(container);
			BookOfSouls bos = new BookOfSouls(mobNBT);

			if (updateManually) {
				ItemStack bosItem = bos.getBook();
				ItemUtils.modifyMeta(bosItem, meta -> meta.displayName(soul.getDisplayName()));
				InventoryUtils.giveItem(player, bosItem);
				player.sendMessage(mobName + " has more than 2 armor; gave BoS with no armor. Must be manually updated.");
			} else {
				SoulsDatabase.getInstance().update(player, bos);
				player.sendMessage("Updated " + mobName + " automatically.");
			}
		}
	}

	private static void refreshArmor(Player player) {
		List<Soul> souls = new ArrayList<>();
		List<SoulEntry> originalSouls = SoulsDatabase.getInstance().getSouls();
		for (SoulEntry soul : originalSouls) {
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			if (!(entityNBT instanceof MobNBT mobNBT)) {
				continue;
			}
			AttributeContainer container = mobNBT.getAttributes();
			Attribute armor = container.getAttribute(AttributeType.ARMOR);
			if (armor == null) {
				if (BASE_ARMOR_TYPES.contains(mobNBT.getEntityType())) {
					souls.add(soul);
				}
			} else {
				if (armor.getBase() > 0) {
					souls.add(soul);
				}
			}
		}
		ARMOR_SOULS = souls.iterator();
		player.sendMessage("Refreshed. Found " + souls.size() + " mobs with armor.");
	}

	private static void getEquipment(Player player, int count) {
		int i = 0;
		while (EQUIPMENT_SOULS.hasNext() && i < count) {
			Soul soul = EQUIPMENT_SOULS.next();
			i++;
			ItemStack bosItem = soul.getBoS();
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(bosItem);
			List<ItemStack> items = getEquippedItems(entityNBT);
			items.removeIf(item -> !isBadEquipment(item));

			String mobName = MessagingUtils.plainText(soul.getDisplayName());

			ItemUtils.modifyMeta(bosItem, meta -> meta.displayName(soul.getDisplayName()));
			InventoryUtils.giveItem(player, bosItem);
			Component msg = Component.text(mobName + " has items with stats: ");
			for (ItemStack item : items) {
				msg = msg.append(item.displayName()).append(Component.text(" "));
			}
			player.sendMessage(msg);
		}
	}

	private static void refreshEquipment(Player player) {
		List<Soul> souls = new ArrayList<>();
		List<SoulEntry> originalSouls = SoulsDatabase.getInstance().getSouls();
		for (SoulEntry soul : originalSouls) {
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			if (getEquippedItems(entityNBT).stream().anyMatch(ScanMobsCommand::isBadEquipment)) {
				souls.add(soul);
			}
		}
		EQUIPMENT_SOULS = souls.iterator();
		player.sendMessage("Refreshed. Found " + souls.size() + " mobs with bad equipment.");
	}

	private static List<ItemStack> getEquippedItems(EntityNBT entityNBT) {
		NBTVariable armorNBT = entityNBT.getVariable("ArmorItems");
		NBTVariable handNBT = entityNBT.getVariable("HandItems");
		List<ItemStack> items = new ArrayList<>();
		if (armorNBT instanceof ItemsVariable armorItems) {
			items.addAll(Arrays.asList(armorItems.getItems()));
		}
		if (handNBT instanceof ItemsVariable handItems) {
			items.addAll(Arrays.asList(handItems.getItems()));
		}
		return items;
	}

	private static boolean isBadEquipment(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		for (Enchantment ench : BAD_ENCHANTMENTS) {
			if (item.getEnchantmentLevel(ench) > 0) {
				return true;
			}
		}

		ItemMeta meta = item.getItemMeta();
		Multimap<org.bukkit.attribute.Attribute, AttributeModifier> attributes = meta.getAttributeModifiers();
		if (attributes != null) {
			for (Map.Entry<org.bukkit.attribute.Attribute, AttributeModifier> entry : attributes.entries()) {
				org.bukkit.attribute.Attribute attr = entry.getKey();
				AttributeModifier mod = entry.getValue();
				if (attr == org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS || attr == org.bukkit.attribute.Attribute.HORSE_JUMP_STRENGTH) {
					continue;
				}
				if (mod.getSlot() == EquipmentSlot.HAND && (attr == org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE || attr == org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED)) {
					continue;
				}
				if (!isIdentity(mod)) {
					return true;
				}
			}
		} else if (ItemUtils.isArmor(item) && item.getType() != Material.ELYTRA) {
			return true;
		}

		List<Component> lore = item.lore();
		if (lore != null && !lore.isEmpty()) {
			return true;
		}

		return false;
	}

	private static boolean isIdentity(AttributeModifier modifier) {
		if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_SCALAR_1) {
			return modifier.getAmount() == 1;
		} else {
			return modifier.getAmount() == 0;
		}
	}

	private static void getHealth(Player player, int count) {
		int i = 0;
		while (HEALTH_SOULS.hasNext() && i < count) {
			Soul soul = HEALTH_SOULS.next();
			i++;
			ItemStack bosItem = soul.getBoS();
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(bosItem);
			if (!(entityNBT instanceof MobNBT mobNBT)) {
				continue;
			}

			AttributeContainer container = mobNBT.getAttributes();
			Attribute maxHealthAttr = container.getAttribute(AttributeType.MAX_HEALTH);
			if (maxHealthAttr == null) {
				continue;
			}
			double maxHealth = maxHealthAttr.getBase();
			NBTVariable healthVar = mobNBT.getVariable("Health");
			if (!(healthVar instanceof FloatVariable floatVar)) {
				continue;
			}

			String floatString = floatVar.get();
			if (floatString == null) {
				continue;
			}
			float health = Float.parseFloat(floatString);
			healthVar.set(Float.toString((float) maxHealth), player);
			BookOfSouls newBos = new BookOfSouls(mobNBT);

			String mobName = MessagingUtils.plainText(soul.getDisplayName());

			ItemStack newBosItem = newBos.getBook();
			ItemUtils.modifyMeta(bosItem, meta -> meta.displayName(soul.getDisplayName()));
			InventoryUtils.giveItem(player, newBosItem);
			player.sendMessage(mobName + " has " + health + " out of " + maxHealth + " health; gave BoS with max health. Must be manually updated.");
		}
	}

	private static void refreshHealth(Player player) {
		List<Soul> souls = new ArrayList<>();
		List<SoulEntry> originalSouls = SoulsDatabase.getInstance().getSouls();
		for (SoulEntry soul : originalSouls) {
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			if (!(entityNBT instanceof MobNBT mobNBT)) {
				continue;
			}

			AttributeContainer container = mobNBT.getAttributes();
			Attribute maxHealthAttr = container.getAttribute(AttributeType.MAX_HEALTH);
			if (maxHealthAttr == null) {
				continue;
			}
			double maxHealth = maxHealthAttr.getBase();

			NBTVariable healthVar = mobNBT.getVariable("Health");
			if (healthVar instanceof FloatVariable floatVar) {
				String floatString = floatVar.get();
				if (floatString == null) {
					continue;
				}
				float health = Float.parseFloat(floatString);
				if (Math.abs(maxHealth - health) > 0.001) {
					souls.add(soul);
				}
			}
		}
		HEALTH_SOULS = souls.iterator();
		player.sendMessage("Refreshed. Found " + souls.size() + " mobs with non-max health.");
	}

	private static void getEffects(Player player, int count) {
		int i = 0;
		while (EFFECT_SOULS.hasNext() && i < count) {
			Soul soul = EFFECT_SOULS.next();
			i++;
			ItemStack bosItem = soul.getBoS();
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(bosItem);
			List<PotionEffect> effects = getPotionEffects(entityNBT);

			String mobName = MessagingUtils.plainText(soul.getDisplayName());

			ItemUtils.modifyMeta(bosItem, meta -> meta.displayName(soul.getDisplayName()));
			InventoryUtils.giveItem(player, bosItem);
			Component msg = Component.text(mobName + " has bad effects: ");
			for (PotionEffect effect : effects) {
				if (!isBadEffect(effect)) {
					continue;
				}
				msg = msg.append(Component.text("{" + effect.getType() + " (" + StringUtils.ticksToTime(effect.getDuration()) + ")} "));
			}

			player.sendMessage(msg);
		}
	}

	private static void refreshEffects(Player player) {
		List<Soul> souls = new ArrayList<>();
		List<SoulEntry> originalSouls = SoulsDatabase.getInstance().getSouls();
		for (SoulEntry soul : originalSouls) {
			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			for (PotionEffect effect : getPotionEffects(entityNBT)) {
				if (isBadEffect(effect)) {
					souls.add(soul);
					break;
				}
			}
		}
		EFFECT_SOULS = souls.iterator();
		player.sendMessage("Refreshed. Found " + souls.size() + " mobs with bad effects.");
	}

	private static List<PotionEffect> getPotionEffects(EntityNBT nbt) {
		if (nbt.getVariable("ActiveEffects") instanceof EffectsVariable effects) {
			ItemStack item = effects.getItem();
			if (item != null && item.getItemMeta() instanceof PotionMeta potionMeta) {
				return potionMeta.getCustomEffects();
			}
		}
		return List.of();
	}

	private static boolean isBadEffect(PotionEffect effect) {
		// Arbitrary choice of 10 minutes
		return effect.getDuration() >= 12000;
	}
}
