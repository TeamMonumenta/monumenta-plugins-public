package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile";

	public static class Parameters {
		public int DAMAGE = 0;
		public int DISTANCE = 64;
		public double SPEED = 0.4;
		public int DETECTION = 24;
		public int DELAY = 20 * 5;
		public int COOLDOWN = 20 * 10;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public boolean SINGLE_TARGET = true;
		public double DAMAGE_PERCENTAGE = 0.0;
		public boolean LAUNCH_TRACKING = true;
		public double TURN_RADIUS = Math.PI / 30;
		public boolean COLLIDES_WITH_BLOCKS = true;

		/*Effects applied to the player when he got hit */
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//particle & sound used!
		/** Sound played at the start */
		public SoundsList SOUND_START = SoundsList.fromString("[(ENTITY_BLAZE_AMBIENT,1.5,1)]");
		/** Particle used when launching the projectile */
		public ParticlesList PARTICLE_LAUNCH = ParticlesList.fromString("[(EXPLOSION_LARGE,1)]");
		/** Sound used when launching the projectile */
		public SoundsList SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,0.5,0.5)]");
		/** Particle used for the projectile*/
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.fromString("[(FLAME, 4, 0.05, 0.05, 0.05, 0.1),(SMOKE_LARGE, 3, 0.25, 0.25, 0.25)]");
		/** Sound summoned every 2 sec on the projectile location */
		public SoundsList SOUND_PROJECTILE = SoundsList.fromString("[(ENTITY_BLAZE_BURN,0.5,0.2)]");
		/** Particle used when the projectile hit something */
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CLOUD,50,0,0,0,0.25)]");
		/** Sound used when the projectile hit something */
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_GENERIC_DEATH,0.5,0.5)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ProjectileBoss(plugin, boss);
	}


	public ProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE/p.SPEED);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
					p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DELAY, 0));
						p.SOUND_START.play(loc);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						p.PARTICLE_LAUNCH.spawn(loc);
						p.SOUND_LAUNCH.play(loc);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						p.PARTICLE_PROJECTILE.spawn(loc, 0.1, 0.1, 0.1, 0.1);
						if (ticks % 40 == 0) {
							p.SOUND_PROJECTILE.play(loc);
						}
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						p.SOUND_HIT.play(loc, 0.5f, 0.5f);
						p.PARTICLE_HIT.spawn(loc, 0d, 0d, 0d, 0.25d);
						if (player != null) {
							if (p.DAMAGE > 0) {
								BossUtils.bossDamage(boss, player, p.DAMAGE);
							}

							if (p.DAMAGE_PERCENTAGE > 0.0) {
								BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
							}
							p.EFFECTS.apply(player, boss);

						}
					})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);

	}
}
