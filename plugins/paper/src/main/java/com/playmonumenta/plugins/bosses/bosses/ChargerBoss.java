package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;

public class ChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charger";

	public static class Parameters {
		//i would very like to set the damage to 0 but for this we need to rework all the mobs
		//with boss_charger in the game...so BIG NOPE
		public float DAMAGE = 15;
		public int DURATION = 25;
		public int DELAY = 5 * 20;
		public int DETECTION = 20;
		public int COOLDOWN = 8 * 20;
		public boolean STOP_ON_HIT = false;
		public boolean TARGET_FURTHEST = false;

		//other stats not used by the defaults ones
		public int FIRE_TICKS = 0;
		public int SILENCE_TICKS = 0;
		public int EFFECT_DURATION = 0;
		public int EFFECT_AMPLIFIED = 0;
		public double DAMAGE_PERCENTAGE = 0.0;
		public PotionEffectType EFFECT = PotionEffectType.BLINDNESS;

		//Particle & Sounds!
		/** particle & sound summoned at boss location when starting the ability */
		public Particle PARTICLE_WARNING = Particle.VILLAGER_ANGRY;
		public Sound SOUND_WARNING = Sound.ENTITY_ELDER_GUARDIAN_CURSE;
		/** particle to show the player where the boss want to charge */
		public Particle PARTICLE_TELL = Particle.CRIT;
		/** particle & Particle summoned at the start and end of the charge */
		public Particle PARTICLE_ROAR = Particle.SMOKE_LARGE;
		public Sound SOUND_ROAR = Sound.ENTITY_ENDER_DRAGON_GROWL;
		/** particle summoned when the charge hit a player*/
		public Particle PARTICLE_ATTACK = Particle.FLAME;

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChargerBoss(plugin, boss);
	}

	public ChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseCharge(plugin, boss, p.DETECTION, p.COOLDOWN, p.DURATION, p.STOP_ON_HIT,
			0, 0, 0, p.TARGET_FURTHEST,
			// Warning sound/particles at boss location and slow boss
			(Player player) -> {
				boss.getWorld().spawnParticle(p.PARTICLE_WARNING, boss.getLocation(), 50, 2, 2, 2, 0);
				boss.getWorld().playSound(boss.getLocation(), p.SOUND_WARNING, 1f, 1.5f);
				boss.setAI(false);
			},
			// Warning particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(p.PARTICLE_TELL, loc, 2, 0.65, 0.65, 0.65, 0);
			},
			// Charge attack sound/particles at boss location
			(Player player) -> {
				boss.getWorld().spawnParticle(p.PARTICLE_ROAR, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
				boss.getWorld().playSound(boss.getLocation(), p.SOUND_ROAR, 1f, 1.5f);
			},
			// Attack hit a player
			(Player player) -> {
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
				if (p.DAMAGE > 0) {
					BossUtils.bossDamage(boss, player, p.DAMAGE);
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
				}

				if (p.EFFECT_DURATION > 0) {
					player.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_AMPLIFIED));
				}

				if (p.FIRE_TICKS > 0) {
					player.setFireTicks(p.FIRE_TICKS);
				}

				if (p.SILENCE_TICKS > 0) {
					AbilityUtils.silencePlayer(player, p.SILENCE_TICKS);
				}
			},
			// Attack particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(p.PARTICLE_ATTACK, loc, 4, 0.5, 0.5, 0.5, 0.075);
				loc.getWorld().spawnParticle(p.PARTICLE_TELL, loc, 8, 0.5, 0.5, 0.5, 0.75);
			},
			// Ending particles on boss
			() -> {
				boss.getWorld().spawnParticle(p.PARTICLE_ROAR, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
				boss.getWorld().playSound(boss.getLocation(), p.SOUND_ROAR, 1f, 1.5f);
				boss.setAI(true);
			})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
