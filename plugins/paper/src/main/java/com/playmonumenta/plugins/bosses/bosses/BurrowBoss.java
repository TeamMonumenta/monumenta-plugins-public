package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBurrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BurrowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_burrow";

	public static class Parameters extends BossParameters {
		@BossParam(help = "maximum range for the mob to follow the aggroed player")
		public int RANGE = 50;

		@BossParam(help = "how long before first cast of the spell")
		public int DELAY = 5 * 20;

		@BossParam(help = "time between casts of the spell; is combined with MAX_BURROW_DURATION for the final cooldown")
		public int COOLDOWN = 4 * 20;

		@BossParam(help = "Maximum duration of the burrowing in ticks")
		public int MAX_BURROW_DURATION = 6 * 20;

		@BossParam(help = "How fast the boss can move while burrowed in blocks per tick")
		public double BURROW_SPEED_START = 0.3;
		public double BURROW_SPEED_END = 0.7;

		@BossParam(help = "How fast the boss can move while burrowed in radians per tick")
		public double BURROW_ACCELERATION_START = 0.04;
		public double BURROW_ACCELERATION_END = 0.10;

		@BossParam(help = "If the trajectory is away from the target, the velocity gets multiplied by this every tick. Lower numbers correct overshoots faster.")
		public double BURROW_CORRECTION_MULTIPLIER = 0.65;

		@BossParam(help = "Decrease the acceleration when near players to make it easier to dodge.")
		public double BURROW_NEARBY_ACCELERATION_RANGE = 3.5;

		@BossParam(help = "Multiplier to decrease acceleration when near players to make dodging easier.")
		public double BURROW_NEARBY_ACCELERATION_MULTIPLIER = 0.35;

		@BossParam(help = "Radius within which the boss checks to emerge from the burrow.")
		public double EMERGE_CHECK_RADIUS = 1.25;

		@BossParam(help = "Radius within which the boss will emerge from the burrow.")
		public double EMERGE_RADIUS = 5;

		@BossParam(help = "Vertical hitbox when the boss will emerge from the burrow.")
		public double EMERGE_VERTICAL_HITBOX = 0.2;

		@BossParam(help = "Damage amount upon unborrowing.")
		public int DAMAGE = 20;

		@BossParam(help = "Minimum damage falloff at max radius from 0-1")
		public double MINIMUM_FALLOFF = 0.5;

		@BossParam(help = "At what block range should the damage start linearly decreasing towards the minimum falloff")
		public double FALLOFF_START = 2;

		@BossParam(help = "Horizontal knockback upon unborrowing.")
		public float KNOCKBACK_X = 0.1f;

		@BossParam(help = "Vertical Knockback upon unborrowing.")
		public float KNOCKBACK_Y = 1.0f;

		@BossParam(help = "Delay in ticks before the boss emerges after burrowing.")
		public int EMERGE_DELAY = 11;

		@BossParam(help = "Number of summons to throw")
		public int SUMMON_COUNT = 1;

		@BossParam(help = "LibraryOfSouls name or pool of the mob(s) spawned")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "a target is necessary to start casting")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;
	}

	public BurrowBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellBurrow(plugin, boss, p);
		super.constructBoss(spell, (int) p.TARGETS.getRange(), null, p.DELAY);
	}
}
