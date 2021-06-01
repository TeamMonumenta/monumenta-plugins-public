package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseNova;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class NovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_nova";

	public static class Parameters {
		public int RADIUS = 9;
		public int DAMAGE = 0;
		public int DELAY = 100;
		public int DURATION = 70;
		public int DETECTION = 40;
		public int COOLDOWN = 8 * 20;
		public boolean CAN_MOVE = false;
		public double DAMAGE_PERCENTAGE = 0.0;


		//this effects are given to the player after he recive the damage
		public int FIRE_TICKS = 0;
		public int SILENCE_TICKS = 0;
		public int EFFECT_DURATION = 0;
		public int EFFECT_AMPLIFIER = 0;
		public PotionEffectType EFFECT = PotionEffectType.BLINDNESS;

		//particle & sound used!
		/**Sound used when charging the ability */
		public Sound SOUND_CHARGE = Sound.ENTITY_WITCH_CELEBRATE;

		/*Particle summon arround the boss in the air */
		public Particle PARTICLE_AIR = Particle.CLOUD;
		/*Number of particle summon arround the boss in the air */
		public int PARTICLE_AIR_NUMBER = 5;

		/**Particle summon arround the boss when loading the spell */
		public Particle PARTICLE_LOAD = Particle.CRIT;

		/**Sound used when the spell is casted (when explode) */
		public Sound SOUND_CAST = Sound.ENTITY_WITCH_DRINK;

		/**Particle summoned when the spell explode */
		public Particle PARTICLE_EXPLODE_MAIN = Particle.CRIT;
		/**Particle summoned when the spell explode */
		public Particle PARTICLE_EXPLODE_SECOND = Particle.CRIT_MAGIC;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NovaBoss(plugin, boss);
	}

	public NovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseNova(plugin, boss, p.RADIUS, p.DURATION, p.COOLDOWN, p.CAN_MOVE, p.SOUND_CHARGE,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(p.PARTICLE_AIR, loc, p.PARTICLE_AIR_NUMBER, ((double) p.RADIUS) / 2, ((double) p.RADIUS) / 2, ((double) p.RADIUS) / 2, 0.05);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(p.PARTICLE_LOAD, loc, 1, 0.25, 0.25, 0.25, 0);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, p.SOUND_CAST, 1.5f, 0.65F);
				world.playSound(loc, p.SOUND_CAST, 1.5f, 0.55F);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(p.PARTICLE_EXPLODE_MAIN, loc, 1, 0.1, 0.1, 0.1, 0.3);
				world.spawnParticle(p.PARTICLE_EXPLODE_SECOND, loc, 2, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), p.RADIUS)) {

					if (p.DAMAGE > 0) {
						BossUtils.bossDamage(boss, player, p.DAMAGE);
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
					}

					if (p.EFFECT_DURATION > 0) {
						player.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_AMPLIFIER));
					}

					if (p.FIRE_TICKS > 0) {
						player.setFireTicks(p.FIRE_TICKS);
					}

					if (p.SILENCE_TICKS > 0) {
						AbilityUtils.silencePlayer(player, p.SILENCE_TICKS);
					}
				}
			})));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}