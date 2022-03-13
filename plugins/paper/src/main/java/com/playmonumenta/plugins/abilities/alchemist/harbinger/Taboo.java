package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;


public class Taboo extends Ability {
	private static final int CHARGE_TIME_REDUCTION = 10;
	private static final double PERCENT_HEALTH_DAMAGE = 0.05;
	private static final double PERCENT_KNOCKBACK_RESIST = 5;
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "TabooKnockbackResistanceEffect";
	private static final double MAGIC_DAMAGE_INCREASE_1 = 0.15;
	private static final double MAGIC_DAMAGE_INCREASE_2 = 0.25;
	private static final double PERCENT_HEALTH_HEALING = 0.2;
	private static final int COOLDOWN = 5 * 20;

	private boolean mActive;
	private double mMagicDamageIncrease;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable AlchemicalArtillery mAlchemicalArtillery;

	public Taboo(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Taboo");
		mInfo.mLinkedSpell = ClassAbility.TABOO;
		mInfo.mScoreboardId = "Taboo";
		mInfo.mShorthandName = "Tb";
		mInfo.mDescriptions.add("Swap hands while sneaking and holding an Alchemist's Bag to drink a potion. Drinking the potion causes you to recharge potions 0.5s faster and deal +15% magic damage. However, you lose 5% of your health per second, which bypasses resistances and absorption, but cannot kill you. Swapping hands while sneaking and holding an Alchemist's Bag disables the effect. Taboo can also be toggled by sneaking and swapping hands while holding a bow, crossbow, or trident while Alchemical Artillery is active.");
		mInfo.mDescriptions.add("Extra magic damage increased to 25%. Additionally, while the effect is active, swap hands while looking down, sneaking, and holding an Alchemist's Bag to consume 2 potions and heal 20% of your health. This healing has a 5s cooldown.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.HONEY_BOTTLE, 1);

		mActive = false;
		mMagicDamageIncrease = isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mAlchemicalArtillery = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemicalArtillery.class);
		});
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer != null && mPlayer.isSneaking() && mAlchemistPotions != null && (ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) || (mAlchemicalArtillery != null && mAlchemicalArtillery.isActive() && ItemUtils.isBowOrTrident(mPlayer.getInventory().getItemInMainHand())))) {
			World world = mPlayer.getWorld();
			if (mActive) {
				if (mPlayer.getLocation().getPitch() > 50 && isLevelTwo()) {
					if (mAlchemistPotions.getCharges() >= 2 && !isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
						putOnCooldown();
						mAlchemistPotions.decrementCharge();
						mAlchemistPotions.decrementCharge();
						PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * PERCENT_HEALTH_HEALING, mPlayer);
						world.playSound(mPlayer.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1, 1.2f);
						world.spawnParticle(Particle.HEART, mPlayer.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0);
					}
				} else {
					mAlchemistPotions.increaseChargeTime(CHARGE_TIME_REDUCTION);
					mActive = false;
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
				}
			} else if (mAlchemistPotions.decrementCharge()) {
				mAlchemistPotions.reduceChargeTime(CHARGE_TIME_REDUCTION);
				mActive = true;
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1, 0.9f);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mActive && mPlayer != null) {
			if (oneSecond) {
				double selfDamage = EntityUtils.getMaxHealth(mPlayer) * PERCENT_HEALTH_DAMAGE;
				if (mPlayer.getHealth() > selfDamage) {
					mPlayer.setHealth(mPlayer.getHealth() - selfDamage);
					mPlayer.damage(0);
					mPlayer.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0);
					mPlayer.getWorld().spawnParticle(Particle.SQUID_INK, mPlayer.getEyeLocation(), 1, 0.2, 0.2, 0.2, 0);
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);
				}
			}
			mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(6, PERCENT_KNOCKBACK_RESIST, KNOCKBACK_RESIST_EFFECT_NAME));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity damgee) {
		if (mActive && event.getType() == DamageType.MAGIC) {
			event.setDamage(event.getDamage() * (1 + mMagicDamageIncrease));
		}
		return false;
	}
}
