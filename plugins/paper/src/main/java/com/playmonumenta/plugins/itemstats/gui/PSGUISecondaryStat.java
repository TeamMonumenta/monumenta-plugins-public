package com.playmonumenta.plugins.itemstats.gui;

import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.enchantments.Cloaked;
import com.playmonumenta.plugins.itemstats.enchantments.Ethereal;
import com.playmonumenta.plugins.itemstats.enchantments.Evasion;
import com.playmonumenta.plugins.itemstats.enchantments.Guard;
import com.playmonumenta.plugins.itemstats.enchantments.Poise;
import com.playmonumenta.plugins.itemstats.enchantments.Reflexes;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.Steadfast;
import com.playmonumenta.plugins.itemstats.enchantments.Tempo;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

enum PSGUISecondaryStat {
	POISE(1, Material.LILY_OF_THE_VALLEY, EnchantmentType.POISE, true, """
		Gain A EHP as B
		when above %s%% Max Health
		or half the Armor bonus
		when above %s%% Max Health.""".formatted(StringUtils.multiplierToPercentage(Poise.MIN_HEALTH_PERCENT), StringUtils.multiplierToPercentage(Poise.HALF_BONUS_PERCENT))),
	INURE(2, Material.NETHERITE_SCRAP, EnchantmentType.INURE, true, """
		Gain A EHP as B
		when taking the same type of mob damage consecutively
		(Melee, Projectile, Blast, or Magic)."""),
	STEADFAST(3, Material.LEAD, EnchantmentType.STEADFAST, true, """
		Gain A EHP as B
		scaling with the amount of missing HP, up to %s%%.
		Also calculates bonus from Second Wind when enabled.""".formatted(StringUtils.multiplierToPercentage(Steadfast.MISSING_HEALTH_MAXIMUM))),
	ETHEREAL(5, Material.PHANTOM_MEMBRANE, EnchantmentType.ETHEREAL, false, """
		Gain A EHP as B
		on hits taken within %s seconds of any previous hit.""".formatted(StringUtils.ticksToSeconds(Ethereal.PAST_HIT_DURATION_TIME))),
	REFLEXES(6, Material.ENDER_EYE, EnchantmentType.REFLEXES, false, """
		Gain A EHP as B
		after damaging enemies directly for %ss.
		Damage over Time (DoT) abilities and enchants do not
		trigger Reflexes. Half of the bonus is granted for
		an additional %ss.""".formatted(StringUtils.ticksToSeconds(Reflexes.REFLEXES_MAIN_DURATION), StringUtils.ticksToSeconds(Reflexes.REFLEXES_TOTAL_DURATION - Reflexes.REFLEXES_MAIN_DURATION))),
	EVASION(7, Material.ELYTRA, EnchantmentType.EVASION, false, """
		Gain A EHP as B
		when taking damage from a source further
		than %s blocks from the player.""".formatted(Evasion.DISTANCE)),
	SHIELDING(10, Material.NAUTILUS_SHELL, EnchantmentType.SHIELDING, true, """
		Gain A EHP as B
		when taking damage from an enemy within %s blocks.
		Taking damage that would stun a shield
		halves Shielding reduction for %s seconds.""".formatted(Shielding.DISTANCE, StringUtils.ticksToSeconds(Shielding.DISABLE_DURATION))),
	GUARD(11, Material.SHULKER_SHELL, EnchantmentType.GUARD, true, """
		Gain A EHP as B
		after blocking an attack with a shield.
		The duration lasts for %ss if blocked
		from offhand, and %ss from mainhand.
		Guard also activates for %ss when taking
		%s%% or greater damage in a single strike.""".formatted(
		StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_OFFHAND), StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_MAINHAND),
		StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_HEALTH), StringUtils.multiplierToPercentage(Guard.HEALTH_RATIO))),
	TEMPO(15, Material.CLOCK, EnchantmentType.TEMPO, false, """
		Gain A EHP as B
		on the first hit taken after
		%s seconds of taking no damage.
		Half of the bonus is granted after
		%s seconds of taking no damage.""".formatted(
		StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME),
		StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME_HALF)
	)),
	CLOAKED(16, Material.BLACK_DYE, EnchantmentType.CLOAKED, false, """
		Gain A EHP as B
		when there are %s or fewer enemies within %s blocks.
		Half of the bonus is granted if there are %s or
		fewer enemies. Cloaked also activates for %ss after
		killing an Elite or Boss mob.""".formatted(Cloaked.MOB_CAP, Cloaked.RADIUS, Cloaked.MOB_CAP_HALF_EFFECT, StringUtils.ticksToSeconds(Cloaked.CLOAKED_DURATION)));

	private final int mSlot;
	private final Material mIcon;
	private final String mName;
	private final boolean mIsArmorModifier;
	private final EnchantmentType mEnchantmentType;
	private final String mDescription;

	PSGUISecondaryStat(int slot, Material icon, EnchantmentType enchantmentType, boolean isArmorModifier, String description) {
		mSlot = slot;
		mIcon = icon;
		mName = enchantmentType.getName();
		mEnchantmentType = enchantmentType;
		mIsArmorModifier = isArmorModifier;
		mDescription = description;
	}

	public int getSlot() {
		return mSlot;
	}

	public Material getIcon() {
		return mIcon;
	}

	public String getName() {
		return mName;
	}

	public EnchantmentType getEnchantmentType() {
		return mEnchantmentType;
	}

	public boolean isArmorModifier() {
		return mIsArmorModifier;
	}

	public Component getDisplay(boolean enabled) {
		return Component.text(String.format("Calculate Bonus from %s%s", mName, enabled ? " - Enabled" : " - Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	}

	public List<Component> getDisplayLore(Region region) {
		String[] descriptionArray = mDescription.split("\n");
		String scaledEHPAmount = StringUtils.multiplierToPercentage(Armor.getSecondaryEHPMultiplier(region));
		descriptionArray[0] = "Gain up to (Level*%s%%) Effective Health as %s".formatted(scaledEHPAmount, mIsArmorModifier ? "Armor" : "Agility");
		return Arrays.stream(descriptionArray).map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList();
	}
}
