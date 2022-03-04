package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;




public class Cleric extends PlayerClass {

	Cleric(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new CelestialBlessing(plugin, player));
		mAbilities.add(new CleansingRain(plugin, player));
		mAbilities.add(new DivineJustice(plugin, player));
		mAbilities.add(new HandOfLight(plugin, player));
		mAbilities.add(new HeavenlyBoon(plugin, player));
		mAbilities.add(new Crusade(plugin, player));
		mAbilities.add(new SacredProvisions(plugin, player));
		mAbilities.add(new SanctifiedArmor(plugin, player));
		mClass = 3;
		mClassName = "Cleric";
		mClassColor = NamedTextColor.YELLOW;
		mChatColor = ChatColor.YELLOW;
		mDisplayItem = new ItemStack(Material.POPPY, 1);
		mClassDescription = "Clerics are mighty healers and specialize in fighting the undead.";
		mClassPassiveDescription = "You and all allies in a 12 block radius heal 5% of max health every 5s while under 50% health.";

		mSpecOne.mAbilities.add(new HolyJavelin(plugin, player));
		mSpecOne.mAbilities.add(new ChoirBells(plugin, player));
		mSpecOne.mAbilities.add(new LuminousInfusion(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103d";
		mSpecOne.mSpecialization = 5;
		mSpecOne.mSpecName = "Paladin";
		mSpecOne.mDisplayItem = new ItemStack(Material.GHAST_TEAR, 1);
		mSpecOne.mDescription = "Paladins are mighty healers that have mastered combating undead foes. They will make sure the dead stay dead.";

		mSpecTwo.mAbilities.add(new EnchantedPrayer(plugin, player));
		mSpecTwo.mAbilities.add(new ThuribleProcession(plugin, player));
		mSpecTwo.mAbilities.add(new HallowedBeam(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103k";
		mSpecTwo.mSpecialization = 6;
		mSpecTwo.mSpecName = "Hierophant";
		mSpecTwo.mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
		mSpecTwo.mDescription = "Hierophants specialize in support and have mastered healing. They will make sure the living stay living.";

	}
}
