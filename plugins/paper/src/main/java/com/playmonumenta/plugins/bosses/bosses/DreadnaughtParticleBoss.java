package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadnaughtParticle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DreadnaughtParticleBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_dreadnaughtparticle";
	public static final int detectionRange = 40;

	private static final String DREADLING_TERRAIN_SOUL_NAME = "Dreadling";
	private static final String DREADLING_WATER_SOUL_NAME = "Hydraling";

	private static final int DAMAGE_IMMUNE_DISTANCE = 8;

	private double mDamageCounter = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadnaughtParticleBoss(plugin, boss);
	}

	public DreadnaughtParticleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = List.of(
			new SpellDreadnaughtParticle(boss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		Location loc = mBoss.getLocation();

		if (loc.distance(source.getLocation()) > DAMAGE_IMMUNE_DISTANCE) {
			event.setCancelled(true);
			World world = mBoss.getWorld();
			world.playSound(damager.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);
			world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);
			world.spawnParticle(Particle.SPELL_WITCH, loc.add(0, 1, 0), 100, 0, 0, 0, 0.5);
			return;
		}

		mDamageCounter += event.getFinalDamage(false);

		if (mDamageCounter >= 80) {
			mDamageCounter -= 80;

			if (loc.getBlock().getType() == Material.WATER) {
				LibraryOfSoulsIntegration.summon(loc, DREADLING_WATER_SOUL_NAME);
				LibraryOfSoulsIntegration.summon(loc, DREADLING_WATER_SOUL_NAME);
				LibraryOfSoulsIntegration.summon(loc, DREADLING_WATER_SOUL_NAME);
			} else {
				LibraryOfSoulsIntegration.summon(loc, DREADLING_TERRAIN_SOUL_NAME);
				LibraryOfSoulsIntegration.summon(loc, DREADLING_TERRAIN_SOUL_NAME);
				LibraryOfSoulsIntegration.summon(loc, DREADLING_TERRAIN_SOUL_NAME);
			}

			loc.add(0, 1, 0);

			World world = mBoss.getWorld();
			world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1, 0.5f);
			world.spawnParticle(Particle.FLAME, loc, 50, 3, 1, 3, 0);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 1, 3, 0);
		}
	}
}
