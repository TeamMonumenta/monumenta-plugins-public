package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.Channeling;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Mage extends PlayerClass {

	Mage() {
		mAbilities.add(ArcaneStrike.INFO);
		mAbilities.add(ElementalArrows.INFO);
		mAbilities.add(FrostNova.INFO);
		mAbilities.add(MagmaShield.INFO);
		mAbilities.add(ManaLance.INFO);
		mAbilities.add(Spellshock.INFO);
		mAbilities.add(ThunderStep.INFO);
		mAbilities.add(PrismaticShield.INFO);
		mClass = 1;
		mClassName = "Mage";
		mClassColor = NamedTextColor.LIGHT_PURPLE;
		mChatColor = ChatColor.LIGHT_PURPLE;
		mDisplayItem = new ItemStack(Material.BLAZE_ROD, 1);
		mClassDescription = "Mages are masters of area control, freezing, wounding, and igniting enemies with their strikes.";
		mClassPassiveDescription = String.format("After casting a spell, your next melee attack with a wand deals %s%% more damage.", (int) (Channeling.PERCENT_MELEE_INCREASE * 100));
		mClassPassiveName = "Channeling";

		mSpecOne.mAbilities.add(AstralOmen.INFO);
		mSpecOne.mAbilities.add(CosmicMoonblade.INFO);
		mSpecOne.mAbilities.add(SagesInsight.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103i";
		mSpecOne.mSpecialization = 1;
		mSpecOne.mSpecName = "Arcanist";
		mSpecOne.mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
		mSpecOne.mDescription = "Arcanists are mages that specialize at controlling their skill cooldowns and getting up close.";

		mSpecTwo.mAbilities.add(Blizzard.INFO);
		mSpecTwo.mAbilities.add(ElementalSpiritFire.INFO);
		mSpecTwo.mAbilities.add(Starfall.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103b";
		mSpecTwo.mSpecialization = 2;
		mSpecTwo.mSpecName = "Elementalist";
		mSpecTwo.mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
		mSpecTwo.mDescription = "Elementalists are the undisputed masters of the elements. They excel at zoning and crowd control.";

		mTriggerOrder = ImmutableList.of(
			CosmicMoonblade.INFO,

			Blizzard.INFO,
			Starfall.INFO,

			FrostNova.INFO,
			MagmaShield.INFO, // after blizzard
			ManaLance.INFO,
			ThunderStep.INFO
		);
	}
}
