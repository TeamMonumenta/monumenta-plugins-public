package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseTrail;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.NmsUtils;

public class FlameTrailBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flametrail";

	public static class Parameters {
		public int DELAY = 100;
		public int DAMAGE = 10;
		public int TICK_RATE = 5;
		public int DETECTION = 24;
		public int TRAIL_RATE = 5;
		public int HITBOX_LENGTH = 1;
		public int FIRE_DURATION = 20 * 8;
		public int TRAIL_DURATION = 20 * 5;
		public boolean TRAIL_CONSUMED = true;
		public boolean TRAIL_GROUND_ONLY = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameTrailBoss(plugin, boss);
	}

	public FlameTrailBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());


		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseTrail(boss, p.TICK_RATE, p.TRAIL_RATE, p.TRAIL_DURATION, p.TRAIL_GROUND_ONLY, p.TRAIL_CONSUMED, p.HITBOX_LENGTH,
					// Trail Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.LAVA, loc, 1, 0.3, 0.1, 0.3, 0.02);
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_BURN, 0.5f, 1f);
						player.setFireTicks(p.FIRE_DURATION);
						NmsUtils.unblockableEntityDamageEntity(player, p.DAMAGE, boss);
					},
					// Expire Action
					(World world, Location loc) -> { })
		);

		super.constructBoss(null, passiveSpells, p.DETECTION, null, p.DELAY);
	}
}
