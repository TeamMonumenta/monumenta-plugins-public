package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WeaponMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.GloriousBattle;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class Warrior extends PlayerClass {

	Warrior(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new BruteForce(plugin, player));
		mAbilities.add(new CounterStrike(plugin, player));
		mAbilities.add(new DefensiveLine(plugin, player));
		mAbilities.add(new Frenzy(plugin, player));
		mAbilities.add(new Riposte(plugin, player));
		mAbilities.add(new ShieldBash(plugin, player));
		mAbilities.add(new Toughness(plugin, player));
		mAbilities.add(new WeaponMastery(plugin, player));
		mClass = 2;
		mClassName = "Warrior";
		mClassColor = NamedTextColor.RED;
		mChatColor = ChatColor.RED;
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);
		mClassDescription = "Warriors specialize in melee combat, being able to both deal and survive heavy damage.";
		mClassPassiveDescription = "Gain a base of 20% Knockback Resistance. This stacks with other skills and items.";

		mSpecOne.mAbilities.add(new GloriousBattle(plugin, player));
		mSpecOne.mAbilities.add(new MeteorSlam(plugin, player));
		mSpecOne.mAbilities.add(new Rampage(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103a";
		mSpecOne.mSpecialization = 3;
		mSpecOne.mSpecName = "Berserker";
		mSpecOne.mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mSpecOne.mDescription = "Berserkers specialize in melee combat. They thrive in the heart of battle taking heavy risks for great rewards.";

		mSpecTwo.mAbilities.add(new Bodyguard(plugin, player));
		mSpecTwo.mAbilities.add(new Challenge(plugin, player));
		mSpecTwo.mAbilities.add(new ShieldWall(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103h";
		mSpecTwo.mSpecialization = 4;
		mSpecTwo.mSpecName = "Guardian";
		mSpecTwo.mDisplayItem = new ItemStack(Material.SHIELD, 1);
		mSpecTwo.mDescription = "Guardians specialize in melee combat. Their skills are focused on taking heavy damage and drawing enemy attacks to themselves.";
	}
}
