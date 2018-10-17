package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public class SoulRend extends Ability {

	private static final double SOUL_REND_HEAL_1_MULT = 0.4;
	private static final double SOUL_REND_HEAL_2_MULT = 0.5;
	private static final int SOUL_REND_RADIUS = 7;
	private static final int SOUL_REND_COOLDOWN = 6 * 20;

	public SoulRend(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 7;
		mInfo.specId = -1;
		mInfo.scoreboardId = "SoulRend";
		mInfo.linkedSpell = Spells.SOUL_REND;
		mInfo.cooldown = SOUL_REND_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();
		double damage = event.getDamage();
		if (EntityUtils.isHostileMob(damagee)) {
			int soulRend = getAbilityScore();
			double healMult = (soulRend == 1) ? SOUL_REND_HEAL_1_MULT : SOUL_REND_HEAL_2_MULT;
			double soulHealValue = damage * healMult;

			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();

			if (soulRend == 1) {
				world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);
			} else if (soulRend == 2) {
				world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 60, 2.0, 0.75, 2.0, 0.0);
			}

			world.playSound(loc, Sound.ENTITY_MAGMA_CUBE_SQUISH, 1.0f, 0.66f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);

			for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, SOUL_REND_RADIUS, true)) {
				// If this is us or we're allowing anyone to get it.
				if (p == mPlayer || soulRend > 1) {
					PlayerUtils.healPlayer(p, soulHealValue);
				}
			}

			// Put Soul Rend on cooldown
			putOnCooldown();
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer);
	}

}
