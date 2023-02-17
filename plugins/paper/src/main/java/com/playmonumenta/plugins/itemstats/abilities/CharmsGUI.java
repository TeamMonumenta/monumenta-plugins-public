package com.playmonumenta.plugins.itemstats.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CharmsGUI extends Gui {
	private static final int START_OF_CHARMS = 45;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material RED_FILLER = Material.RED_STAINED_GLASS_PANE;
	private static final Material YELLOW_FILLER = Material.YELLOW_STAINED_GLASS_PANE;

	private final Player mTargetPlayer;
	private final boolean mMayEdit;

	public CharmsGUI(Player player) {
		this(player, player, true); // a player can always edit their own charms
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer) {
		this(requestingPlayer, targetPlayer, requestingPlayer.equals(targetPlayer)); // a player can always edit their own charms
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer, boolean mayEdit) {
		super(requestingPlayer, 54, Component.text(targetPlayer.getName() + "'s Charms"));
		mTargetPlayer = targetPlayer;
		mMayEdit = mayEdit;
		setFiller(FILLER);
	}

	@Override
	protected void setup() {
		// getOrDefault could hypothetically return a null, but present value. It won't, but it makes IntelliJ feel better I guess.
		List<ItemStack> charms = CharmManager.getInstance().mPlayerCharms.get(mTargetPlayer.getUniqueId());
		if (charms == null) {
			charms = new ArrayList<>();
		}
		int totalBudget = ScoreboardUtils.getScoreboardValue(mTargetPlayer, AbilityUtils.CHARM_POWER).orElse(0);
		if (totalBudget <= 0) {
			if (mTargetPlayer.equals(mPlayer)) {
				mTargetPlayer.sendMessage(ChatColor.RED + "You have no Charm Power!");
			} else {
				mPlayer.sendMessage(Component.text(mTargetPlayer.getName() + " has no Charm Power!", NamedTextColor.RED));
			}
			Bukkit.getScheduler().runTask(Plugin.getInstance(), this::close);
			return;
		}

		for (int i = START_OF_CHARMS; i < 52; i++) {
			setItem(i, new ItemStack(RED_FILLER, 1));
		}

		// Fill out yellow stained glass for visual display of charm budget
		for (int i = 0; i < totalBudget; i++) {
			int slot;
			if (i > 9) {
				slot = 29 + (i - 10);
			} else if (i > 4) {
				slot = 20 + (i - 5);
			} else {
				slot = 11 + i;
			}
			setItem(slot, new ItemStack(YELLOW_FILLER, 1));
		}

		Consumer<ItemStack> onCharmClick = charm -> {
			if (CharmManager.getInstance().removeCharm(mTargetPlayer, charm)) {
				InventoryUtils.giveItem(mPlayer, charm);
				mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
				update();
			} else {
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.5f, 1f);
			}
		};

		// Display active charms
		List<ItemStack> indexedCharms = new ArrayList<>();
		for (int i = 0; i < charms.size(); i++) {
			ItemStack charm = charms.get(i);
			if (charm == null || charm.getType() == Material.AIR) {
				continue;
			}

			setItem(i + START_OF_CHARMS, ItemUtils.clone(charm))
				.onLeftClick(() -> onCharmClick.accept(charm));

			for (int j = 0; j < ItemStatUtils.getCharmPower(charm); j++) {
				indexedCharms.add(charm);
			}
		}

		for (int i = 0; i < indexedCharms.size(); i++) {
			int slot;
			if (i > 9) {
				slot = 29 + (i - 10);
			} else if (i > 4) {
				slot = 20 + (i - 5);
			} else {
				slot = 11 + i;
			}
			ItemStack charm = indexedCharms.get(i);
			setItem(slot, ItemUtils.clone(charm))
				.onLeftClick(() -> onCharmClick.accept(charm));
		}

		int charmPower = CharmManager.getInstance().getCharmPower(mTargetPlayer);

		{ // Charm power indicator
			ItemStack item = new ItemStack(Material.GLOWSTONE_DUST, Math.max(1, charmPower));

			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("" + charmPower + " Charm Power Used", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


			List<Component> lore = new ArrayList<>();
			lore.add(Component.text(String.format("%d Total Charm Power", totalBudget), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);

			item.setItemMeta(meta);
			ItemUtils.setPlainTag(item);
			setItem(0, item);
		}

		{ // Charm effect indicator
			ItemStack item = new ItemStack(Material.BOOK, 1);

			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Charm Effect Summary", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


			List<Component> lore = CharmManager.getInstance().getSummaryOfAllAttributesAsComponents(mTargetPlayer);
			meta.lore(lore);

			item.setItemMeta(meta);
			ItemUtils.setPlainTag(item);
			setItem(9, item);
		}

		{ // Escape gui button
			ItemStack item = new ItemStack(Material.BARRIER, 1);

			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Save and Exit GUI", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			item.setItemMeta(meta);
			ItemUtils.setPlainTag(item);
			setItem(53, item).onLeftClick(this::close);
		}

		if (charmPower > totalBudget && mMayEdit && mPlayer.equals(mTargetPlayer)) {
			for (ItemStack charm : new ArrayList<>(charms)) {
				if (CharmManager.getInstance().removeCharm(mTargetPlayer, charm)) {
					InventoryUtils.giveItem(mTargetPlayer, charm);
				}
			}
			mTargetPlayer.sendMessage(ChatColor.RED + "Your equipped charms cost more than your budget (likely because their cost was adjusted). Your charms have all be unequipped");
			mTargetPlayer.playSound(mTargetPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
			update();
		}
	}

	@Override
	protected boolean onGuiClick(InventoryClickEvent event) {
		return mMayEdit;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		// Attempt to load charm if clicked in inventory
		ItemStack item = event.getCurrentItem();
		if (item != null && item.getType() != Material.AIR) {
			if (CharmManager.getInstance().validateCharm(mTargetPlayer, item)) {
				if (CharmManager.getInstance().addCharm(mTargetPlayer, item)) {
					item.subtract();
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
					update();
				} else {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.5f, 1f);
				}
			} else {
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.5f, 1f);
			}
		}
	}

}
