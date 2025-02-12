package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CleansingRainCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class CleansingRain extends Ability implements AbilityWithDuration {
	private static final int CLEANSING_DURATION = TICKS_PER_SECOND * 15;
	private static final double PERCENT_DAMAGE_RESIST = -0.2;
	private static final int CLEANSING_EFFECT_DURATION = TICKS_PER_SECOND * 3;
	private static final int CLEANSING_APPLY_PERIOD = 1;
	private static final int CLEANSING_RADIUS = 4;
	private static final int CLEANSING_RADIUS_ENHANCED = 6;
	private static final int CLEANSING_1_COOLDOWN = TICKS_PER_SECOND * 45;
	private static final int CLEANSING_2_COOLDOWN = TICKS_PER_SECOND * 30;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "CleansingPercentDamageResistEffect";

	public static final String CHARM_REDUCTION = "Cleansing Rain Damage Reduction";
	public static final String CHARM_DURATION = "Cleansing Rain Duration";
	public static final String CHARM_RANGE = "Cleansing Rain Range";
	public static final String CHARM_COOLDOWN = "Cleansing Rain Cooldown";

	public static final AbilityInfo<CleansingRain> INFO =
		new AbilityInfo<>(CleansingRain.class, "Cleansing Rain", CleansingRain::new)
			.linkedSpell(ClassAbility.CLEANSING_RAIN)
			.scoreboardId("Cleansing")
			.shorthandName("CR")
			.descriptions(
				"Right click while sneaking and looking upwards to summon a \"cleansing rain\" that follows you, " +
					"removing negative effects from players within %s blocks, including yourself, and lasts for %s seconds. Cooldown: %ss."
						.formatted(CLEANSING_RADIUS, StringUtils.ticksToSeconds(CLEANSING_DURATION), StringUtils.ticksToSeconds(CLEANSING_1_COOLDOWN)),
				"Additionally grants %s%% Damage Reduction to all players in the radius. Cooldown: %ss."
					.formatted(StringUtils.multiplierToPercentage(Math.abs(PERCENT_DAMAGE_RESIST)), StringUtils.ticksToSeconds(CLEANSING_2_COOLDOWN)),
				"The radius increases to %s blocks, and each player touched by the rain keeps its effect for the cast duration."
					.formatted(CLEANSING_RADIUS_ENHANCED))
			.simpleDescription("Summon a rain cloud that cleanses debuffs and grants resistance to players below it.")
			.cooldown(CLEANSING_1_COOLDOWN, CLEANSING_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingRain::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON)))
			.displayItem(Material.NETHER_STAR);

	private final int mRainDuration;
	private final double mRadius;
	private final double mResistancePotency;
	private final CleansingRainCS mCosmetic;

	private @Nullable BukkitRunnable mRainRunnable;
	private int mCurrDuration = -1;

	public CleansingRain(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRainDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, CLEANSING_DURATION);
		mRadius = CharmManager.getRadius(player, CHARM_RANGE, isEnhanced() ? CLEANSING_RADIUS_ENHANCED : CLEANSING_RADIUS);
		mResistancePotency = PERCENT_DAMAGE_RESIST - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);
		mRainRunnable = null;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CleansingRainCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		mCosmetic.rainCast(mPlayer, mRadius);
		putOnCooldown();

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);
		mRainRunnable = new BukkitRunnable() {
			int mTicks = 0;
			final List<Player> mCleansedPlayers = new ArrayList<>();

			@Override
			public void run() {

				if (!mPlayer.isOnline() || mPlayer.isDead()) {
					this.cancel();
					return;
				}

				final double ratio = mRadius / CLEANSING_RADIUS;
				final double smallRatio = ratio / 3;
				mCosmetic.rainCloud(mPlayer, ratio, mRadius);

				final List<Player> rainPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true);
				for (final Player player : rainPlayers) {
					if (isEnhanced() && !mCleansedPlayers.contains(player)) {
						mCleansedPlayers.add(player);
						continue;
					}
					PotionUtils.clearNegatives(mPlugin, player);
					EntityUtils.setWeakenTicks(mPlugin, player, 0);
					EntityUtils.setSlowTicks(mPlugin, player, 0);

					if (player.getFireTicks() > 1) {
						player.setFireTicks(1);
					}

					if (isLevelTwo()) {
						mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME,
							new PercentDamageReceived(CLEANSING_EFFECT_DURATION, mResistancePotency)
								.deleteOnAbilityUpdate(true));
					}
				}
				//Loop through already affected players for enhanced cleansing rain
				if (isEnhanced()) {
					for (final Player player : mCleansedPlayers) {
						if (!rainPlayers.contains(player) && player != mPlayer) {
							mCosmetic.rainEnhancement(player, smallRatio, mRadius);
						}

						PotionUtils.clearNegatives(mPlugin, player);
						EntityUtils.setWeakenTicks(mPlugin, player, 0);
						EntityUtils.setSlowTicks(mPlugin, player, 0);

						if (player.getFireTicks() > 1) {
							player.setFireTicks(1);
						}

						if (isLevelTwo()) {
							mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME,
								new PercentDamageReceived(CLEANSING_EFFECT_DURATION, mResistancePotency)
									.deleteOnAbilityUpdate(true));
						}
					}
				}

				mTicks += CLEANSING_APPLY_PERIOD;

				if (mCurrDuration >= 0) {
					mCurrDuration++;
				}

				if (mTicks > getInitialAbilityDuration()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, CleansingRain.this);
			}
		};
		cancelOnDeath(mRainRunnable.runTaskTimer(mPlugin, 0, CLEANSING_APPLY_PERIOD));

		return true;
	}

	@Override
	public void invalidate() {
		if (mRainRunnable != null && !mRainRunnable.isCancelled()) {
			mRainRunnable.cancel();
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mRainDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
