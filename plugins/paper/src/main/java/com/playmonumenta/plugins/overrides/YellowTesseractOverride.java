package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBTItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;

public class YellowTesseractOverride extends BaseOverride {
	private static final TextComponent TESSERACT_NAME = Component.text("Tesseract of the Elements", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	private static final TextComponent CONFIGURED = Component.text("Tesseract of the Elements - Configured", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	private static final String CLASS_STR = "Class: ";
	private static final String SPEC_STR = "Specialization: ";
	private static final String PREFIX = " - ";
	private static final String CLASSL_STR = "Class Level: ";
	private static final String SPECL_STR = "Specialization Level: ";
	private static final String ENHANCE_STR = "Enhancement Level: ";
	private static final String TOTAL_LEVEL = "TotalLevel";
	private static final String TOTAL_SPEC = "TotalSpec";
	private static final String LEVEL = "Skill";
	private static final String SPEC_LEVEL = "SkillSpec";

	private static final double MOB_RANGE = 20;

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return interaction(player, action, item);
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return interaction(player, action, item);
	}

	@Override
	public boolean inventoryClickInteraction(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() != ClickType.RIGHT) {
			return true;
		}
		return interaction(player, Action.RIGHT_CLICK_AIR, item);
	}

	private boolean interaction(Player player, Action action, ItemStack item) {
		if (player == null) {
			return true;
		}

		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME.content()) || InventoryUtils.testForItemWithName(item, "(u)")) {
			return true;
		}

		if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return true;
		}
		// If player doesn't have the quest done, tell them they can't use it
		if (ScoreboardUtils.getScoreboardValue(player, "Quest114").orElse(0) < 15) {
			player.sendMessage(Component.text("You have not completed Primeval Creations III!", NamedTextColor.RED));
			return false;
		}
		// If a player doesn't have any abilities, tell them that's required
		if (AbilityManager.getManager().getPlayerAbilities(player).getAbilitiesIgnoringSilence().isEmpty()) {
			player.sendMessage(Component.text("You need to have a class and abilities first!", NamedTextColor.RED));
			return false;
		}
		// If the player is silenced/stasised, they cannot change abilities
		// This is to fix an exploit where multiple classes' abilities could be obtained at once
		if (AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
			player.sendMessage(Component.text("You cannot use this Tesseract while silenced!", NamedTextColor.RED));
			return false;
		}

		// This tesseract is not updated or otherwise broken, replace it with a fresh one
		if (ItemStatUtils.getTier(item) != ItemStatUtils.Tier.UNIQUE) {
			for (ItemStack tess : Bukkit.getLootTable(NamespacedKey.fromString("epic:r2/quests/114_elements")).populateLoot(FastUtils.RANDOM, new LootContext.Builder(player.getLocation()).build())) {
				item.setItemMeta(tess.getItemMeta());
				player.sendMessage(ChatColor.RED + "Your Tesseract had incorrect data, so it has been replaced. Only report this if it happens multiple times on the same Tesseract or if the replacement does not function.");
				return false;
			}
		}

		if (!InventoryUtils.testForItemWithLore(item, CLASS_STR)
				|| !InventoryUtils.testForItemWithName(item, CONFIGURED.content())) {
			/* Not active */
			if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
				storeSkills(player, item);
			}
		} else if (player.isSneaking()
			    && (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK))) {
			//Reset Tesseract with shift + left click
			resetTesseract(player, item);
		} else if (InventoryUtils.isSoulboundToPlayer(item, player)) {
			/* Active and soulbound to player */
			if ((action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK))) {
				boolean safeZone = ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5);
				int cd = ScoreboardUtils.getScoreboardValue(player, "YellowCooldown").orElse(0);
				// If the player is in a safezone and the Tesseract is on CD, remove CD and continue.
				if (safeZone && cd > 0) {
					ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 0);
					cd = 0;
				}
				// If the CD hasn't hit 0, tell the player and exit.
				if (cd != 0) {
					player.sendMessage(Component.text("The Tesseract is still on cooldown! You have ", NamedTextColor.RED)
						                   .append(Component.text(cd, NamedTextColor.RED).decorate(TextDecoration.BOLD))
						                   .append(Component.text(" minute" + (cd == 1 ? "" : "s") + " remaining until you can use it again.", NamedTextColor.RED)));
					return false;
				}
				// If there's a mob in range and the player isn't in a safezone,
				// tell the player and exit
				if (EntityUtils.withinRangeOfMonster(player, MOB_RANGE) && !safeZone) {
					player.sendMessage(Component.text("There are mobs nearby!", NamedTextColor.RED));
					return false;
				}
				// If Right-Click and YellowCooldown = 0 and either no mobs in range or in safezone,
				// we can change skills.
				changeSkills(player, item);
			}
		}

		return false;
	}

	private void resetTesseract(Player player, ItemStack item) {
		clearTesseractLore(item);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(TESSERACT_NAME);
		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		new PartialParticle(Particle.BLOCK_DUST, pLoc, 10, 0.5, 0.5, 0.5, 0, Material.BLACK_CONCRETE.createBlockData()).spawnAsPlayerActive(player);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has been reset!", NamedTextColor.YELLOW));
	}

	private void changeSkills(Player player, ItemStack item) {
		loadAbilityFromLore(player, item);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		new PartialParticle(Particle.FIREWORKS_SPARK, pLoc, 10, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has swapped your class!", NamedTextColor.YELLOW));

		if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5)) {
			ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 5);
		}
	}

	private static void storeSkills(Player player, ItemStack item) {
		item.setItemMeta(generateAbilityLore(player, item));

		ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.SOULBOUND, 1, player.getUniqueId(), false);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(CONFIGURED);
		item.setItemMeta(meta);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		new PartialParticle(Particle.SNOWBALL, pLoc, 10, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has stored your skills!", NamedTextColor.YELLOW));

		ItemStatUtils.generateItemStats(item);
		ItemUtils.setPlainTag(item);
	}

	private static void clearTesseractLore(ItemStack item) {
		NBTItem nbt = new NBTItem(item);
		List<String> lore = ItemStatUtils.getPlainLore(nbt);

		ItemStatUtils.removeInfusion(item, ItemStatUtils.InfusionType.SOULBOUND, false);
		for (int i = lore.size() - 1; i >= 0; --i) {
			String line = lore.get(i);
			if (line.startsWith(CLASS_STR)
				|| line.startsWith(SPEC_STR)
				|| line.startsWith(CLASSL_STR)
				|| line.startsWith(SPECL_STR)
				|| line.startsWith(ENHANCE_STR)
				|| line.startsWith(PREFIX)) {
				ItemStatUtils.removeLore(item, i);
			}
		}

		ItemStatUtils.generateItemStats(item);
	}

	// Returns an ItemMeta containing the lore based of Player's currently equipped skills.
	public static ItemMeta generateAbilityLore(Player player, ItemStack item) {
		// Prepare lore first.
		Integer classLevel = ScoreboardUtils.getScoreboardValue(player, LEVEL).orElse(0);
		Integer totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL).orElse(0);
		Integer specLevel = ScoreboardUtils.getScoreboardValue(player, SPEC_LEVEL).orElse(0);
		Integer totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC).orElse(0);
		Integer enhanceLevel = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE).orElse(0);
		Integer totalEnhance = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0);

		NBTItem nbt = new NBTItem(item);
		ItemStack copyItem = item.clone();
		List<String> lore = ItemStatUtils.getPlainLore(nbt);
		int newLoreIdx = lore.size();

		if (InventoryUtils.testForItemWithLore(copyItem, CLASS_STR)) {
			clearTesseractLore(copyItem);
		}

		ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(CLASS_STR + AbilityUtils.getClass(player), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(CLASSL_STR + (totalLevel - classLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(SPEC_STR + AbilityUtils.getSpec(player), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(SPECL_STR + (totalSpec - specLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(ENHANCE_STR + (totalEnhance - enhanceLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

		MonumentaClasses mClasses = new MonumentaClasses();
		List<AbilityInfo<?>> allAbilities = mClasses.mClasses.stream()
			                                    .flatMap(x -> Stream.concat(x.mAbilities.stream(), Stream.concat(x.mSpecOne.mAbilities.stream(), x.mSpecTwo.mAbilities.stream())))
			                                    .toList();
		for (AbilityInfo<?> reference : allAbilities) {
			if (reference.getDisplayName() != null && reference.getScoreboard() != null) {
				int value = ScoreboardUtils.getScoreboardValue(player, reference.getScoreboard()).orElse(0);
				if (value > 0) {
					boolean enhanced = false;
					if (value > 2) {
						enhanced = true;
						value -= 2;
					}
					ItemStatUtils.addLore(copyItem, newLoreIdx++, Component.text(PREFIX + reference.getDisplayName() + " : " + value + (enhanced ? "*" : ""),
						NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				}
			}
		}

		ItemStatUtils.generateItemStats(copyItem);
		ItemUtils.setPlainTag(copyItem);

		return copyItem.getItemMeta();
	}

	// Load abilities for player from Item Lore. Returns a boolean false if player had too many skills.\
	// And Tesseract needs to be reset.
	public static boolean loadAbilityFromLore(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = meta.lore();
		if (lore == null) {
			return true;
		}
		Map<String, Integer> targetSkills = new HashMap<>();
		int classVal = 0;
		int specVal = 0;
		int totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL).orElse(0);
		int totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC).orElse(0);
		int totalEnhancement = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0);

		/* Get all the target skills */
		for (Component comp : lore) {
			String str = MessagingUtils.plainText(comp);
			if (str.startsWith(CLASS_STR)) {
				classVal = AbilityUtils.getClass(str.substring(CLASS_STR.length()));
				ScoreboardUtils.setScoreboardValue(player, "Class", classVal);
			} else if (str.startsWith(SPEC_STR)) {
				specVal = AbilityUtils.getSpec(str.substring(SPEC_STR.length()));
				ScoreboardUtils.setScoreboardValue(player, "Specialization", specVal);
			} else if (str.startsWith(PREFIX)) {
				boolean enhanced = false;
				if (str.endsWith("*")) {
					enhanced = true;
					str = str.substring(0, str.length() - 1);
				}
				int level = Integer.parseInt(str.substring(str.length() - 1));
				if (enhanced) {
					level += 2;
				}
				String name = str.substring(PREFIX.length(), str.indexOf(" : "));
				targetSkills.put(name, level);
			}
		}

		MonumentaClasses classes = new MonumentaClasses();
		List<AbilityInfo<?>> allAbilities = classes.mClasses.stream()
			                                    .flatMap(c -> Stream.concat(c.mAbilities.stream(), Stream.concat(c.mSpecOne.mAbilities.stream(), c.mSpecTwo.mAbilities.stream())))
			                                    .toList();
		/* Remove all the player's current skills */
		for (AbilityInfo<?> reference : allAbilities) {
			if (reference.getScoreboard() != null) {
				ScoreboardUtils.setScoreboardValue(player, reference.getScoreboard(), 0);
			}
		}

		int totalSkillsAdded = 0;
		int totalSpecAdded = 0;
		int totalEnhancementsAdded = 0;
		// We want to separate different points here because of fast track, since people with fast track can cheese the skill cap otherwise
		// Check all abilities of the selected class + spec for matches
		int finalClassVal = classVal;
		List<AbilityInfo<?>> classAbilities = classes.mClasses.stream()
			                                      .filter(c -> c.mClass == finalClassVal)
			                                      .flatMap(x -> x.mAbilities.stream())
			                                      .toList();
		int finalSpecVal = specVal;
		List<AbilityInfo<?>> specAbilities = classes.mClasses.stream()
			                                     .filter(c -> c.mClass == finalClassVal)
			                                     .flatMap(c -> Stream.of(c.mSpecOne, c.mSpecTwo))
			                                     .filter(spec -> spec.mSpecialization == finalSpecVal)
			                                     .flatMap(spec -> spec.mAbilities.stream())
			                                     .toList();
		for (AbilityInfo<?> reference : classAbilities) {
			@Nullable Integer level = targetSkills.get(reference.getDisplayName());
			if (level != null) {
				String scoreboard = reference.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, scoreboard, level);
					if (level > 2) {
						totalSkillsAdded += level - 2;
						totalEnhancementsAdded++;
					} else {
						totalSkillsAdded += level;
					}
				}
			}
		}
		for (AbilityInfo<?> reference : specAbilities) {
			@Nullable Integer level = targetSkills.get(reference.getDisplayName());
			if (level != null) {
				String scoreboard = reference.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, scoreboard, level);
					totalSpecAdded += level;
				}
			}
		}

		// If the Tesseract had too many skills, reset the player and the item.
		if (totalSkillsAdded > totalLevel || totalSpecAdded > totalSpec || totalEnhancementsAdded > totalEnhancement) {
			player.sendMessage(Component.text(ItemUtils.getPlainName(item) + " has too many skills!", NamedTextColor.RED));
			AbilityManager.getManager().resetPlayerAbilities(player);
			player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
			return false;
		}

		ScoreboardUtils.setScoreboardValue(player, LEVEL, totalLevel - totalSkillsAdded);
		ScoreboardUtils.setScoreboardValue(player, SPEC_LEVEL, totalSpec - totalSpecAdded);
		ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE, totalEnhancement - totalEnhancementsAdded);
		if (totalSkillsAdded < totalLevel || totalSpecAdded < totalSpec || totalEnhancementsAdded < totalEnhancement) {
			player.sendMessage(Component.text("You have additional skill points to spend!", NamedTextColor.YELLOW));
		}

		AbilityManager.getManager().updatePlayerAbilities(player, true);

		return true;
	}
}
