package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeAlchemyCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GruesomeAlchemy extends Ability implements PotionAbility {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.15;
	private static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.25;
	private static final double GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 0.15;
	private static final double GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 0.25;
	private static final double GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER = 0.1;

	public static final double GRUESOME_POTION_DAMAGE_MULTIPLIER = 0.8;

	public static final String CHARM_DAMAGE_MULTIPLIER = "Gruesome Alchemy Damage Multiplier";
	public static final String CHARM_SLOWNESS = "Gruesome Alchemy Slowness Amplifier";
	public static final String CHARM_VULNERABILITY = "Gruesome Alchemy Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Gruesome Alchemy Weakness Amplifier";
	public static final String CHARM_DURATION = "Gruesome Alchemy Duration";

	public static final AbilityInfo<GruesomeAlchemy> INFO =
		new AbilityInfo<>(GruesomeAlchemy.class, "Gruesome Alchemy", GruesomeAlchemy::new)
			.linkedSpell(ClassAbility.GRUESOME_ALCHEMY)
			.scoreboardId("GruesomeAlchemy")
			.shorthandName("GA")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw potions that deal less damage, but slow and apply vulnerability to enemies.")
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", "Toggles between throwing gruesome or brutal potions.",
				GruesomeAlchemy::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP), HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("throwOpposite", "throw opposite potion", "Throws a potion of the opposite type, e.g. a gruesome potion if brutal potions are selected.",
				GruesomeAlchemy::throwOpposite, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).enabled(false), HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.SKELETON_SKULL);

	private final int mDuration;
	private final double mSlownessAmount;
	private final double mVulnerabilityAmount;
	private final double mWeaken;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final GruesomeAlchemyCS mCosmetic;

	public GruesomeAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, GRUESOME_ALCHEMY_DURATION);
		mSlownessAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mVulnerabilityAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER : GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY);
		mWeaken = GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isGruesome) {
			EntityUtils.applySlow(mPlugin, mDuration, mSlownessAmount, mob);
			EntityUtils.applyVulnerability(mPlugin, mDuration, mVulnerabilityAmount, mob);
			EntityUtils.applyWeaken(mPlugin, mDuration, mWeaken, mob);
			if (isEnhanced()) {
				EntityUtils.paralyze(mPlugin, mDuration, mob);
			}
		}
	}

	public boolean toggle() {
		if (mAlchemistPotions != null) {
			mCosmetic.effectsOnSwap(mPlayer, mAlchemistPotions.isGruesomeMode());
			mAlchemistPotions.swapMode();
			return true;
		}
		return false;
	}

	private boolean throwOpposite() {
		if (mAlchemistPotions != null && MetadataUtils.checkOnceInRecentTicks(mPlugin, mPlayer, "GruesomeAlchemy_throwOpposite", 3)) {
			mAlchemistPotions.throwPotion(!mAlchemistPotions.isGruesomeMode());
			return true;
		}
		return false;
	}

	private static Description<GruesomeAlchemy> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to switch to Gruesome potions. These potions deal ")
			.addPercent(GRUESOME_POTION_DAMAGE_MULTIPLIER)
			.add(" of the magic damage of your Brutal potions and do not afflict damage over time. Instead, they apply ")
			.addPercent(a -> a.mSlownessAmount, GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER, false, Ability::isLevelOne)
			.add(" slow, ")
			.addPercent(a -> a.mVulnerabilityAmount, GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER, false, Ability::isLevelOne)
			.add(" vulnerability, and ")
			.addPercent(a -> a.mWeaken, GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER)
			.add(" weaken for ")
			.addDuration(a -> a.mDuration, GRUESOME_ALCHEMY_DURATION)
			.add(" seconds.");
	}

	private static Description<GruesomeAlchemy> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The slow is increased to ")
			.addPercent(a -> a.mSlownessAmount, GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER, false, Ability::isLevelTwo)
			.add(" and the vulnerability is increased to ")
			.addPercent(a -> a.mVulnerabilityAmount, GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<GruesomeAlchemy> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your Gruesome potions now additionally paralyze (25% chance for 100% slowness for a second once a second) mobs for ")
			.addDuration(a -> a.mDuration, GRUESOME_ALCHEMY_DURATION)
			.add(" seconds.");
	}
}
