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
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Taboo extends Ability {
	private static final int CHARGE_TIME_REDUCTION = 10;
	private static final double PERCENT_HEALTH_DAMAGE = 0.05;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.5;
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "TabooKnockbackResistanceEffect";
	private static final double MAGIC_DAMAGE_INCREASE_1 = 0.15;
	private static final double MAGIC_DAMAGE_INCREASE_2 = 0.25;
	private static final double PERCENT_HEALTH_HEALING = 0.2;
	private static final int COOLDOWN = 5 * 20;

	public static final String CHARM_COOLDOWN = "Taboo Cooldown";
	public static final String CHARM_SELF_DAMAGE = "Taboo Self Damage";
	public static final String CHARM_KNOCKBACK_RESISTANCE = "Taboo Knockback Resistance";
	public static final String CHARM_DAMAGE = "Taboo Damage Modifier";
	public static final String CHARM_HEALING = "Taboo Healing";
	public static final String CHARM_RECHARGE = "Taboo Potion Recharge Rate";

	public static final AbilityInfo<Taboo> INFO =
		new AbilityInfo<>(Taboo.class, "Taboo", Taboo::new)
			.linkedSpell(ClassAbility.TABOO)
			.scoreboardId("Taboo")
			.shorthandName("Tb")
			.descriptions(
				("Swap hands while sneaking and holding an Alchemist's Bag to drink a potion. " +
				"Drinking the potion causes you to recharge potions %ss faster, deal +%s%% magic damage, and gain %s%% knockback resistance. " +
				"However, you lose %s%% of your health per second, which bypasses resistances and absorption, but cannot kill you. " +
				"Swapping hands while sneaking and holding an Alchemist's Bag disables the effect. ")
					.formatted(
							StringUtils.ticksToSeconds(CHARGE_TIME_REDUCTION),
							StringUtils.multiplierToPercentage(MAGIC_DAMAGE_INCREASE_1),
							StringUtils.multiplierToPercentage(PERCENT_KNOCKBACK_RESIST),
							StringUtils.multiplierToPercentage(PERCENT_HEALTH_DAMAGE)
					),
				("Extra magic damage increased to %s%%. Additionally, while the effect is active, swap hands while " +
				"looking down, sneaking, and holding an Alchemist's Bag to consume 2 potions and heal %s%% of your health. " +
				"This healing has a %ss cooldown.")
					.formatted(
							StringUtils.multiplierToPercentage(MAGIC_DAMAGE_INCREASE_2),
							StringUtils.multiplierToPercentage(PERCENT_HEALTH_HEALING),
							StringUtils.ticksToSeconds(COOLDOWN)
					)
			)
			.simpleDescription("Periodically lose health in exchange for increased magic damage and potion recharge rate.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("heal", "heal", Taboo::heal, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				                                                                  .lookDirections(AbilityTrigger.LookDirection.DOWN),
				new AbilityTriggerInfo.TriggerRestriction("holding an Alchemist's Bag and Taboo is active",
					player -> {
						if (!ItemUtils.isAlchemistItem(player.getInventory().getItemInMainHand())) {
							return false;
						}
						return isTabooUpgradedAndActive(player);
					})))
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", Taboo::toggle, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.HONEY_BOTTLE);

	private final double mMagicDamageIncrease;
	private final int mRechargeRateDecrease;

	private @Nullable AlchemistPotions mAlchemistPotions;

	private boolean mActive;
	private final TabooCS mCosmetic;

	public Taboo(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mActive = false;
		mMagicDamageIncrease = (isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mRechargeRateDecrease = CharmManager.getDuration(mPlayer, CHARM_RECHARGE, CHARGE_TIME_REDUCTION);

		mActive = false;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TabooCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void toggle() {
		if (mAlchemistPotions != null) {
			if (mActive) {
				mAlchemistPotions.increaseChargeTime(mRechargeRateDecrease);
				mActive = false;
				ClientModHandler.updateAbility(mPlayer, this);
			} else if (mAlchemistPotions.decrementCharge()) {
				mAlchemistPotions.reduceChargeTime(mRechargeRateDecrease);
				mActive = true;
				ClientModHandler.updateAbility(mPlayer, this);
			}
			mCosmetic.toggle(mPlayer, mActive);
		}
	}

	public void heal() {
		if (!isOnCooldown()
			    && mAlchemistPotions != null
			    && mAlchemistPotions.decrementCharges(2)) {
			putOnCooldown();
			PlayerUtils.healPlayer(mPlugin, mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, EntityUtils.getMaxHealth(mPlayer) * PERCENT_HEALTH_HEALING), mPlayer);
			mCosmetic.healEffects(mPlayer);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mActive) {
			if (oneSecond) {
				double maxHealth = EntityUtils.getMaxHealth(mPlayer);
				double selfDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF_DAMAGE, maxHealth * PERCENT_HEALTH_DAMAGE);
				if (mPlayer.getHealth() > selfDamage) {
					mPlayer.setHealth(Math.min(mPlayer.getHealth(), maxHealth) - selfDamage); // Health is sometimes lower than max for whatever reason, raising an exception
					mPlayer.damage(0);
				}
			}
			mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(6, PERCENT_KNOCKBACK_RESIST + CharmManager.getLevel(mPlayer, CHARM_KNOCKBACK_RESISTANCE) / 10, KNOCKBACK_RESIST_EFFECT_NAME).displaysTime(false));

			mCosmetic.periodicEffects(mPlayer, twoHertz, oneSecond, ticks);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity damgee) {
		if (mActive && event.getType() == DamageType.MAGIC) {
			event.setDamage(event.getDamage() * (1 + mMagicDamageIncrease));
		}
		return false;
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}

	private static boolean isTabooUpgradedAndActive(Player player) {
		Taboo taboo = Plugin.getInstance().mAbilityManager.getPlayerAbilities(player).getAbilityIgnoringSilence(Taboo.class);
		return taboo != null && taboo.isLevelTwo() && taboo.mActive;
	}
}
