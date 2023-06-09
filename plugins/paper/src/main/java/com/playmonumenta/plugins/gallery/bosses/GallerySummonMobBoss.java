package com.playmonumenta.plugins.gallery.bosses;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSpawnMobs;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class GallerySummonMobBoss extends BossAbilityGroup {

	public static final String identityTag = "GallerySpawnMobBoss";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int RANGE = 9;
		@BossParam(help = "Minimum Distance from Boss to spawn each mob.")
		public int MIN_RANGE = 0;
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int DETECTION = 20;
		@BossParam(help = "not written")
		public int COOLDOWN = 160;
		@BossParam(help = "not written")
		public String SPAWNEDMOB = "";
		@BossParam(help = "not written")
		public int SPAWNCOUNT = 0;
		@BossParam(help = "Maximum Mobs in detection where the ability fails to spawn mobs (Default = 15)")
		public int MOB_CAP = 15;
	}

	public GallerySummonMobBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		GalleryGame game = GalleryUtils.getGame(boss.getLocation());

		Spell spell = new SpellSpawnMobs(boss, p.SPAWNCOUNT, p.SPAWNEDMOB, p.COOLDOWN, p.RANGE, p.MIN_RANGE, p.MOB_CAP) {
			@Override
			public void summonPlugins(Entity summon) {
				if (summon instanceof LivingEntity livingEntity && !GalleryUtils.ignoreScaling(livingEntity) && game != null) {
					game.scaleMob(livingEntity);
				}
			}
		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
