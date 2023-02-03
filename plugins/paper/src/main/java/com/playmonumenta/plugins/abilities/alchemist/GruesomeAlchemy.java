package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeAlchemyCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GruesomeAlchemy extends PotionAbility {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.2;
	private static final double GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 0.2;
	private static final double GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER = 0.1;

	public static final double GRUESOME_POTION_DAMAGE_MULTIPLIER = 0.8;

	public static final String CHARM_DAMAGE = "Gruesome Alchemy Damage Multiplier";
	public static final String CHARM_SLOWNESS = "Gruesome Alchemy Slowness Amplifier";
	public static final String CHARM_VULNERABILITY = "Gruesome Alchemy Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Gruesome Alchemy Weakness Amplifier";
	public static final String CHARM_DURATION = "Gruesome Alchemy Duration";

	public static final AbilityInfo<GruesomeAlchemy> INFO =
		new AbilityInfo<>(GruesomeAlchemy.class, "Gruesome Alchemy", GruesomeAlchemy::new)
			.linkedSpell(ClassAbility.GRUESOME_ALCHEMY)
			.scoreboardId("GruesomeAlchemy")
			.shorthandName("GA")
			.descriptions(
				"Swap hands while holding an Alchemist's Bag to switch to Gruesome potions. " +
					"These potions deal 80% of the magic damage of your Brutal potions and do not afflict damage over time. " +
					"Instead, they apply 10% Slow, 10% Vulnerability, and 10% Weaken for 8 seconds. ",
				"The Slow and Vulnerability are increased to 20%.",
				"Your Gruesome potions now additionally paralyze (25% chance for 100% slowness for a second once a second) mobs for 8s.")
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", GruesomeAlchemy::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(new ItemStack(Material.SKELETON_SKULL, 1));

	private final double mSlownessAmount;
	private final double mVulnerabilityAmount;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final GruesomeAlchemyCS mCosmetic;

	public GruesomeAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		//This is just for the Alchemical Artillery integration
		mSlownessAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mVulnerabilityAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER : GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS(), GruesomeAlchemyCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isGruesome) {
			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, GRUESOME_ALCHEMY_DURATION);
			EntityUtils.applySlow(mPlugin, duration, mSlownessAmount, mob);
			EntityUtils.applyVulnerability(mPlugin, duration, mVulnerabilityAmount, mob);
			EntityUtils.applyWeaken(mPlugin, duration, GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN), mob);
			if (isEnhanced()) {
				EntityUtils.paralyze(mPlugin, duration, mob);
			}
		}
	}

	public void toggle() {
		if (mAlchemistPotions != null) {
			mCosmetic.effectsOnSwap(mPlayer, mAlchemistPotions.isGruesomeMode());
			mAlchemistPotions.swapMode(mCosmetic.getSwapBrewPitch());
		}
	}
}
