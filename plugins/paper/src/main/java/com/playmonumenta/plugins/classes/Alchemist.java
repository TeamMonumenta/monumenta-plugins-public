package com.playmonumenta.plugins.classes;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BasiliskPoison;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EnfeeblingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.PowerInjection;
import com.playmonumenta.plugins.abilities.alchemist.UnstableArrows;

import com.playmonumenta.plugins.abilities.alchemist.apothecary.AlchemicalAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;

import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.PurpleHaze;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;

import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;


public class Alchemist extends PlayerClass {

	Alchemist(Plugin plugin, Player player) {
		mAbilities.add(new GruesomeAlchemy(plugin, player));
		mAbilities.add(new BrutalAlchemy(plugin, player));
		mAbilities.add(new IronTincture(plugin, player));
		mAbilities.add(new BasiliskPoison(plugin, player));
		mAbilities.add(new PowerInjection(plugin, player));
		mAbilities.add(new UnstableArrows(plugin, player));
		mAbilities.add(new EnfeeblingElixir(plugin, player));
		mAbilities.add(new Bezoar(plugin, player));
		mClass = 5;
		mClassName = "Alchemist";
		mClassColor = NamedTextColor.GREEN;
		mChatColor = ChatColor.GREEN;
		mDisplayItem = new ItemStack(Material.POTION, 1);
		mClassDescription = "Alchemists employ magic potions to weaken and destroy their enemies.";
		mClassPassiveDescription = "You gain 1 potion every 2.5 seconds. Each skill point" +
				" increases your potion damage by 0.5. Each spec point increases potion damage by 1.";

		mSpecOne.mAbilities.add(new PurpleHaze(plugin, player));
		mSpecOne.mAbilities.add(new ScorchedEarth(plugin, player));
		mSpecOne.mAbilities.add(new NightmarishAlchemy(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103m";
		mSpecOne.mSpecialization = 9;
		mSpecOne.mSpecName = "Harbinger";
		mSpecOne.mDisplayItem = new ItemStack(Material.DEAD_BUSH, 1);
		mSpecOne.mDescription = "Harbingers use special potions to weaken and destroy their enemies. Harbingers prefer slinging deadly potions and using strategy.";

		mSpecTwo.mAbilities.add(new AlchemicalAmalgam(plugin, player));
		mSpecTwo.mAbilities.add(new InvigoratingOdor(plugin, player));
		mSpecTwo.mAbilities.add(new WardingRemedy(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103f";
		mSpecTwo.mSpecialization = 10;
		mSpecTwo.mSpecName = "Apothecary";
		mSpecTwo.mDisplayItem = new ItemStack(Material.BLUE_ORCHID, 1);
		mSpecTwo.mDescription = "Apothecaries employ magic potions to weaken enemies and support friends.";

	}
}
