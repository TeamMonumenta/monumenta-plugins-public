package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.advancement.Advancement;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class KnickKnackSackGui extends Gui {

	private static final int INV_SIZE = 36;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Component BASE_TITLE = Component.text("Knick-Knack Sack");

	private enum Page {
		TRINKETS1,
		DEPTHS_TALISMANS,
		DEPTHS_REFUNDS(Component.text("Talisman Refunds", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
		CONTRACT_CONFIRM_DELETE(Component.text("Confirm Deletion", NamedTextColor.DARK_RED, TextDecoration.BOLD));

		public final Component mTitle;

		Page(Component title) {
			mTitle = title;
		}

		Page() {
			mTitle = BASE_TITLE;
		}
	}

	private Page mPage;

	public KnickKnackSackGui(Player player) {
		super(player, INV_SIZE, BASE_TITLE);
		mPage = Page.TRINKETS1;
		setFiller(FILLER);
	}

	@Override
	protected void setup() {
		setTitle(mPage.mTitle);
		switch (mPage) {
			default -> setupTrinketPage1();
			case DEPTHS_TALISMANS -> setupTalismansPage(false);
			case DEPTHS_REFUNDS -> setupTalismansPage(true);
			case CONTRACT_CONFIRM_DELETE -> setupContractConfirmDelete();
		}
	}

	private void setupTrinketPage1() {
		int pebSlot = 10;       // PEB
		int passSlot = 11;      // Season pass
		int cosmeticsSlot = 12; // Personal Cosmetics Interface
		int emoteSlot = 13;     // Emotes Trinket
		int bestiarySlot = 14;  // Bestiary
		int recordSlot = 15;    // Tlaxan Record Player or Soulsinger
		int contractSlot = 16;  // Crimson Contract
		int parrotSlot = 19;    // Parrot Bell
		int depthsSlot = 20;    // Depths Trinket
		int charmSlot = 21;     // Charms Trinket
		int delveSlot = 22;     // Delves Trinket

		// Information sign
		ItemStack info = new ItemStack(Material.OAK_SIGN);
		ItemMeta meta = info.getItemMeta();
		meta.displayName(Component.text("Trinkets", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		info.setItemMeta(meta);
		setItem(4, info);

		// PEB, free
		GuiItem tPeb = new GuiItem(
			getPlayerPEB()
		).onClick((evt) -> runConsoleCommand("openpeb @S"));
		setItem(pebSlot, tPeb);

		// Charm trinket, r3 access
		ItemStack charm = makeTrinketItemStack("epic:r3/charms/charms_trinket");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "R3Access").orElse(0) != 0) {
			overrideLore(charm,
				"Click to open the Charms Menu."
			);
			GuiItem tCharm = new GuiItem(charm).onClick((evt) -> runConsoleCommand("charm gui @S"));
			setItem(charmSlot, tCharm);
		} else {
			charm.setType(Material.BARRIER);
			overrideLore(charm, NamedTextColor.YELLOW,
				"Gain access to the Architect's Ring to unlock!");
			setItem(charmSlot, charm);
		}

		// Depths trinket, unlocked with lobby access, may want to consider making it inaccessible outside of depths
		ItemStack depths = makeTrinketItemStack("epic:items/functions/depths_trinket");
		NamespacedKey findDepthsLobby = NamespacedKey.fromString("monumenta:dungeons/depths/find");
		boolean playerFoundDepths = false;
		if (findDepthsLobby != null) {
			Advancement foundDepthsLobby = Bukkit.getAdvancement(findDepthsLobby);
			if (foundDepthsLobby != null) {
				playerFoundDepths = mPlayer.getAdvancementProgress(foundDepthsLobby).isDone();
			}
		}
		if (playerFoundDepths) {
			GuiItem tDepths = new GuiItem(depths)
				.onRightClick(() -> {
					runConsoleCommand("opendepthsgui summary @S");
				}).onLeftClick(() -> {
					runConsoleCommand("depths party @S");
				});
			setItem(depthsSlot, tDepths);
		} else {
			depths.setType(Material.BARRIER);
			overrideLore(depths, NamedTextColor.YELLOW,
				"Find the Darkest Depths lobby to unlock!");
			setItem(depthsSlot, depths);
		}

		// Bestiary, quest completion
		ItemStack bestiary = makeTrinketItemStack("epic:r1/quests/53_reward");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Quest53").orElse(0) > 10) {
			overrideLore(bestiary,
				"Keeps track of all the mobs",
				"you've killed across the world.",
				"Click to open."
			);
			GuiItem tBestiary = new GuiItem(bestiary).onClick((evt) -> {
				// Resolve this event first so the bestiary doesn't use the same click event once opened
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					runConsoleCommand("bestiary open @S");
				}, 1);
			});
			setItem(bestiarySlot, tBestiary);
		} else {
			bestiary.setType(Material.BARRIER);
			overrideLore(bestiary, NamedTextColor.YELLOW,
				"Complete the quest",
				"\"A Beast of a Book\" to unlock!");
			setItem(bestiarySlot, bestiary);
		}

		// Pass Trinket, always unlocked
		GuiItem tPass = makeTrinketGuiItem(
			"epic:pass/seasonal_pass_trinket",
			"Click to show active pass and",
			"weekly mission progress."
		).onClick((evt) -> runConsoleCommand("battlepass gui @S"));
		setItem(passSlot, tPass);

		// Cosmetics Trinket, always unlocked
		GuiItem tCosmetics = makeTrinketGuiItem(
			"epic:pass/personal_cosmetic_interface",
			"Click to show and equip your",
			"unlocked cosmetics."
		).onClick((evt) -> runConsoleCommand("cosmetics gui @S"));
		setItem(cosmeticsSlot, tCosmetics);

		// Parrot bell, always unlocked
		GuiItem tParrot = makeTrinketGuiItem(
			"epic:r2/items/randommistportjunk/portable_parrot_bell",
			"Click to open the parrot menu."
		).onClick((evt) -> runConsoleCommand("openparrotgui @S"));
		setItem(parrotSlot, tParrot);

		// Tlaxan Record Player OR Soulsinger, quest completion OR purchase of epic
		int recordScore = ScoreboardUtils.getScoreboardValue(mPlayer, "Quest47").orElse(0);
		ItemStack record = makeTrinketItemStack("epic:r1/items/misc/tlaxan_record_player");
		if (recordScore > 2) {
			overrideLore(record,
				"Click to open the menu",
				"and select a song to play."
			);

			// Override with soulsinger, if the player has it unlocked
			if (recordScore >= 5) {
				record = makeTrinketItemStack(
					"epic:r2/depths/loot/soulsinger",
					"Click to select a song from the menu."
				);
			}

			GuiItem tRecord = new GuiItem(record).onClick((evt) -> runConsoleCommand("sqgui show recordplayer @S"));
			setItem(recordSlot, tRecord);
		} else {
			// Locked item if neither record player nor soulsinger is unlocked
			record.setType(Material.BARRIER);
			overrideLore(record, NamedTextColor.YELLOW,
				"Complete the quest",
				"\"Halid's Song\" to unlock!");
			setItem(recordSlot, record);
		}

		// Delves Trinket, requires r3 access
		ItemStack delve = makeTrinketItemStack("epic:r3/delves/items/delves_trinket");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "R3Access").orElse(0) != 0) {
			overrideLore(delve,
				"Click to view Architect's Ring",
				"Overworld Delve Modifiers.",
				"Shift Right Click to clear modifiers."
			);
			GuiItem tDelve = new GuiItem(delve).onClick((evt) -> {
				// Gotta specify right-click from shift-right-click
				String showCommand = "delves show mods @S ring";
				if (evt.getClick() == ClickType.SHIFT_RIGHT) {
					runConsoleCommand("delves clear mods @S ring");
					mPlayer.sendMessage(Component.text("Your Architect's Ring Overworld Delve modifiers have been cleared.", NamedTextColor.GRAY));
				} else {
					runConsoleCommand(showCommand);
				}
			});
			setItem(delveSlot, tDelve);
		} else {
			delve.setType(Material.BARRIER);
			overrideLore(delve, NamedTextColor.YELLOW,
				"Gain access to the Architect's Ring to unlock!"
			);
			setItem(delveSlot, delve);
		}

		// Emotes Trinket, always unlocked
		GuiItem tEmote = makeTrinketGuiItem(
			"epic:r1/items/misc/emotes_trinket",
			"Left click to open the Emotes Menu.",
			"Right click to display Emote."
		).onLeftClick(() -> runConsoleCommand("emoji @S")
		).onRightClick(() -> runConsoleCommand("emote @S"));
		setItem(emoteSlot, tEmote);

		// Crimson Contract, unlocked with Quest 36 - A Study in Crimson
		ItemStack contract = makeTrinketItemStack("epic:r1/quests/36_crimson_contract");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Quest36").orElse(0) >= 28) {
			// Quest is complete
			overrideLore(contract,
				"Click to store/swap your experience.",
				"Shift Left Click to check stored experience.",
				"Shift Right Click to clear stored experience."
			);
			GuiItem tContract = new GuiItem(contract).onClick((evt) -> {
				if (!mPlayer.getGameMode().equals(GameMode.ADVENTURE) && !mPlayer.getGameMode().equals(GameMode.SURVIVAL)) {
					mPlayer.sendMessage(Component.text("You can only use this item in Survival and Adventure mode.", NamedTextColor.RED));
					return;
				}
				// Same mechanism as interactable: run this function, and player's temp score == 1 means contract is enabled
				runConsoleCommand("execute as @S run function monumenta:mechanisms/check/crimson_contract_enabled");
				if (ScoreboardUtils.getScoreboardValue(mPlayer, "temp").orElse(0) != 1) {
					mPlayer.sendMessage(Component.text("The magic of this area prevents you from using your contract.", NamedTextColor.AQUA));
					return;
				}

				switch (evt.getClick()) {
					default -> {
						// Swap experience
						runConsoleCommand("execute as @S in minecraft:overworld run function monumenta:mechanisms/contract/swap");
					}
					case SHIFT_LEFT -> {
						// Check experience
						runFunction("monumenta:mechanisms/contract/check");
					}
					case SHIFT_RIGHT -> {
						// Clear stored experience
						mPage = Page.CONTRACT_CONFIRM_DELETE;
						update();
					}
				}
			});
			setItem(contractSlot, tContract);
		} else {
			// Quest is not complete
			contract.setType(Material.BARRIER);
			overrideLore(contract, NamedTextColor.YELLOW,
				"Complete the quest",
				"\"A Study in Crimson: Part One\" to unlock!"
			);
			setItem(contractSlot, contract);
		}
		addNextButton(Page.DEPTHS_TALISMANS);
	}

	private void setupTalismansPage(boolean refund) {
		int dawnSlot = 10;
		int earthSlot = 11;
		int flameSlot = 12;
		int frostSlot = 13;
		int steelSlot = 14;
		int shadowSlot = 15;
		int windSlot = 16;
		int resetSlot = 22;
		int refundSlot = 31;    // Button to access refunds

		Talisman[] talismans = new Talisman[8]; // size = 8 since arrays are 0-indexed

		// Dawnbringer talisman
		Talisman dawnbringer = new Talisman(
			"Dawnbringer",
			"epic:r2/depths/utility/dawnbringer_talisman",
			"DDT1Purchased",
			1,
			0xf0b326,
			SunriseBrewCS.NAME);
		talismans[1] = dawnbringer;

		// Earthbound talisman
		Talisman earthbound = new Talisman(
			"Earthbound",
			"epic:r2/depths/utility/earthbound_talisman",
			"DDT2Purchased",
			2,
			0x6b3d2d,
			BrambleShellCS.NAME);
		talismans[2] = earthbound;

		// Flamecaller talisman
		Talisman flamecaller = new Talisman(
			"Flamecaller",
			"epic:r2/depths/utility/flamecaller_talisman",
			"DDT3Purchased",
			3,
			0xf04e21,
			VolcanicBurstCS.NAME);
		talismans[3] = flamecaller;

		// Frostborn talisman
		Talisman frostborn = new Talisman(
			"Frostborn",
			"epic:r2/depths/utility/frostborn_talisman",
			"DDT4Purchased",
			4,
			0xa3cbe1,
			AvalanchexCS.NAME);
		talismans[4] = frostborn;

		// Steelsage talisman
		Talisman steelsage = new Talisman(
			"Steelsage",
			"epic:r2/depths/utility/steelsage_talisman",
			"DDT5Purchased",
			6,
			0x929292,
			FireworkStrikeCS.NAME);
		talismans[6] = steelsage;

		// Shadowdancer talisman
		Talisman shadowdancer = new Talisman(
			"Shadowdancer",
			"epic:r2/depths/utility/shadowdancer_talisman",
			"DDT6Purchased",
			5,
			0x7948af,
			DarkPunishmentCS.NAME);
		talismans[5] = shadowdancer;

		// Windwalker talisman
		Talisman windwalker = new Talisman(
			"Windwalker",
			"epic:r2/depths/utility/windwalker_talisman",
			"DDT7Purchased",
			7,
			0xc0dea9,
			WindStepCS.NAME);
		talismans[7] = windwalker;

		if (refund) {
			setItem(dawnSlot, makeRefundItem(dawnbringer));
			setItem(earthSlot, makeRefundItem(earthbound));
			setItem(flameSlot, makeRefundItem(flamecaller));
			setItem(frostSlot, makeRefundItem(frostborn));
			setItem(steelSlot, makeRefundItem(steelsage));
			setItem(shadowSlot, makeRefundItem(shadowdancer));
			setItem(windSlot, makeRefundItem(windwalker));
			return;
		}

		// Information sign
		ItemStack info = new ItemStack(Material.OAK_SIGN);
		ItemMeta meta = info.getItemMeta();
		meta.displayName(Component.text("Depths Talismans", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		info.setItemMeta(meta);
		setItem(4, info);

		setItem(dawnSlot, makeTalismanItem(dawnbringer));
		setItem(earthSlot, makeTalismanItem(earthbound));
		setItem(flameSlot, makeTalismanItem(flamecaller));
		setItem(frostSlot, makeTalismanItem(frostborn));
		setItem(steelSlot, makeTalismanItem(steelsage));
		setItem(shadowSlot, makeTalismanItem(shadowdancer));
		setItem(windSlot, makeTalismanItem(windwalker));

		// Reset button. Ender pearl when no preference is set, ender eye when preference is set
		ItemStack reset = new ItemStack(Material.ENDER_EYE);
		overrideLore(reset,
			"Click to reset your Depths tree preference."
		);
		meta = reset.getItemMeta();
		int preferenceValue = ScoreboardUtils.getScoreboardValue(mPlayer, "DDTalisman").orElse(0);
		if (preferenceValue == 0) {
			reset.setType(Material.ENDER_PEARL);
			meta.displayName(Component.text("No Depths Preference Set!").decoration(TextDecoration.ITALIC, false));
		} else {
			Talisman preference = talismans[preferenceValue];
			meta.displayName(Component.text("Your depths tree preference is: " + preference.mTreeName, TextColor.color(preference.mColor), TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
		}
		reset.setItemMeta(meta);
		setItem(resetSlot, new GuiItem(reset))
			.onClick((evt) -> {
				mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
				ScoreboardUtils.setScoreboardValue(mPlayer, "DDTalisman", 0);
				mPlayer.sendMessage(Component.text("Your Depths tree preference has been reset.", NamedTextColor.LIGHT_PURPLE));
				update();
			});

		if (canRefund(dawnbringer)
			|| canRefund(shadowdancer)
			|| canRefund(flamecaller)
			|| canRefund(windwalker)
			|| canRefund(steelsage)
			|| canRefund(frostborn)
			|| canRefund(earthbound)
		) {
			ItemStack refundButton = new ItemStack(Material.QUARTZ);
			meta = refundButton.getItemMeta();
			meta.displayName(Component.text("You are eligible for talisman refunds!").decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("Click to access and claim your refunds.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			refundButton.setItemMeta(meta);

			setItem(refundSlot, refundButton).onClick((evt) -> {
				mPage = Page.DEPTHS_REFUNDS;
				update();
			});
		}

		addPrevButton(Page.TRINKETS1);
	}

	private boolean canRefund(Talisman t) {
		// Players are eligible for a refund if they have the cosmetic skill, have the talisman in their inventory, and have not claimed the refund before.
		CosmeticsManager cm = CosmeticsManager.INSTANCE;

		return cm.playerHasCosmetic(mPlayer, CosmeticType.COSMETIC_SKILL, t.mAssociatedSkill)
			&& mPlayer.getInventory().contains(makeTrinketItemStack(t.mPath))
			&& ScoreboardUtils.getScoreboardValue(mPlayer, t.mUnlockObjective).orElse(0) == 1;
	}

	private GuiItem makeTalismanItem(Talisman t) {
		Component baseMessage = Component.text("Your Depths tree preference has been set to ", NamedTextColor.LIGHT_PURPLE);

		ItemStack talisman = makeTrinketItemStack(t.mPath);
		if (ScoreboardUtils.getScoreboardValue(mPlayer, t.mUnlockObjective).orElse(0) >= 1) {
			overrideLore(talisman,
				"Click to increase " + t.mTreeName + " tree",
				"odds to 75%. (100% in normal or rigged runs)"
			);
			return new GuiItem(talisman)
				.onClick((evt) -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
					ScoreboardUtils.setScoreboardValue(mPlayer, "DDTalisman", t.mPreferenceValue);
					mPlayer.sendMessage(
						baseMessage.append(Component.text(t.mTreeName, TextColor.color(t.mColor)))
					);
					update();
				});
		} else {
			talisman.setType(Material.BARRIER);
			overrideLore(talisman, NamedTextColor.YELLOW,
				"Purchase the " + t.mTreeName + " Talisman to unlock!"
			);
			return new GuiItem(talisman);
		}
	}

	private GuiItem makeRefundItem(Talisman t) {
		Material displayItem;
		switch (t.mAssociatedSkill) {
			case SunriseBrewCS.NAME -> displayItem = Material.HONEYCOMB_BLOCK;
			case DarkPunishmentCS.NAME -> displayItem = Material.NETHERITE_SWORD;
			case VolcanicBurstCS.NAME -> displayItem = Material.MAGMA_BLOCK;
			case WindStepCS.NAME -> displayItem = Material.FEATHER;
			case FireworkStrikeCS.NAME -> displayItem = Material.FIREWORK_ROCKET;
			case AvalanchexCS.NAME -> displayItem = Material.POWDER_SNOW_BUCKET;
			case BrambleShellCS.NAME -> displayItem = Material.SWEET_BERRIES;
			default -> displayItem = Material.BARRIER;
		}

		if (!canRefund(t)) {
			displayItem = Material.BARRIER;
		}

		ItemStack i = new ItemStack(displayItem);
		ItemMeta m = i.getItemMeta();
		m.displayName(Component.text("Refund Geodes: " + t.mAssociatedSkill, TextColor.color(0x5D2D87), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		if (canRefund(t)) {
			m.lore(List.of(
				Component.text("Click to claim a refund of 64 Voidstaned Geodes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("This will consume a " + t.mTreeName + " Talisman from your inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			));
			i.setItemMeta(m);
		} else {
			m.lore(List.of(
				Component.text("You are not eligible for this refund.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			));
			i.setItemMeta(m);
			return new GuiItem(i);
		}

		// The item will only have an onClick if the player is eligible for a refund
		return new GuiItem(i).onClick((evt) -> {
			ItemStack trinket = makeTrinketItemStack(t.mPath);
			if (mPlayer.getInventory().contains(trinket)) {
				if (mPlayer.getInventory().firstEmpty() < 0) {
					// if player inventory is full
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
					mPlayer.sendMessage(Component.text("Your inventory is full. Please clear a slot and try again.", NamedTextColor.RED));
					return;
				}
				// Give the player 64 geodes
				InventoryUtils.giveItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode"), 64);

				// Set scoreboard value so refund can't be claimed twice
				ScoreboardUtils.setScoreboardValue(mPlayer, t.mUnlockObjective, 2);

				// Remove the talisman from the player's inventory
				mPlayer.getInventory().remove(trinket);

				mPlayer.sendMessage(Component.text("You have been given a refund of 64 Voidstained Geodes!", NamedTextColor.GREEN));
				mPage = Page.DEPTHS_TALISMANS;
				update();
			} else {
				mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
				mPlayer.sendMessage(Component.text("You do not have a " + t.mTreeName + " Talisman in your inventory.", NamedTextColor.RED));
			}
		});
	}

	private void setupContractConfirmDelete() {
		ItemStack contract = makeTrinketItemStack("epic:r1/quests/36_crimson_contract");
		overrideLore(contract,
			NamedTextColor.RED,
			"Clearing stored levels and xp cannot be undone.",
			"Do you want to proceed?"
		);
		setItem(13, contract);

		ItemMeta meta;
		ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		meta = confirm.getItemMeta();
		meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.lore(List.of(Component.text("Clear stored levels and xp.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		confirm.setItemMeta(meta);
		ItemUtils.setPlainTag(confirm);
		setItem(20, confirm).onLeftClick(() -> {
			runFunction("monumenta:mechanisms/contract/clear");
			close();
		});

		ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
		meta = cancel.getItemMeta();
		meta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.lore(List.of(Component.text("Return to trinkets.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		cancel.setItemMeta(meta);
		ItemUtils.setPlainTag(cancel);
		setItem(24, cancel).onLeftClick(() -> {
			mPage = Page.TRINKETS1;
			update();
		});
	}

	private void addNextButton(Page nextPage) {
		ItemStack arrow = new ItemStack(Material.ARROW);
		ItemMeta meta = arrow.getItemMeta();
		meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		arrow.setItemMeta(meta);
		// Add to bottom-right corner
		setItem(INV_SIZE - 1, new GuiItem(arrow)).onLeftClick(() -> {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
			mPage = nextPage;
			update();
		});
	}

	private void addPrevButton(Page prevPage) {
		ItemStack arrow = new ItemStack(Material.ARROW);
		ItemMeta meta = arrow.getItemMeta();
		meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		arrow.setItemMeta(meta);
		// Add to bottom-left corner
		setItem(INV_SIZE - 9, new GuiItem(arrow)).onLeftClick(() -> {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
			mPage = prevPage;
			update();
		});
	}

	private void overrideLore(ItemStack item, String... loreOverride) {
		overrideLore(item, NamedTextColor.GRAY, loreOverride);
	}

	private void overrideLore(ItemStack item, TextColor color, String... loreOverride) {
		ItemMeta meta = item.getItemMeta();
		ArrayList<Component> loreList = new ArrayList<>();
		for (String line : loreOverride) {
			loreList.add(Component.text(line, color).decoration(TextDecoration.ITALIC, false));
		}
		if (loreList.size() != 0) {
			meta.lore(loreList);
		}
		item.setItemMeta(meta);
	}

	private GuiItem makeTrinketGuiItem(String path, String... loreOverride) {
		return new GuiItem(makeTrinketItemStack(path, loreOverride));
	}

	private ItemStack makeTrinketItemStack(String path, String... loreOverride) {
		ItemStack item = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(path));
		if (item == null) {
			item = new ItemStack(Material.RED_CONCRETE);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("ERROR LOADING ITEM", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(
				Component.text("Item path:"),
				Component.text(path)
			));
			item.setItemMeta(meta);
		}

		if (loreOverride.length > 0) {
			overrideLore(item, NamedTextColor.GRAY, loreOverride);
		}

		return item;
	}

	private ItemStack getPlayerPEB() {
		// Currently, PEBs are generated through the /give command, so for now I'm just using a vanilla book.
		// minecraft:written_book{resolved:1b,Enchantments:[{lvl:1s,id:"minecraft:power"}],Monumenta:{Lore:['{"italic":true,"color":"dark_purple","text":"* Skin : Enchanted Book *"}']},HideFlags:71,title:"PEB",author:"P.E.B.E.",plain:{display:{Name:"Personal Enchanted Book",Lore:["* Skin : Enchanted Book *"]}},AttributeModifiers:[{Name:"Dummy",Amount:1.0d,Operation:2,UUID:[I;-78197774,2127905715,-1380854353,659436837],AttributeName:"minecraft:generic.armor_toughness"}],display:{Name:'{"bold":true,"italic":false,"underlined":false,"color":"dark_purple","text":"Personal Enchanted Book"}',Lore:['{"italic":false,"color":"dark_gray","extra":[{"italic":true,"color":"dark_purple","text":"* Skin : Enchanted Book *"}],"text":""}']}}
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		book.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
		book.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		BookMeta bookMeta = (BookMeta) book.getItemMeta();
		bookMeta.setTitle(ChatColor.RESET + "" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "" + "Personal Enchanted Book");
		bookMeta.displayName(Component.text("Personal Enchanted Book", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		bookMeta.setAuthor("P.E.B.E.");
		bookMeta.lore(List.of(
			Component.text("Click to open your PEB", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		));

		book.setItemMeta(bookMeta);

		return book;
	}

	private void runConsoleCommand(String command) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("@S", mPlayer.getName()));
	}

	private void runFunction(String function) {
		// This is how it's done in ScriptedQuests
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + mPlayer.getName() + " run function " + function);
	}

	private static class Talisman {
		String mTreeName;
		String mPath;
		String mUnlockObjective;
		int mPreferenceValue;
		int mColor;
		String mAssociatedSkill;

		public Talisman(String treeName, String path, String unlockObjective, int preferenceValue, int color, String associatedSkill) {
			this.mTreeName = treeName;
			this.mPath = path;
			this.mUnlockObjective = unlockObjective;
			this.mPreferenceValue = preferenceValue;
			this.mColor = color;
			this.mAssociatedSkill = associatedSkill;
		}
	}
}
