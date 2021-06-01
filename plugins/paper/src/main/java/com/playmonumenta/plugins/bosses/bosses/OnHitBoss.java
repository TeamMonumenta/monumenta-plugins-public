package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class OnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_onhit";

	public static class Parameters {
		public int DETECTION = 20;
		public int FIRE_TICKS = 0;
		public int SILENCE_TICKS = 0;
		public int EFFECT_DURATION = 0;
		public int EFFECT_AMPLIFIER = 0;
		public boolean CAN_BLOCK = true;
		public double PERCENTAGE_DAMAGE = 0.0;
		public PotionEffectType EFFECT = PotionEffectType.BLINDNESS;

		public int PARTICLE_NUMBER = 100;
		public boolean PARTICLE_ON = true;
		//Particle & Sounds!

		/**Color of the particle */
		public Color PARTICLE_COLOR = Color.WHITE;
		public Particle PARTICLE = Particle.REDSTONE;
		public Sound SOUND = Sound.BLOCK_PORTAL_TRIGGER;

	}

	private Parameters mParams;

	public OnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		//this boss has no ability
		mParams = BossUtils.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mParams.CAN_BLOCK) {
			if (event.getFinalDamage() == 0) {
				// Attack was blocked
				return;
			}
		}

		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			World world = target.getWorld();
			Location loc = target.getLocation().add(0, 1, 0);
			Player player = (Player) target;

			if (mParams.EFFECT_DURATION > 0) {
				player.addPotionEffect(new PotionEffect(mParams.EFFECT, mParams.EFFECT_DURATION, mParams.EFFECT_AMPLIFIER, false, false));
			}

			if (mParams.FIRE_TICKS > 0) {
				player.setFireTicks(mParams.FIRE_TICKS);
			}

			if (mParams.SILENCE_TICKS > 0) {
				AbilityUtils.silencePlayer(player, mParams.SILENCE_TICKS);
			}

			if (mParams.PERCENTAGE_DAMAGE > 0) {
				BossUtils.bossDamagePercent(mBoss, player, mParams.PERCENTAGE_DAMAGE);
			}

			//Particle & Sound
			world.playSound(loc, mParams.SOUND, 0.25f, 2f);
			if (mParams.PARTICLE_ON) {
				if (mParams.PARTICLE.equals(Particle.REDSTONE)) {
					world.spawnParticle(Particle.REDSTONE, loc, mParams.PARTICLE_NUMBER, 0, 0, 0, 0.5, mParams.PARTICLE_COLOR);
				} else {
					world.spawnParticle(mParams.PARTICLE, loc, mParams.PARTICLE_NUMBER, 0, 0, 0, 0.5);
				}
			}

		}


	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new OnHitBoss(plugin, boss);
	}

}
