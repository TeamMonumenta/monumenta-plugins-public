package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.InventoryUtils;
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
import org.bukkit.Material;
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

	public KnickKnackSackGui(Player player) {
		super(player, INV_SIZE, Component.text("Knick-Knack Sack"));
		setFiller(FILLER);
	}

	@Override
	protected void setup() {
		// PEB, free
		int pebSlot = 10;
		GuiItem tPeb = new GuiItem(
			getPlayerPEB()
		).onClick((evt) -> runConsoleCommand("openpeb @S"));
		setItem(pebSlot, tPeb);

		// Charm trinket, r3 access
		ItemStack charm = makeTrinketItemStack("epic:r3/charms/charms_trinket");
		int charmSlot = 11;
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

		// Depths trinket, depths access, may want to consider making it inaccessible outside of depths
		int depthsSlot = 12;
		GuiItem tDepths = makeTrinketGuiItem(
			"epic:items/functions/depths_trinket"
		).onRightClick(() -> {
			runConsoleCommand("opendepthsgui summary @S");
		}).onLeftClick(() -> {
			runConsoleCommand("depths party @S");
		});
		setItem(depthsSlot, tDepths);

		// Bestiary, quest completion
		ItemStack bestiary = makeTrinketItemStack("epic:r1/quests/53_reward");
		int bestiarySlot = 13;
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Quest53").orElse(0) > 10) {
			overrideLore(bestiary,
				"Keeps track of all the mobs",
				"you've killed across the world.",
				"Click to open."
			);
			GuiItem tBestiary = new GuiItem(bestiary).onClick((evt) -> runConsoleCommand("bestiary open @S"));
			setItem(bestiarySlot, tBestiary);
		} else {
			bestiary.setType(Material.BARRIER);
			overrideLore(bestiary, NamedTextColor.YELLOW,
				"Complete the quest",
				"\"A Beast of a Book\" to unlock!");
			setItem(bestiarySlot, bestiary);
		}

		// Pass Trinket, always unlocked
		int passSlot = 14;
		GuiItem tPass = makeTrinketGuiItem(
			"epic:pass/seasonal_pass_trinket",
			"Click to show active pass and",
			"weekly mission progress."
		).onClick((evt) -> runConsoleCommand("battlepass gui @S"));
		setItem(passSlot, tPass);

		// Cosmetics Trinket, always unlocked
		int cosmeticsSlot = 15;
		GuiItem tCosmetics = makeTrinketGuiItem(
			"epic:pass/personal_cosmetic_interface",
			"Click to show and equip your",
			"unlocked cosmetics."
		).onClick((evt) -> runConsoleCommand("cosmetics gui @S"));
		setItem(cosmeticsSlot, tCosmetics);

		// Parrot bell, always unlocked
		int parrotSlot = 16;
		GuiItem tParrot = makeTrinketGuiItem(
			"epic:r2/items/randommistportjunk/portable_parrot_bell",
			"Click to open the parrot menu."
		).onClick((evt) -> runConsoleCommand("openparrotgui @S"));
		setItem(parrotSlot, tParrot);

		// Tlaxan Record Player OR Soulsinger, quest completion OR purchase of epic
		int recordScore = ScoreboardUtils.getScoreboardValue(mPlayer, "Quest47").orElse(0);
		ItemStack record = makeTrinketItemStack("epic:r1/items/misc/tlaxan_record_player");
		int recordSlot = 19;
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
		int delveSlot = 20;
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
		int emoteSlot = 21;
		GuiItem tEmote = makeTrinketGuiItem(
			"epic:r1/items/misc/emotes_trinket",
			"Left click to open the Emotes Menu.",
			"Right click to display Emote."
		).onLeftClick(() -> runConsoleCommand("emoji @S")
		).onRightClick(() -> runConsoleCommand("emote @S"));
		setItem(emoteSlot, tEmote);
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
}
