package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;

public class TinyBombTossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tinybombtoss";
	public static final int detectionRange = 20;

	public static final int LOBS = 1;
	public static final int FUSE = 50;
	public static final double RADIUS = 3;
	public static final int POINT_BLANK_DAMAGE = 8;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TinyBombTossBoss(plugin, boss);
	}

	public TinyBombTossBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, boss, detectionRange, LOBS, FUSE,
				(World world, TNTPrimed tnt, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(boss);

					for (Player player : PlayerUtils.playersInRange(loc, RADIUS, true)) {
						if (player.hasLineOfSight(tnt)) {
							double multiplier = (RADIUS - player.getLocation().distance(loc)) / RADIUS;
							BossUtils.blockableDamage(boss, player, DamageType.BLAST, POINT_BLANK_DAMAGE * multiplier, tnt.getLocation());
						}
					}
				})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}
