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
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class YellowTesseractOverride extends BaseOverride {
	private static final String TESSERACT_NAME = "Tesseract of the Elements";
	private static final String CLASS_STR = ChatColor.YELLOW + "Class: ";
	private static final String SPEC_STR = ChatColor.YELLOW + "Specialization: ";
	private static final String PREFIX = ChatColor.YELLOW + " - ";


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

		if (!InventoryUtils.testForItemWithLore(item, CLASS_STR)) {
			/* Not active */
			if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
				storeSkills(player, item);
			}

		} else if (InventoryUtils.isSoulboundToPlayer(item, player)) {
			/* Active and soulbound to player */

			if (player.isSneaking()
			    && (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK))) {
				resetTesseract(player, item);
			} else if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
				changeSkills(player, item);
			}
		}

		return true;
	}

	private void resetTesseract(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();

		Iterator<String> iter = lore.iterator();
		while (iter.hasNext()) {
			String current = iter.next();
			if (current.startsWith(CLASS_STR)
				|| current.startsWith(SPEC_STR)
			    || current.startsWith(PREFIX)
				|| current.startsWith("* Soulbound to")) {
				iter.remove();
			}
		}

		meta.setLore(lore);
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
	}

	private void storeSkills(Player player, ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();

		lore.add(CLASS_STR + AbilityUtils.getClass(player));
		lore.add(SPEC_STR + AbilityUtils.getSpec(player));

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
		item.setItemMeta(meta);

		Location pLoc = player.getLocation();
		pLoc.setY(pLoc.getY() + player.getEyeHeight() - 0.5);
		player.getWorld().spawnParticle(Particle.SNOWBALL, pLoc, 10, 0.5, 0.5, 0.5, 0);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 2.5f);
		player.sendMessage(ChatColor.YELLOW + "The Tesseract of the Elements has stored your skills!");
	}
}
