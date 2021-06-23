package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
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
		public double DAMAGE_PERCENTAGE = 0.0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particle & Sounds!
		/** Particle summoned at boss location when starting the ability */
		public ParticlesList PARTICLE_WARNING = ParticlesList.fromString("[(VILLAGER_ANGRY,50)]");
		/** Sound summoned at boss location when starting the ability */
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,1,1.5)]");
		/** Particle to show the player where the boss want to charge */
		public ParticlesList PARTICLE_TELL = ParticlesList.fromString("[(CRIT,2)]");
		/** Particle summon when the ability hit a player */
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(BLOCK_CRACK,5,0.4,0.4,0.4,0.4,REDSTONE_BLOCK),(BLOCK_CRACK,12,0.4,0.4,0.4,0.4,REDSTONE_WIRE)]");
		/** Particle summoned at the start and end of the charge */
		public ParticlesList PARTICLE_ROAR = ParticlesList.fromString("[(SMOKE_LARGE,125)]");
		/** Sound summoned at the start and end of the charge */
		public SoundsList SOUND_ROAR = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,1,1.5)]");
		/** Particle summoned when the charge hit a player*/
		public ParticlesList PARTICLE_ATTACK = ParticlesList.fromString("[(FLAME,4,0.5,0.5,0.5,0.075),(CRIT,8,0.5,0.5,0.5,0.75)]");

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
				p.PARTICLE_WARNING.spawn(boss.getLocation(), 2d, 2d, 2d);
				p.SOUND_WARNING.play(boss.getLocation(), 1f, 1.5f);
				boss.setAI(false);
			},
			// Warning particles
			(Location loc) -> {
				p.PARTICLE_TELL.spawn(loc, 0.65d, 0.65d, 0.65d);
			},
			// Charge attack sound/particles at boss location
			(Player player) -> {
				p.PARTICLE_ROAR.spawn(boss.getLocation(), 0.3d, 0.3d, 0.3d, 0.15d);
				p.SOUND_ROAR.play(boss.getLocation(), 1f, 1.5f);
			},
			// Attack hit a player
			(Player player) -> {
				p.PARTICLE_HIT.spawn(player.getLocation().add(0, 1, 0), 0.4d, 0.4d, 0.4d, 0.4d);
				if (p.DAMAGE > 0) {
					BossUtils.bossDamage(boss, player, p.DAMAGE);
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
				}

				p.EFFECTS.apply(player, mBoss);
			},
			// Attack particles
			(Location loc) -> {
				p.PARTICLE_ATTACK.spawn(loc);
			},
			// Ending particles on boss
			() -> {
				p.PARTICLE_ROAR.spawn(boss.getLocation(), 0.3, 0.3, 0.3, 0.15);
				p.SOUND_ROAR.play(boss.getLocation(), 1f, 1.5f);
				boss.setAI(true);
			})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
