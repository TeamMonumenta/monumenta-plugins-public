package com.playmonumenta.plugins.itemstats.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.PlayerDisplayCustomInventory;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
	private final ItemStack AVAILABLE_CHARM_POWER_SLOT;
	private final ItemStack UNAVAILABLE_CHARM_POWER_SLOT;
	private final ItemStack UNAVAILABLE_CHARM_POWER_SLOT_ZENITH;
	private final ItemStack CHARM_SLOT;
	private final ItemStack EXIT;
	private final ItemStack BACK_TO_PDGUI;
	private final boolean mGuiTextures;

	{
		AVAILABLE_CHARM_POWER_SLOT = GUIUtils.createBasicItem(Material.YELLOW_STAINED_GLASS_PANE,
			"Available Charm Power",
			NamedTextColor.YELLOW,
			"This slot can power\nadditional charms.");

		UNAVAILABLE_CHARM_POWER_SLOT = GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE,
			"Unlockable Charm Power",
			NamedTextColor.RED,
			"Progress further in the Architect's Ring to unlock more Charm Power.");

		UNAVAILABLE_CHARM_POWER_SLOT_ZENITH = GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE,
			"Purchasable Charm Power",
			NamedTextColor.RED,
			"Purchase more Charm Power in the Celestial Zenith lobby with Indigo Blightdust.");

		CHARM_SLOT = GUIUtils.createBasicItem(Material.YELLOW_STAINED_GLASS_PANE,
			"Available Charm Slot",
			NamedTextColor.YELLOW,
			"This slot can hold a charm if you have enough charm power.");

		EXIT = GUIUtils.createBasicItem(Material.BARRIER,
			"Save and Exit GUI",
			NamedTextColor.RED,
			true);

		BACK_TO_PDGUI = GUIUtils.createBasicItem(Material.ARROW,
			"Back to Player Details GUI",
			NamedTextColor.GRAY,
			true);
	}

	private final Player mTargetPlayer;
	private final boolean mMayEdit;
	private final CharmManager.CharmType mCharmType;
	private final boolean mFromPDGUI;

	public CharmsGUI(Player player) {
		this(player, player, true); // a player can always edit their own charms
	}

	public CharmsGUI(Player player, CharmManager.CharmType charmType) {
		this(player, player, true, charmType, false);
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer) {
		this(requestingPlayer, targetPlayer, requestingPlayer.equals(targetPlayer));
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer, CharmManager.CharmType charmType) {
		this(requestingPlayer, targetPlayer, requestingPlayer.equals(targetPlayer), charmType, false);
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer, boolean mayEdit) {
		this(requestingPlayer, targetPlayer, mayEdit, CharmManager.getInstance().mEnabledCharmType, false);
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer, boolean mayEdit, boolean fromPDGUI) {
		this(requestingPlayer, targetPlayer, mayEdit, CharmManager.getInstance().mEnabledCharmType, fromPDGUI);
	}

	public CharmsGUI(Player requestingPlayer, Player targetPlayer, boolean mayEdit, CharmManager.CharmType charmType, boolean fromPDGUI) {
		super(requestingPlayer, 54, Component.text(targetPlayer.getName() + "'s Charms"), true);
		mTargetPlayer = targetPlayer;
		mMayEdit = mayEdit;
		mCharmType = charmType;
		mFromPDGUI = fromPDGUI;
		mGuiTextures = GUIUtils.getGuiTextureObjective(requestingPlayer);
	}


	@Override
	protected void setup() {
		ItemStack unavailableItem = mCharmType == CharmManager.CharmType.NORMAL ? UNAVAILABLE_CHARM_POWER_SLOT : UNAVAILABLE_CHARM_POWER_SLOT_ZENITH;

		setItem(0, 8, GUIUtils.createGuiIdentifierItem("gui_charms_1", mGuiTextures));
		GUIUtils.setGuiNbtTag(AVAILABLE_CHARM_POWER_SLOT, "texture", "power_charms_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(unavailableItem, "texture", "power_charms_2", mGuiTextures);
		GUIUtils.setGuiNbtTag(CHARM_SLOT, "texture", "slot_charms_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(EXIT, "texture", "exit_charms_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(BACK_TO_PDGUI, "texture", "back_charms_1", mGuiTextures);

		// getOrDefault could hypothetically return a null, but present value. It won't, but it makes IntelliJ feel better I guess.
		List<ItemStack> charms = mCharmType.mPlayerCharms.get(mTargetPlayer.getUniqueId());
		if (charms == null) {
			charms = new ArrayList<>();
		}
		int totalBudget = mCharmType.getTotalCharmPower(mTargetPlayer);
		if (totalBudget <= 0) {
			if (mTargetPlayer.equals(mPlayer)) {
				mTargetPlayer.sendMessage(Component.text("You have no Charm Power!", NamedTextColor.RED));
			} else {
				mPlayer.sendMessage(Component.text(mTargetPlayer.getName() + " has no Charm Power!", NamedTextColor.RED));
			}
			Bukkit.getScheduler().runTask(Plugin.getInstance(), this::close);
			return;
		}

		for (int i = START_OF_CHARMS; i < 52; i++) {
			setItem(i, CHARM_SLOT);
		}

		// Fill out yellow stained glass for visual display of charm budget
		for (int i = 0; i < 15; i++) {
			int x = i % 5 + 2;
			int y = i / 5 + 1;
			if (i < totalBudget) {
				setItem(y, x, AVAILABLE_CHARM_POWER_SLOT);
			} else {
				setItem(y, x, unavailableItem);
			}
		}

		Consumer<ItemStack> onCharmClick = charm -> {
			if (InventoryUtils.canFitInInventory(charm, mTargetPlayer.getInventory())) {
				if (CharmManager.getInstance().removeCharm(mTargetPlayer, charm, mCharmType)) {
					InventoryUtils.giveItem(mPlayer, charm);
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
					update();
				} else {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.5f, 1f);
				}
			} else {
				mTargetPlayer.sendMessage(Component.text("You have no free inventory slots available. Please free up an inventory slot before retrieving this charm.", NamedTextColor.RED));
				mTargetPlayer.playSound(mTargetPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
				update();
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

		int charmPower = CharmManager.getInstance().getUsedCharmPower(mTargetPlayer, mCharmType);

		{ // Charm effect indicator
			ItemStack summary = new ItemStack(Material.BOOK, Math.max(1, charmPower));

			ItemMeta meta = summary.getItemMeta();
			meta.displayName(Component.text("Charm Effect Summary", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			List<Component> lore = CharmManager.getInstance().getSummaryOfAllAttributesAsComponents(mTargetPlayer, mCharmType);
			lore.add(Component.empty());
			lore.add(Component.text(charmPower + " Charm Power Used", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(totalBudget + " Total Charm Power", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);

			summary.setItemMeta(meta);
			ItemUtils.setPlainTag(summary);
			GUIUtils.setGuiNbtTag(summary, "texture", "summary_charms_1", mGuiTextures);
			setItem(9, summary);
		}

		// Back gui button (if here from player details GUI
		if (mFromPDGUI) {
			setItem(0, BACK_TO_PDGUI).onLeftClick(() -> {
				this.close();
				new PlayerDisplayCustomInventory(mPlayer, mTargetPlayer).openInventory(mPlayer, mPlugin);
			});
		}

		// Exit button
		setItem(5, 8, EXIT).onLeftClick(this::close);

		if (charmPower > totalBudget && mMayEdit && mPlayer.equals(mTargetPlayer)) {
			for (ItemStack charm : new ArrayList<>(charms)) {
				if (CharmManager.getInstance().removeCharm(mTargetPlayer, charm, mCharmType)) {
					InventoryUtils.giveItem(mTargetPlayer, charm);
				}
			}
			mTargetPlayer.sendMessage(Component.text("Your equipped charms cost more than your budget (likely because their cost was adjusted). Your charms have all be unequipped", NamedTextColor.RED));
			mTargetPlayer.playSound(mTargetPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
			update();
		}
	}

	@Override
	protected boolean onGuiClick(InventoryClickEvent event) {
		if (event.getSlot() != 0) {
			return mMayEdit;
		}
		return true;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (!mMayEdit) {
			event.setCancelled(true);
			return;
		}
		// Attempt to load charm if clicked in inventory
		ItemStack item = event.getCurrentItem();
		if (!ItemUtils.isNullOrAir(item) && CharmManager.getInstance().addCharm(mTargetPlayer, item, mCharmType)) {
			item.subtract();
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
			update();
		} else {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.5f, 1f);
		}
	}

}
