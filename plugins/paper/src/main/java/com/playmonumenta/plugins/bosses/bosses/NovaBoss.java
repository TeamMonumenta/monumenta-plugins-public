package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.CustomString;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseNova;
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

		public EffectsList EFFECTS = EffectsList.EMPTY;
		/** The spell name showed when the player die by this skill */
		public CustomString SPELL_NAME = CustomString.EMPTY;

		//particle & sound used!
		/** Particle summon on the air */
		public ParticlesList PARTICLE_AIR = ParticlesList.fromString("[(cloud,5)]");
		/** Sound used when charging the ability */
		public Sound SOUND_CHARGE = Sound.ENTITY_WITCH_CELEBRATE;
		/** Particle summon arround the boss when loading the spell */
		public ParticlesList PARTICLE_LOAD = ParticlesList.fromString("[(crit,1)]");
		/** Sound used when the spell is casted (when explode) */
		public SoundsList SOUND_CAST = SoundsList.fromString("[(ENTITY_WITCH_DRINK,1.5,0.65),(ENTITY_WITCH_DRINK,1.5,0.55)]");
		/*Particle summoned when the spell explode */
		public ParticlesList PARTICLE_EXPLODE = ParticlesList.fromString("[(CRIT,1,0.1,0.1,0.1,0.3),(CRIT_MAGIC,1,0.25,0.25,0.25,0.1)]");

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
				p.PARTICLE_AIR.spawn(loc, ((double) p.RADIUS) / 2, ((double) p.RADIUS) / 2, ((double) p.RADIUS) / 2, 0.05);
			},
			(Location loc) -> {
				p.PARTICLE_LOAD.spawn(loc, 0.25d, 0.25d, 0.25d, (double) 0.0d);
			},
			(Location loc) -> {
				p.SOUND_CAST.play(loc, 1.5f, 0.65f);
			},
			(Location loc) -> {
				p.PARTICLE_EXPLODE.spawn(loc, 0.2, 0.2, 0.2, 0.2);
			},
			(Location loc) -> {
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), p.RADIUS, true)) {

					if (p.DAMAGE > 0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamage(boss, player, p.DAMAGE);
						} else {
							BossUtils.bossDamage(boss, player, p.DAMAGE, mBoss.getLocation(), p.SPELL_NAME.getString());
						}
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						if (p.SPELL_NAME.isEmpty()) {
							BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
						} else {
							BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE, p.SPELL_NAME.getString());
						}
					}
					p.EFFECTS.apply(player, mBoss);
				}
			})));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}