package com.playmonumenta.plugins.overrides;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class YellowTesseractOverride extends BaseOverride {
	private static final TextComponent TESSERACT_NAME = Component.text("Tesseract of the Elements", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	private static final TextComponent CONFIGURED = Component.text("Tesseract of the Elements - Configured", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	private static final String CLASS_STR = "Class: ";
	private static final String SPEC_STR = "Specialization: ";
	private static final String PREFIX = " - ";
	private static final String CLASSL_STR = "Class Level: ";
	private static final String SPECL_STR = "Specialization Level: ";
	private static final String TOTAL_LEVEL = "TotalLevel";
	private static final String TOTAL_SPEC = "TotalSpec";
	private static final String LEVEL = "Skill";
	private static final String SPEC_LEVEL = "SkillSpec";

	private static final double MOB_RANGE = 20;

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return interaction(player, action, item);
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return interaction(player, action, item);
	}

	private boolean interaction(Player player, Action action, ItemStack item) {
		if (player == null) {
			return true;
		}

		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME.content())) {
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
		if (AbilityManager.getManager().getPlayerAbilities(player).getAbilities().isEmpty()) {
			player.sendMessage(Component.text("You need to have a class and abilities first!", NamedTextColor.RED));
			return false;
		}
		// If the player is silenced/stasised, they cannot change abilities
		// This is to fix an exploit where multiple classes' abilities could be obtained at once
		if (AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
			player.sendMessage(Component.text("You cannot use this Tesseract while silenced!", NamedTextColor.RED));
			return false;
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
					player.sendMessage(Component.text("The Tesseract is on cooldown!", NamedTextColor.RED));
					return true;
				}
				// If there's a mob in range and the player isn't in a safezone,
				// tell the player and exit
				if (EntityUtils.withinRangeOfMonster(player, MOB_RANGE) && !safeZone) {
					player.sendMessage(Component.text("There are mobs nearby!", NamedTextColor.RED));
					return true;
				}
				// If Right-Click and YellowCooldown = 0 and either no mobs in range or in safezone,
				// we can change skills.
				changeSkills(player, item);
			}
		}

		return true;
	}

	private void resetTesseract(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = meta.lore();

		clearTesseractLore(lore);

		meta.lore(lore);
		meta.displayName(TESSERACT_NAME);
		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.BLOCK_DUST, pLoc, 10, 0.5, 0.5, 0.5, 0, Material.BLACK_CONCRETE.createBlockData());
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has been reset!", NamedTextColor.YELLOW));
	}

	private void changeSkills(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = meta.lore();
		Map<String, Integer> targetSkills = new HashMap<String, Integer>();
		Integer totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL).orElse(0);
		Integer totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC).orElse(0);

		/* Get all the target skills */
		for (Component comp : lore) {
			String str = MessagingUtils.plainText(comp);
			if (str.startsWith(CLASS_STR)) {
				int classVal = AbilityUtils.getClass(str.substring(CLASS_STR.length()));
				ScoreboardUtils.setScoreboardValue(player, "Class", classVal);
			} else if (str.startsWith(SPEC_STR)) {
				int specVal = AbilityUtils.getSpec(str.substring(SPEC_STR.length()));
				ScoreboardUtils.setScoreboardValue(player, "Specialization", specVal);
			} else if (str.startsWith(PREFIX)) {
				int level = str.endsWith("2") ? 2 : 1;
				targetSkills.put(str.substring(PREFIX.length(), str.indexOf(" : ")), level);
			} else if (str.startsWith(CLASSL_STR)) {
				Integer classLevel = Integer.parseInt(str.substring(CLASSL_STR.length()));
				if (totalLevel != null && classLevel != null) {
					ScoreboardUtils.setScoreboardValue(player, LEVEL, totalLevel - classLevel);
				} else {
					ScoreboardUtils.setScoreboardValue(player, LEVEL, 0);
				}
			} else if (str.startsWith(SPECL_STR)) {
				Integer specLevel = Integer.parseInt(str.substring(SPECL_STR.length()));
				if (totalSpec != null && specLevel != null) {
					ScoreboardUtils.setScoreboardValue(player, SPEC_LEVEL, totalSpec - specLevel);
				} else {
					ScoreboardUtils.setScoreboardValue(player, SPEC_LEVEL, 0);
				}
			}
		}

		/* Remove all the player's current skills */
		AbilityCollection coll = AbilityManager.getManager().getPlayerAbilities(player);
		if (coll != null) {
			for (Ability ability : coll.getAbilities()) {
				String scoreboard = ability.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
				}
			}
		}

		/* Remove any disabled skills */
		List<Ability> dColl = AbilityManager.getManager().getDisabledAbilities();
		if (dColl != null) {
			for (Ability ability : dColl) {
				String scoreboard = ability.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
				}
			}
		}

		int totalSkillsAdded = 0;
		// Check Reference abilities for enabled skills
		for (Ability reference : AbilityManager.getManager().getReferenceAbilities()) {
			@Nullable Integer level = targetSkills.get(reference.getDisplayName());
			if (level != null) {
				String scoreboard = reference.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, reference.getScoreboard(), level);
					totalSkillsAdded += level;
				}
			}
		}

		// Check DisabledAbilities for disabled skills to be added (makes sure we get the spec skills even in R1)
		for (Ability reference : AbilityManager.getManager().getDisabledAbilities()) {
			@Nullable Integer level = targetSkills.get(reference.getDisplayName());
			if (level != null) {
				String scoreboard = reference.getScoreboard();
				if (scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, reference.getScoreboard(), level);
					totalSkillsAdded += level;
				}
			}
		}

		// If the Tesseract had too many skills, reset the player and the item.
		if (totalSkillsAdded > (totalLevel + totalSpec)) {
			player.sendMessage(Component.text("The Tesseract of the Elements has too many skills!", NamedTextColor.RED));
			resetTesseract(player, item);
			AbilityManager.getManager().resetPlayerAbilities(player);
			player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
			return;
		} else if (totalSkillsAdded < (totalLevel + totalSpec)) {
			player.sendMessage(Component.text("You have additional skill points to spend!", NamedTextColor.YELLOW));
		}

		AbilityManager.getManager().updatePlayerAbilities(player);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, pLoc, 10, 0.5, 0.5, 0.5, 0);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has swapped your class!", NamedTextColor.YELLOW));

		if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5)) {
			ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 5);
		}
	}

	private void storeSkills(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = meta.lore();
		Integer classLevel = ScoreboardUtils.getScoreboardValue(player, LEVEL).orElse(0);
		Integer totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL).orElse(0);
		Integer specLevel = ScoreboardUtils.getScoreboardValue(player, SPEC_LEVEL).orElse(0);
		Integer totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC).orElse(0);

		if (InventoryUtils.testForItemWithLore(item, CLASS_STR)) {
			clearTesseractLore(lore);
		}

		lore.add(Component.text(CLASS_STR + AbilityUtils.getClass(player), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(CLASSL_STR + (totalLevel - classLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(SPEC_STR + AbilityUtils.getSpec(player), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(SPECL_STR + (totalSpec - specLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

		AbilityCollection coll = AbilityManager.getManager().getPlayerAbilities(player);
		if (coll != null) {
			for (Ability ability : coll.getAbilities()) {
				if (ability.getDisplayName() != null) {
					lore.add(Component.text(PREFIX + ability.getDisplayName() + " : " + ability.getAbilityScore(),
					    NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				}
			}
		}

		lore.add(Component.text("* Soulbound to " + player.getName() + " *"));

		meta.lore(lore);
		meta.displayName(CONFIGURED);
		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.SNOWBALL, pLoc, 10, 0.5, 0.5, 0.5, 0);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 2.5f);
		player.sendMessage(Component.text("The Tesseract of the Elements has stored your skills!", NamedTextColor.YELLOW));
	}

	private void clearTesseractLore(List<Component> lore) {
		Iterator<Component> iter = lore.iterator();
		while (iter.hasNext()) {
			String current = MessagingUtils.plainText(iter.next());
			if (current.startsWith(CLASS_STR)
				|| current.startsWith(SPEC_STR)
				|| current.startsWith(CLASSL_STR)
				|| current.startsWith(SPECL_STR)
			    || current.startsWith(PREFIX)
				|| current.startsWith("* Soulbound to")) {
				iter.remove();
			}
		}
	}
}
