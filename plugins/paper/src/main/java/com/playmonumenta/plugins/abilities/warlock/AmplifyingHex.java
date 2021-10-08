package com.playmonumenta.plugins.abilities.warlock;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;



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

	private static final float FLAT_DAMAGE = 3f;
	private static final float DAMAGE_PER_SKILL_POINT = 0.5f;
	private static final int AMPLIFIER_DAMAGE_1 = 1;
	private static final int AMPLIFIER_DAMAGE_2 = 2;
	private static final int AMPLIFIER_CAP_1 = 2;
	private static final int AMPLIFIER_CAP_2 = 3;
	private static final float R1_CAP = 3.5f;
	private static final float R2_CAP = 5f;
	private static final int RADIUS_1 = 8;
	private static final int RADIUS_2 = 10;
	private static final double DOT_ANGLE = 0.33;
	private static final int COOLDOWN = 20 * 10;
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
	private final int mAmplifierCap;
	private final int mRadius;
	private float mRegionCap;
	private float mDamage = 0f;

	public AmplifyingHex(Plugin plugin, Player player) {
		super(plugin, player, "Amplifying Hex");
		mInfo.mScoreboardId = "AmplifyingHex";
		mInfo.mShorthandName = "AH";
		mInfo.mDescriptions.add("Left-click while sneaking with a scythe to fire a magic cone up to 8 blocks in front of you, dealing 3 + (0.5 * number of Skill Points, capped at the maximum availble Skill Points for each Region) damage to each enemy per debuff (potion effects like Weakness or Wither, as well as Fire and custom effects like Bleed) they have, and an extra +1 damage per extra level of debuff, capped at 2 extra levels. 10% Slowness, Weaken, etc. count as one level. Cooldown: 10s.");
		mInfo.mDescriptions.add("The range is increased to 10 blocks, extra damage increased to +2 per extra level, and the extra level cap is increased to 3 extra levels.");
		mInfo.mLinkedSpell = ClassAbility.AMPLIFYING;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
		mAmplifierDamage = getAbilityScore() == 1 ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2;
		mAmplifierCap = getAbilityScore() == 1 ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2;
		mRadius = getAbilityScore() == 1 ? RADIUS_1 : RADIUS_2;

		if (player != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Ability[] abilities = new Ability[8];

					abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
					abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, CholericFlames.class);
					abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
					abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
					abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, SanguineHarvest.class);
					abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, MelancholicLament.class);
					abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, CursedWound.class);
					abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, PhlegmaticResolve.class);

					for (Ability classAbility : abilities) {
						if (classAbility != null) {
							mDamage += DAMAGE_PER_SKILL_POINT * classAbility.getAbilityScore();
						}
					}
				}
			}.runTaskLater(mPlugin, 5);
		}
	}

	@Override
	public void cast(Action action) {
		World world = mPlayer.getWorld();
		mRegionCap = ServerProperties.getClassSpecializationsEnabled() == true ? R2_CAP : R1_CAP;
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadiusIncrement = 0.5;

			@Override
			public void run() {
				if (mRadiusIncrement == 0.5) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadiusIncrement += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement, 0.15, FastUtils.sin(radian1) * mRadiusIncrement);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);
					world.spawnParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1);
					world.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1);
				}

				if (mRadiusIncrement >= mRadius + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.7f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.65f);
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > DOT_ANGLE) {
				int debuffCount = 0;
				int amplifierCount = 0;
				for (PotionEffectType effectType: DEBUFFS) {
					PotionEffect effect = mob.getPotionEffect(effectType);
					if (effect != null) {
						debuffCount++;
						amplifierCount += Math.min(mAmplifierCap, effect.getAmplifier());
					}
				}

				if (mob.getFireTicks() > 0) {
					debuffCount++;
					amplifierCount += Math.min(mAmplifierCap, Inferno.getMobInfernoLevel(mPlugin, mob));
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
					amplifierCount += Math.min(mAmplifierCap, EntityUtils.getBleedLevel(mPlugin, mob) - 1);
				}

				//Custom slow effect interaction
				if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
					debuffCount++;
					double slowAmp = EntityUtils.getSlowAmount(mPlugin, mob);
					amplifierCount += Math.min(mAmplifierCap, Math.max((int) Math.floor(slowAmp * 10) - 1, 0));
				}

				//Custom weaken interaction
				if (EntityUtils.isWeakened(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.WEAKNESS) == null) {
					debuffCount++;
					double weakAmp = EntityUtils.getWeakenAmount(mPlugin, mob);
					amplifierCount += Math.min(mAmplifierCap, Math.max((int) Math.floor(weakAmp * 10) - 1, 0));
				}

				if (debuffCount > 0) {
					float finalDamage = AmplifyingHexDamageEnchantment.getExtraPercentDamage(mPlayer, AmplifyingHexDamageEnchantment.class, debuffCount * (FLAT_DAMAGE + Math.min(mDamage, mRegionCap)) + amplifierCount * mAmplifierDamage);
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
				&& ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return AmplifyingHexCooldownEnchantment.class;
	}
}