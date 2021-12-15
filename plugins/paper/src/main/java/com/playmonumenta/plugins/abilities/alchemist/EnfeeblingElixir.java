package com.playmonumenta.plugins.abilities.alchemist;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;



public class EnfeeblingElixir extends Ability {

	public static class EnfeeblingElixirCooldownEnchantment extends BaseAbilityEnchantment {
		public EnfeeblingElixirCooldownEnchantment() {
			super("Enfeebling Elixir Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final int COOLDOWN = 16 * 20;
	private static final int DURATION_1 = 7 * 20;
	private static final int DURATION_2 = 10 * 20;
	private static final float KNOCKBACK_SPEED_1 = 0.35f;
	private static final float KNOCKBACK_SPEED_2 = 0.5f;
	private static final double WEAKEN_AMPLIFIER_1 = 0.15;
	private static final double WEAKEN_AMPLIFIER_2 = 0.3;
	private static final double SPEED_AMPLIFIER_1 = 0.2;
	private static final double SPEED_AMPLIFIER_2 = 0.4;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EEPercentSpeedEffect";
	private static final int JUMP_LEVEL = 1;
	private static final int ENFEEBLING_RADIUS = 3;

	private final int mDuration;
	private final double mWeakenEffect;
	private final double mSpeedAmp;
	private final float mKnockbackSpeed;

	public EnfeeblingElixir(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Enfeebling Elixir");
		mInfo.mLinkedSpell = ClassAbility.ENFEEBLING_ELIXIR;
		mInfo.mScoreboardId = "EnfeeblingElixir";
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add("When you crouch and attack a mob or left click, all mobs within 3 blocks are knocked back several blocks and gain 15% Weaken for 7s. You gain Jump Boost II and +20% Speed for 7s. Cooldown: 16s.");
		mInfo.mDescriptions.add("The knockback increases by 50%, you apply 30% Weaken to mobs and are given +40% Speed, and the duration of buffs and debuffs is increased to 10s.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDuration = getAbilityScore() == 1 ? DURATION_1 : DURATION_2;
		mWeakenEffect = getAbilityScore() == 1 ? WEAKEN_AMPLIFIER_1 : WEAKEN_AMPLIFIER_2;
		mSpeedAmp = getAbilityScore() == 1 ? SPEED_AMPLIFIER_1 : SPEED_AMPLIFIER_2;
		mKnockbackSpeed = getAbilityScore() == 1 ? KNOCKBACK_SPEED_1 : KNOCKBACK_SPEED_2;
		mDisplayItem = new ItemStack(Material.RABBIT_FOOT, 1);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		ItemStack hand = mPlayer.getInventory().getItemInMainHand();

		if (!ItemUtils.isSomeBow(hand) && hand.getType() != Material.SPLASH_POTION) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), ENFEEBLING_RADIUS, mPlayer)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockbackSpeed);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeakenEffect, mob);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
			                                 new PercentSpeed(mDuration, mSpeedAmp, PERCENT_SPEED_EFFECT_NAME));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, mDuration, JUMP_LEVEL));

			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SPELL_MOB, mPlayer.getLocation(), 100, 2, 1.5, 2, 0);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);

			putOnCooldown();
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity() instanceof LivingEntity) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking();
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return EnfeeblingElixirCooldownEnchantment.class;
	}
}
