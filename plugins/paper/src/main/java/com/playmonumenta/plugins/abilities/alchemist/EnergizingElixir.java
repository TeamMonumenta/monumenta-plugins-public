package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.EnergizingElixirCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class EnergizingElixir extends Ability implements PotionAbility, AbilityWithDuration {
	private static final double SPEED_AMPLIFIER = 0.15;
	private static final double DAMAGE_AMPLIFIER = 0.15;
	private static final double ENHANCEMENT_ABSORPTION_AMOUNT = 1;
	private static final double ENHANCEMENT_ABSORPTION_MAX = 2;
	private static final int ENHANCEMENT_ABSORPTION_DURATION = 4 * 20;
	private static final int ENHANCEMENT_DEBUFF_REDUCTION = 20;
	private static final int EFFECTS_DURATION = 8 * 20;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EnergizingElixirPercentSpeedEffect";
	private static final String DAMAGE_AMPLIFIER_EFFECT_NAME = "EnergizingElixirPercentDamageDealtEffect";
	private static final int JUMP_LEVEL = 1; // Jump Boost 2, effect potency is 0 indexed
	private static final int COSMETIC_APPLICATION_COOLDOWN = 20;
	private static final int CLOUD_LINGER_TIME = 20;
	private static final int CLOUD_TICK_INTERVAL = 10;
	private static final double CLOUD_RADIUS_MULTIPLIER = 1.25;
	private static final double CLOUD_HEIGHT = 5;

	private static final String DISABLE_JUMP_BOOST_TAG = "EnergizingElixirNoJumpBoost";

	public static final String CHARM_DURATION = "Energizing Elixir Effect Duration";
	public static final String CHARM_SPEED = "Energizing Elixir Speed Modifier";
	public static final String CHARM_JUMP_BOOST = "Energizing Elixir Jump Boost Modifier";
	public static final String CHARM_COOLDOWN = "Energizing Elixir Cooldown";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Energizing Elixir Damage Amplifier";
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
			.displayItem(Material.RABBIT_FOOT)
			// Fake cooldown to make it show up on the ability hotbar
			.cooldown(1);

	private class CloudInstance {
		private final Location mLoc;
		private final ItemStatManager.PlayerItemStats mPlayerItemStats;
		private final boolean mIsGruesome;
		private final double mRadius;
		private final int mLifetime;
		private int mTimer = 0;

		private CloudInstance(Location loc, double radius, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
			mLoc = loc;
			mRadius = radius;
			mIsGruesome = isGruesome;
			mPlayerItemStats = playerItemStats;
			mLifetime = CLOUD_LINGER_TIME;
		}

		private boolean tick() {
			mCosmetic.displayCloud(mPlayer, mLoc, mRadius, CLOUD_HEIGHT, mIsGruesome, mAlchemistPotions);
			mTimer += 5;
			if (mTimer % CLOUD_TICK_INTERVAL == 0) {
				applyEffects(mLoc, mIsGruesome, mPlayerItemStats, false);
			}
			return mTimer >= mLifetime;
		}
	}

	private final double mSpeedAmp;
	private final int mJumpBoostAmp;
	private final double mDamageAmp;
	private final double mAbsorptionAmount;
	private final double mAbsorptionMax;
	private final int mAbsorptionDuration;
	private final int mDebuffReduction;
	private final int mDuration;
	private final EnergizingElixirCS mCosmetic;
	private final ArrayList<CloudInstance> mCloudInstances = new ArrayList<>();
	private final HashMap<UUID, Integer> mAffectedPlayersMap = new HashMap<>();
	private int mLastApplicationTime = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mRemainingDuration = 0;

	public EnergizingElixir(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpeedAmp = SPEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mJumpBoostAmp = JUMP_LEVEL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EFFECTS_DURATION);
		mDamageAmp = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_DURATION, ENHANCEMENT_ABSORPTION_DURATION);
		mAbsorptionAmount = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_AMOUNT, ENHANCEMENT_ABSORPTION_AMOUNT);
		mAbsorptionMax = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_MAX, ENHANCEMENT_ABSORPTION_MAX);
		mDebuffReduction = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_DEBUFF_REDUCTION, ENHANCEMENT_DEBUFF_REDUCTION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new EnergizingElixirCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		});
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
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mRemainingDuration;
	}

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, Vector originalPotionVelocity, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return false;
		}

		boolean isGruesome = mAlchemistPotions.isGruesome(potion);
		applyEffects(loc, isGruesome, playerItemStats, true);
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mAlchemistPotions == null || !mPlayer.isOnline()) {
			mCloudInstances.clear();
			return;
		}
		mCloudInstances.removeIf(CloudInstance::tick);
		mRemainingDuration = Math.max(0, mRemainingDuration - 5);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private void applyEffects(Location splashLoc, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, boolean createCloud) {
		if (mAlchemistPotions == null) {
			return;
		}
		double radius = mAlchemistPotions.getRadius(playerItemStats) * CLOUD_RADIUS_MULTIPLIER;
		new Hitbox.UprightCylinderHitbox(splashLoc.clone().subtract(0, CLOUD_HEIGHT, 0), CLOUD_HEIGHT * 2, radius)
			.getHitPlayers(true).stream()
			.filter(player -> {
				// Only allow players to get affected by one cloud at a time, preventing splashing all your potions
				// down at once from actually being effective (mostly for the Enhancement).
				int currentTick = Bukkit.getCurrentTick();
				Integer lastPlayerApplicationTicks = mAffectedPlayersMap.get(player.getUniqueId());
				if (lastPlayerApplicationTicks == null || lastPlayerApplicationTicks == 0) {
					mAffectedPlayersMap.put(player.getUniqueId(), currentTick);
					return true;
				}
				if (currentTick - lastPlayerApplicationTicks >= CLOUD_TICK_INTERVAL) {
					mAffectedPlayersMap.put(player.getUniqueId(), currentTick);
					return true;
				}
				return false;
			})
			.forEach(player -> applyEffects(player, isGruesome));
		if (createCloud) {
			CloudInstance cloudInstance = new CloudInstance(splashLoc, radius, isGruesome, playerItemStats);
			mCloudInstances.add(cloudInstance);
		}
	}

	private void applyEffects(Player player, boolean isGruesome) {
		if (player.equals(mPlayer)) {
			mRemainingDuration = mDuration;
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
			if (mPlayer.getTicksLived() > mLastApplicationTime + COSMETIC_APPLICATION_COOLDOWN) {
				mCosmetic.activate(mPlayer);
				mLastApplicationTime = mPlayer.getTicksLived();
			}
			return;
		}

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(
				player,
				DAMAGE_AMPLIFIER_EFFECT_NAME,
				new PercentDamageDealt(mDuration, mDamageAmp)
			);
		}

		if (isEnhanced()) {
			if (isGruesome) {
				PotionUtils.reduceAllDebuffsDuration(mPlugin, player, mDebuffReduction);
				Optional.ofNullable(mPlugin.mEffectManager.getEffects(player))
					.ifPresent(effectList -> effectList.stream()
						.filter(effect -> effect != null && effect.isDebuff())
						.forEach(effect -> effect.setDuration(Math.max(0, effect.getDuration() - ENHANCEMENT_DEBUFF_REDUCTION))));
			} else {
				AbsorptionUtils.addAbsorption(player, mAbsorptionAmount, mAbsorptionMax, mAbsorptionDuration);
			}
		}
	}

	private static Description<EnergizingElixir> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Your potions leave lingering clouds that")
			.addLine("grant yourself speed and jump boost.")
			.addLine()
			.addStat("Effect: +%p Speed for %t")
				.statValues(stat(a -> a.mSpeedAmp, SPEED_AMPLIFIER), stat(a -> a.mDuration, EFFECTS_DURATION))
			.addStat("Effect: Jump Boost %d for %t")
				.statValues(stat(a -> a.mJumpBoostAmp + 1, JUMP_LEVEL + 1), stat(a -> a.mDuration, EFFECTS_DURATION))
			.addDashedLine();
	}

	private static Description<EnergizingElixir> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("The clouds now grant other players")
			.addLine("increased damage.")
			.addLine()
			.addStat("Effect: +%p Damage for %t")
				.statValues(stat(a -> a.mDamageAmp, DAMAGE_AMPLIFIER), stat(a -> a.mDuration, EFFECTS_DURATION))
			.addDashedLine();
	}

	private static Description<EnergizingElixir> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Clouds formed by *Brutal* potions grant").styles(Alchemist.BRUTAL_COLOR)
			.addLine("other players absorption.")
			.addLine()
			.addStat("Effect: +%d Absorption for %t (max +%d)")
				.statValues(stat(a -> a.mAbsorptionAmount, ENHANCEMENT_ABSORPTION_AMOUNT), stat(a -> a.mAbsorptionDuration, ENHANCEMENT_ABSORPTION_DURATION), stat(a -> a.mAbsorptionMax, ENHANCEMENT_ABSORPTION_MAX))
			.addLine()
			.addLine("Clouds formed by *Gruesome* potions reduce").styles(Alchemist.GRUESOME_COLOR)
			.addLine("the duration of other players' debuffs.")
			.addLine()
			.addStat("Effect: -%t Debuff Duration")
				.statValues(stat(a -> a.mDebuffReduction, ENHANCEMENT_DEBUFF_REDUCTION))
			.addDashedLine();
	}
}
