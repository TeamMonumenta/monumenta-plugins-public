package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.TransmutationRingCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class TransmutationRing extends Ability implements PotionAbility, AbilityWithDuration {
	private static final int TRANSMUTATION_RING_1_COOLDOWN = 25 * 20;
	private static final int TRANSMUTATION_RING_2_COOLDOWN = 20 * 20;
	private static final int TRANSMUTATION_RING_RADIUS = 5;
	private static final int TRANSMUTATION_RING_DURATION = 10 * 20;
	private static final String TRANSMUTATION_RING_DAMAGE_EFFECT_NAME = "TransmutationRingDamageEffect";
	private static final double DAMAGE_AMPLIFIER = 0.15;
	private static final double DAMAGE_PER_DEATH_AMPLIFIER = 0.01;
	private static final int MAX_KILLS = 15;
	private static final int DURATION_INCREASE = 7; // was 0.333333 seconds but minecraft tick system is bad
	private static final int MAX_DURATION_INCREASE = 5 * 20;
	private static final double REFUND_POTION_AMOUNT = 0.5;

	public static final String TRANSMUTATION_POTION_METAKEY = "TransmutationRingPotion";

	public static final String CHARM_COOLDOWN = "Transmutation Ring Cooldown";
	public static final String CHARM_RADIUS = "Transmutation Ring Radius";
	public static final String CHARM_DURATION = "Transmutation Ring Duration";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Transmutation Ring Damage Amplifier";
	public static final String CHARM_PER_KILL_AMPLIFIER = "Transmutation Ring Per Death Amplifier";
	public static final String CHARM_MAX_KILLS = "Transmutation Ring Max Kills";

	public static final AbilityInfo<TransmutationRing> INFO =
		new AbilityInfo<>(TransmutationRing.class, "Transmutation Ring", TransmutationRing::new)
			.linkedSpell(ClassAbility.TRANSMUTATION_RING)
			.scoreboardId("Transmutation")
			.shorthandName("TR")
			.descriptions(
				("Sneak while throwing an Alchemist's Potion to create a Transmutation Ring at the potion's landing location that lasts for %ss. " +
				"The ring has a radius of %s blocks. Other players within this ring deal %s%% extra damage on all attacks. " +
				"The caster gets half the bonus of other players. " +
				"Mobs that die within this ring increase the damage bonus by %s%% per mob, up to %s%% extra damage. Cooldown: %ss.")
					.formatted(
							StringUtils.ticksToSeconds(TRANSMUTATION_RING_DURATION),
							StringUtils.to2DP(TRANSMUTATION_RING_RADIUS),
							StringUtils.multiplierToPercentage(DAMAGE_AMPLIFIER),
							StringUtils.multiplierToPercentage(DAMAGE_PER_DEATH_AMPLIFIER),
							StringUtils.multiplierToPercentage(MAX_KILLS * DAMAGE_PER_DEATH_AMPLIFIER),
							StringUtils.ticksToSeconds(TRANSMUTATION_RING_1_COOLDOWN)
					),
				("Mobs that die within this ring refunds %s Alchemist Potions and " +
				"increase its duration by %ss per mob, up to %s extra seconds. Cooldown: %ss.")
					.formatted(
							REFUND_POTION_AMOUNT,
							StringUtils.ticksToSeconds(DURATION_INCREASE),
							StringUtils.ticksToSeconds(MAX_DURATION_INCREASE),
							StringUtils.ticksToSeconds(TRANSMUTATION_RING_2_COOLDOWN)
					)
			)
			.simpleDescription("Deploy a circular zone that buffs the damage dealt by allies.")
			.cooldown(TRANSMUTATION_RING_1_COOLDOWN, TRANSMUTATION_RING_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GOLD_NUGGET);

	private final double mRadius;

	private @Nullable Location mCenter;
	private int mKills = 0;

	private final TransmutationRingCS mCosmetic;

	private @Nullable BukkitTask mActiveTask;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public TransmutationRing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TRANSMUTATION_RING_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TransmutationRingCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void alchemistPotionThrown(ThrownPotion potion) {
		if (!isOnCooldown() && mPlayer.isSneaking() && (mActiveTask == null || mActiveTask.isCancelled())) {
			putOnCooldown();
			potion.setMetadata(TRANSMUTATION_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
		}
	}

	private int mCurrDuration = -1;

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!potion.hasMetadata(TRANSMUTATION_POTION_METAKEY)) {
			return false;
		}

		mCenter = loc;
		mCenter.setDirection(mCenter.toVector().subtract(mPlayer.getLocation().toVector()));

		mCosmetic.startEffect(mPlayer, mCenter, mRadius);

		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TRANSMUTATION_RING_DURATION);
		double amplifier = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		double perKillAmplifier = DAMAGE_PER_DEATH_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PER_KILL_AMPLIFIER);
		int maxKills = MAX_KILLS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_KILLS);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		mActiveTask = new BukkitRunnable() {
			int mTicks = 0;
			int mMaxTicks = duration;

			@Override
			public void run() {
				if (isLevelTwo()) {
					mMaxTicks = duration + Math.min(mKills * DURATION_INCREASE, MAX_DURATION_INCREASE);
				}

				if (mTicks >= mMaxTicks || mCenter == null) {
					mCenter = null;
					mKills = 0;
					this.cancel();
					return;
				}

				double damageBoost = amplifier + Math.min(mKills, maxKills) * perKillAmplifier;
				List<Player> players = PlayerUtils.playersInRange(mCenter, mRadius, true);
				for (Player player : players) {
					if (player == mPlayer) {
						mPlugin.mEffectManager.addEffect(mPlayer, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME, new PercentDamageDealt(20, damageBoost / 2.0).displaysTime(false));
					} else {
						mPlugin.mEffectManager.addEffect(player, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME, new PercentDamageDealt(20, damageBoost).displaysTime(false));
					}
				}

				mCosmetic.periodicEffect(mPlayer, mCenter, mRadius, mTicks, mMaxTicks, duration + MAX_DURATION_INCREASE);

				mTicks += 5;
				if (mCurrDuration >= 0) {
					mCurrDuration += 5;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, TransmutationRing.this);
			}
		}.runTaskTimer(mPlugin, 0, 5);

		return true;
	}

	@Override
	public @Nullable Location entityDeathRadiusCenterLocation() {
		return mCenter;
	}

	@Override
	public double entityDeathRadius() {
		return mRadius;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills++;
		mCosmetic.effectOnKill(mPlayer, event.getEntity().getLocation());
		if (mAlchemistPotions != null && isLevelTwo()) {
			mAlchemistPotions.modifyCurrentPotionTimer(mAlchemistPotions.getChargeTime() * REFUND_POTION_AMOUNT);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return CharmManager.getDuration(mPlayer, CHARM_DURATION, TRANSMUTATION_RING_DURATION);
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
