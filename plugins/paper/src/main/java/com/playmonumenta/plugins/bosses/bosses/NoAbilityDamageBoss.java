package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class NoAbilityDamageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_ability_damage";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NoAbilityDamageBoss(plugin, boss);
	}

	public NoAbilityDamageBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getType() == DamageType.MAGIC) {
			event.setCancelled(true);

			Location loc = event.getDamagee().getLocation().add(0, 1, 0);
			World world = loc.getWorld();
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.25f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.25f, 0.75f);
			new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.3).spawnAsEntityActive(mBoss);
		}
	}
}
