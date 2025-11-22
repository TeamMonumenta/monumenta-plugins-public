package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBarrier;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class BarrierBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_barrier";

	@BossParam(help = "The launcher gains a barrier that blocks all damage until enough damage instances break it")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 100;

		@BossParam(help = "Time in ticks before the barrier refreshes after it is broken")
		public int COOLDOWN = TICKS_PER_SECOND * 5;

		@BossParam(help = "Amount of damage instances that need to be dealt to the launcher to break the barrier")
		public int HITS_TO_BREAK = 1;

		@BossParam(help = "If true, the launcher gains a 30% damage buff on attacks while the barrier is active")
		public boolean IS_CARAPACE = false;

		@BossParam(help = "Particles summoned around the launcher")
		public ParticlesList PARTICLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 4, 0.0, 1.0, 0.0, 0.0, new Particle.DustOptions(Color.WHITE, 2.0f)))
			.build();

		@BossParam(help = "Sounds played when the barrier refreshes")
		public SoundsList SOUND_REFRESH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f))
			.build();

		@BossParam(help = "Sounds played when the barrier is broken")
		public SoundsList SOUND_BREAK = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f))
			.build();

		@BossParam(help = "If IS_CARAPACE is true, launcher gains this much damage buff on attacks while the barrier is active. Default 1.3 -> +30% damage buff.")
		public double CARAPACE_DAMAGE_MODIFIER = 1.3;
	}

	public BarrierBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		final List<Spell> passives = List.of(new SpellBarrier(mPlugin, mBoss, p.DETECTION, p.COOLDOWN, p.HITS_TO_BREAK,
			p.IS_CARAPACE,
			(Location loc) -> p.SOUND_REFRESH.play(loc),
			(Location loc) -> p.PARTICLE.spawn(mBoss, loc, 0, 1, 0),
			(Location loc) -> p.SOUND_BREAK.play(loc),
			p.CARAPACE_DAMAGE_MODIFIER));
		super.constructBoss(SpellManager.EMPTY, passives, p.DETECTION, null);
	}
}
