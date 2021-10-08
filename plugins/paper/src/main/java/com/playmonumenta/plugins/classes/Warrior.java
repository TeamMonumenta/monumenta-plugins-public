package com.playmonumenta.plugins.classes;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WeaponryMastery;

import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.berserker.RecklessSwing;

import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;

import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;



public class Warrior extends PlayerClass {

	Warrior(Plugin plugin, Player player) {
		mAbilities.add(new BruteForce(plugin, player));
		mAbilities.add(new CounterStrike(plugin, player));
		mAbilities.add(new DefensiveLine(plugin, player));
		mAbilities.add(new Frenzy(plugin, player));
		mAbilities.add(new Riposte(plugin, player));
		mAbilities.add(new ShieldBash(plugin, player));
		mAbilities.add(new Toughness(plugin, player));
		mAbilities.add(new WeaponryMastery(plugin, player));
		mClass = 2;
		mClassName = "Warrior";
		mClassColor = NamedTextColor.RED;
		mChatColor = ChatColor.RED;
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);

		mSpecOne.mAbilities.add(new MeteorSlam(plugin, player));
		mSpecOne.mAbilities.add(new Rampage(plugin, player));
		mSpecOne.mAbilities.add(new RecklessSwing(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103a";
		mSpecOne.mSpecialization = 3;
		mSpecOne.mSpecName = "Berserker";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);

		mSpecTwo.mAbilities.add(new Bodyguard(plugin, player));
		mSpecTwo.mAbilities.add(new Challenge(plugin, player));
		mSpecTwo.mAbilities.add(new ShieldWall(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103h";
		mSpecTwo.mSpecialization = 4;
		mSpecTwo.mSpecName = "Guardian";
		mSpecTwo.mDisplayItem = new ItemStack(Material.SHIELD, 1);
	}
}
