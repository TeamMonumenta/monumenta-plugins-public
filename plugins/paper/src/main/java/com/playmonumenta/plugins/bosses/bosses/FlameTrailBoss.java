package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseTrail;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FlameTrailBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flametrail";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DELAY = 100;

		@BossParam(help = "not written")
		public int DAMAGE = 15;

		@BossParam(help = "not written")
		public int TICK_RATE = 20;

		@BossParam(help = "not written")
		public int DETECTION = 24;

		@BossParam(help = "not written")
		public int TRAIL_RATE = 5;

		@BossParam(help = "not written")
		public int HITBOX_LENGTH = 1;

		@BossParam(help = "not written")
		public int FIRE_DURATION = 20 * 8;

		@BossParam(help = "not written")
		public int TRAIL_DURATION = 20 * 5;

		@BossParam(help = "not written")
		public boolean TRAIL_CONSUMED = true;

		@BossParam(help = "not written")
		public boolean TRAIL_GROUND_ONLY = true;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameTrailBoss(plugin, boss);
	}

	public FlameTrailBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());


		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseTrail(boss, p.TICK_RATE, p.TRAIL_RATE, p.TRAIL_DURATION, p.TRAIL_GROUND_ONLY, p.TRAIL_CONSUMED, p.HITBOX_LENGTH,
				// Trail Aesthetic
				(World world, Location loc) -> {
					new PartialParticle(Particle.LAVA, loc, 1, 0.3, 0.1, 0.3, 0.02).spawnAsEntityActive(boss);
				},
				// Hit Action
				(World world, Player player, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_GENERIC_BURN, SoundCategory.HOSTILE, 0.5f, 1f);
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), p.FIRE_DURATION, player, boss);
					DamageUtils.damage(boss, player, DamageType.MAGIC, p.DAMAGE, null, false, false, p.SPELL_NAME);
				},
				// Expire Action
				(World world, Location loc) -> {
				})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, p.DELAY);
	}
}
