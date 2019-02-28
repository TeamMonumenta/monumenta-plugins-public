package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;

public class EagleEye extends Ability {

	private static final int EAGLE_EYE_EFFECT_LVL = 0;
	private static final int EAGLE_EYE_DURATION = 10 * 20;
	private static final int EAGLE_EYE_COOLDOWN = 24 * 20; // Was 30
	private static final int EAGLE_EYE_1_VULN_LEVEL = 3; // 20%
	private static final int EAGLE_EYE_2_VULN_LEVEL = 6; // 35%
	private static final int EAGLE_EYE_RADIUS = 20;
	private static final double EAGLE_EYE_DOT_ANGLE = 0.33;

	public EagleEye(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.EAGLE_EYE;
		mInfo.scoreboardId = "Tinkering";
		mInfo.cooldown = EAGLE_EYE_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		Player player = mPlayer;
		int eagleEye = getAbilityScore();
		World world = player.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), EAGLE_EYE_RADIUS, mPlayer)) {
			// Don't apply vulnerability to arena mobs
			if (mob.getScoreboardTags().contains("arena_mob")) {
				continue;
			}

			mob.addPotionEffect(
			    new PotionEffect(PotionEffectType.GLOWING, EAGLE_EYE_DURATION, EAGLE_EYE_EFFECT_LVL, true, false));

			int eagleLevel = (eagleEye == 1) ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL;
			mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, EAGLE_EYE_DURATION, eagleLevel, true, false));

			new BukkitRunnable() {
				int t = 0;

				@Override
				public void run() {
					t++;
					if (mob.isDead() || !mob.isValid()) {
						mPlugin.mTimers.UpdateCooldown(mPlayer, Spells.EAGLE_EYE, 20 * 2);
						this.cancel();
					}
					if (t >= EAGLE_EYE_DURATION) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
			world.spawnParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
			world.playSound(player.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, 0.4f, 1.7f);
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
