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
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
	private static final int INV_SIZE = 45;
	private static final Component BASE_TITLE = Component.text("Knick-Knack Sack");

	private static final Talisman[] DEPTHS_TALISMAN_LIST = {
		// Dawnbringer talisman
		 new Talisman(
			"Dawnbringer",
			"epic:r2/depths/utility/dawnbringer_talisman",
			"DDT1Purchased",
			1,
			0xf0b326,
			SunriseBrewCS.NAME),
		// Earthbound talisman
		new Talisman(
			"Earthbound",
			"epic:r2/depths/utility/earthbound_talisman",
			"DDT2Purchased",
			2,
			0x6b3d2d,
			BrambleShellCS.NAME),
		// Flamecaller talisman
		new Talisman(
			"Flamecaller",
			"epic:r2/depths/utility/flamecaller_talisman",
			"DDT3Purchased",
			3,
			0xf04e21,
			VolcanicBurstCS.NAME),
		// Frostborn talisman
		new Talisman(
			"Frostborn",
			"epic:r2/depths/utility/frostborn_talisman",
			"DDT4Purchased",
			4,
			0xa3cbe1,
			AvalanchexCS.NAME),
		// Steelsage talisman
		// The unlock and preference scores for shadow and steel are switched!!
		new Talisman(
			"Steelsage",
			"epic:r2/depths/utility/steelsage_talisman",
			"DDT5Purchased",
			6,
			0x929292,
			FireworkStrikeCS.NAME),
		// Shadowdancer talisman
		new Talisman(
			"Shadowdancer",
			"epic:r2/depths/utility/shadowdancer_talisman",
			"DDT6Purchased",
			5,
			0x7948af,
			DarkPunishmentCS.NAME),
		// Windwalker talisman
		new Talisman(
			"Windwalker",
			"epic:r2/depths/utility/windwalker_talisman",
			"DDT7Purchased",
			7,
			0xc0dea9,
			WindStepCS.NAME)
	};

	private static final Talisman[] ZENITH_TALISMAN_LIST = {
		// Dawnbringer talisman
		new Talisman(
			"Dawnbringer",
			"epic:r3/depths2/dawnbringer_talisman_zenith",
			"CZT1Purchased",
			1,
			0xf0b326,
			SunriseBrewCS.NAME),
		// Earthbound talisman
		new Talisman(
			"Earthbound",
			"epic:r3/depths2/earthbound_talisman_zenith",
			"CZT2Purchased",
			2,
			0x6b3d2d,
			BrambleShellCS.NAME),
		// Flamecaller talisman
		new Talisman(
			"Flamecaller",
			"epic:r3/depths2/flamecaller_talisman_zenith",
			"CZT3Purchased",
			3,
			0xf04e21,
			VolcanicBurstCS.NAME),
		// Frostborn talisman
		new Talisman(
			"Frostborn",
			"epic:r3/depths2/frostborn_talisman_zenith",
			"CZT4Purchased",
			4,
			0xa3cbe1,
			AvalanchexCS.NAME),
		// Steelsage talisman
		// The unlock and preference scores for shadow and steel are switched!!
		new Talisman(
			"Steelsage",
			"epic:r3/depths2/steelsage_talisman_zenith",
			"CZT5Purchased",
			6,
			0x929292,
			FireworkStrikeCS.NAME),
		// Shadowdancer talisman
		new Talisman(
			"Shadowdancer",
			"epic:r3/depths2/shadowdancer_talisman_zenith",
			"CZT6Purchased",
			5,
			0x7948af,
			DarkPunishmentCS.NAME),
		// Windwalker talisman
		new Talisman(
			"Windwalker",
			"epic:r3/depths2/windwalker_talisman_zenith",
			"CZT7Purchased",
			7,
			0xc0dea9,
			WindStepCS.NAME)
	};

	private enum Page {
		TRINKETS1,
		DEPTHS_TALISMANS,
		DEPTHS_REFUNDS(Component.text("Depths Talisman Refunds", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
		ZENITH_TALISMANS,
		ZENITH_REFUNDS(Component.text("Zenith Talisman Refunds", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
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
	}

	@Override
	protected void setup() {
		setTitle(mPage.mTitle);
		switch (mPage) {
			default -> setupTrinketPage1();
			case DEPTHS_TALISMANS -> setupTalismansPage(false, false);
			case DEPTHS_REFUNDS -> setupTalismansPage(true, false);
			case ZENITH_TALISMANS -> setupTalismansPage(false, true);
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
		int depthsTalismanSlot = 23;    // Depths Talisman
		int zenithTalismanSlot = 24;    // Zenith Trinket
		int guildSlot = 25;     // Guild GUI
		int questSlot = 28;     // Quest guide
		int enchantSlot = 29;   // Enchantopedia

		// Information sign
		ItemStack info = GUIUtils.createBasicItem(Material.OAK_SIGN, "Trinkets", NamedTextColor.WHITE, true);
		setItem(4, info);

		// PEB, free
		GuiItem tPeb = new GuiItem(
			getPlayerPEB()
		).onClick((evt) -> runConsoleCommand("openpeb @S"));
		setItem(pebSlot, tPeb);

		// Charm trinket, r3 access
		ItemStack charm = makeTrinketItemStack("epic:r3/charms/charms_trinket");
		if (PlayerUtils.hasUnlockedRing(mPlayer)) {
			GUIUtils.splitLoreLine(charm, "Click to open the Charms Menu.", NamedTextColor.GRAY, true);
			GuiItem tCharm = new GuiItem(charm).onClick((evt) -> runConsoleCommand("charm gui @S"));
			setItem(charmSlot, tCharm);
		} else {
			charm = charm.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(charm, "Gain access to the Architect's Ring to unlock!", NamedTextColor.YELLOW, true);
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
		NamespacedKey findZenith = NamespacedKey.fromString("monumenta:dungeons/zenith/find");
		boolean playerFoundZenith = false;
		if (findZenith != null) {
			Advancement foundZenithLobby = Bukkit.getAdvancement(findZenith);
			if (foundZenithLobby != null) {
				playerFoundZenith = mPlayer.getAdvancementProgress(foundZenithLobby).isDone();
			}
		}
		if (playerFoundDepths || playerFoundZenith) {
			GuiItem tDepths = new GuiItem(depths)
				.onRightClick(() -> {
					runConsoleCommand("opendepthsgui summary @S");
				}).onLeftClick(() -> {
					runConsoleCommand("depths party @S");
				});
			setItem(depthsSlot, tDepths);
		} else {
			depths = depths.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(depths, "Find the Darkest Depths lobby to unlock!", NamedTextColor.YELLOW, true);
			setItem(depthsSlot, depths);
		}

		// Bestiary, quest completion
		ItemStack bestiary = makeTrinketItemStack("epic:r1/quests/53_reward");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Quest53").orElse(0) > 10) {
			GUIUtils.splitLoreLine(bestiary, "Keeps track of all the mobs you've killed across the world. Click to open.", NamedTextColor.GRAY, true);
			GuiItem tBestiary = new GuiItem(bestiary).onClick((evt) -> {
				// Resolve this event first so the bestiary doesn't use the same click event once opened
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					runConsoleCommand("bestiary open @S");
				}, 1);
			});
			setItem(bestiarySlot, tBestiary);
		} else {
			bestiary = bestiary.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(bestiary, "Complete the quest \"A Beast of a Book\" to unlock!", NamedTextColor.YELLOW, true);
			setItem(bestiarySlot, bestiary);
		}

		// Pass Trinket, always unlocked
		GuiItem tPass = makeTrinketGuiItem(
			"epic:pass/seasonal_pass_trinket",
			"Click to show active pass and weekly mission progress."
		).onClick((evt) -> runConsoleCommand("battlepass gui @S"));
		setItem(passSlot, tPass);

		// Cosmetics Trinket, always unlocked
		GuiItem tCosmetics = makeTrinketGuiItem(
			"epic:pass/personal_cosmetic_interface",
			"Click to show and equip your unlocked cosmetics."
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
			// Override with soulsinger, if the player has it unlocked
			if (recordScore >= 5) {
				record = makeTrinketItemStack(
					"epic:r2/depths/loot/soulsinger"
				);
			}
			GUIUtils.splitLoreLine(record, "Click to open the menu and select a song to play.", NamedTextColor.GRAY, true);

			GuiItem tRecord = new GuiItem(record).onClick((evt) -> runConsoleCommand("sqgui show recordplayer @S"));
			setItem(recordSlot, tRecord);
		} else {
			// Locked item if neither record player nor soulsinger is unlocked
			record = record.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(record, "Complete the quest \"Halid's Song\" to unlock!", NamedTextColor.YELLOW, true);
			setItem(recordSlot, record);
		}

		// Delves Trinket, requires r3 access
		ItemStack delve = makeTrinketItemStack("epic:r3/delves/items/delves_trinket");
		if (PlayerUtils.hasUnlockedRing(mPlayer)) {
			GUIUtils.splitLoreLine(delve, "Click to view Architect's Ring Overworld Delve Modifiers. Shift Right Click to clear modifiers.", NamedTextColor.GRAY, true);

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
			delve = delve.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(delve, "Gain access to the Architect's Ring to unlock!", NamedTextColor.YELLOW, true);
			setItem(delveSlot, delve);
		}

		// Emotes Trinket, always unlocked
		GuiItem tEmote = makeTrinketGuiItem(
			"epic:r1/items/misc/emotes_trinket",
			"Left click to open the Emotes Menu.\nRight click to display Emote."
		).onLeftClick(() -> runConsoleCommand("emoji @S")
		).onRightClick(() -> runConsoleCommand("emote @S"));
		setItem(emoteSlot, tEmote);

		// Crimson Contract, unlocked with Quest 36 - A Study in Crimson
		ItemStack contract = makeTrinketItemStack("epic:r1/quests/36_crimson_contract");
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Quest36").orElse(0) >= 28) {
			// Quest is complete
			GUIUtils.splitLoreLine(contract, "Click to store/swap your experience.\nShift Left Click to check stored experience.\nShift Right Click to clear stored experience.", NamedTextColor.GRAY, 50, true);
			GuiItem tContract = new GuiItem(contract).onClick((evt) -> {
				if (!mPlayer.getGameMode().equals(GameMode.ADVENTURE) && !mPlayer.getGameMode().equals(GameMode.SURVIVAL)) {
					mPlayer.sendMessage(Component.text("You can only use this item in Survival and Adventure mode.", NamedTextColor.RED));
					return;
				}
				// Same mechanism as interactable: run this function, and player's temp score == 1 means contract is enabled
				runConsoleCommand("execute as @S run function monumenta:mechanisms/check/crimson_contract_enabled");
				if (ScoreboardUtils.getScoreboardValue(mPlayer, "temp").orElse(0) != 1) {
					mPlayer.sendMessage(Component.text("The magic of this area prevents you from using your contract.", NamedTextColor.RED));
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
			contract = contract.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(contract, "Complete the quest \"A Study in Crimson: Part One\" to unlock!", NamedTextColor.YELLOW, true);
			setItem(contractSlot, contract);
		}

		// Depths Talismans, unlocked with Depths access
		if (playerFoundDepths) {
			ItemStack dTaliBase = GUIUtils.createBasicItem(Material.BLACK_DYE, "Depths Talismans", Location.DEPTHS.getColor(), true, "Click to view your Depths talismans.", NamedTextColor.YELLOW);
			GuiItem depthsTalismans = new GuiItem(dTaliBase)
				.onLeftClick(() -> {
					mPage = Page.DEPTHS_TALISMANS;
					update();
				});
			setItem(depthsTalismanSlot, depthsTalismans);
		} else {
			ItemStack dTaliBase = GUIUtils.createBasicItem(Material.BARRIER, "Depths Talismans", Location.DEPTHS.getColor(), true, "Find the Darkest Depths lobby to unlock!", NamedTextColor.YELLOW);
			setItem(depthsTalismanSlot, dTaliBase);
		}

		// Zenith Talismans, unlocked with Zenith access
		if (playerFoundZenith) {
			ItemStack zTaliBase = GUIUtils.createBasicItem(Material.ENDER_EYE, "Zenith Talismans", Location.ZENITH.getColor(), true, "Click to view your Zenith talismans.", NamedTextColor.YELLOW);
			GuiItem zenithTalismans = new GuiItem(zTaliBase)
				.onLeftClick(() -> {
					mPage = Page.ZENITH_TALISMANS;
					update();
				});
			setItem(zenithTalismanSlot, zenithTalismans);
		} else {
			ItemStack zTaliBase = GUIUtils.createBasicItem(Material.BARRIER, "Zenith Talismans", Location.ZENITH.getColor(), true, "Find the Celestial Zenith lobby to unlock!", NamedTextColor.YELLOW);
			setItem(zenithTalismanSlot, zTaliBase);
		}

		// Guild GUI
		ItemStack guildBase = LuckPermsIntegration.getNonNullGuildBanner(mPlayer);
		ItemMeta meta = guildBase.getItemMeta();
		meta.displayName(Component.text("Guild GUI", NamedTextColor.GOLD, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		} else {
			lore = new ArrayList<>(lore);
		}
		lore.add(0, Component.text("View Monumenta's guilds", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		guildBase.setItemMeta(meta);
		GuiItem guildItem = new GuiItem(guildBase)
			.onLeftClick(() -> GuildGui.showDefaultView(mPlugin, mPlayer));
		setItem(guildSlot, guildItem);

		// Quest Guide
		GuiItem questItem = new GuiItem(
			GUIUtils.createBasicItem(
				Material.SCUTE,
				"Quest Guide",
				NamedTextColor.GOLD,
				true,
				"Click to see available quests across Monumenta by region and town.",
				NamedTextColor.GRAY)
		).onClick(evt -> runConsoleCommand("sqgui show regionqg @S"));
		setItem(questSlot, questItem);

		// Enchantopedia
		GuiItem enchantopedia = new GuiItem(
			GUIUtils.createBasicItem(
				Material.ENCHANTED_BOOK,
				"Enchantopedia",
				NamedTextColor.AQUA,
				true,
				"Click to view enchantments and their descriptions.",
				NamedTextColor.GRAY)
		).onClick(evt -> {
			if (mPlayer.hasPermission("monumenta.command.openenchantexplanations.gui")) {
				runConsoleCommand("openenchantexplanationsfor @S");
			}
		});
		setItem(enchantSlot, enchantopedia);
	}

	private void setupTalismansPage(boolean refund, boolean celestialZenith) {
		int dawnSlot = 10;
		int earthSlot = 11;
		int flameSlot = 12;
		int frostSlot = 13;
		int steelSlot = 14;
		int shadowSlot = 15;
		int windSlot = 16;
		int resetSlot = 22;
		int refundSlot = 31;    // Button to access refunds

		Talisman[] talismans = celestialZenith ? ZENITH_TALISMAN_LIST : DEPTHS_TALISMAN_LIST;

		if (refund) {
			setItem(dawnSlot, makeRefundItem(talismans[0]));
			setItem(earthSlot, makeRefundItem(talismans[1]));
			setItem(flameSlot, makeRefundItem(talismans[2]));
			setItem(frostSlot, makeRefundItem(talismans[3]));
			setItem(steelSlot, makeRefundItem(talismans[4]));
			setItem(shadowSlot, makeRefundItem(talismans[5]));
			setItem(windSlot, makeRefundItem(talismans[6]));
			return;
		}

		// Information sign
		ItemStack info = GUIUtils.createBasicItem(Material.OAK_SIGN, celestialZenith ? "Zenith Talismans" : "Depths Talismans", (celestialZenith ? Location.ZENITH : Location.DEPTHS).getColor(), true);
		setItem(4, info);

		setItem(dawnSlot, makeTalismanItem(talismans[0], celestialZenith));
		setItem(earthSlot, makeTalismanItem(talismans[1], celestialZenith));
		setItem(flameSlot, makeTalismanItem(talismans[2], celestialZenith));
		setItem(frostSlot, makeTalismanItem(talismans[3], celestialZenith));
		setItem(steelSlot, makeTalismanItem(talismans[4], celestialZenith));
		setItem(shadowSlot, makeTalismanItem(talismans[5], celestialZenith));
		setItem(windSlot, makeTalismanItem(talismans[6], celestialZenith));

		// Reset button. Ender pearl when no preference is set, ender eye when preference is set
		String name = celestialZenith ? "Zenith" : "Depths";
		String objective = celestialZenith ? "CZTalisman" : "DDTalisman";

		int preferenceValue = ScoreboardUtils.getScoreboardValue(mPlayer, objective).orElse(0);
		Talisman preference = null;
		for (Talisman t : DEPTHS_TALISMAN_LIST) {
			if (t.mPreferenceValue == preferenceValue) {
				preference = t;
			}
		}

		if (preference != null) {
			ItemStack reset = GUIUtils.createBasicItem(Material.ENDER_EYE, "Your " + name + " tree preference is: " + preference.mTreeName, TextColor.color(preference.mColor), true, "Click to reset your " + name + " tree preference.", NamedTextColor.GRAY);
			setItem(resetSlot, new GuiItem(reset))
				.onClick((evt) -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
					ScoreboardUtils.setScoreboardValue(mPlayer, objective, 0);
					mPlayer.sendMessage(Component.text("Your " + name + " tree preference has been reset.", NamedTextColor.LIGHT_PURPLE));
					update();
				});
		} else {
			ItemStack reset = GUIUtils.createBasicItem(Material.ENDER_PEARL, "No " + name + " preference set!", NamedTextColor.WHITE, false);
			setItem(resetSlot, new GuiItem(reset));
		}

		if (!celestialZenith && Arrays.stream(talismans).anyMatch(this::canRefund)) {
			ItemStack refundButton = GUIUtils.createBasicItem(Material.QUARTZ, "You are eligible for talisman refunds!", NamedTextColor.WHITE, false, "Click to access and claim your refunds.", NamedTextColor.GRAY);
			setItem(refundSlot, refundButton).onClick((evt) -> {
				mPage = Page.DEPTHS_REFUNDS;
				update();
			});
		}

		addPrevButton();
	}

	private boolean canRefund(Talisman t) {
		// Players are eligible for a refund if they have the cosmetic skill, have the talisman in their inventory, and have not claimed the refund before.
		CosmeticsManager cm = CosmeticsManager.INSTANCE;

		return cm.playerHasCosmetic(mPlayer, CosmeticType.COSMETIC_SKILL, t.mAssociatedSkill)
			&& mPlayer.getInventory().containsAtLeast(makeTrinketItemStack(t.mPath), 1)
			&& ScoreboardUtils.getScoreboardValue(mPlayer, t.mUnlockObjective).orElse(0) >= 1;
	}

	private GuiItem makeTalismanItem(Talisman t, boolean celestialZenith) {
		Component baseMessage = Component.text("Your " + (celestialZenith ? "Zenith" : "Depths") + " tree preference has been set to ", NamedTextColor.LIGHT_PURPLE);

		ItemStack talisman = makeTrinketItemStack(t.mPath);
		if (ScoreboardUtils.getScoreboardValue(mPlayer, t.mUnlockObjective).orElse(0) >= 1) {
			GUIUtils.splitLoreLine(talisman, "Click to guarantee the " + t.mTreeName + " tree as one of the options at the beginning of the dungeon.", NamedTextColor.GRAY, true);
			return new GuiItem(talisman)
				.onClick((evt) -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1f);
					ScoreboardUtils.setScoreboardValue(mPlayer, celestialZenith ? "CZTalisman" : "DDTalisman", t.mPreferenceValue);
					mPlayer.sendMessage(
						baseMessage.append(Component.text(t.mTreeName, TextColor.color(t.mColor)))
					);
					update();
				});
		} else {
			talisman = talisman.withType(Material.BARRIER);
			GUIUtils.splitLoreLine(talisman, "Purchase the " + t.mTreeName + " " + (celestialZenith ? "Zenith" : "Depths") + " Talisman to unlock!", NamedTextColor.YELLOW, true);
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

		boolean canRefund = canRefund(t);
		if (!canRefund) {
			displayItem = Material.BARRIER;
		}

		ItemStack i = GUIUtils.createBasicItem(displayItem, "Refund Geodes: " + t.mAssociatedSkill, TextColor.color(0x5D2D87), true,
			canRefund ? "Click to claim a refund of 64 Voidstained Geodes. This will consume a " + t.mTreeName + " Talisman from your inventory." : "You are not eligible for this refund.", NamedTextColor.GRAY);

		if (!canRefund) {
			return new GuiItem(i);
		}

		// The item will only have an onClick if the player is eligible for a refund
		return new GuiItem(i).onClick((evt) -> {
			ItemStack trinket = makeTrinketItemStack(t.mPath);
			if (mPlayer.getInventory().containsAtLeast(trinket, 1)) {
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
				mPlayer.getInventory().removeItem(trinket);

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
		GUIUtils.splitLoreLine(contract, "Clearing stored levels and xp cannot be undone.\nDo you want to proceed?", NamedTextColor.RED, true);
		setItem(13, contract);

		ItemStack confirm = GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, "Confirm", NamedTextColor.GREEN, true, "Clear stored levels and xp.", NamedTextColor.GRAY);
		setItem(20, confirm).onLeftClick(() -> {
			runFunction("monumenta:mechanisms/contract/clear");
			close();
		});

		ItemStack cancel = GUIUtils.createBasicItem(Material.ORANGE_STAINED_GLASS_PANE, "Cancel", NamedTextColor.RED, true, "Return to trinkets.", NamedTextColor.GRAY);
		setItem(24, cancel).onLeftClick(() -> {
			mPage = Page.TRINKETS1;
			update();
		});
	}

	private void addPrevButton() {
		ItemStack arrow = GUIUtils.createBasicItem(Material.ARROW, "Previous Page", NamedTextColor.WHITE, true);
		// Add to bottom-left corner
		setItem(INV_SIZE - 9, new GuiItem(arrow)).onLeftClick(() -> {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
			mPage = Page.TRINKETS1;
			update();
		});
	}

	private GuiItem makeTrinketGuiItem(String path, String lore) {
		return new GuiItem(makeTrinketItemStack(path, lore));
	}

	private ItemStack makeTrinketItemStack(String path) {
		return makeTrinketItemStack(path, "");
	}

	private ItemStack makeTrinketItemStack(String path, String lore) {
		ItemStack item = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(path));
		if (item == null) {
			item = GUIUtils.createBasicItem(Material.RED_CONCRETE, "ERROR LOADING ITEM", NamedTextColor.RED, true, "Item path:\n" + path);
		} else if (!lore.isEmpty()) {
			GUIUtils.splitLoreLine(item, lore, NamedTextColor.GRAY, true);
		}

		return item;
	}

	private ItemStack getPlayerPEB() {
		// Currently, PEBs are generated through the /give command, so for now I'm just using a vanilla book.
		// minecraft:written_book{resolved:1b,Enchantments:[{lvl:1s,id:"minecraft:power"}],Monumenta:{Lore:['{"italic":true,"color":"dark_purple","text":"* Skin : Enchanted Book *"}']},HideFlags:71,title:"PEB",author:"P.E.B.E.",plain:{display:{Name:"Personal Enchanted Book",Lore:["* Skin : Enchanted Book *"]}},AttributeModifiers:[{Name:"Dummy",Amount:1.0d,Operation:2,UUID:[I;-78197774,2127905715,-1380854353,659436837],AttributeName:"minecraft:generic.armor_toughness"}],display:{Name:'{"bold":true,"italic":false,"underlined":false,"color":"dark_purple","text":"Personal Enchanted Book"}',Lore:['{"italic":false,"color":"dark_gray","extra":[{"italic":true,"color":"dark_purple","text":"* Skin : Enchanted Book *"}],"text":""}']}}
		Component title = Component.text("Personal Enchanted Book", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		ItemStack book = GUIUtils.createBasicItem(Material.WRITTEN_BOOK, 1, title, "Click to open your PEB", NamedTextColor.GRAY, 30, true);
		book.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
		book.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		BookMeta bookMeta = (BookMeta) book.getItemMeta();
		bookMeta.title(title);
		bookMeta.setAuthor("P.E.B.E.");
		book.setItemMeta(bookMeta);

		return book;
	}

	private void runConsoleCommand(String command) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(command.replace("@S", mPlayer.getName()));
	}

	private void runFunction(String function) {
		// This is how it's done in ScriptedQuests
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute as " + mPlayer.getName() + " run function " + function);
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
