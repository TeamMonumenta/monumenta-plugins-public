package com.playmonumenta.plugins.abilities.warlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Exorcism  extends Ability {

	private static final int EXORCISM_RANGE = 12;
	private static final int EXORCISM_DURATION = 15 * 20;
	private static final double EXORCISM_ANGLE = 50.0;
	private static final int EXORCISM_1_COOLDOWN = 25 * 20;
	private static final int EXORCISM_2_COOLDOWN = 15 * 20;

	public Exorcism(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.EXORCISM;
		mInfo.scoreboardId = "Exorcism";
		mInfo.cooldown = getAbilityScore() == 1 ? EXORCISM_1_COOLDOWN : EXORCISM_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	private List<PotionEffect> debuffs = new ArrayList<PotionEffect>();

	@Override
	public void cast() {
		//	needs better sound
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, -3f);

		putOnCooldown();

		boolean onFire = false;
		for (PotionEffect effect : mPlayer.getActivePotionEffects()) {
			if (PotionUtils.hasNegativeEffects(effect.getType())) {
				// createEffect() multiplies the given duration, so it needs to be divided by the same factor, effect.getType().getDurationModifier().
				debuffs.add(effect.getType().createEffect((int) (EXORCISM_DURATION / effect.getType().getDurationModifier()), effect.getAmplifier()));
			} else  if (getAbilityScore() > 1) {
				if (effect.getType().equals(PotionEffectType.SPEED)) {
					debuffs.add(new PotionEffect(PotionEffectType.SLOW, EXORCISM_DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
					debuffs.add(new PotionEffect(PotionEffectType.WEAKNESS, EXORCISM_DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					debuffs.add(new PotionEffect(PotionEffectType.WITHER, EXORCISM_DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.FAST_DIGGING)) {
					debuffs.add(new PotionEffect(PotionEffectType.SLOW_DIGGING, EXORCISM_DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
					debuffs.add(new PotionEffect(PotionEffectType.UNLUCK, EXORCISM_DURATION, (effect.getAmplifier()*2)+1));
				} else if (effect.getType().equals(PotionEffectType.FIRE_RESISTANCE)) {
					onFire = true;
				}
			}
		}
		PotionUtils.clearNegatives(mPlugin, mPlayer);
		if (mPlayer.getFireTicks() > 1) {
			onFire = true;
			mPlayer.setFireTicks(1);
		}

		for (Entity e : mPlayer.getNearbyEntities(EXORCISM_RANGE, EXORCISM_RANGE * 2, EXORCISM_RANGE)) {
			if (EntityUtils.isHostileMob(e)) {
				LivingEntity mob = (LivingEntity) e;

				for (PotionEffect debuff : debuffs) {
					PotionUtils.applyPotion(mPlayer, mob, debuff);
				}
				if (onFire == true) {
					// Tags the mobs with the damager so that they drop xp if they die due to the fire.
					// Sets iFrames to not influence any damage stacking.
					int ticks = mob.getNoDamageTicks();
					mob.setNoDamageTicks(0);
					Vector v = mob.getVelocity();
					EntityUtils.damageEntity(mPlugin, mob, 0.01, mPlayer, null, false /* do not register CustomDamageEvent */);
					mob.setVelocity(v);
					mob.setNoDamageTicks(ticks);
					EntityUtils.applyFire(mPlugin, EXORCISM_DURATION, mob);
				}
				mWorld.spawnParticle(Particle.SQUID_INK, e.getLocation(), 40, 0.1, 0.2, 0.1, 0.15);
			}
		}

		// a cool particle effect on the player would be nice too
		debuffs.clear();

	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > EXORCISM_ANGLE &&
		       (mainHand == null || mainHand.getType() != Material.BOW) &&
		       (offHand == null || offHand.getType() != Material.BOW) &&
		       (!mPlayer.getActivePotionEffects().isEmpty() || (mPlayer.getFireTicks() > 1));
	}

}