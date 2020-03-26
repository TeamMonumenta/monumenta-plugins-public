package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SoulRend extends Ability {

	private static final int SOUL_REND_RADIUS = 7;
	private static final int SOUL_REND_1_COOLDOWN = 6 * 20;
	private static final int SOUL_REND_2_COOLDOWN = 5 * 20;
	private static final int SOUL_REND_1_HEAL = 2;
	private static final int SOUL_REND_2_HEAL = 4;
	private static final double SOUL_REND_HEAL_MULTIPLIER = 0.2;

	public SoulRend(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Soul Rend");
		mInfo.scoreboardId = "SoulRend";
		mInfo.mShorthandName = "SR";
		mInfo.mDescriptions.add("Getting a critical hit with a scythe heals you for 2 hp + 20% of the damage dealt. (Cooldown: 6s)");
		mInfo.mDescriptions.add("The healing increases to 4 hp + 20% of the damage dealt and nearby allies are healed as well.");
		mInfo.linkedSpell = Spells.SOUL_REND;
		mInfo.cooldown = getAbilityScore() == 1 ? SOUL_REND_1_COOLDOWN : SOUL_REND_2_COOLDOWN;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity damagee = (LivingEntity) event.getEntity();
			double damage = event.getDamage();
			if (EntityUtils.isHostileMob(damagee)) {
				int soulRend = getAbilityScore();
				double heal = soulRend == 1 ? SOUL_REND_1_HEAL : SOUL_REND_2_HEAL;
				heal += damage * SOUL_REND_HEAL_MULTIPLIER;

				Location loc = damagee.getLocation();
				World world = mPlayer.getWorld();
				if (soulRend == 0) {
					world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0);
					world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);
					world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0);
				} else {
					world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0);
					world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0);
					world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0);
				}


				world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.4f, 1.5f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.15f);

				// If Dark Pact is active, damage nearby mobs - otherwise, skill proceeds as normal
				DarkPact dp = (DarkPact) AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
				if (dp != null && dp.isActive()) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SOUL_REND_RADIUS)) {
						world.spawnParticle(Particle.DAMAGE_INDICATOR, mob.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
						EntityUtils.damageEntity(mPlugin, mob, heal / 2, mPlayer, MagicType.DARK_MAGIC, true, mInfo.linkedSpell);
					}
				} else {
					if (soulRend > 1) {
						for (Player p : PlayerUtils.playersInRange(mPlayer, SOUL_REND_RADIUS, true)) {
							world.spawnParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
							PlayerUtils.healPlayer(p, heal);
						}
					} else {
						world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
						PlayerUtils.healPlayer(mPlayer, heal);
					}
				}

				// Put Soul Rend on cooldown
				putOnCooldown();
			}
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
