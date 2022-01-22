package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.arcanist.SpatialShatter;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;




public class Mage extends PlayerClass {

	Mage(Plugin plugin, @Nullable Player player) {
		mAbilities.add(new ArcaneStrike(plugin, player));
		mAbilities.add(new ElementalArrows(plugin, player));
		mAbilities.add(new FrostNova(plugin, player));
		mAbilities.add(new MagmaShield(plugin, player));
		mAbilities.add(new ManaLance(plugin, player));
		mAbilities.add(new Spellshock(plugin, player));
		mAbilities.add(new ThunderStep(plugin, player));
		mAbilities.add(new PrismaticShield(plugin, player));
		mClass = 1;
		mClassName = "Mage";
		mClassColor = NamedTextColor.LIGHT_PURPLE;
		mChatColor = ChatColor.LIGHT_PURPLE;
		mDisplayItem = new ItemStack(Material.BLAZE_ROD, 1);
		mClassDescription = "Mages are masters of area control, freezing, wounding, and igniting enemies with their strikes.";
		mClassPassiveDescription = "After casting a spell, your next melee attack with a wand deals 20% more damage.";

		mSpecOne.mAbilities.add(new AstralOmen(plugin, player));
		mSpecOne.mAbilities.add(new SpatialShatter(plugin, player));
		mSpecOne.mAbilities.add(new SagesInsight(plugin, player));
		mSpecOne.mSpecQuestScoreboard = "Quest103i";
		mSpecOne.mSpecialization = 1;
		mSpecOne.mSpecName = "Arcanist";
		mSpecOne.mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
		mSpecOne.mDescription = "Arcanists are mages that specialize at controlling their skill cooldowns and getting up close.";

		mSpecTwo.mAbilities.add(new Blizzard(plugin, player));
		mSpecTwo.mAbilities.add(new ElementalSpiritFire(plugin, player));
		mSpecTwo.mAbilities.add(new Starfall(plugin, player));
		mSpecTwo.mSpecQuestScoreboard = "Quest103b";
		mSpecTwo.mSpecialization = 2;
		mSpecTwo.mSpecName = "Elementalist";
		mSpecTwo.mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
		mSpecTwo.mDescription = "Elementalists are the undisputed masters of the elements. They excel at zoning and crowd control.";
	}
}
