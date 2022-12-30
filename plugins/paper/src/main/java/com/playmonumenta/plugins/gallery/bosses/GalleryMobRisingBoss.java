package com.playmonumenta.plugins.gallery.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class GalleryMobRisingBoss extends BossAbilityGroup {
	public static final String identityTag = "GalleryMobRisingBoss";


	public static class Parameters extends BossParameters {

		public int DETECTION = 100;
		public int DELAY = 80;
		public int COOLDOWN = 160;
		public int DURATION = 80;
		public int RANGE = 20;
		public boolean CAN_BE_STOPPED = false;
		public boolean CAN_MOVE = false;
		public boolean SINGLE_TARGET = false;
		public int MOB_NUMBER = 0;
		public float DEPTH = 2.5f;

		public LoSPool MOB_POOL = LoSPool.EMPTY;

		public EntityTargets TARGETS = EntityTargets.GENERIC_SELF_TARGET;

		public SoundsList SOUNDS = SoundsList.EMPTY;

	}

	public static BossAbilityGroup deserialize(com.playmonumenta.plugins.Plugin plugin, LivingEntity boss) throws Exception {
		return new GalleryMobRisingBoss(plugin, boss);
	}

	public GalleryMobRisingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		GalleryGame game = GalleryUtils.getGame(boss.getLocation());
		if (p.MOB_POOL != LoSPool.EMPTY && game != null) {
			SpellManager activeSpells = new SpellManager(List.of(
				new SpellBaseSummon(
					plugin,
					boss,
					p.COOLDOWN,
					p.DURATION,
					p.RANGE,
					p.DEPTH,
					p.CAN_BE_STOPPED,
					p.CAN_MOVE,
					p.SINGLE_TARGET,
					() -> p.MOB_NUMBER,
					() -> p.TARGETS.getTargetsLocationList(boss),
					(Location loc, int times) -> {
						Entity entity = p.MOB_POOL.spawn(loc);
						if (entity instanceof LivingEntity livingEntity && !GalleryUtils.ignoreScaling(livingEntity)) {
							game.scaleMob(livingEntity);
						}
						return entity;
					},
					(LivingEntity bos, Location loc, int ticks) -> {
						if (ticks == 0) {
							bos.setGlowing(true);
						}

						if (p.SOUNDS != SoundsList.EMPTY) {
							p.SOUNDS.play(bos.getLocation());
						}

						loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);

						if (ticks >= p.DURATION) {
							bos.setGlowing(false);
						}

					},
					(LivingEntity mob, Location loc, int ticks) -> {
						if (ticks == 0) {
							mob.setGlowing(true);
						}
						loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);

						if (ticks >= p.DURATION) {
							mob.setGlowing(false);
						}
					})
			));


			super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
		} else {
			com.playmonumenta.plugins.Plugin.getInstance().getLogger().warning("[GalleryMobRisingBoss] tried to summon a boss with default LoSPool MOB_POOL = EMPTY");
		}

	}
}
