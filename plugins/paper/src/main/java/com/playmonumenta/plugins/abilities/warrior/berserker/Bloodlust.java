package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Bloodlust extends Ability implements AbilityWithChargesOrStacks {

	private static final int MAX_BLOODLUST_PER_TICK = 3;
	private static final int R2_DAMAGE_REQ = 70;
	private static final int R3_DAMAGE_REQ = 120;
	private static final double AOE_PENALTY = 0.33;
	private static final int MAX_STACKS = 10;
	private static final int MAX_PASSIVE_GAIN = 2;
	private static final int OUT_OF_COMBAT_TIME = Constants.TICKS_PER_SECOND * 12;
	private static final int TIME_PER_RAMPAGE = Constants.TICKS_PER_SECOND * 5;

	public static final String CHARM_STACKS = "Bloodlust Max Stacks";
	public static final String CHARM_THRESHOLD = "Bloodlust Stack Threshold";

	private final int mStackLimit;
	private final double mDamageReq;

	private int mStacks;
	private int mCombatTime;
	private double mDamageCounter = 0;
	private final BukkitRunnable mBloodlustRunnable;
	private int mMaxBloodlust = 3;
	private @Nullable Rampage mRampage;
	private @Nullable GloriousBattle mGloriousBattle;


	public static final AbilityInfo<Bloodlust> INFO =
		new AbilityInfo<>(Bloodlust.class, "Bloodlust", Bloodlust::new)
			.hotbarName("Bl")
			.linkedSpell(ClassAbility.BLOODLUST)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Warrior.BERSERKER_SPEC_ID);

	public Bloodlust(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mDamageReq = CharmManager.calculateFlatAndPercentValue(player, CHARM_THRESHOLD, ServerProperties.getAbilityEnhancementsEnabled(mPlayer) ? R3_DAMAGE_REQ : R2_DAMAGE_REQ);
		mStackLimit = MAX_STACKS + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mRampage = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Rampage.class);
			mGloriousBattle = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, GloriousBattle.class);
		});

		mBloodlustRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}

				if (AbilityManager.getManager().getPlayerAbility(player, Bloodlust.class) == null
					|| player.isDead()
					|| !player.isOnline()) {
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				mMaxBloodlust = MAX_BLOODLUST_PER_TICK;
				if (mTicks % 20 == 0 && !EntityUtils.getNearbyMobs(mPlayer.getLocation(), 16).isEmpty() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.RESIST_5)) {
					mCombatTime = Bukkit.getServer().getCurrentTick();
				}
				if (mTicks % TIME_PER_RAMPAGE == 0) {
					if (Bukkit.getServer().getCurrentTick() - OUT_OF_COMBAT_TIME > mCombatTime && mStacks < MAX_PASSIVE_GAIN) {
						addStacks(1);
					}
					mTicks = 0;
				}
				mTicks++;
			}
		};
		cancelOnDeath(mBloodlustRunnable.runTaskTimer(plugin, 0, 1));

		mStacks = 0;
	}

	@Override
	public void invalidate() {
		if (mBloodlustRunnable != null) {
			mBloodlustRunnable.cancel();
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.RESIST_5)) {
			mCombatTime = Bukkit.getServer().getCurrentTick();
		}

		final DamageEvent.DamageType type = event.getType();
		final boolean isMelee = type == DamageEvent.DamageType.MELEE;
		final boolean isMeleeAbil = type == DamageEvent.DamageType.MELEE_SKILL || type == DamageEvent.DamageType.MELEE_ENCH;
		final boolean isRampage = event.getAbility() == ClassAbility.RAMPAGE;

		// Single target abilities
		final boolean isShieldBash = event.getAbility() == ClassAbility.SHIELD_BASH;
		final boolean isBruteForce = event.getAbility() == ClassAbility.BRUTE_FORCE;

		// Critical Strike with Glorious receives penalty
		final boolean isGloriousBattleCrit = mGloriousBattle != null && mGloriousBattle.isGloriousCritical() && isMelee && PlayerUtils.isFallingAttack(mPlayer);

		if ((isMelee || isMeleeAbil) && !isRampage) {
			double damage = event.getFinalDamage(false);
			if (!(isMelee || isShieldBash || isBruteForce) || isGloriousBattleCrit) {
				damage *= AOE_PENALTY;
			}
			damageDealt(damage);
		}
		return false;
	}

	/*
	Damage cannot exceed the req*limit on the single tick
	If the max BL been reached, do not count any more damage on that tick
	Subtract stack gained * req to not forget previous damage values
	 */
	private void damageDealt(double damage) {
		if (mMaxBloodlust <= 0) {
			return;
		}

		double cappedDamage = Math.min(mDamageReq * MAX_BLOODLUST_PER_TICK, damage);
		mDamageCounter += cappedDamage;

		if (mDamageCounter >= mDamageReq) {
			int stack = Math.min(MAX_BLOODLUST_PER_TICK, (int) (mDamageCounter / mDamageReq));
			mDamageCounter -= stack * mDamageReq;
			mMaxBloodlust -= stack;
			addStacks(stack);
		}
	}

	public int getStacks() {
		return mStacks;
	}

	public void addStacks(int stacks) {
		if (mStacks == mStackLimit) {
			return;
		}

		if (mRampage != null) {
			mRampage.triggerOnBloodlust(stacks);
		}

		mStacks = Math.min(mStackLimit, mStacks + stacks);

		ClientModHandler.updateAbility(mPlayer, this);
	}

	/**
	 * Consumes stacks, if there aren't enough stacks no stacks are consumed and the method returns false
	 *
	 * @param stacks amount of stacks
	 * @return returns true if there are enough stacks, otherwise false
	 */
	public boolean useStacks(int stacks) {
		if (mStacks >= stacks) {
			mStacks -= stacks;
			ClientModHandler.updateAbility(mPlayer, this);
			return true;
		}
		return false;
	}

	private static Description<Bloodlust> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain Bloodlust stacks upon dealing ")
			.add(R2_DAMAGE_REQ)
			.add("/")
			.add(a -> a.mDamageReq, R3_DAMAGE_REQ)
			.add(" melee damage, stacking up to ")
			.add(a -> a.mStackLimit, MAX_STACKS)
			.add(". ")
			.addPercent(AOE_PENALTY)
			.add(" of melee damage from AoE contribute towards Bloodlust. Passively gain ")
			.add(a -> MAX_PASSIVE_GAIN, MAX_PASSIVE_GAIN)
			.add(" Bloodlust when out of combat.");
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();
		final int charges = getCharges();
		final int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges,
			(charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}
}
