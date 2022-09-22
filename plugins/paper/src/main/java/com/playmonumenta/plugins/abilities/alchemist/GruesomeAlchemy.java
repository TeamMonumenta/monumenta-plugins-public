package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeAlchemyCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class GruesomeAlchemy extends PotionAbility {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.2;
	private static final double GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER = 0.2;
	private static final double GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER = 0.1;

	public static final double GRUESOME_POTION_DAMAGE_MULTIPLIER = 0.8;

	public static final String CHARM_DAMAGE = "Gruesome Alchemy Damage Mutliplier";
	public static final String CHARM_SLOWNESS = "Gruesome Alchemy Slowness Amplifier";
	public static final String CHARM_VULNERABILITY = "Gruesome Alchemy Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Gruesome Alchemy Weakness Amplifier";
	public static final String CHARM_DURATION = "Gruesome Alchemy Duration";

	private final double mSlownessAmount;
	private final double mVulnerabilityAmount;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable AlchemicalArtillery mAlchemicalArtillery;

	private boolean mHasTaboo;
	private boolean mHasWardingRemedy;
	private boolean mHasPanacea;

	private final GruesomeAlchemyCS mCosmetic;

	public GruesomeAlchemy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Gruesome Alchemy", 0, 0);
		mInfo.mScoreboardId = "GruesomeAlchemy";
		mInfo.mShorthandName = "GA";
		mInfo.mDescriptions.add("Swap hands while holding an Alchemist's Bag to switch to Gruesome potions. These potions deal 80% of the damage of your Brutal potions and do not afflict damage over time. Instead, they apply 10% Slow, 10% Vulnerability, and 10% Weaken for 8 seconds. If Alchemical Artillery is active, left clicking while holding a bow, crossbow, or trident will also swap modes.");
		mInfo.mDescriptions.add("The Slow and Vulnerability are increased to 20%.");
		mInfo.mDescriptions.add("Your Gruesome potions now additionally paralyze (25% chance for 100% slowness for a second once a second) mobs for 8s.");

		//This is just for the Alchemical Artillery integration
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mLinkedSpell = ClassAbility.GRUESOME_ALCHEMY;

		mSlownessAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mVulnerabilityAmount = (isLevelOne() ? GRUESOME_ALCHEMY_1_VULNERABILITY_AMPLIFIER : GRUESOME_ALCHEMY_2_VULNERABILITY_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY);
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GruesomeAlchemyCS(), GruesomeAlchemyCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mAlchemicalArtillery = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemicalArtillery.class);

			mHasTaboo = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Taboo.class) != null;
			mHasWardingRemedy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, WardingRemedy.class) != null;
			mHasPanacea = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Panacea.class) != null;
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome) {
		if (isGruesome) {
			int duration = GRUESOME_ALCHEMY_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
			EntityUtils.applySlow(mPlugin, duration, mSlownessAmount, mob);
			EntityUtils.applyVulnerability(mPlugin, duration, mVulnerabilityAmount, mob);
			EntityUtils.applyWeaken(mPlugin, duration, GRUESOME_ALCHEMY_WEAKEN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN), mob);
			if (isEnhanced()) {
				EntityUtils.paralyze(mPlugin, duration, mob);
			}
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || (mPlayer.isSneaking() && (mHasTaboo || mHasWardingRemedy))) {
			return;
		}

		if (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && mAlchemistPotions != null) {
			mCosmetic.effectsOnSwap(mPlayer, mAlchemistPotions.isGruesomeMode());
			mAlchemistPotions.swapMode(mCosmetic.getSwapBrewPitch());
		}
	}

	//Alchemical Artillery integration
	@Override
	public void cast(Action action) {
		if (mPlayer != null && mAlchemicalArtillery != null && mAlchemistPotions != null && mAlchemicalArtillery.isActive() && ItemUtils.isBowOrTrident(mPlayer.getInventory().getItemInMainHand()) && !(mHasPanacea && mPlayer.isSneaking())) {
			mCosmetic.effectsOnSwap(mPlayer, mAlchemistPotions.isGruesomeMode());
			mAlchemistPotions.swapMode(mCosmetic.getSwapBrewPitch());
		}
	}
}
