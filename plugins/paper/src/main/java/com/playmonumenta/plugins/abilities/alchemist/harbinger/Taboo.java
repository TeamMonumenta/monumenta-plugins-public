package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
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

	private final double mMagicDamageIncrease;
	private final int mRechargeRateDecrease;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable AlchemicalArtillery mAlchemicalArtillery;

	private boolean mActive;

	public Taboo(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Taboo");
		mInfo.mLinkedSpell = ClassAbility.TABOO;
		mInfo.mScoreboardId = "Taboo";
		mInfo.mShorthandName = "Tb";
		mInfo.mDescriptions.add("Swap hands while sneaking and holding an Alchemist's Bag to drink a potion. Drinking the potion causes you to recharge potions 0.5s faster and deal +15% magic damage. However, you lose 5% of your health per second, which bypasses resistances and absorption, but cannot kill you. Swapping hands while sneaking and holding an Alchemist's Bag disables the effect. Taboo can also be toggled by sneaking and swapping hands while holding a bow, crossbow, or trident while Alchemical Artillery is active.");
		mInfo.mDescriptions.add("Extra magic damage increased to 25%. Additionally, while the effect is active, swap hands while looking down, sneaking, and holding an Alchemist's Bag to consume 2 potions and heal 20% of your health. This healing has a 5s cooldown.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.HONEY_BOTTLE, 1);

		mMagicDamageIncrease = (isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mRechargeRateDecrease = CHARGE_TIME_REDUCTION + CharmManager.getExtraDuration(mPlayer, CHARM_RECHARGE);

		mActive = false;

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mAlchemicalArtillery = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemicalArtillery.class);
		});
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer != null && mPlayer.isSneaking() && mAlchemistPotions != null && (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) || (mAlchemicalArtillery != null && mAlchemicalArtillery.isActive() && ItemUtils.isProjectileWeapon(mPlayer.getInventory().getItemInMainHand())))) {
			World world = mPlayer.getWorld();
			if (mActive) {
				if (mPlayer.getLocation().getPitch() > 50 && isLevelTwo()) {
					if (mAlchemistPotions.getCharges() >= 2 && !isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
						putOnCooldown();
						mAlchemistPotions.decrementCharge();
						mAlchemistPotions.decrementCharge();
						PlayerUtils.healPlayer(mPlugin, mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, EntityUtils.getMaxHealth(mPlayer) * PERCENT_HEALTH_HEALING), mPlayer);
						world.playSound(mPlayer.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1, 1.2f);
						new PartialParticle(Particle.HEART, mPlayer.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(mPlayer);
					}
				} else {
					mAlchemistPotions.increaseChargeTime(mRechargeRateDecrease);
					mActive = false;
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
					ClientModHandler.updateAbility(mPlayer, this);
				}
			} else if (mAlchemistPotions.decrementCharge()) {
				mAlchemistPotions.reduceChargeTime(mRechargeRateDecrease);
				mActive = true;
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1, 0.9f);
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mActive && mPlayer != null) {
			if (oneSecond) {
				double selfDamage = EntityUtils.getMaxHealth(mPlayer) * (PERCENT_HEALTH_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SELF_DAMAGE));
				if (mPlayer.getHealth() > selfDamage) {
					mPlayer.setHealth(mPlayer.getHealth() - selfDamage);
					mPlayer.damage(0);
					new PartialParticle(Particle.DAMAGE_INDICATOR, mPlayer.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(mPlayer);
					new PartialParticle(Particle.SQUID_INK, mPlayer.getEyeLocation(), 1, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(mPlayer);
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);
				}
			}
			mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(6, PERCENT_KNOCKBACK_RESIST + CharmManager.getLevel(mPlayer, CHARM_KNOCKBACK_RESISTANCE) / 10, KNOCKBACK_RESIST_EFFECT_NAME).displaysTime(false));
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
}
