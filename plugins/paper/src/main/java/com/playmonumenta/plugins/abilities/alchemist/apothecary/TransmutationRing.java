package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
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
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deploy a circular zone that buffs the damage dealt by allies.")
			.cooldown(TRANSMUTATION_RING_1_COOLDOWN, TRANSMUTATION_RING_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GOLD_NUGGET);

	private final double mRadius;
	private final int mDuration;
	private final double mAmplifier;
	private final double mPerKillAmplifier;
	private final int mMaxKills;

	private @Nullable Location mCenter;
	private int mKills = 0;

	private final TransmutationRingCS mCosmetic;

	private @Nullable BukkitTask mActiveTask;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mCurrDuration = -1;

	public TransmutationRing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TRANSMUTATION_RING_RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TRANSMUTATION_RING_DURATION);
		mAmplifier = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		mPerKillAmplifier = DAMAGE_PER_DEATH_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PER_KILL_AMPLIFIER);
		mMaxKills = MAX_KILLS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_KILLS);

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

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!potion.hasMetadata(TRANSMUTATION_POTION_METAKEY)) {
			return false;
		}

		mCenter = loc;
		mCenter.setDirection(mCenter.toVector().subtract(mPlayer.getLocation().toVector()));

		mCosmetic.startEffect(mPlayer, mCenter, mRadius);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		mActiveTask = new BukkitRunnable() {
			int mTicks = 0;
			int mMaxTicks = mDuration;

			@Override
			public void run() {
				if (isLevelTwo()) {
					mMaxTicks = mDuration + Math.min(mKills * DURATION_INCREASE, MAX_DURATION_INCREASE);
				}

				if (mTicks >= mMaxTicks || mCenter == null) {
					mCenter = null;
					mKills = 0;
					this.cancel();
					return;
				}

				double damageBoost = mAmplifier + Math.min(mKills, mMaxKills) * mPerKillAmplifier;
				List<Player> players = PlayerUtils.playersInRange(mCenter, mRadius, true);
				for (Player player : players) {
					if (player == mPlayer) {
						mPlugin.mEffectManager.addEffect(mPlayer, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME,
							new PercentDamageDealt(20, damageBoost / 2.0).deleteOnAbilityUpdate(true)
								.displaysTime(false));
					} else {
						mPlugin.mEffectManager.addEffect(player, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME,
							new PercentDamageDealt(20, damageBoost).deleteOnAbilityUpdate(true)
								.displaysTime(false));
					}
				}

				mCosmetic.periodicEffect(mPlayer, mCenter, mRadius, mTicks, mMaxTicks, mDuration + MAX_DURATION_INCREASE);

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
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<TransmutationRing> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Throw an Alchemist's Potion while sneaking to create a Transmutation Ring at the potion's landing location that lasts for ")
			.addDuration(a -> a.mDuration, TRANSMUTATION_RING_DURATION)
			.add(" seconds. The ring has a radius of ")
			.add(a -> a.mRadius, TRANSMUTATION_RING_RADIUS)
			.add(" blocks. Other players within this ring deal ")
			.addPercent(a -> a.mAmplifier, DAMAGE_AMPLIFIER)
			.add(" extra damage on all attacks. The caster gets half the bonus of other players. Mobs that die within this ring increase the damage bonus by ")
			.addPercent(a -> a.mPerKillAmplifier, DAMAGE_PER_DEATH_AMPLIFIER)
			.add(" per mob, up to ")
			.add(a -> a.mMaxKills, MAX_KILLS)
			.add(" mobs.")
			.addCooldown(TRANSMUTATION_RING_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<TransmutationRing> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Mobs that die within this ring refund ")
			.add(a -> REFUND_POTION_AMOUNT, REFUND_POTION_AMOUNT)
			.add(" Alchemist Potions and increase its duration by ")
			.addDuration(DURATION_INCREASE)
			.add(" seconds per mob, up to ")
			.addDuration(MAX_DURATION_INCREASE)
			.add(" extra seconds.")
			.addCooldown(TRANSMUTATION_RING_2_COOLDOWN, Ability::isLevelTwo);
	}
}
