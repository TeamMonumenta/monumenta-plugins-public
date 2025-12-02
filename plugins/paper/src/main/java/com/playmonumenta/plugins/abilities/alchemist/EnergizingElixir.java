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
import com.playmonumenta.plugins.cosmetics.skills.alchemist.EnergizingElixirCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EnergizingElixir extends Ability implements PotionAbility {
	private static final double SPEED_AMPLIFIER = 0.15;
	private static final double DAMAGE_AMPLIFIER = 0.1;
	private static final int DAMAGE_AMPLIFIER_DURATION = 4 * 20;
	private static final double ENHANCEMENT_ABSORPTION_AMOUNT = 1;
	private static final double ENHANCEMENT_ABSORPTION_MAX = 2;
	private static final int ENHANCEMENT_ABSORPTION_DURATION = 4 * 20;
	private static final int ENHANCEMENT_DEBUFF_REDUCTION = 20;
	private static final int EFFECTS_DURATION = 8 * 20;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EnergizingElixirPercentSpeedEffect";
	private static final String DAMAGE_AMPLIFIER_EFFECT_NAME = "EnergizingElixirPercentDamageDealtEffect";
	private static final int JUMP_LEVEL = 1; // Jump Boost 2, effect potency is 0 indexed
	private static final int COSMETIC_APPLICATION_COOLDOWN = 20;

	private static final String DISABLE_JUMP_BOOST_TAG = "EnergizingElixirNoJumpBoost";

	public static final String CHARM_DURATION = "Energizing Elixir Effect Duration";
	public static final String CHARM_SPEED = "Energizing Elixir Speed Modifier";
	public static final String CHARM_JUMP_BOOST = "Energizing Elixir Jump Boost Modifier";
	public static final String CHARM_COOLDOWN = "Energizing Elixir Cooldown";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Energizing Elixir Damage Amplifier";
	public static final String CHARM_DAMAGE_AMPLIFIER_DURATION = "Energizing Elixir Damage Amplifier Duration";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_AMOUNT = "Energizing Elixir Enhancement Absorption Amount";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_MAX = "Energizing Elixir Enhancement Absorption Max";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_DURATION = "Energizing Elixir Enhancement Absorption Duration";
	public static final String CHARM_ENHANCEMENT_DEBUFF_REDUCTION = "Energizing Elixir Enhancement Debuff Reduction";

	public static final AbilityInfo<EnergizingElixir> INFO =
		new AbilityInfo<>(EnergizingElixir.class, "Energizing Elixir", EnergizingElixir::new)
			.linkedSpell(ClassAbility.ENERGIZING_ELIXIR)
			.scoreboardId("EnergizingElixir")
			.shorthandName("EE")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Splash potions on yourself and allies to grant buffs.")
			.addTrigger(new AbilityTriggerInfo<>("toggleJumpBoost", "toggle jump boost",
				EnergizingElixir::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).enabled(false).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false), HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.RABBIT_FOOT);

	private final double mSpeedAmp;
	private final int mJumpBoostAmp;
	private final double mDamageAmp;
	private final int mDamageAmpDuration;
	private final double mAbsorptionAmount;
	private final double mAbsorptionMax;
	private final int mAbsorptionDuration;
	private final int mDebuffReduction;
	private final int mDuration;
	private final EnergizingElixirCS mCosmetic;
	private int lastApplicationTime = 0;

	public EnergizingElixir(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpeedAmp = SPEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mJumpBoostAmp = JUMP_LEVEL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EFFECTS_DURATION);
		mDamageAmp = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		mDamageAmpDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_AMPLIFIER_DURATION, DAMAGE_AMPLIFIER_DURATION);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_DURATION, ENHANCEMENT_ABSORPTION_DURATION);
		mAbsorptionAmount = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_AMOUNT, ENHANCEMENT_ABSORPTION_AMOUNT);
		mAbsorptionMax = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_MAX, ENHANCEMENT_ABSORPTION_MAX);
		mDebuffReduction = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_DEBUFF_REDUCTION, ENHANCEMENT_DEBUFF_REDUCTION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new EnergizingElixirCS());
	}

	private boolean toggleJumpBoost() {
		if (ScoreboardUtils.toggleTag(mPlayer, DISABLE_JUMP_BOOST_TAG)) {
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been disabled"));
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP, mJumpBoostAmp);
		} else {
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been enabled"));
		}
		return true;
	}

	@Override
	public void applyToPlayer(final Player player, final ThrownPotion potion, final boolean isGruesome) {
		if (player.equals(mPlayer)) {
			mPlugin.mEffectManager.addEffect(
				mPlayer,
				PERCENT_SPEED_EFFECT_NAME,
				new PercentSpeed(mDuration, mSpeedAmp, PERCENT_SPEED_EFFECT_NAME)
					.deleteOnAbilityUpdate(true)
			);
			if (!mPlayer.getScoreboardTags().contains(DISABLE_JUMP_BOOST_TAG)) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					new PotionEffect(PotionEffectType.JUMP, mDuration, mJumpBoostAmp, true, false, true)
				);
			}
			if (mPlayer.getTicksLived() > lastApplicationTime + COSMETIC_APPLICATION_COOLDOWN) {
				mCosmetic.activate(mPlayer);
				lastApplicationTime = mPlayer.getTicksLived();
			}
			return;
		}

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(
				player,
				DAMAGE_AMPLIFIER_EFFECT_NAME,
				new PercentDamageDealt(mDamageAmpDuration, mDamageAmp)
			);
		}

		if (isEnhanced()) {
			if (isGruesome) {
				PotionUtils.reduceAllDebuffsDuration(mPlugin, player, mDebuffReduction);
			} else {
				AbsorptionUtils.addAbsorption(player, mAbsorptionAmount, mAbsorptionMax, mAbsorptionDuration);
			}
		}
	}

	private static Description<EnergizingElixir> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Splash yourself with a potion to gain +")
			.addPercent(a -> a.mSpeedAmp, SPEED_AMPLIFIER)
			.add(" speed and Jump Boost ")
			.addPotionAmplifier(a -> a.mJumpBoostAmp, JUMP_LEVEL)
			.add(" for ")
			.addDuration(a -> a.mDuration, EFFECTS_DURATION)
			.add("s.");
	}

	private static Description<EnergizingElixir> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your potions now buff splashed allies with +")
			.addPercent(a -> a.mDamageAmp, DAMAGE_AMPLIFIER)
			.add(" damage dealt for ")
			.addDuration(a -> a.mDamageAmpDuration, DAMAGE_AMPLIFIER_DURATION)
			.add("s, refreshing on each application.");
	}

	private static Description<EnergizingElixir> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Splashing allies with your Brutal potions now grants them ")
			.add(a -> a.mAbsorptionAmount, ENHANCEMENT_ABSORPTION_AMOUNT)
			.add(" absorption health for ")
			.addDuration(a -> a.mAbsorptionDuration, ENHANCEMENT_ABSORPTION_DURATION)
			.add("s, up to a maximum of ")
			.add(a -> a.mAbsorptionMax, ENHANCEMENT_ABSORPTION_MAX)
			.add(" absorption health, and splashing allies with your Gruesome potions now reduces the duration of all their vanilla potion debuffs by ")
			.addDuration(a -> a.mDebuffReduction, ENHANCEMENT_DEBUFF_REDUCTION)
			.add("s.");
	}
}
