package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.DeadlyRondeCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class DeadlyRonde extends Ability implements AbilityWithChargesOrStacks {

	private static final int RONDE_1_DAMAGE = 4;
	private static final int RONDE_2_DAMAGE = 6;
	private static final int RONDE_1_MAX_STACKS = 2;
	private static final int RONDE_2_MAX_STACKS = 3;
	private static final double RONDE_SPEED_BONUS = 0.2;
	private static final int RONDE_DECAY_TIMER = 4 * 20;
	private static final double RONDE_RADIUS = 4.5;
	private static final double RONDE_ANGLE = 35;
	private static final float RONDE_KNOCKBACK_SPEED = 0.14f;
	private static final double RONDE_ATTACK_SPEED_SCALING_PORTION = 0.35;
	private static final int RONDE_STACKS_REQ = 1;

	public static final String CHARM_DAMAGE = "Deadly Ronde Damage";
	public static final String CHARM_RADIUS = "Deadly Ronde Radius";
	public static final String CHARM_ANGLE = "Deadly Ronde Angle";
	public static final String CHARM_KNOCKBACK = "Deadly Ronde Knockback";
	public static final String CHARM_STACKS = "Deadly Ronde Max Stacks";
	public static final String CHARM_STACK_GAIN = "Deadly Ronde Stacks Per Ability";
	public static final String CHARM_SPEED = "Deadly Ronde Speed Amplifier";
	public static final String CHARM_DECAY_TIME = "Deadly Ronde Stack Decay Time";
	public static final String CHARM_STACKS_REQ = "Deadly Ronde Stack Requirement";
	public static final String CHARM_ATTACK_SPEED_SCALING_PORTION = "Deadly Ronde Attack Speed Scaling";

	public static final AbilityInfo<DeadlyRonde> INFO =
		new AbilityInfo<>(DeadlyRonde.class, "Deadly Ronde", DeadlyRonde::new)
			.linkedSpell(ClassAbility.DEADLY_RONDE)
			.scoreboardId("DeadlyRonde")
			.shorthandName("DR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Damage nearby mobs when striking after casting an ability.")
			.displayItem(Material.BLAZE_ROD);

	private int mRondeStacks = 0;
	private int mTimeUntilDecay = -1;

	private final double mRadius;
	private final double mDamage;
	private final float mKnockback;
	private final int mMaxStacks;
	private final int mStackGain;
	private final double mSpeed;
	private final int mDecayTime;
	private final int mStacksReq;
	private final double mAttackSpeedScalingPortion;
	private final DeadlyRondeCS mCosmetic;

	public DeadlyRonde(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RONDE_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? RONDE_1_DAMAGE : RONDE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, RONDE_KNOCKBACK_SPEED);
		mMaxStacks = (isLevelOne() ? RONDE_1_MAX_STACKS : RONDE_2_MAX_STACKS) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mSpeed = RONDE_SPEED_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDecayTime = CharmManager.getDuration(mPlayer, CHARM_DECAY_TIME, RONDE_DECAY_TIMER);
		mStacksReq = RONDE_STACKS_REQ + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS_REQ);
		mStackGain = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_STACK_GAIN);
		mAttackSpeedScalingPortion = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ATTACK_SPEED_SCALING_PORTION, RONDE_ATTACK_SPEED_SCALING_PORTION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DeadlyRondeCS());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mRondeStacks > 0) {
			mTimeUntilDecay -= 5;
			if (mTimeUntilDecay <= 0) {
				mRondeStacks--;
				mTimeUntilDecay = mDecayTime;

				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		/* Re-up the duration every time an ability is cast */
		double speed = RONDE_SPEED_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		if (mTimeUntilDecay != -1) {
			mTimeUntilDecay = mDecayTime;
		}
		cancelOnDeath(new BukkitRunnable() {
			private final int mTimeout = mDecayTime * mMaxStacks;
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mCosmetic.rondeTickEffect(mPlayer, getCharges(), mTicks);
				mPlugin.mEffectManager.addEffect(mPlayer, "DeadlyRonde",
					new PercentSpeed(6, speed, "DeadlyRondeMod").deleteOnAbilityUpdate(true));
				if (mRondeStacks <= 0) {
					this.cancel();
				}

				//Logged off or something probably
				if (mTicks > mTimeout) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		mTimeUntilDecay = mDecayTime;

		if (mRondeStacks < mMaxStacks) {
			mCosmetic.rondeGainStackEffect(mPlayer, mPlayer.getLocation());
			mRondeStacks += mStackGain;
			if (mRondeStacks > mMaxStacks) {
				mRondeStacks = mMaxStacks;
			}

			ClientModHandler.updateAbility(mPlayer, this);
		}

		showChargesMessage();

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			&& InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)
			&& mRondeStacks > 0 && mRondeStacks >= mStacksReq) {
			double cooldownRatio = mPlayer.getCooledAttackStrength(0);
			double damageRatio = (1 - mAttackSpeedScalingPortion) + (mAttackSpeedScalingPortion * cooldownRatio);
			double damage = mDamage * damageRatio;

			double angle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, RONDE_ANGLE);
			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RONDE_RADIUS);
			Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), radius, Math.toRadians(angle));

			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
			}

			World world = mPlayer.getWorld();
			mCosmetic.rondeHitEffect(world, mPlayer, enemy, mRadius, RONDE_RADIUS, isLevelTwo());

			mTimeUntilDecay = mDecayTime;

			mRondeStacks -= mStacksReq;
			showChargesMessage();
			ClientModHandler.updateAbility(mPlayer, this);
			return true; // only trigger once per attack
		}
		return false;
	}

	@Override
	public int getCharges() {
		return mRondeStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	private static Description<DeadlyRonde> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After casting an ability, gain a stack of Deadly Ronde for ")
			.addDuration(a -> a.mDecayTime, RONDE_DECAY_TIMER)
			.add(" seconds, stacking up to ")
			.add(a -> a.mMaxStacks, RONDE_1_MAX_STACKS, false, Ability::isLevelOne)
			.add(" times. While Deadly Ronde is active, you gain ")
			.addPercent(a -> a.mSpeed, RONDE_SPEED_BONUS)
			.add(" speed, and your next melee attack consumes a stack to fire a flurry of blades in a thin cone with a radius of ")
			.add(a -> a.mRadius, RONDE_RADIUS)
			.add(" blocks, dealing ")
			.add(a -> a.mDamage, RONDE_1_DAMAGE, false, Ability::isLevelOne)
			.add(" melee damage to mobs they hit. Ronde damage is reduced by ")
			.addPercent(a -> a.mAttackSpeedScalingPortion, RONDE_ATTACK_SPEED_SCALING_PORTION)
			.add("%, proportional to how far from fully charged your melee attack is.");
	}

	private static Description<DeadlyRonde> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamage, RONDE_2_DAMAGE, false, Ability::isLevelTwo)
			.add(", and you can now have up to ")
			.add(a -> a.mMaxStacks, RONDE_2_MAX_STACKS, false, Ability::isLevelTwo)
			.add(" stacks.");
	}
}
