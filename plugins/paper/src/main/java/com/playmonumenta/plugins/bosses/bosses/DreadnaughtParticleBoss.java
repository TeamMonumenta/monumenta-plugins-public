package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import com.playmonumenta.plugins.abilities.delves.twisted.Dreadful;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadnaughtParticle;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class DreadnaughtParticleBoss extends BossAbilityGroup {

	public static final String identityTag = Dreadful.DREADFUL_DREADNAUGHT_TAG;
	public static final int detectionRange = 40;

	private static final String DREADLING_1_SUMMON_COMMAND_DATA = "NarsenDreadling";
	private static final String DREADLING_2_SUMMON_COMMAND_DATA = "CelsianDreadling";

	private static final int DAMAGE_IMMUNE_DISTANCE = 6;

	LivingEntity mBoss;

	private final String mDreadlingSummonCommandData;
	private double mDamageCounter = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadnaughtParticleBoss(plugin, boss);
	}

	public DreadnaughtParticleBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		List<Spell> passiveSpells = Arrays.asList(
			new SpellDreadnaughtParticle(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);

		mDreadlingSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? DREADLING_2_SUMMON_COMMAND_DATA : DREADLING_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		Location loc = mBoss.getLocation();
		Entity damager = event.getDamager();
		if (damager instanceof Projectile) {
			ProjectileSource source = ((Projectile) damager).getShooter();
			if (source instanceof LivingEntity) {
				damager = (LivingEntity) source;
			}
		}

		if (loc.distance(damager.getLocation()) > DAMAGE_IMMUNE_DISTANCE) {
			event.setCancelled(true);
			World world = mBoss.getWorld();
			world.playSound(damager.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);
			world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);
			world.spawnParticle(Particle.SPELL_WITCH, loc.add(0, 1, 0), 100, 0, 0, 0, 0.5);
			return;
		}

		mDamageCounter += event.getDamage();

		if (mDamageCounter >= 80) {
			mDamageCounter -= 80;

			LibraryOfSoulsIntegration.summon(loc, mDreadlingSummonCommandData);
			LibraryOfSoulsIntegration.summon(loc, mDreadlingSummonCommandData);
			LibraryOfSoulsIntegration.summon(loc, mDreadlingSummonCommandData);

			loc.add(0, 1, 0);

			World world = event.getEntity().getWorld();
			world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1, 0.5f);
			world.spawnParticle(Particle.FLAME, loc, 50, 3, 1, 3, 0);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 1, 3, 0);
		}
	}
}
