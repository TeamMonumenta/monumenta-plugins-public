package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadnaughtParticle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class DreadnaughtParticleBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_dreadnaughtparticle";
	public static final int detectionRange = 40;

	private static final String DREADLING_SOUL_NAME = "Dreadling";

	private static final int DAMAGE_IMMUNE_DISTANCE = 6;

	private double mDamageCounter = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadnaughtParticleBoss(plugin, boss);
	}

	public DreadnaughtParticleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
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

			LibraryOfSoulsIntegration.summon(loc, DREADLING_SOUL_NAME);
			LibraryOfSoulsIntegration.summon(loc, DREADLING_SOUL_NAME);
			LibraryOfSoulsIntegration.summon(loc, DREADLING_SOUL_NAME);

			loc.add(0, 1, 0);

			World world = mBoss.getWorld();
			world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1, 0.5f);
			world.spawnParticle(Particle.FLAME, loc, 50, 3, 1, 3, 0);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 1, 3, 0);
		}
	}
}
