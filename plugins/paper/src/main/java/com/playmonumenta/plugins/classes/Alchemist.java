package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EmpoweringOdor;
import com.playmonumenta.plugins.abilities.alchemist.EnergizingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.utils.StringUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Alchemist extends PlayerClass {

	Alchemist(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new GruesomeAlchemy(plugin, player));
		mAbilities.add(new BrutalAlchemy(plugin, player));
		mAbilities.add(new IronTincture(plugin, player));
		mAbilities.add(new AlchemicalArtillery(plugin, player));
		mAbilities.add(new EmpoweringOdor(plugin, player));
		mAbilities.add(new UnstableAmalgam(plugin, player));
		mAbilities.add(new EnergizingElixir(plugin, player));
		mAbilities.add(new Bezoar(plugin, player));
		mClass = 5;
		mClassName = "Alchemist";
		mClassColor = NamedTextColor.GREEN;
		mChatColor = ChatColor.GREEN;
		mDisplayItem = new ItemStack(Material.POTION, 1);
		mClassDescription = "Alchemists employ magic potions to weaken and destroy their enemies.";
		mClassPassiveDescription = ("You gain 1 potion every %s seconds, up to a maximum of %s. Each skill point" +
			                            " increases your potion damage by %s. Each spec point and enhancement point increases potion damage by %s.")
			                           .formatted(StringUtils.ticksToSeconds(AlchemistPotions.POTIONS_TIMER_BASE), AlchemistPotions.MAX_CHARGES,
				                           AlchemistPotions.DAMAGE_PER_SKILL_POINT, AlchemistPotions.DAMAGE_PER_SPEC_POINT);
		mClassPassiveName = "Alchemist Potions";

		mSpecOne.mAbilities.add(new Taboo(plugin, player));
		mSpecOne.mAbilities.add(new ScorchedEarth(plugin, player));
		mSpecOne.mAbilities.add(new EsotericEnhancements(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103m";
		mSpecOne.mSpecialization = 9;
		mSpecOne.mSpecName = "Harbinger";
		mSpecOne.mDisplayItem = new ItemStack(Material.DEAD_BUSH, 1);
		mSpecOne.mDescription = "Harbingers use special potions to weaken and destroy their enemies. Harbingers prefer slinging deadly potions and using strategy.";

		mSpecTwo.mAbilities.add(new Panacea(plugin, player));
		mSpecTwo.mAbilities.add(new TransmutationRing(plugin, player));
		mSpecTwo.mAbilities.add(new WardingRemedy(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103f";
		mSpecTwo.mSpecialization = 10;
		mSpecTwo.mSpecName = "Apothecary";
		mSpecTwo.mDisplayItem = new ItemStack(Material.BLUE_ORCHID, 1);
		mSpecTwo.mDescription = "Apothecaries employ magic potions to weaken enemies and support friends.";

	}
}
