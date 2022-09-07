package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionSelection;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

public final class DelveInfusionCustomInventory extends CustomInventory {

	@FunctionalInterface
	private interface ItemClicked {
		void run(Player player, Inventory clickedInventory, int slot);
	}

	private static final int MAX_LORE_LENGHT = 30;
	private static final Material JUNK_ITEM = Material.GRAY_STAINED_GLASS_PANE;

	private static final Map<DelveInfusionSelection, List<ItemStack>> mDelveInfusionPannelsMap = new HashMap<>();
	private static final Map<DelveInfusionSelection, String> mDelveMatsMap = new HashMap<>();
	private static final List<ItemStack> mDelvePannelList = new ArrayList<>();

	private static final List<ItemStack> mInvalidItems = new ArrayList<>();
	private static final ItemStack mRefundItem = new ItemStack(Material.GRINDSTONE);
	private static final ItemStack mMaxLevelReachedItem = new ItemStack(Material.CAKE);

	private final Map<Integer, ItemClicked> mMapFunction;

	private int mRowSelected = 99;

	static {
		//----------------------------------------------------------------------
		//                    DELVE INFUSIONS!
		//---------------------------------------------------------------------

		//Delve Mats
		//r1
		mDelveMatsMap.put(DelveInfusionSelection.PENNATE, "Soul Essences");
		mDelveMatsMap.put(DelveInfusionSelection.CARAPACE, "Beastly Broods");
		mDelveMatsMap.put(DelveInfusionSelection.AURA, "Plagueroot Saps");
		mDelveMatsMap.put(DelveInfusionSelection.EXPEDITE, "Arcane Crystals");
		mDelveMatsMap.put(DelveInfusionSelection.CHOLER, "Season's Wraths");
		mDelveMatsMap.put(DelveInfusionSelection.UNYIELDING, "Echoes of the Veil");
		mDelveMatsMap.put(DelveInfusionSelection.USURPER, "Nightmare Fuels");
		mDelveMatsMap.put(DelveInfusionSelection.VENGEFUL, "Persistent Parchments");
		//r2
		mDelveMatsMap.put(DelveInfusionSelection.EMPOWERED, "Refound Knowledge");
		mDelveMatsMap.put(DelveInfusionSelection.NUTRIMENT, "Roots of Balance");
		mDelveMatsMap.put(DelveInfusionSelection.EXECUTION, "Forgotten Ashes");
		mDelveMatsMap.put(DelveInfusionSelection.REFLECTION, "Aurora Shards");
		mDelveMatsMap.put(DelveInfusionSelection.MITOSIS, "Feverish Flesh");
		mDelveMatsMap.put(DelveInfusionSelection.ARDOR, "Despondent Doubloons");
		mDelveMatsMap.put(DelveInfusionSelection.EPOCH, "Weathered Runes");
		mDelveMatsMap.put(DelveInfusionSelection.NATANT, "Primordial Clay");
		mDelveMatsMap.put(DelveInfusionSelection.UNDERSTANDING, "Binah Leaves");
		//r3
		mDelveMatsMap.put(DelveInfusionSelection.REFRESH, "Silver Remnants");
		mDelveMatsMap.put(DelveInfusionSelection.SOOTHING, "Sorceress' Staves");
		mDelveMatsMap.put(DelveInfusionSelection.QUENCH, "Fenian Flowers");
		mDelveMatsMap.put(DelveInfusionSelection.GRACE, "Iridium Catalysts");

		//Load all the pannels for delves
		//mDelvePannelList

		//R1
		//White
		ItemStack whiteItem = new ItemStack(Material.WHITE_WOOL);
		ItemMeta whiteMeta = whiteItem.getItemMeta();
		whiteMeta.displayName(Component.text("Pennate", TextColor.fromCSSHexString("#FFFFFF")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(whiteMeta, "Fall damage is reduced by 5% per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> whiteLore = whiteMeta.lore();
		whiteLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.PENNATE), TextColor.fromHexString("#555555")));
		whiteMeta.lore(whiteLore);
		whiteItem.setItemMeta(whiteMeta);
		mDelvePannelList.add(whiteItem);

		//Orange
		ItemStack orangeItem = new ItemStack(Material.ORANGE_WOOL);
		ItemMeta orangeMeta = orangeItem.getItemMeta();
		orangeMeta.displayName(Component.text("Carapace", TextColor.fromCSSHexString("#FFAA00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(orangeMeta, "After being hit, you gain 1.25% damage reduction per level for 5s. Being hit again while active refreshes the duration.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> orangeLore = orangeMeta.lore();
		orangeLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.CARAPACE), TextColor.fromHexString("#555555")));
		orangeMeta.lore(orangeLore);
		orangeItem.setItemMeta(orangeMeta);
		mDelvePannelList.add(orangeItem);

		//Magenta
		ItemStack magentaItem = new ItemStack(Material.MAGENTA_WOOL);
		ItemMeta magentaMeta = magentaItem.getItemMeta();
		magentaMeta.displayName(Component.text("Aura", TextColor.fromCSSHexString("#FF55FF")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(magentaMeta, "Mobs in a 3 block radius from you are slowed by 2% per level for 0.5 seconds. This is refreshed as long as they are in range.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> magentaLore = magentaMeta.lore();
		magentaLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.AURA), TextColor.fromHexString("#555555")));
		magentaMeta.lore(magentaLore);
		magentaItem.setItemMeta(magentaMeta);
		mDelvePannelList.add(magentaItem);

		//Light Blue
		ItemStack lbItem = new ItemStack(Material.LIGHT_BLUE_WOOL);
		ItemMeta lbMeta = lbItem.getItemMeta();
		lbMeta.displayName(Component.text("Expedite", TextColor.fromCSSHexString("#4AC2E5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(lbMeta, "Damaging an enemy with an ability increases your movement speed by 1% per level for 5 seconds, stacking up to 3 times.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> lbLore = lbMeta.lore();
		lbLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.EXPEDITE), TextColor.fromHexString("#555555")));
		lbMeta.lore(lbLore);
		lbItem.setItemMeta(lbMeta);
		mDelvePannelList.add(lbItem);

		//Yellow
		ItemStack yellowItem = new ItemStack(Material.YELLOW_WOOL);
		ItemMeta yellowMeta = yellowItem.getItemMeta();
		yellowMeta.displayName(Component.text("Choler", TextColor.fromCSSHexString("#FFFF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(yellowMeta, "Deal 1% additional damage per level to any mob that is on fire, slowed, or stunned.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> yellowLore = yellowMeta.lore();
		yellowLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.CHOLER), TextColor.fromHexString("#555555")));
		yellowMeta.lore(yellowLore);
		yellowItem.setItemMeta(yellowMeta);
		mDelvePannelList.add(yellowItem);

		//Bonus
		ItemStack bonusItem = new ItemStack(Material.MOSSY_COBBLESTONE);
		ItemMeta bonusMeta = bonusItem.getItemMeta();
		bonusMeta.displayName(Component.text("Unyielding", TextColor.fromCSSHexString("#006400")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(bonusMeta, "Gain 0.4 Knockback Resistance per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> bonusLore = bonusMeta.lore();
		bonusLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.UNYIELDING), TextColor.fromHexString("#555555")));
		bonusMeta.lore(bonusLore);
		bonusItem.setItemMeta(bonusMeta);
		mDelvePannelList.add(bonusItem);

		//Reverie
		ItemStack reverieItem = new ItemStack(Material.NETHER_WART_BLOCK);
		ItemMeta reverieMeta = reverieItem.getItemMeta();
		reverieMeta.displayName(Component.text("Usurper", TextColor.fromCSSHexString("#790E47")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(reverieMeta, "Heal 2.5% of your max health per level whenever you slay an elite or boss enemy.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> reverieLore = reverieMeta.lore();
		reverieLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.USURPER), TextColor.fromHexString("#555555")));
		reverieMeta.lore(reverieLore);
		reverieItem.setItemMeta(reverieMeta);
		mDelvePannelList.add(reverieItem);

		//Ephemeral Corridors
		ItemStack corridorsItem = new ItemStack(Material.MAGMA_BLOCK);
		ItemMeta corridorsMeta = corridorsItem.getItemMeta();
		corridorsMeta.displayName(Component.text("Vengeful", TextColor.fromCSSHexString("#8B0000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(corridorsMeta, "Gain 2% damage per level against the last enemy that damaged you.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> corridorsLore = corridorsMeta.lore();
		corridorsLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.VENGEFUL), TextColor.fromHexString("#555555")));
		corridorsMeta.lore(corridorsLore);
		corridorsItem.setItemMeta(corridorsMeta);
		mDelvePannelList.add(corridorsItem);

		//R2
		//Lime
		ItemStack limeItem = new ItemStack(Material.LIME_WOOL);
		ItemMeta limeMeta = limeItem.getItemMeta();
		limeMeta.displayName(Component.text("Empowered", TextColor.fromCSSHexString("#55FF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(limeMeta, "When you gain XP, you have a 0.25% chance per XP point per level to repair all currently equipped items by 1% of their max durability.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> limeLore = limeMeta.lore();
		limeLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.EMPOWERED), TextColor.fromHexString("#555555")));
		limeMeta.lore(limeLore);
		limeItem.setItemMeta(limeMeta);
		mDelvePannelList.add(limeItem);

		//Pink
		ItemStack pinkItem = new ItemStack(Material.PINK_WOOL);
		ItemMeta pinkMeta = pinkItem.getItemMeta();
		pinkMeta.displayName(Component.text("Nutriment", TextColor.fromCSSHexString("#FF69B4")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(pinkMeta, "Gain 1.5% extra healing per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> pinkLore = pinkMeta.lore();
		pinkLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.NUTRIMENT), TextColor.fromHexString("#555555")));
		pinkMeta.lore(pinkLore);
		pinkItem.setItemMeta(pinkMeta);
		mDelvePannelList.add(pinkItem);

		//Gray
		ItemStack greyItem = new ItemStack(Material.GRAY_WOOL);
		ItemMeta greyMeta = greyItem.getItemMeta();
		greyMeta.displayName(Component.text("Execution", TextColor.fromCSSHexString("#555555")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(greyMeta, "After killing an enemy, you deal 1.5% extra damage per level for 4 seconds.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> greyLore = greyMeta.lore();
		greyLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.EXECUTION), TextColor.fromHexString("#555555")));
		greyMeta.lore(greyLore);
		greyItem.setItemMeta(greyMeta);
		mDelvePannelList.add(greyItem);

		//Light Grey
		ItemStack lgItem = new ItemStack(Material.LIGHT_GRAY_WOOL);
		ItemMeta lgMeta = lgItem.getItemMeta();
		lgMeta.displayName(Component.text("Reflection", TextColor.fromCSSHexString("#AAAAAA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(lgMeta, "1 second after taking magic or blast damage, deal 6% per level of the spell's damage to all mobs in a 4 block radius.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> lgLore = lgMeta.lore();
		lgLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.REFLECTION), TextColor.fromHexString("#555555")));
		lgMeta.lore(lgLore);
		lgItem.setItemMeta(lgMeta);
		mDelvePannelList.add(lgItem);

		//Cyan
		ItemStack cyanItem = new ItemStack(Material.CYAN_WOOL);
		ItemMeta cyanMeta = cyanItem.getItemMeta();
		cyanMeta.displayName(Component.text("Mitosis", TextColor.fromCSSHexString("#00AAAA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(cyanMeta, "Mining a spawner debuffs all mobs in a 5 block radius with 3.75% per level Weakness for 3 seconds.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> cyanLore = cyanMeta.lore();
		cyanLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.MITOSIS), TextColor.fromHexString("#555555")));
		cyanMeta.lore(cyanLore);
		cyanItem.setItemMeta(cyanMeta);
		mDelvePannelList.add(cyanItem);

		//Purple
		ItemStack purpleItem = new ItemStack(Material.PURPLE_WOOL);
		ItemMeta purpleMeta = purpleItem.getItemMeta();
		purpleMeta.displayName(Component.text("Ardor", TextColor.fromCSSHexString("#AA00AA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(purpleMeta, "Mining a spawner outside of water grants you 3.75% speed per level for 4s. Mining a spawner underwater refreshes 0.5 breath per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> purpleLore = purpleMeta.lore();
		purpleLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.ARDOR), TextColor.fromHexString("#555555")));
		purpleMeta.lore(purpleLore);
		purpleItem.setItemMeta(purpleMeta);
		mDelvePannelList.add(purpleItem);

		//Teal
		ItemStack tealItem = new ItemStack(Material.CYAN_CONCRETE_POWDER);
		ItemMeta tealMeta = tealItem.getItemMeta();
		tealMeta.displayName(Component.text("Epoch", TextColor.fromCSSHexString("#47B6B5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(tealMeta, "Class abilities cooldowns are reduced by 1% per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> tealLore = tealMeta.lore();
		tealLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.EPOCH), TextColor.fromHexString("#555555")));
		tealMeta.lore(tealLore);
		tealItem.setItemMeta(tealMeta);
		mDelvePannelList.add(tealItem);

		//shifting
		ItemStack shiftingItem = new ItemStack(Material.BLUE_CONCRETE);
		ItemMeta shiftingMeta = shiftingItem.getItemMeta();
		shiftingMeta.displayName(Component.text("Natant", TextColor.fromCSSHexString("#7FFFD4")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(shiftingMeta, "You move 4% per level faster when in water.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> shiftingLore = shiftingMeta.lore();
		shiftingLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.NATANT), TextColor.fromHexString("#555555")));
		shiftingMeta.lore(shiftingLore);
		shiftingItem.setItemMeta(shiftingMeta);
		mDelvePannelList.add(shiftingItem);

		//Fallen Forum
		ItemStack fallenItem = new ItemStack(Material.BOOKSHELF);
		ItemMeta fallenMeta = fallenItem.getItemMeta();
		fallenMeta.displayName(Component.text("Understanding", TextColor.fromCSSHexString("#808000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(fallenMeta, "All other Delve Infusions you are currently benefiting from gain .25 levels per level of this Infusion.", MAX_LORE_LENGHT, ChatColor.GRAY);
		List<Component> fallenLore = fallenMeta.lore();
		fallenLore.add(Component.text("Requires " + mDelveMatsMap.get(DelveInfusionSelection.UNDERSTANDING), TextColor.fromHexString("#555555")));
		fallenMeta.lore(fallenLore);
		fallenItem.setItemMeta(fallenMeta);
		mDelvePannelList.add(fallenItem);

		// Silver Knight's Tomb
		ItemStack sktItem = new ItemStack(Material.POLISHED_DEEPSLATE);
		ItemMeta sktMeta = sktItem.getItemMeta();
		sktMeta.displayName(Component.text("Refresh", TextColor.fromCSSHexString("#C0C0C0")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(sktMeta, "Reduces the cooldown of infinite consumable foods by 2% per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		sktItem.setItemMeta(sktMeta);
		mDelvePannelList.add(sktItem);

		// Blue
		ItemStack blueItem = new ItemStack(Material.BLUE_WOOL);
		ItemMeta blueMeta = blueItem.getItemMeta();
		blueMeta.displayName(Component.text("Soothing", TextColor.fromCSSHexString("#0C2CA2")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(blueMeta, "Regenerate 0.0825 health per level each second.", MAX_LORE_LENGHT, ChatColor.GRAY);
		blueItem.setItemMeta(blueMeta);
		mDelvePannelList.add(blueItem);

		// Wolfswood
		ItemStack woodItem = new ItemStack(Material.DARK_OAK_WOOD);
		ItemMeta woodMeta = woodItem.getItemMeta();
		woodMeta.displayName(Component.text("Quench", TextColor.fromCSSHexString("#4C8F4D")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(woodMeta, "Gain 1% damage per level when you have a non-infinite vanilla potion effect active.", MAX_LORE_LENGHT, ChatColor.GRAY);
		woodItem.setItemMeta(woodMeta);
		mDelvePannelList.add(woodItem);

		// Keep
		ItemStack keepItem = new ItemStack(Material.CRACKED_STONE_BRICKS);
		ItemMeta keepMeta = keepItem.getItemMeta();
		keepMeta.displayName(Component.text("Grace", TextColor.fromCSSHexString("#C4BBA5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		splitLoreLine(keepMeta, "Gain 2% attack speed per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
		keepItem.setItemMeta(keepMeta);
		mDelvePannelList.add(keepItem);


		//LOADING mDelveInfusionPannelsMap
		//-----------------------------------------------------
		//   items showed for each delve infusion on item
		//-----------------------------------------------------


		//white
		List<ItemStack> whiteItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.WHITE_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Pennate level " + (i + 1), TextColor.fromCSSHexString("#FFFFFF")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Fall damage is reduced by " + 5 * (i + 1) + "%.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			whiteItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.PENNATE, whiteItems);

		//orange
		List<ItemStack> orangeItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.ORANGE_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Carapace level " + (i + 1), TextColor.fromCSSHexString("#FFAA00")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "After being hit, you gain " + 1.25 * (i + 1) + "% damage reduction for 5s. Being hit again while active refreshes the duration.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			orangeItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.CARAPACE, orangeItems);

		//magenta
		List<ItemStack> magentaItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.MAGENTA_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Aura level " + (i + 1), TextColor.fromCSSHexString("#FF55FF")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Mobs in a 3 block radius from you are slowed by " + 2 * (i + 1) + "% for 0.5 seconds. This is refreshed as long as they are in range.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			magentaItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.AURA, magentaItems);

		//light blue
		List<ItemStack> lbItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.LIGHT_BLUE_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Expedite level " + (i + 1), TextColor.fromCSSHexString("#4AC2E5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Damaging an enemy with an ability increases your movement speed by " + 1 * (i + 1) + "% for 5 seconds, stacking up to 3 times.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			lbItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.EXPEDITE, lbItems);

		//yellow
		List<ItemStack> yellowItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.YELLOW_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Choler level " + (i + 1), TextColor.fromCSSHexString("#FFFF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Deal " + 1 * (i + 1) + "% additional damage to any mob that is on fire, slowed, or stunned.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			yellowItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.CHOLER, yellowItems);

		//bonus
		List<ItemStack> bonusItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.MOSSY_COBBLESTONE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Unyielding level " + (i + 1), TextColor.fromCSSHexString("#006400")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Gain " + String.format("%,.1f", (0.4 * (i + 1))) + " Knockback Resistance", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			bonusItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.UNYIELDING, bonusItems);

		//reverie
		List<ItemStack> reverieItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.NETHER_WART_BLOCK, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Usurper level " + (i + 1), TextColor.fromCSSHexString("#790E47")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Heal " + 2.5 * (1 + i) + "% of your max health whenever you slay an elite or boss enemy.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			reverieItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.USURPER, reverieItems);

		//corridors
		List<ItemStack> corridorsItems = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.MAGMA_BLOCK, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Vengeful level " + (i + 1), TextColor.fromCSSHexString("#8B0000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Gain " + 2 * (1 + i) + "% damage against the last enemy that damaged you.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			corridorsItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.VENGEFUL, corridorsItems);

		//lime
		List<ItemStack> limeItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.LIME_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Empowered level " + (i + 1), TextColor.fromCSSHexString("#55FF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "When you gain XP, you have a " + String.format("%,.1f", (0.25 * (i + 1))) + "% chance per XP point to repair all currently equipped items by 1% of their max durability.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			limeItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.EMPOWERED, limeItems);

		//pink
		List<ItemStack> pinkItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.PINK_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Nutriment level " + (i + 1), TextColor.fromCSSHexString("#FF69B4")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Gain " + 1.5 * (i + 1) + "% extra healing.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			pinkItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.NUTRIMENT, pinkItems);

		//gray
		List<ItemStack> grayItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.GRAY_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Execution level " + (i + 1), TextColor.fromCSSHexString("#555555")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "After killing an enemy, you deal " + 1.5 * (i + 1) + "% extra damage for 4 seconds.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			grayItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.EXECUTION, grayItems);

		//light gray
		List<ItemStack> lgItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.LIGHT_GRAY_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Reflection level " + (i + 1), TextColor.fromCSSHexString("#AAAAAA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "1 second after taking ability damage, deal " + 6 * (1 + i) + "% of the spell's damage to all mobs in a 4 block radius.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			lgItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.REFLECTION, lgItems);

		//cyan
		List<ItemStack> cyanItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.CYAN_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Mitosis level " + (i + 1), TextColor.fromCSSHexString("#00AAAA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Mining a spawner debuffs all mobs in a 5 block radius with " + 3.75 * (i + 1) + "% Weakness for 3 seconds.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			cyanItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.MITOSIS, cyanItems);

		//purple
		List<ItemStack> purpleItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.PURPLE_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Ardor level " + (i + 1), TextColor.fromCSSHexString("#AA00AA")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Mining a spawner outside of water grants you " + 3.75 * (i + 1) + "% speed for 4s. Mining a spawner underwater refreshes 0.5 breath per level.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			purpleItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.ARDOR, purpleItems);

		//teal
		List<ItemStack> tealItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.CYAN_CONCRETE_POWDER, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Epoch level " + (i + 1), TextColor.fromCSSHexString("#47B6B5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Class abilities cooldowns are reduced by " + (i + 1) + "%.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			tealItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.EPOCH, tealItems);

		//shifting
		List<ItemStack> shiftingItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.BLUE_CONCRETE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Natant level " + (i + 1), TextColor.fromCSSHexString("#7FFFD4")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "You move " + 4 * (i + 1) + "% faster when in water.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			shiftingItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.NATANT, shiftingItems);

		//forum
		List<ItemStack> forumItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.BOOKSHELF, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Understanding level " + (i + 1), TextColor.fromCSSHexString("#808000")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "All other Delve Infusions you are currently benefiting from gain " + .25 * (i + 1) + " levels.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			forumItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.UNDERSTANDING, forumItems);

		// SKT
		List<ItemStack> sktItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.POLISHED_DEEPSLATE, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Refresh level " + (i + 1), TextColor.fromCSSHexString("#C0C0C0")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Reduces the cooldown of infinite consumable foods by " + (i + 1) * 2 + "%.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			sktItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.REFRESH, sktItems);

		// Blue
		List<ItemStack> blueItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.BLUE_WOOL, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Soothing level " + (i + 1), TextColor.fromCSSHexString("#0C2CA2")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Regenerate " + 0.0825 * (i + 1) + " health each second.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			blueItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.SOOTHING, blueItems);

		// Wolfswood
		List<ItemStack> forestItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.DARK_OAK_WOOD, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Quench level " + (i + 1), TextColor.fromCSSHexString("#4C8F4D")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Gain " + (i + 1) + "% damage when you have a non-infinite vanilla potion effect active.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			forestItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.QUENCH, forestItems);

		// Keep
		List<ItemStack> keepItems = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			ItemStack pannel = new ItemStack(Material.CRACKED_STONE_BRICKS, 1);
			ItemMeta meta = pannel.getItemMeta();
			meta.displayName(Component.text("Grace level " + (i + 1), TextColor.fromCSSHexString("#C4BBA5")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			splitLoreLine(meta, "Gain " + 2 * (i + 1) + "% attack speed.", MAX_LORE_LENGHT, ChatColor.GRAY);
			pannel.setItemMeta(meta);
			keepItems.add(pannel);
		}
		mDelveInfusionPannelsMap.put(DelveInfusionSelection.GRACE, keepItems);

		//INVALIDS ITEM.
		//placeholder when an item can't be infused.

		ItemStack invalidItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta meta = invalidItem.getItemMeta();
		meta.displayName(Component.text("Invalid item", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, true));

		List<Component> itemLore = new ArrayList<Component>();
		itemLore.add(Component.text("Your helmet can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);

		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your chestplate can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your leggings can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("Your boots can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your main hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());
		itemLore.clear();
		itemLore.add(Component.text("The item in your off hand can't be infused.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		meta.lore(itemLore);
		invalidItem.setItemMeta(meta);
		mInvalidItems.add(invalidItem.clone());

		//Refund item
		ItemMeta refundMeta = mRefundItem.getItemMeta();
		refundMeta.displayName(Component.text("Click to refund this item's infusions.", NamedTextColor.DARK_GRAY)
							.decoration(TextDecoration.ITALIC, false)
							.decoration(TextDecoration.BOLD, true));
		splitLoreLine(refundMeta, "You will receive " + (DelveInfusionUtils.FULL_REFUND ? "100" : "50") + "% of the experience, but all of the materials back.", MAX_LORE_LENGHT, ChatColor.GRAY);
		mRefundItem.setItemMeta(refundMeta);

		//Cake for max level reached
		ItemMeta maxMeta = mMaxLevelReachedItem.getItemMeta();
		maxMeta.displayName(Component.text("Congratulations!", NamedTextColor.DARK_AQUA)
						.decoration(TextDecoration.BOLD, true)
						.decoration(TextDecoration.ITALIC, false));
		splitLoreLine(maxMeta, "You've reached the max Delve Infusion level on this item.", MAX_LORE_LENGHT, ChatColor.DARK_AQUA);
		mMaxLevelReachedItem.setItemMeta(maxMeta);
	}

	private static void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (!currentString.equals(defaultColor + "")) {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}


	public DelveInfusionCustomInventory(Player owner) {
		super(owner, 54, "Delve Infusions");
		mMapFunction = new HashMap<>();
		mRowSelected = 99;
		loadInv(owner);
	}

	private void loadInv(Player player) {
		mInventory.clear();
		mMapFunction.clear();
		PlayerInventory pi = player.getInventory();
		List<ItemStack> items = new ArrayList<>();
		items.addAll(Arrays.asList(pi.getArmorContents()));
		Collections.reverse(items);
		items.add(pi.getItemInMainHand());
		items.add(pi.getItemInOffHand());

		if (mRowSelected == 99) {
			loadDelveInfusionPage(items);
		} else {
			loadDelveInfusionSelection(items.get(mRowSelected));
		}

		fillWithJunk();
	}


	private void loadDelveInfusionSelection(ItemStack infusedItem) {
		//we need to delay this loading to make the item skin applied
		new BukkitRunnable() {
			@Override
			public void run() {
				ItemStack itemStack = new ItemStack(infusedItem.getType());
				ItemMeta originalMeta = infusedItem.getItemMeta();
				ItemMeta meta = itemStack.getItemMeta();
				if (originalMeta instanceof LeatherArmorMeta oldLeather && meta instanceof LeatherArmorMeta newLeather) {
					newLeather.setColor(oldLeather.getColor());
				}
				meta.displayName(Component.text("Placeholder", TextColor.fromCSSHexString("000000"))
					                 .decoration(TextDecoration.BOLD, true)
					                 .decoration(TextDecoration.ITALIC, false));
				itemStack.setItemMeta(meta);
				ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(infusedItem));
				mInventory.setItem(4, itemStack);
			}
		}.runTaskLater(Plugin.getInstance(), 2);

		//R1
		mInventory.setItem(9, mDelvePannelList.get(0));
		mInventory.setItem(10, mDelvePannelList.get(1));
		mInventory.setItem(11, mDelvePannelList.get(2));
		mInventory.setItem(12, mDelvePannelList.get(3));
		mInventory.setItem(14, mDelvePannelList.get(4));
		mInventory.setItem(15, mDelvePannelList.get(5));
		mInventory.setItem(16, mDelvePannelList.get(6));
		mInventory.setItem(17, mDelvePannelList.get(7));

		//R2
		mInventory.setItem(18, mDelvePannelList.get(8));
		mInventory.setItem(19, mDelvePannelList.get(9));
		mInventory.setItem(20, mDelvePannelList.get(10));
		mInventory.setItem(21, mDelvePannelList.get(11));
		mInventory.setItem(22, mDelvePannelList.get(12));
		mInventory.setItem(23, mDelvePannelList.get(13));
		mInventory.setItem(24, mDelvePannelList.get(14));
		mInventory.setItem(25, mDelvePannelList.get(15));
		mInventory.setItem(26, mDelvePannelList.get(16));

		//R3 Dungeon
		mInventory.setItem(30, mDelvePannelList.get(17));
		mInventory.setItem(32, mDelvePannelList.get(18));

		//R3 Other
		mInventory.setItem(39, mDelvePannelList.get(19));
		mInventory.setItem(41, mDelvePannelList.get(20));


		ItemStack swapPage = new ItemStack(Material.PAPER);
		ItemMeta meta = swapPage.getItemMeta();
		meta.displayName(Component.text("Back!")
				.decoration(TextDecoration.BOLD, true)
				.decoration(TextDecoration.ITALIC, false)
				.color(TextColor.fromCSSHexString("ffa000")));
		swapPage.setItemMeta(meta);
		mInventory.setItem(53, swapPage);

		mMapFunction.put(53, (p, clickedInventory, slot) -> {
			mRowSelected = 99;

		});


		mMapFunction.put(9, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.PENNATE);
		});

		mMapFunction.put(10, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.CARAPACE);
		});

		mMapFunction.put(11, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.AURA);
		});

		mMapFunction.put(12, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.EXPEDITE);
		});

		mMapFunction.put(14, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.CHOLER);
		});

		mMapFunction.put(15, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.UNYIELDING);
		});

		mMapFunction.put(16, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.USURPER);
		});

		mMapFunction.put(17, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.VENGEFUL);
		});

		//R2
		mMapFunction.put(18, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.EMPOWERED);
		});

		mMapFunction.put(19, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.NUTRIMENT);
		});

		mMapFunction.put(20, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.EXECUTION);
		});

		mMapFunction.put(21, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.REFLECTION);
		});

		mMapFunction.put(22, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.MITOSIS);
		});

		mMapFunction.put(23, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.ARDOR);
		});

		mMapFunction.put(24, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.EPOCH);
		});

		mMapFunction.put(25, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.NATANT);
		});

		mMapFunction.put(26, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.UNDERSTANDING);
		});

		//R3 Dungeon
		mMapFunction.put(30, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.REFRESH);
		});

		mMapFunction.put(32, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.SOOTHING);
		});

		//R3 Other
		mMapFunction.put(39, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.QUENCH);
		});

		mMapFunction.put(41, (p, inventory, slot) -> {
			attemptInfusion(p, infusedItem, DelveInfusionSelection.GRACE);
		});
	}

	private void attemptInfusion(Player p, ItemStack item, DelveInfusionSelection infusion) {
		if (item.getAmount() > 1) {
			p.sendMessage(ChatColor.RED + "You cannot infuse stacked items.");
			return;
		}

		try {
			if (DelveInfusionUtils.canPayInfusion(item, infusion, p)) {
				if (DelveInfusionUtils.payInfusion(item, infusion, p)) {
					DelveInfusionUtils.infuseItem(p, item, infusion);
				} else {
					p.sendMessage(ChatColor.RED + "If you see this message please contact a mod! (Error in paying infusion cost)");
				}
			} else {
				p.sendMessage(ChatColor.RED + "You don't have enough experience and/or currency for this infusion.");
			}
			mRowSelected = 99;
		} catch (Exception e) {
			p.sendMessage(ChatColor.RED + "If you see this message please contact a mod! (Error in infusing)");
			e.printStackTrace();
		}
	}

	private void loadDelveInfusionPage(List<ItemStack> items) {
		//load pannels for each item with the corresponding infusions.
		int row = 0;
		for (ItemStack item : items) {
			if (item != null) {
				//check valid item
				if (InfusionUtils.isInfusionable(item)) {
					//same tier needed.
					DelveInfusionSelection infusion = DelveInfusionUtils.getCurrentInfusion(item);
					final int rowF = row;

					//we need to delay this loading to make the item skin applied
					new BukkitRunnable() {
						@Override
						public void run() {
							ItemStack itemStack = new ItemStack(item.getType());
							ItemMeta meta = itemStack.getItemMeta();
							meta.displayName(item.getItemMeta().displayName()
								.decoration(TextDecoration.BOLD, true)
								.decoration(TextDecoration.ITALIC, false));
							meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
							itemStack.setItemMeta(meta);
							ItemUtils.setPlainName(itemStack, ItemUtils.getPlainName(item));
							mInventory.setItem((rowF * 9) + 1, itemStack);
						}
					}.runTaskLater(Plugin.getInstance(), 2);

					if (infusion != null) {
						mInventory.setItem((row * 9), mRefundItem);
						mMapFunction.put((row * 9), (p, inventory, slot) -> {
							DelveInfusionUtils.refundInfusion(item, p);
						});

						//load the infusion.
						int level = DelveInfusionUtils.getInfusionLevel(item, infusion);
						List<ItemStack> pannelsList = mDelveInfusionPannelsMap.get(infusion);
						if (pannelsList != null) {
							for (int i = 0; i < level; i++) {
								if (pannelsList.get(i) != null) {
									mInventory.setItem((row * 9) + 2 + i, pannelsList.get(i));
								}
							}
						}

						if (level < DelveInfusionUtils.MAX_LEVEL) {
							//if we didn't reach max level then load item to infuse
							int slot = (row * 9) + 2 + level;

							ItemStack infuseItem = new ItemStack(Material.ENCHANTED_BOOK, 1);
							ItemMeta infuseMeta = infuseItem.getItemMeta();
							infuseMeta.displayName(Component.text("Click to infuse to level " + (level + 1), NamedTextColor.DARK_AQUA)
											.decoration(TextDecoration.ITALIC, false)
											.decoration(TextDecoration.BOLD, true));
							List<Component> itemLore = new ArrayList<>();

							itemLore.add(Component.text("You will need " + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[level] + " Voidstained Geodes,", NamedTextColor.GRAY)
											.decoration(TextDecoration.ITALIC, false));

							itemLore.add(Component.text(DelveInfusionUtils.MAT_COST_PER_INFUSION[level] + " " + mDelveMatsMap.get(infusion) + ",", NamedTextColor.GRAY)
											.decoration(TextDecoration.ITALIC, false));

							itemLore.add(Component.text("and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY)
											.decoration(TextDecoration.ITALIC, false));

							infuseMeta.lore(itemLore);
							infuseItem.setItemMeta(infuseMeta);
							mInventory.setItem(slot, infuseItem);

							mMapFunction.put(slot, (p, inventory, slotClicked) -> {
								try {
									if (DelveInfusionUtils.canPayInfusion(item, infusion, p)) {
										if (DelveInfusionUtils.payInfusion(item, infusion, p)) {
											DelveInfusionUtils.infuseItem(p, item, infusion);
										} else {
											p.sendMessage("If you see this message please contact a mod. Error paying.");
										}
									} else {
										p.sendMessage("You don't have enough experience and/or currency for this infusion.");
									}
								} catch (Exception e) {
									p.sendMessage("Error while infusing, please contact a mod: " + e);
								}

							});
						} else {
							//Max level reached
							int slot = (row * 9) + 2 + level;
							mInventory.setItem(slot, mMaxLevelReachedItem);
						}
					} else {
						//Item with no infusion -> load item to swap page
						ItemStack infuseItem = new ItemStack(Material.ENCHANTED_BOOK, 1);
						ItemMeta infuseMeta = infuseItem.getItemMeta();
						infuseMeta.displayName(Component.text("Click to select a Delve Infusion.", NamedTextColor.DARK_AQUA)
											.decoration(TextDecoration.ITALIC, false)
											.decoration(TextDecoration.BOLD, true));
						List<Component> itemLore = new ArrayList<>();
						itemLore.add(Component.text("The first Delve Infusion level costs", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
						itemLore.add(Component.text("" + DelveInfusionUtils.MAT_DEPTHS_COST_PER_INFUSION[0] + " Voidstained Geodes,", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
						itemLore.add(Component.text("" + DelveInfusionUtils.MAT_COST_PER_INFUSION[0] + " corresponding Delve Materials", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
						itemLore.add(Component.text("and " + DelveInfusionUtils.getExpLvlInfuseCost(item) + " experience levels", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));

						infuseMeta.lore(itemLore);
						infuseItem.setItemMeta(infuseMeta);

						mInventory.setItem((rowF * 9) + 2 + 4, infuseItem);
						mMapFunction.put((rowF * 9) + 2 + 4, (p, inventory, slot) -> {
							mRowSelected = rowF;

						});
					}
				} else {
					ItemStack invalidItem = mInvalidItems.get(row);
					mInventory.setItem((row * 9) + 1, invalidItem);
				}
			} else {
				ItemStack invalidItem = mInvalidItems.get(row);
				mInventory.setItem((row * 9) + 1, invalidItem);
			}
			row++;
		}
	}

	private void fillWithJunk() {
		ItemStack junk = new ItemStack(JUNK_ITEM, 1);
		ItemMeta meta = junk.getItemMeta();
		meta.displayName(Component.text(""));
		junk.setItemMeta(meta);

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, junk);
			}
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		Inventory clickedInventory = event.getClickedInventory();
		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		event.setCancelled(true);

		if (event.isShiftClick()) {
			return;
		}

		if (!mInventory.equals(clickedInventory)) {
			return;
		}

		ItemClicked itemClicked = mMapFunction.get(slot);
		if (itemClicked == null) {
			return;
		}
		itemClicked.run(player, clickedInventory, slot);

		loadInv(player);

	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player) event.getPlayer();
			PlayerTracking.getInstance().updateItemSlotProperties(player, player.getInventory().getHeldItemSlot());
			PlayerTracking.getInstance().updateItemSlotProperties(player, 36);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 37);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 38);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 39);
			PlayerTracking.getInstance().updateItemSlotProperties(player, 40);
		}
	}

}
