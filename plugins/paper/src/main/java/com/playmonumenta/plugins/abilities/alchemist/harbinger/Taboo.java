package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.TabooCS;
import com.playmonumenta.plugins.effects.PercentAbsorption;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Taboo extends Ability {
	private static final int CHARGE_TIME_REDUCTION = 10;
	private static final double PERCENT_HEALTH_DAMAGE = 0.07;
	private static final double PERCENT_HEALING_PENALTY = 0.5;
	private static final double PERCENT_ABSORPTION_PENALTY = 0.5;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.5;
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "TabooKnockbackResistanceEffect";
	private static final double MAGIC_DAMAGE_INCREASE_1 = 0.15;
	private static final double MAGIC_DAMAGE_INCREASE_2 = 0.25;
	private static final double MAGIC_DAMAGE_INCREASE_BURST = 0.4;
	private static final int BURST_SECONDS = 5;
	private static final int BURST_COOLDOWN = 20 * 20;
	private static final String TABOO_HEALING_SICKNESS = "TabooHealingSickness";
	private static final String TABOO_ABSORPTION_SICKNESS = "TabooAbsorptionSickness";

	public static final String CHARM_COOLDOWN = "Taboo Cooldown";
	public static final String CHARM_SELF_DAMAGE = "Taboo Self Damage";
	public static final String CHARM_KNOCKBACK_RESISTANCE = "Taboo Knockback Resistance";
	public static final String CHARM_DAMAGE = "Taboo Damage Modifier";
	public static final String CHARM_HEALING_PENALTY = "Taboo Healing Penalty";
	public static final String CHARM_ABSORPTION_PENALTY = "Taboo Absorption Penalty";
	public static final String CHARM_RECHARGE = "Taboo Potion Recharge Rate";

	private enum TabooState {
		INACTIVE(null),
		ACTIVE("active"),
		BURST("burst"),
		;

		private final @Nullable String mCooldownModString;

		TabooState(@Nullable String cooldownModString) {
			mCooldownModString = cooldownModString;
		}

		public @Nullable String getCooldownModString() {
			return mCooldownModString;
		}
	}

	private TabooState mCurrentState;

	public static final AbilityInfo<Taboo> INFO =
		new AbilityInfo<>(Taboo.class, "Taboo", Taboo::new)
			.linkedSpell(ClassAbility.TABOO)
			.scoreboardId("Taboo")
			.shorthandName("Tb")
			.descriptions(
				("Swap hands while sneaking and holding an Alchemist's Bag to consume a potion and undergo a taboo transformation. " +
				"While transformed, you recharge potions %ss faster, deal +%s%% magic damage, and gain %s%% knockback resistance. " +
				"However, you also lose %s%% of your current absorption, and receive %s%% less absorption and %s%% less healing from every source.")
					.formatted(
						StringUtils.ticksToSeconds(CHARGE_TIME_REDUCTION),
						StringUtils.multiplierToPercentage(MAGIC_DAMAGE_INCREASE_1),
						StringUtils.multiplierToPercentage(PERCENT_KNOCKBACK_RESIST),
						StringUtils.multiplierToPercentage(PERCENT_ABSORPTION_PENALTY),
						StringUtils.multiplierToPercentage(PERCENT_ABSORPTION_PENALTY),
						StringUtils.multiplierToPercentage(PERCENT_HEALING_PENALTY)
					),
				("The effect now grants you +%s%% magic damage. Additionally, while it is active, activate it again while " +
				"looking down to consume another potion and enter burst mode, which gives you an additional +%s%% magic damage, " +
				"but makes you lose %s%% of your health per second, which bypasses resistances and absorption, but cannot kill you. " +
				"This empowered effect lasts for %ss, and you cannot deactivate Taboo during it. Burst Cooldown: %ss")
					.formatted(
						StringUtils.multiplierToPercentage(MAGIC_DAMAGE_INCREASE_2),
						StringUtils.multiplierToPercentage(MAGIC_DAMAGE_INCREASE_BURST),
						StringUtils.multiplierToPercentage(PERCENT_HEALTH_DAMAGE),
						BURST_SECONDS,
						StringUtils.ticksToSeconds(BURST_COOLDOWN)
					)
			)
			.simpleDescription("Receive 50% less absorption and healing from every source, in exchange for increased magic damage and potion recharge rate.")
			.cooldown(BURST_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("burst", "burst", Taboo::burst, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN),
				new AbilityTriggerInfo.TriggerRestriction("holding an Alchemist's Bag and Taboo is active",
					player -> {
						if (!ItemUtils.isAlchemistItem(player.getInventory().getItemInMainHand())) {
							return false;
						}
						return isTabooUpgradedAndNonBurstActive(player);
					})))
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", Taboo::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.HONEY_BOTTLE);

	private final double mMagicDamageIncrease;
	private final int mRechargeRateDecrease;

	private @Nullable AlchemistPotions mAlchemistPotions;

	private int mBurstTimer = 0;
	private final TabooCS mCosmetic;

	public Taboo(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMagicDamageIncrease = (isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mRechargeRateDecrease = CharmManager.getDuration(mPlayer, CHARM_RECHARGE, CHARGE_TIME_REDUCTION);

		mCurrentState = TabooState.INACTIVE;
		mBurstTimer = 0;
		clearSicknessEffects();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TabooCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void toggle() {
		// Don't allow toggling during burst.
		if (mCurrentState == TabooState.BURST) {
			return;
		}

		if (mCurrentState == TabooState.INACTIVE) {
			activate();
		} else {
			deactivate();
		}
	}

	public void activate() {
		if (mAlchemistPotions == null || mCurrentState != TabooState.INACTIVE || !mAlchemistPotions.decrementCharges(1)) {
			return;
		}

		mCurrentState = TabooState.ACTIVE;
		mAlchemistPotions.reduceChargeTime(mRechargeRateDecrease);
		clearSicknessEffects();
		applySicknessEffects();
		double absorptionLostMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PENALTY, PERCENT_ABSORPTION_PENALTY);
		AbsorptionUtils.subtractAbsorption(mPlayer, AbsorptionUtils.getAbsorption(mPlayer) * absorptionLostMultiplier);
		mCosmetic.toggle(mPlayer, true);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public void deactivate() {
		if (mAlchemistPotions == null) {
			return;
		}

		mBurstTimer = 0;
		mCurrentState = TabooState.INACTIVE;
		mAlchemistPotions.increaseChargeTime(mRechargeRateDecrease);
		mCosmetic.toggle(mPlayer, false);
		clearSicknessEffects();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public void burst() {
		if (!isOnCooldown() && mCurrentState == TabooState.ACTIVE && mAlchemistPotions != null && mAlchemistPotions.decrementCharges(1)) {
			mBurstTimer = BURST_SECONDS;
			mCurrentState = TabooState.BURST;
			mCosmetic.burstEffects(mPlayer);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	public void unburst() {
		mBurstTimer = 0;
		mCurrentState = TabooState.ACTIVE;
		putOnCooldown(getModifiedCooldown(BURST_COOLDOWN));
		mCosmetic.unburstEffects(mPlayer);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mCurrentState != TabooState.INACTIVE) {
			if (mCurrentState == TabooState.BURST) {
				double maxHealth = EntityUtils.getMaxHealth(mPlayer);
				double selfDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF_DAMAGE, maxHealth * PERCENT_HEALTH_DAMAGE);
				if (mPlayer.getHealth() > selfDamage) {
					mPlayer.setHealth(Math.min(mPlayer.getHealth(), maxHealth) - selfDamage); // Health is sometimes lower than max for whatever reason, raising an exception
					mPlayer.damage(0);
				}
				mBurstTimer--;
				sendActionBarMessage("Taboo Burst: %s".formatted(mBurstTimer));
				if (mBurstTimer <= 0) {
					unburst();
				}
			}

			mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, PERCENT_KNOCKBACK_RESIST + CharmManager.getLevel(mPlayer, CHARM_KNOCKBACK_RESISTANCE) / 10, KNOCKBACK_RESIST_EFFECT_NAME).displaysTime(false));
			mCosmetic.periodicEffects(mPlayer, twoHertz, true, ticks, mCurrentState == TabooState.BURST);
			applySicknessEffects();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity damgee) {
		if (mCurrentState != TabooState.INACTIVE && event.getType() == DamageType.MAGIC) {
			double actualIncrease = mMagicDamageIncrease + ((mCurrentState == TabooState.BURST) ? MAGIC_DAMAGE_INCREASE_BURST : 0);
			event.setDamage(event.getDamage() * (1 + actualIncrease));
		}
		return false;
	}

	@Override
	public @Nullable String getMode() {
		return mCurrentState.getCooldownModString();
	}

	private static boolean isTabooUpgradedAndNonBurstActive(Player player) {
		Taboo taboo = Plugin.getInstance().mAbilityManager.getPlayerAbilities(player).getAbilityIgnoringSilence(Taboo.class);
		return taboo != null && taboo.isLevelTwo() && (taboo.mCurrentState == TabooState.ACTIVE);
	}

	private void applySicknessEffects() {
		double healing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING_PENALTY, PERCENT_HEALING_PENALTY);
		double absorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PENALTY, PERCENT_ABSORPTION_PENALTY);
		mPlugin.mEffectManager.addEffect(mPlayer, TABOO_HEALING_SICKNESS, new PercentHeal(20, -healing).displaysTime(false));
		mPlugin.mEffectManager.addEffect(mPlayer, TABOO_ABSORPTION_SICKNESS, new PercentAbsorption(20, -absorption).displaysTime(false));
	}

	private void clearSicknessEffects() {
		mPlugin.mEffectManager.clearEffects(mPlayer, TABOO_HEALING_SICKNESS);
		mPlugin.mEffectManager.clearEffects(mPlayer, TABOO_ABSORPTION_SICKNESS);
	}
}
