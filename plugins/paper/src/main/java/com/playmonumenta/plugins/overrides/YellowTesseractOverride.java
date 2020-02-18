package com.playmonumenta.plugins.overrides;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class YellowTesseractOverride extends BaseOverride {
	private static final String TESSERACT_NAME = "Tesseract of the Elements";
	private static final String CONFIG = " - Configured";
	private static final String CLASS_STR = ChatColor.YELLOW + "Class: ";
	private static final String SPEC_STR = ChatColor.YELLOW + "Specialization: ";
	private static final String PREFIX = ChatColor.YELLOW + " - ";
	private static final String CLASSL_STR = ChatColor.YELLOW + "Class Level: ";
	private static final String SPECL_STR = ChatColor.YELLOW + "Specialization Level: ";
	private static final String TOTAL_LEVEL = "TotalLevel";
	private static final String TOTAL_SPEC = "TotalSpec";
	private static final String LEVEL = "Skill";
	private static final String SPEC_LEVEL = "SkillSpec";

	private static final double MOB_RANGE = 20;

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return interaction(plugin, player, action, item);
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return interaction(plugin, player, action, item);
	}

	private boolean interaction(Plugin plugin, Player player, Action action, ItemStack item) {
		if (player == null) {
			return true;
		}

		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME)) {
			return true;
		}

		if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return true;
		}
		// If player doesn't have the quest done, tell them they can't use it
		if (ScoreboardUtils.getScoreboardValue(player, "Quest114") < 15) {
			player.sendMessage(ChatColor.RED + "You have not completed Primeval Creations III!");
			return false;
		}
		// If a player doesn't have any abilities, tell them that's required
		if (AbilityManager.getManager().getPlayerAbilities(player).getAbilities().isEmpty()) {
			player.sendMessage(ChatColor.RED + "You need to have a class and abilities first!");
			return false;
		}

		if (!InventoryUtils.testForItemWithLore(item, CLASS_STR)
				|| !InventoryUtils.testForItemWithName(item, CONFIG)) {
			/* Not active */
			if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
				storeSkills(player, item);
			}

		} else if (InventoryUtils.isSoulboundToPlayer(item, player)) {
			/* Active and soulbound to player */

			if (player.isSneaking()
			    && (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK))) {
				resetTesseract(player, item);
			} else if ((action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK))) {
				boolean safeZone = ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5);
				int cd = ScoreboardUtils.getScoreboardValue(player, "YellowCooldown");
				// If the player is in a safezone and the Tesseract is on CD, remove CD and continue.
				if (safeZone && cd > 0) {
					ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 0);
					cd = 0;
				}
				// If the CD hasn't hit 0, tell the player and exit.
				if (cd != 0) {
					player.sendMessage(ChatColor.RED + "The Tesseract is on cooldown!");
					return true;
				}
				// If there's a mob in range and the player isn't in a safezone,
				// tell the player and exit
				if (EntityUtils.withinRangeOfMonster(player, MOB_RANGE) && !safeZone) {
					player.sendMessage(ChatColor.RED + "There are mobs nearby!");
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
		List<String> lore = meta.getLore();

		clearTesseractLore(lore);

		meta.setLore(lore);
		meta.setDisplayName(ChatColor.YELLOW.toString() + ChatColor.BOLD + TESSERACT_NAME);
		item.setItemMeta(meta);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.BLOCK_DUST, pLoc, 10, 0.5, 0.5, 0.5, 0, Material.BLACK_CONCRETE.createBlockData());
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 2.5f);
		player.sendMessage(ChatColor.YELLOW + "The Tesseract of the Elements has been reset!");
	}

	private void changeSkills(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		Map<String, Integer> targetSkills = new HashMap<String, Integer>();
		Integer totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL);
		Integer totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC);

		/* Get all the target skills */
		for (String str : lore) {
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
				if (ability.getDisplayName() != null && scoreboard != null) {
					ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
				}
			}
		}

		for (Ability reference : AbilityManager.getManager().getReferenceAbilities()) {
			Integer level = targetSkills.get(reference.getDisplayName());
			if (level != null) {
				ScoreboardUtils.setScoreboardValue(player, reference.getScoreboard(), level);
			}
		}

		AbilityManager.getManager().updatePlayerAbilities(player);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, pLoc, 10, 0.5, 0.5, 0.5, 0);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 2.5f);
		player.sendMessage(ChatColor.YELLOW + "The Tesseract of the Elements has swapped your class!");

		if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5)) {
			ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 5);
		}
	}

	private void storeSkills(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		Integer classLevel = ScoreboardUtils.getScoreboardValue(player, LEVEL);
		Integer totalLevel = ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL);
		Integer specLevel = ScoreboardUtils.getScoreboardValue(player, SPEC_LEVEL);
		Integer totalSpec = ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC);

		if (InventoryUtils.testForItemWithLore(item, CLASS_STR)) {
			clearTesseractLore(lore);
		}

		lore.add(CLASS_STR + AbilityUtils.getClass(player));
		lore.add(CLASSL_STR + (totalLevel - classLevel));
		lore.add(SPEC_STR + AbilityUtils.getSpec(player));
		lore.add(SPECL_STR + (totalSpec - specLevel));

		AbilityCollection coll = AbilityManager.getManager().getPlayerAbilities(player);
		if (coll != null) {
			for (Ability ability : coll.getAbilities()) {
				if (ability.getDisplayName() != null) {
					lore.add(PREFIX + ability.getDisplayName() + " : " + ability.getAbilityScore());
				}
			}
		}

		lore.add("* Soulbound to " + player.getName() + " *");

		meta.setLore(lore);
		meta.setDisplayName(ChatColor.YELLOW.toString() + ChatColor.BOLD + TESSERACT_NAME + CONFIG);
		item.setItemMeta(meta);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.SNOWBALL, pLoc, 10, 0.5, 0.5, 0.5, 0);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 2.5f);
		player.sendMessage(ChatColor.YELLOW + "The Tesseract of the Elements has stored your skills!");
	}

	private void clearTesseractLore(List<String> lore) {
		Iterator<String> iter = lore.iterator();
		while (iter.hasNext()) {
			String current = iter.next();
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
