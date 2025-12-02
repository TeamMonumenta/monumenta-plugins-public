package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.IronTinctureCS;
import com.playmonumenta.plugins.effects.IronTinctureEnhancementAbsorptionInfused;
import com.playmonumenta.plugins.effects.PercentDamageReceivedSingle;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class IronTincture extends MultipleChargeAbility {

	private static final int DECAY_TIME = 20 * 10;
	private static final int ABSORPTION_1 = 4;
	private static final int ABSORPTION_2 = 6;
	private static final int COOLDOWN_1 = 20 * 40;
	private static final int COOLDOWN_2 = 20 * 35;
	private static final double FIRST_HIT_RESISTANCE_2 = 0.1;
	private static final String FIRST_HIT_RESISTANCE_2_EFFECT_STRING = "IronTinctureLevelTwoFirstHitResistanceEffect";
	private static final int CHARGES = 2;
	private static final int ABSORPTION_DURATION = 20 * 40;
	private static final int TICK_PERIOD = 2;
	private static final int POTION_REFILL = 1;
	private static final int ALLY_POTION_REFILL = 2;
	private static final double VELOCITY = 0.7;

	private static final int ENHANCEMENT_EFFECT_RADIUS = 3;
	private static final int ENHANCEMENT_STUN_DURATION = 10;
	private static final int ENHANCEMENT_ABSORPTION_ON_KILL_TIMEOUT = 20 * 5;
	private static final int ENHANCEMENT_ABSORPTION_ON_KILL_DURATION = 20 * 20;
	private static final double ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT = 1;
	private static final double ENHANCEMENT_ABSORPTION_ON_KILL_MAX_1 = 4;
	private static final double ENHANCEMENT_ABSORPTION_ON_KILL_MAX_2 = 6;
	private static final String ENHANCEMENT_ABSORPTION_ON_KILL_EFFECT_STRING = "IronTinctureEnhancementAbsorptionOnKillEffect";
	private static final int CAST_DELAY = 5;

	public static final String CHARM_COOLDOWN = "Iron Tincture Cooldown";
	public static final String CHARM_ABSORPTION = "Iron Tincture Absorption Health";
	public static final String CHARM_DURATION = "Iron Tincture Duration";
	public static final String CHARM_VELOCITY = "Iron Tincture Velocity";
	public static final String CHARM_RESISTANCE = "Iron Tincture Resistance Amplifier";
	public static final String CHARM_REFILL = "Iron Tincture Potion Refill";
	public static final String CHARM_ALLY_REFILL = "Iron Tincture Ally Potion Refill";
	public static final String CHARM_STUN_DURATION = "Iron Tincture Stun Duration";
	public static final String CHARM_CHARGES = "Iron Tincture Charges";
	public static final String CHARM_ENHANCEMENT_EFFECT_RADIUS = "Iron Tincture Enhancement Effect Radius";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT = "Iron Tincture Enhancement Absorption on Kill";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_MAX = "Iron Tincture Enhancement Absorption on Kill Max";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_EFFECT_DURATION = "Iron Tincture Enhancement Absorption Effect Duration";
	public static final String CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_DURATION = "Iron Tincture Enhancement Absorption on Kill Duration";

	public static final AbilityInfo<IronTincture> INFO =
		new AbilityInfo<>(IronTincture.class, "Iron Tincture", IronTincture::new)
			.linkedSpell(ClassAbility.IRON_TINCTURE)
			.scoreboardId("IronTincture")
			.shorthandName("IT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a potion on the ground that you or other players can collect to gain absorption hearts.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IronTincture::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.SPLASH_POTION);

	private final double mAbsorption;
	private final int mRefill;
	private final int mAllyRefill;
	private final int mDuration;
	private final int mEnhancementAbsorptionDuration;
	private final int mEnhancementEffectDuration;
	private final double mResistance;
	private final double mEnhancementEffectRadius;
	private final int mStunDuration;
	private final double mEnhancementAbsorptionOnKillAmount;
	private final double mEnhancementAbsorptionMaxLevelOne;
	private final double mEnhancementAbsorptionMaxLevelTwo;
	private final IronTinctureCS mCosmetic;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int lastCastTime = 0;

	public IronTincture(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_1 : ABSORPTION_2);
		mRefill = POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_REFILL);
		mAllyRefill = ALLY_POTION_REFILL + (int) CharmManager.getLevel(mPlayer, CHARM_ALLY_REFILL);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, ABSORPTION_DURATION);
		mEnhancementAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_DURATION, ENHANCEMENT_ABSORPTION_ON_KILL_DURATION);
		mEnhancementEffectDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_EFFECT_DURATION, ENHANCEMENT_ABSORPTION_ON_KILL_TIMEOUT);
		mResistance = FIRST_HIT_RESISTANCE_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, ENHANCEMENT_STUN_DURATION);
		mEnhancementEffectRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCEMENT_EFFECT_RADIUS, ENHANCEMENT_EFFECT_RADIUS);
		mEnhancementAbsorptionOnKillAmount = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT, ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT);
		mEnhancementAbsorptionMaxLevelOne = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_MAX, ENHANCEMENT_ABSORPTION_ON_KILL_MAX_1);
		mEnhancementAbsorptionMaxLevelTwo = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_MAX, ENHANCEMENT_ABSORPTION_ON_KILL_MAX_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new IronTinctureCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public boolean cast() {
		if (mPlayer.getTicksLived() <= lastCastTime + CAST_DELAY) {
			return false;
		}

		if (!consumeCharge()) {
			return false;
		}

		lastCastTime = mPlayer.getTicksLived();
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		double velocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, VELOCITY);
		Item tincture = AbilityUtils.spawnAbilityItem(world, loc, Material.SPLASH_POTION, mCosmetic.tinctureName(), false, velocity, true, true);
		mCosmetic.onThrow(world, loc);

		// Full duration cooldown - is shortened if not picked up
		putOnCooldown();

		new BukkitRunnable() {
			int mTinctureDecay = 0;
			boolean mDidStunEffects = false;

			private void doEnhancementEffects(Location centerLoc) {
				Hitbox hitbox = new Hitbox.SphereHitbox(centerLoc, mEnhancementEffectRadius);
				hitbox.getHitMobs().forEach(mob -> {
					if (EntityUtils.isBoss(mob)) {
						return;
					}
					if (!EntityUtils.isElite(mob)) {
						EntityUtils.applyStun(mPlugin, mStunDuration, mob);
					}
					mPlugin.mEffectManager.addEffect(
						mob,
						ENHANCEMENT_ABSORPTION_ON_KILL_EFFECT_STRING,
						new IronTinctureEnhancementAbsorptionInfused(
							mEnhancementEffectDuration,
							mEnhancementAbsorptionOnKillAmount,
							isLevelOne() ? mEnhancementAbsorptionMaxLevelOne : mEnhancementAbsorptionMaxLevelTwo,
							mEnhancementAbsorptionDuration,
							mPlayer.getUniqueId()
						)
					);
				});
				mCosmetic.hitGroundEffect(centerLoc, mPlayer, mEnhancementEffectRadius);
				mDidStunEffects = true;
			}

			@Override
			public void run() {
				Location loc = tincture.getLocation();
				if (isEnhanced() && !mDidStunEffects && tincture.isOnGround()) {
					doEnhancementEffects(loc);
				}
				mCosmetic.onGroundEffect(loc, mPlayer, mTinctureDecay / TICK_PERIOD);

				for (Player player : new Hitbox.UprightCylinderHitbox(loc, 0.7, 0.7).getHitPlayers(true)) {
					// Prevent players from picking up their own tincture instantly
					if (player == mPlayer && tincture.getTicksLived() < 12) {
						continue;
					}

					mCosmetic.pickupEffects(loc.getWorld(), loc, player);

					tincture.remove();

					execute(mPlayer, loc);
					if (isEnhanced() && !mDidStunEffects) {
						doEnhancementEffects(player.getLocation());
					}
					if (player != mPlayer) {
						execute(player, loc);
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharges(mAllyRefill);
						}
					} else {
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharges(mRefill);
						}
					}

					this.cancel();
					return;
				}

				mTinctureDecay += TICK_PERIOD;
				if (mTinctureDecay >= DECAY_TIME || !tincture.isValid() || tincture.isDead()) {
					mCosmetic.tinctureExpireEffects(loc, mPlayer);
					tincture.remove();
					this.cancel();

					// Refund one tincture if charges are below max, and skill is not enhanced (to prevent abusing the stun)
					if (!isEnhanced()) {
						incrementCharge();
						if (mCharges == mMaxCharges) {
							mPlugin.mTimers.setCooldown(mPlayer, ClassAbility.IRON_TINCTURE, 0);
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, TICK_PERIOD);

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mWasOnCooldown && !isOnCooldown()) {
			mCharges = mMaxCharges;
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.IRON_TINCTURE, mCharges);
			showOffCooldownMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = isOnCooldown();

		if (!isOnCooldown() && mCharges != mMaxCharges) {
			putOnCooldown();
		}
	}

	private void execute(Player player, Location tinctureLocation) {
		AbsorptionUtils.addAbsorption(player, mPlayer, mAbsorption, mAbsorption * 2, mDuration);

		if (isLevelTwo()) {
			mPlugin.mEffectManager.clearEffects(player, FIRST_HIT_RESISTANCE_2_EFFECT_STRING);
			mPlugin.mEffectManager.addEffect(player, FIRST_HIT_RESISTANCE_2_EFFECT_STRING, new PercentDamageReceivedSingle(mDuration, -mResistance));
		}

		mCosmetic.pickupEffectsForPlayer(player, tinctureLocation);
	}

	private static Description<IronTincture> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to throw a tincture. If you walk over the tincture, gain ")
			.add(a -> a.mAbsorption, ABSORPTION_1, false, Ability::isLevelOne)
			.add(" stacking absorption health for ")
			.addDuration(a -> a.mDuration, ABSORPTION_DURATION)
			.add(" seconds. If an ally walks over it, you both gain the effect. If it isn't grabbed before it disappears, it will refund you a charge. When another player grabs the tincture, you gain 2 Alchemist's Potions. When you grab the tincture, you gain 1 Alchemist's Potion. ")
			.add("Charges: ")
			.add(a -> a.mMaxCharges, CHARGES)
			.add(" (all charges are refunded when the skill comes off cooldown).")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<IronTincture> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The absorption is increased to ")
			.add(a -> a.mAbsorption, ABSORPTION_2, false, Ability::isLevelTwo)
			.add(". The tincture now also grants ")
			.addPercent(a -> a.mResistance, FIRST_HIT_RESISTANCE_2)
			.add(" resistance to the first hit taken.")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<IronTincture> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The tincture now stuns non-elite, non-boss mobs within ")
			.add(a -> a.mEnhancementEffectRadius, ENHANCEMENT_EFFECT_RADIUS)
			.add(" blocks of its landing location, or the location of the player that picks it up before it lands, for ")
			.addDuration(a -> a.mStunDuration, ENHANCEMENT_STUN_DURATION)
			.add("s. If a player kills the affected mobs within ")
			.addDuration(a -> a.mEnhancementEffectDuration, ENHANCEMENT_ABSORPTION_ON_KILL_TIMEOUT)
			.add("s, they gain ")
			.add(a -> a.mEnhancementAbsorptionOnKillAmount, ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT)
			.add(" absorption health for each kill, up to a maximum of ")
			.add(a -> a.mEnhancementAbsorptionMaxLevelOne, ENHANCEMENT_ABSORPTION_ON_KILL_MAX_1)
			.add(" for level 1, and ")
			.add(a -> a.mEnhancementAbsorptionMaxLevelTwo, ENHANCEMENT_ABSORPTION_ON_KILL_MAX_2)
			.add(" for level 2, which lasts for ")
			.addDuration(a -> a.mEnhancementAbsorptionDuration, ENHANCEMENT_ABSORPTION_ON_KILL_DURATION)
			.add("s. However, if the tincture disappears, it will no longer refund you a charge.");
	}
}
