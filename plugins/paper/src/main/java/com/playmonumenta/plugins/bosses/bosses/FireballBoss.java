package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFireball;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FireballBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_fireball";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FireballBoss(plugin, boss);
	}

	public FireballBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFireball(plugin, boss, detectionRange, 30, 1, 160, 2.0f, true, true,
				// Launch effect
				(Location loc) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
					new PartialParticle(Particle.VILLAGER_ANGRY, loc, 10, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(boss);
				})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}
