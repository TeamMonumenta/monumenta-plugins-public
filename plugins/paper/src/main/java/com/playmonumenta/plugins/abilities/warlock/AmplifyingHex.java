package com.playmonumenta.plugins.abilities.warlock;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class AmplifyingHex extends Ability {

	private static final int AMPLIFYING_1_EFFECT_DAMAGE = 5;
	private static final int AMPLIFYING_2_EFFECT_DAMAGE = 7;
	private static final int AMPLIFYING_1_AMPLIFIER_DAMAGE = 1;
	private static final int AMPLIFYING_2_AMPLIFIER_DAMAGE = 2;
	private static final int AMPLIFYING_AMPLIFIER_CAP = 2;
	private static final int AMPLIFYING_RADIUS = 8;
	private static final double AMPLIFYING_DOT_ANGLE = 0.33;
	private static final int AMPLIFYING_1_COOLDOWN = 12 * 20;
	private static final int AMPLIFYING_2_COOLDOWN = 10 * 20;
	private static final float AMPLIFYING_KNOCKBACK_SPEED = 0.12f;

	private static final List<PotionEffectType> DEBUFFS = Arrays.asList(
	                                                          PotionEffectType.WITHER,
	                                                          PotionEffectType.SLOW,
	                                                          PotionEffectType.WEAKNESS,
	                                                          PotionEffectType.SLOW_DIGGING,
	                                                          PotionEffectType.POISON,
	                                                          PotionEffectType.UNLUCK,
	                                                          PotionEffectType.BLINDNESS,
	                                                          PotionEffectType.HUNGER
	                                                      );

	private final int mEffectDamage;
	private final int mAmplifierDamage;

	public AmplifyingHex(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Amplifying Hex");
		mInfo.scoreboardId = "AmplifyingHex";
		mInfo.mShorthandName = "AH";
		mInfo.mDescriptions.add("If you left-click with a scythe while sneaking, you fire a magic cone up to 8 blocks in front of you, dealing 5 damage to each enemy per debuff (potion effects like slowness or wither, as well as stun) they have, and an extra +1 damage per extra level of debuff (capped at 2 extra levels. Extra levels of Vulnerability not counted). Cooldown: 12s.");
		mInfo.mDescriptions.add("The damage is increased to 7 damage per debuff, and extra damage increased to +2 per extra level. Cooldown: 10 seconds.");
		mInfo.linkedSpell = Spells.AMPLIFYING;
		mInfo.cooldown = (getAbilityScore() == 1) ? AMPLIFYING_1_COOLDOWN : AMPLIFYING_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mEffectDamage = getAbilityScore() == 1 ? AMPLIFYING_1_EFFECT_DAMAGE : AMPLIFYING_2_EFFECT_DAMAGE;
		mAmplifierDamage = getAbilityScore() == 1 ? AMPLIFYING_1_AMPLIFIER_DAMAGE : AMPLIFYING_2_AMPLIFIER_DAMAGE;
	}

	@Override
	public void cast(Action action) {
		new BukkitRunnable() {
			Location loc = mPlayer.getLocation();
			double radius = 0.5;

			@Override
			public void run() {
				if (radius == 0.5) {
					loc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				radius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(Math.cos(radian1) * radius, 0.15, Math.sin(radian1) * radius);
					vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().clone().add(0, 0.15, 0).add(vec);
					mWorld.spawnParticle(Particle.DRAGON_BREATH, l, 2, 0.05, 0.05, 0.05, 0.1);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.05, 0.05, 0.05, 0.1);
				}

				if (radius >= AMPLIFYING_RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.65f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.65f);

		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), AMPLIFYING_RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > AMPLIFYING_DOT_ANGLE) {
				int debuffCount = (int) DEBUFFS.stream()
						.filter(effect -> (mob.getPotionEffect(effect) != null))
						.count();
				int amplifierCount = DEBUFFS.stream()
						.filter(effect -> (mob.getPotionEffect(effect) != null))
						.filter(effect -> (effect != PotionEffectType.UNLUCK))		// Don't count Vulnerability "levels"
						.mapToInt(effect -> Math.min(mob.hasMetadata(FractalEnervation.FRACTAL_CAP_REMOVED_METAKEY) ? FractalEnervation.FRACTAL_AMPLIFYING_HEX_CAP : AMPLIFYING_AMPLIFIER_CAP,
								mob.getPotionEffect(effect).getAmplifier()))
						.sum();

				ConsumingFlames cf = (ConsumingFlames) AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);

				if (cf != null)	{
					if (cf.getAbilityScore() > 0 && mob.getFireTicks() > 0) {
						debuffCount++;
						amplifierCount += Math.min(mob.hasMetadata(FractalEnervation.FRACTAL_CAP_REMOVED_METAKEY) ? FractalEnervation.FRACTAL_AMPLIFYING_HEX_CAP : AMPLIFYING_AMPLIFIER_CAP,
								Inferno.getMobInfernoLevel(mPlugin, mob));
					}
				}
				if (EntityUtils.isStunned(mob)) {
					debuffCount++;
				}
				if (debuffCount > 0) {
					EntityUtils.damageEntity(mPlugin, mob,
							debuffCount * mEffectDamage + amplifierCount * mAmplifierDamage,
							mPlayer, MagicType.DARK_MAGIC, true, mInfo.linkedSpell);
					MovementUtils.knockAway(mPlayer, mob, AMPLIFYING_KNOCKBACK_SPEED);
				}
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
