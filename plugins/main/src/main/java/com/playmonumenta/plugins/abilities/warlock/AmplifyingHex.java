package com.playmonumenta.plugins.abilities.warlock;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;

public class AmplifyingHex extends Ability {

	private static final int AMPLIFYING_1_EFFECT_DAMAGE = 5;
	private static final int AMPLIFYING_2_EFFECT_DAMAGE = 7;
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

	public AmplifyingHex(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "AmplifyingHex";
		mInfo.linkedSpell = Spells.AMPLIFYING;
		mInfo.cooldown = (getAbilityScore() == 1) ? AMPLIFYING_1_COOLDOWN : AMPLIFYING_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast() {
		Player player = mPlayer;
		ParticleUtils.explodingConeEffect(mPlugin, player, AMPLIFYING_RADIUS, Particle.DRAGON_BREATH, 0.6f, Particle.SMOKE_NORMAL, 0.4f, AMPLIFYING_DOT_ANGLE);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 1.6f);

		Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), AMPLIFYING_RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > AMPLIFYING_DOT_ANGLE) {
				int debuffCount = (int)DEBUFFS.stream()
				                  .filter(effect -> (mob.getPotionEffect(effect) != null))
				                  .count();
				int damageMult = (getAbilityScore() == 1) ? AMPLIFYING_1_EFFECT_DAMAGE : AMPLIFYING_2_EFFECT_DAMAGE;
				if (getAbilityScore() > 1 && mob.getFireTicks() > 0) {
					debuffCount++;
				}
				if (debuffCount > 0) {
					EntityUtils.damageEntity(mPlugin, mob, debuffCount * damageMult, player);
					MovementUtils.KnockAway(player, mob, AMPLIFYING_KNOCKBACK_SPEED);
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
