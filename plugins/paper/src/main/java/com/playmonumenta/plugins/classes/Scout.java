package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;


public class Scout extends PlayerClass {

	Scout(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new Agility(plugin, player));
		mAbilities.add(new HuntingCompanion(plugin, player));
		mAbilities.add(new EagleEye(plugin, player));
		mAbilities.add(new WindBomb(plugin, player));
		mAbilities.add(new Sharpshooter(plugin, player));
		mAbilities.add(new SwiftCuts(plugin, player));
		mAbilities.add(new Swiftness(plugin, player));
		mAbilities.add(new Volley(plugin, player));
		mClass = 6;
		mClassName = "Scout";
		mClassColor = NamedTextColor.AQUA;
		mChatColor = ChatColor.AQUA;
		mDisplayItem = new ItemStack(Material.BOW, 1);
		mClassDescription = "Scouts are agile masters of archery and exploration.";
		mClassPassiveDescription = "Whenever you fire a bow, you have a 20% chance to not consume the arrow.";

		mSpecOne.mAbilities.add(new Quickdraw(plugin, player));
		mSpecOne.mAbilities.add(new WhirlingBlade(plugin, player));
		mSpecOne.mAbilities.add(new TacticalManeuver(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = 11;
		mSpecOne.mSpecName = "Ranger";
		mSpecOne.mDisplayItem = new ItemStack(Material.LARGE_FERN, 1);
		mSpecOne.mDescription = "Rangers are agile masters of archery and that have unparalleled mastery of movement.";

		mSpecTwo.mAbilities.add(new PinningShot(plugin, player));
		mSpecTwo.mAbilities.add(new SplitArrow(plugin, player));
		mSpecTwo.mAbilities.add(new PredatorStrike(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = 12;
		mSpecTwo.mSpecName = "Hunter";
		mSpecTwo.mDisplayItem = new ItemStack(Material.LEATHER, 1);
		mSpecTwo.mDescription = "Hunters are agile masters of exploration that have dedicated their lives to archery.";

	}
}
