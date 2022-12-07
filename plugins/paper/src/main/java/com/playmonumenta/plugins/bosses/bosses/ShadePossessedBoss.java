package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShadePossessedParticle;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class ShadePossessedBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_shadepossessed";
	public static final int detectionRange = 40;

	private static final String SHADE_OF_DEATH = "ShadeofDeath";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShadePossessedBoss(plugin, boss);
	}

	public ShadePossessedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellShadePossessedParticle(mBoss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();

		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.5f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 20, 0.2, 0.3, 0.2, 0.1).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 200, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);

		LibraryOfSoulsIntegration.summon(loc, SHADE_OF_DEATH);
	}
}
