package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


public class SwingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_swing";

	public static class Parameters {
		public int DETECTION = 30;
		public int RADIUS = 3;
		public int DELAY = 5 * 20;
		public int DURATION = 15;
		public int COOLDOWN = 20 * 14;

		public int DAMAGE = 30;
		public double DAMAGE_PERCENT = 0.0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particle & Sounds
		/** Sound played every few ticks */
		public Sound SOUND = Sound.ENTITY_PLAYER_ATTACK_SWEEP;
		/** Particle summon arround the boss in the air */
		public ParticlesList PARTICLE_CHARGE = ParticlesList.fromString("[(SWEEP_ATTACK,1)]");
		/** Particle summon arround the boss on the terrain */
		public ParticlesList PARTICLE_CIRCLE = ParticlesList.fromString("[(CRIT,1)]");
		/** Sound played when the ability explode */
		public SoundsList SOUND_EXPLODE = SoundsList.fromString("[(ENTITY_PLAYER_ATTACK_STRONG,1.5,0.65)]");
		/** Particle summon when the ability explode */
		public ParticlesList PARTICLE_CIRCLE_EXPLODE = ParticlesList.fromString("[(SWEEP_ATTACK,1,0.1,0.1,0.1,0.3),(REDSTONE,2,0.25,0.25,0.25,#ffffff,2)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SwingBoss(plugin, boss);
	}

	public SwingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellBaseAoE(plugin, boss, p.RADIUS, p.DURATION, p.COOLDOWN, true, p.SOUND,
						(Location loc) -> {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1, 2));
							p.PARTICLE_CHARGE.spawn(loc, ((double) p.RADIUS) / 3, ((double) p.RADIUS) / 3, ((double) p.RADIUS) / 3, 0.05);
						}, (Location loc) -> {
							p.PARTICLE_CIRCLE.spawn(loc, 0.25, 0.25, 0.25, 0.05);
						}, (Location loc) -> {
							p.SOUND_EXPLODE.play(loc, 1.5f, 0.65F);
						}, (Location loc) -> {
							p.PARTICLE_CIRCLE_EXPLODE.spawn(loc, 0.2, 0.2, 0.2, 0.2);
						}, (Location loc) -> {
							for (Player player : PlayerUtils.playersInRange(boss.getLocation(), p.RADIUS, true)) {
								if (p.DAMAGE > 0) {
									BossUtils.bossDamage(boss, player, p.DAMAGE);
								}

								if (p.DAMAGE_PERCENT > 0.0) {
									BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENT);
								}

								p.EFFECTS.apply(player, mBoss);
							}
						})));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
