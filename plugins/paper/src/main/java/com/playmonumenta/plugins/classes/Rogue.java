package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.RoguePassive;
import com.playmonumenta.plugins.abilities.rogue.Skirmisher;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.rogue.assassin.BodkinBlitz;
import com.playmonumenta.plugins.abilities.rogue.assassin.CloakAndDagger;
import com.playmonumenta.plugins.abilities.rogue.assassin.CoupDeGrace;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.abilities.rogue.swordsage.DeadlyRonde;
import com.playmonumenta.plugins.abilities.rogue.swordsage.WindWalk;
import com.playmonumenta.plugins.utils.StringUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class Rogue extends PlayerClass {

	Rogue(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new AdvancingShadows(plugin, player));
		mAbilities.add(new ByMyBlade(plugin, player));
		mAbilities.add(new DaggerThrow(plugin, player));
		mAbilities.add(new Dodging(plugin, player));
		mAbilities.add(new EscapeDeath(plugin, player));
		mAbilities.add(new Skirmisher(plugin, player));
		mAbilities.add(new Smokescreen(plugin, player));
		mAbilities.add(new ViciousCombos(plugin, player));
		mClass = 4;
		mClassName = "Rogue";
		mDisplayItem = new ItemStack(Material.STONE_SWORD, 1);
		mClassDescription = "Rogues excel in one-on-one battles, using precise strikes to bring down dangerous elite enemies.";
		mClassPassiveDescription = String.format("While holding two swords, deal %s%% damage to elite enemies, and %s%% more to bosses.", StringUtils.multiplierToPercentage(RoguePassive.PASSIVE_DAMAGE_ELITE_MODIFIER - 1), StringUtils.multiplierToPercentage(RoguePassive.PASSIVE_DAMAGE_BOSS_MODIFIER - 1));
		mSpecOne.mAbilities.add(new BladeDance(plugin, player));
		mSpecOne.mAbilities.add(new DeadlyRonde(plugin, player));
		mSpecOne.mAbilities.add(new WindWalk(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103c";
		mSpecOne.mSpecialization = 7;
		mClassColor = NamedTextColor.WHITE;
		mChatColor = ChatColor.WHITE;
		mSpecOne.mSpecName = "Swordsage";
		mSpecOne.mDisplayItem = new ItemStack(Material.IRON_HELMET, 1);
		mSpecOne.mDescription = "Swordsages specialize in tackling multiple enemies through dexterous movement.";

		mSpecTwo.mAbilities.add(new BodkinBlitz(plugin, player));
		mSpecTwo.mAbilities.add(new CloakAndDagger(plugin, player));
		mSpecTwo.mAbilities.add(new CoupDeGrace(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103j";
		mSpecTwo.mSpecialization = 8;
		mSpecTwo.mSpecName = "Assassin";
		mSpecTwo.mDisplayItem = new ItemStack(Material.WITHER_ROSE, 1);
		mSpecTwo.mDescription = "Assassins excel in precise strikes and deception to devastate their enemies.";

	}
}
