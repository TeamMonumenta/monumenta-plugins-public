package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SoulRend extends Ability {

	private static final int SOUL_REND_RADIUS = 7;
	private static final int SOUL_REND_1_COOLDOWN = 6 * 20;
	private static final int SOUL_REND_2_COOLDOWN = 5 * 20;

	public SoulRend(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SoulRend";
		mInfo.linkedSpell = Spells.SOUL_REND;
		mInfo.cooldown = getAbilityScore() == 1 ? SOUL_REND_1_COOLDOWN : SOUL_REND_2_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();
		double damage = event.getDamage();
		if (EntityUtils.isHostileMob(damagee)) {
			int soulRend = getAbilityScore();
			double heal = (soulRend == 1) ? 2 : 4;
			double soulHealValue = damage * 0.2;

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


			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.65f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.15f);

			for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, SOUL_REND_RADIUS, true)) {
				// If this is us or we're allowing anyone to get it.
				if (p == mPlayer || soulRend > 1) {
					world.spawnParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
					PlayerUtils.healPlayer(p, heal + soulHealValue);
				}
			}

			// Put Soul Rend on cooldown
			putOnCooldown();
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
