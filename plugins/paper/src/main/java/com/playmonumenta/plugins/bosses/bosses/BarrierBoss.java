package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBarrier;
import com.playmonumenta.plugins.utils.BossUtils;

public class BarrierBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_barrier";

	public static class Paramaters {
		public int DETECTION = 100;
		public int COOLDOWN = 5 * 20;
		public int HITS_TO_BREAK = 1;

		/** Particle*/
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,4,0,1,0,#ffffff,2)]");
		/** Sound played when the barrier refresh */
		public SoundsList SOUND_REFRESH = SoundsList.fromString("[(BLOCK_BEACON_ACTIVATE,1,1)]");
		/** Sound played when the barrier break */
		public SoundsList SOUND_BREAK = SoundsList.fromString("[(ITEM_SHIELD_BREAK,1,1)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BarrierBoss(plugin, boss);
	}

	public BarrierBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Paramaters p = BossUtils.getParameters(boss, identityTag, new Paramaters());

		List<Spell> passives = new ArrayList<>(Arrays.asList(new SpellBarrier(plugin, boss, p.DETECTION, p.COOLDOWN, p.HITS_TO_BREAK,
				(Location loc) -> {
					p.SOUND_REFRESH.play(loc);
				}, (Location loc) -> {
					p.PARTICLE.spawn(loc, 0, 1, 0);
				}, (Location loc) -> {
					p.SOUND_BREAK.play(loc);
				})));
		super.constructBoss(null, passives, p.DETECTION, null);
	}

}
