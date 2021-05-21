package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellGenericCharge;
import com.playmonumenta.plugins.utils.BossUtils;

public class ChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charger";

	public static class Parameters {
		public int COOLDOWN = 8 * 20;
		public int DETECTION = 20;
		public int DELAY = 5 * 20;
		public float DAMAGE = 15;
		public int DURATION = 25;
		public boolean STOP_ON_HIT = false;
		public Particle PARTICLE_START = Particle.VILLAGER_ANGRY;
		public Particle PARTICLE_WARNING = Particle.CRIT;
		public Particle PARTICLE_CHARGE = Particle.SMOKE_LARGE;
		public Particle PARTICLE_ATTACK = Particle.FLAME;
		public Particle PARTICLE_END = Particle.SMOKE_LARGE;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChargerBoss(plugin, boss);
	}

	public ChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellGenericCharge(plugin, boss, p.DETECTION, p.DAMAGE, p.COOLDOWN, p.DURATION, p.STOP_ON_HIT,
				p.PARTICLE_START, p.PARTICLE_WARNING, p.PARTICLE_CHARGE, p.PARTICLE_ATTACK, p.PARTICLE_END)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
