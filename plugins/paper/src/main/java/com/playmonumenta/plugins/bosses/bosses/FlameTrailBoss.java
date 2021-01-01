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

public class FlameTrailBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flametrail";
	public static final int detectionRange = 24;

	private static final int TICK_RATE = 5;
	private static final int TRAIL_RATE = 5;
	private static final int TRAIL_DURATION = 20 * 5;
	private static final boolean TRAIL_GROUND_ONLY = true;
	private static final boolean TRAIL_CONSUMED = true;
	private static final int HITBOX_LENGTH = 1;
	private static final int DAMAGE = 10;
	private static final int FIRE_DURATION = 20 * 8;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameTrailBoss(plugin, boss);
	}

	public FlameTrailBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseTrail(boss, TICK_RATE, TRAIL_RATE, TRAIL_DURATION, TRAIL_GROUND_ONLY, TRAIL_CONSUMED, HITBOX_LENGTH,
					// Trail Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.LAVA, loc, 1, 0.3, 0.1, 0.3, 0.02);
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_BURN, 0.5f, 1f);
						BossUtils.bossDamage(boss, player, DAMAGE);
						player.setFireTicks(FIRE_DURATION);
					},
					// Expire Action
					(World world, Location loc) -> { })
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
