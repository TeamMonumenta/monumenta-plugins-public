package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.time.Instant;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EmojiCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
	public static final String EMOJI_CHOICE_BOARD = "DefaultEmoji";
	private static final String PATREON_BOARD = "Patreon";
	private static final int PATREON_MINIMUM = 10;
	private static final int[] EMOJI_LOCS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52};

	public static class Emoji {
		String mName;
		String mLore;
		Boolean mPatreon;
		Material mType;
		String mLeftClick;
		int mDefaultID;

		public Emoji(String name, String lore, Material type, Boolean patreon, String leftClick, int defaultId) {
			mName = name;
			mLore = lore;
			mType = type;
			mPatreon = patreon;
			mLeftClick = leftClick;
			mDefaultID = defaultId;
		}
	}

	public static final ArrayList<Emoji> EMOJI_LIST = new ArrayList<>();

	static {
		EMOJI_LIST.add(new Emoji("Thumbs Up", "Default Discord thumbs up!", Material.RABBIT_FOOT, false, "thumbsup", 1));
		EMOJI_LIST.add(new Emoji("Thumbs Down", "Default Discord thumbs down.", Material.PUFFERFISH, false, "thumbsdown", 2));
		EMOJI_LIST.add(new Emoji("Smiley Face", "Default Discord smile!", Material.PLAYER_HEAD, false, "smiley", 3));
		EMOJI_LIST.add(new Emoji("Croggers", "Monumenta's own happy croc!", Material.TURTLE_EGG, true, "croggers", 4));
		EMOJI_LIST.add(new Emoji("Gigachad", "The internet-famous man with the endlessly sharp jawline.", Material.DIAMOND_SWORD, true, "gigachad", 5));
		EMOJI_LIST.add(new Emoji("Confetti", "Party Time!", Material.BELL, true, "confetti", 6));
		EMOJI_LIST.add(new Emoji("Exclamation", "!!!!", Material.IRON_BARS, true, "exclamation", 7));
		EMOJI_LIST.add(new Emoji("Heart", "Love and prosperity.", Material.APPLE, true, "heart", 8));
		EMOJI_LIST.add(new Emoji("Jebaited", "idk man", Material.WITHER_SKELETON_SKULL, true, "jebaited", 9));
		EMOJI_LIST.add(new Emoji("NotLikeThis", "Why does it always come to this!", Material.RED_CONCRETE_POWDER, true, "notlikethis", 10));
		EMOJI_LIST.add(new Emoji("OK", "Generally neutral.", Material.BONE, true, "ok", 11));
		EMOJI_LIST.add(new Emoji("Pray", "Living on a ______!", Material.GOLDEN_APPLE, true, "pray", 12));
		EMOJI_LIST.add(new Emoji("Question", "What is the meaning of life?", Material.COD, true, "question", 13));
		EMOJI_LIST.add(new Emoji("Skull", "Literally dead.", Material.SKELETON_SKULL, true, "skull", 14));
	}


	public EmojiCustomInventory(Player player) {
		super(player, 54, "Emoji Choices");
		setLayout(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		Player player;
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}

		if (clickedItem != null && clickedItem.getType() != FILLER && clickedItem.getItemMeta().hasDisplayName() && !event.isShiftClick()) {
			String chosenName = clickedItem.getItemMeta().getDisplayName();
			for (Emoji item : EMOJI_LIST) {
				if (chosenName.contains(item.mName) && !chosenName.contains("(Locked)")) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mLeftClick);
					} else {
						ScoreboardUtils.setScoreboardValue(player, EMOJI_CHOICE_BOARD, item.mDefaultID);
						setLayout(player);
					}
				}
			}
		}
	}

	public static void completeCommand(Player player, String cmd) {
		if (cmd.isEmpty()) {
			return;
		}
		long timeLeft = COOLDOWNS.getOrDefault(player.getUniqueId(), 0L) - Instant.now().getEpochSecond();

		if (timeLeft > 0) {
			player.sendMessage("Too fast! You can only emote once every 15s (" + timeLeft + "s remaining)");
		} else {
			COOLDOWNS.put(player.getUniqueId(), Instant.now().getEpochSecond() + 15);
			String command = "execute as @S at @S run function monumenta:mechanisms/emojis/" + cmd + "_run";
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("@S", player.getName()));
		}
		player.closeInventory();
	}

	public ItemStack createCustomItem(Player player, Emoji emoji) {
		ItemStack newItem = new ItemStack(emoji.mType, 1);
		ItemMeta meta = newItem.getItemMeta();
		if (!emoji.mPatreon || ScoreboardUtils.getScoreboardValue(player, PATREON_BOARD) >= PATREON_MINIMUM) {
			if (ScoreboardUtils.getScoreboardValue(player, EMOJI_CHOICE_BOARD) == emoji.mDefaultID) {
				meta.displayName(Component.text(emoji.mName + " (Default)", NamedTextColor.GOLD)
					.decoration(TextDecoration.ITALIC, false));
			} else {
				meta.displayName(Component.text(emoji.mName, NamedTextColor.GOLD)
					.decoration(TextDecoration.ITALIC, false));
			}
			if (!emoji.mLore.isEmpty()) {
				GUIUtils.splitLoreLine(meta, emoji.mLore, 30, ChatColor.AQUA, true);
				GUIUtils.splitLoreLine(meta, "Right click to set as your default emoji.", 30, ChatColor.GRAY, false);
			}
		} else {
			meta.displayName(Component.text(emoji.mName + " (Locked)", NamedTextColor.DARK_RED)
				.decoration(TextDecoration.ITALIC, false));
			if (!emoji.mLore.isEmpty()) {
				GUIUtils.splitLoreLine(meta, emoji.mLore, 30, ChatColor.GRAY, true);
			}
			GUIUtils.splitLoreLine(meta, "Patron Tier 2+ Required to use.", 30, ChatColor.GRAY, false);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		ItemUtils.setPlainName(newItem, emoji.mName);
		return newItem;
	}

	public void setLayout(Player player) {
		mInventory.clear();

		mInventory.setItem(4, createBasicItem(Material.ENCHANTING_TABLE, "Choose an emoji below!", NamedTextColor.AQUA,
							true, "", ChatColor.GRAY));

		int locationIndex = 0;
		for (Emoji item : EMOJI_LIST) {
			mInventory.setItem(EMOJI_LOCS[locationIndex++], createCustomItem(player, item));
		}

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}

	public ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold));
		if (!desc.isEmpty()) {
			GUIUtils.splitLoreLine(meta, desc, 30, loreColor, true);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}
}
