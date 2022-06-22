package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBarrier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BarrierBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_barrier";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DETECTION = 100;
		@BossParam(help = "not written")
		public int COOLDOWN = 5 * 20;
		@BossParam(help = "not written")
		public int HITS_TO_BREAK = 1;

		@BossParam(help = "not written")
		public boolean IS_CARAPACE = false;

		@BossParam(help = "Particle summon at boss loc")
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,4,0,1,0,0,#ffffff,2)]");

		@BossParam(help = "Sound played when the barrier refresh")
		public SoundsList SOUND_REFRESH = SoundsList.fromString("[(BLOCK_BEACON_ACTIVATE,1,1)]");

		@BossParam(help = "Sound played when the barrier break")
		public SoundsList SOUND_BREAK = SoundsList.fromString("[(ITEM_SHIELD_BREAK,1,1)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BarrierBoss(plugin, boss);
	}

	public BarrierBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passives = new ArrayList<>(Arrays.asList(new SpellBarrier(plugin, boss, p.DETECTION, p.COOLDOWN, p.HITS_TO_BREAK, p.IS_CARAPACE,
				(Location loc) -> {
					p.SOUND_REFRESH.play(loc);
				}, (Location loc) -> {
					p.PARTICLE.spawn(boss, loc, 0, 1, 0);
				}, (Location loc) -> {
					p.SOUND_BREAK.play(loc);
				})));
		super.constructBoss(SpellManager.EMPTY, passives, p.DETECTION, null);
	}

}
