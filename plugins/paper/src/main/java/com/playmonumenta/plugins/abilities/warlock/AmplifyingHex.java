package com.playmonumenta.plugins.abilities.warlock;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class AmplifyingHex extends Ability {
	public static class AmplifyingHexDamageEnchantment extends BaseAbilityEnchantment {
		public AmplifyingHexDamageEnchantment() {
			super("Amplifying Hex Damage", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class AmplifyingHexCooldownEnchantment extends BaseAbilityEnchantment {
		public AmplifyingHexCooldownEnchantment() {
			super("Amplifying Hex Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final float EFFECT_DAMAGE_1 = 5f;
	private static final float EFFECT_DAMAGE_2 = 6.5f;
	private static final int AMPLIFIER_DAMAGE_1 = 1;
	private static final int AMPLIFIER_DAMAGE_2 = 2;
	private static final int AMPLIFIER_CAP = 2;
	private static final int RADIUS = 8;
	private static final double DOT_ANGLE = 0.33;
	private static final int COOLDOWN_1 = 20 * 12;
	private static final int COOLDOWN_2 = 20 * 10;
	private static final float KNOCKBACK_SPEED = 0.12f;

	private static final List<PotionEffectType> DEBUFFS = Arrays.asList(
	                                                          PotionEffectType.WITHER,
	                                                          PotionEffectType.SLOW,
	                                                          PotionEffectType.WEAKNESS,
	                                                          PotionEffectType.SLOW_DIGGING,
	                                                          PotionEffectType.POISON,
	                                                          PotionEffectType.UNLUCK,
	                                                          PotionEffectType.BLINDNESS,
	                                                          PotionEffectType.CONFUSION,
	                                                          PotionEffectType.HUNGER
	                                                      );

	private final int mAmplifierDamage;

	public AmplifyingHex(Plugin plugin, Player player) {
		super(plugin, player, "Amplifying Hex");
		mInfo.mScoreboardId = "AmplifyingHex";
		mInfo.mShorthandName = "AH";
		mInfo.mDescriptions.add("Left-click while sneaking with a scythe to fire a magic cone up to 8 blocks in front of you, dealing 5 damage to each enemy per debuff (potion effects like Weakness or Wither, as well as Fire and custom effects like Bleed) they have, and an extra +1 damage per extra level of debuff, capped at 2 extra levels. 10% Slowness, Weaken, etc. count as one level. Cooldown: 10s.");
		mInfo.mDescriptions.add("The damage is increased to 6.5 damage per debuff, and extra damage increased to +2 per extra level.");
		mInfo.mLinkedSpell = Spells.AMPLIFYING;
		mInfo.mCooldown = (getAbilityScore() == 1) ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mAmplifierDamage = getAbilityScore() == 1 ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2;
	}

	@Override
	public void cast(Action action) {
		int cd = (getAbilityScore() == 1) ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mCooldown = (int) AmplifyingHexCooldownEnchantment.getCooldown(mPlayer, cd, AmplifyingHexCooldownEnchantment.class);

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0.5;

			@Override
			public void run() {
				if (mRadius == 0.5) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);
					world.spawnParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1);
					world.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1);
				}

				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		float effectDamage = getAbilityScore() == 1 ? EFFECT_DAMAGE_1 : EFFECT_DAMAGE_2;
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.65f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.65f);
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > DOT_ANGLE) {
				int debuffCount = 0;
				int amplifierCount = 0;
				for (PotionEffectType effectType: DEBUFFS) {
					PotionEffect effect = mob.getPotionEffect(effectType);
					if (effect != null) {
						debuffCount++;
						amplifierCount += Math.min(AMPLIFIER_CAP, effect.getAmplifier());
					}
				}

				if (mob.getFireTicks() > 0) {
					debuffCount++;
					amplifierCount += Math.min(AMPLIFIER_CAP, Inferno.getMobInfernoLevel(mPlugin, mob));
				}

				if (EntityUtils.isStunned(mob)) {
					debuffCount++;
				}

				if (EntityUtils.isConfused(mob)) {
					debuffCount++;
				}

				if (EntityUtils.isSilenced(mob)) {
					debuffCount++;
				}

				if (EntityUtils.isBleeding(mPlugin, mob)) {
					debuffCount++;
					amplifierCount += Math.min(AMPLIFIER_CAP, EntityUtils.getBleedLevel(mPlugin, mob) - 1);
				}

				//Custom slow effect interaction
				if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
					debuffCount++;
					double slowAmp = EntityUtils.getSlowAmount(mPlugin, mob);
					amplifierCount += Math.min(AMPLIFIER_CAP, Math.max((int) Math.floor(slowAmp * 10) - 1, 0));
				}

				//Custom weaken interaction
				if (EntityUtils.isWeakened(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.WEAKNESS) == null) {
					debuffCount++;
					double weakAmp = EntityUtils.getWeakenAmount(mPlugin, mob);
					amplifierCount += Math.min(AMPLIFIER_CAP, Math.max((int) Math.floor(weakAmp * 10) - 1, 0));
				}

				if (debuffCount > 0) {
					float finalDamage = (float) AmplifyingHexDamageEnchantment.getSpellDamage(mPlayer, debuffCount * effectDamage + amplifierCount * mAmplifierDamage);
					EntityUtils.damageEntity(mPlugin, mob, finalDamage, mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
					MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED);
				}
			}
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		double pitch = mPlayer.getLocation().getPitch();
		return (mPlayer.isSneaking() && pitch < 50 && pitch > -50
				&& InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand()));
	}
}
