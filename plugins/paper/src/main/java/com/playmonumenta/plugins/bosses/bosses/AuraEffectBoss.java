package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AuraEffectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_auraeffect";

	@BossParam(help = "Applies effects to players within an aura around the entity")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Spherical effect radius")
		public int RADIUS = 35;

		@BossParam(help = "Only apply to players within this much y value")
		public int HEIGHT = 20;

		@BossParam(help = "Effects only run if a player is within this range")
		public int DETECTION = 45;

		@BossParam(help = "Delay after spawning before starting effects")
		public int DELAY = 20;

		@BossParam(help = "How often effects will be applied")
		public int PASSIVE_RATE = PASSIVE_RUN_INTERVAL_DEFAULT;

		@BossParam(help = "Particles summoned in the effect area")
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,20,0,0,0,0,#ffffff,2.0)]");

		@BossParam(help = "Particles summoned near the entity")
		public ParticlesList PARTICLE_ENTITY = ParticlesList.fromString("[(REDSTONE,2,1,1,1,0,#ffffff,2.0)]");

		@BossParam(help = "Effects applied to the player when inside the range")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "If the aura is turned off by stunts and silences")
		public boolean CANCELABLE = false;
	}

	public AuraEffectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = List.of(
			new SpellBaseAura(boss, p.RADIUS, p.HEIGHT, p.RADIUS,
				(Entity entity) -> {
					p.PARTICLE.spawn(boss, LocationUtils.getEntityCenter(entity), p.RADIUS / 2f, p.HEIGHT / 2f, p.RADIUS / 2f);
					p.PARTICLE_ENTITY.spawn(boss, LocationUtils.getEntityCenter(entity));
				},
				(p.EFFECTS == EffectsList.EMPTY ? null :
					 (Player player) -> {
						 p.EFFECTS.apply(player, mBoss);
					 }
				), p.CANCELABLE
			)
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, p.DELAY, p.PASSIVE_RATE);
	}
}
