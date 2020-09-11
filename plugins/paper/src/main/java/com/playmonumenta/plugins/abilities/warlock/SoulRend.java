package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SoulRend extends Ability {

	private static final double PERCENT_HEAL = 0.2;
	private static final int HEAL_1 = 2;
	private static final int HEAL_2 = 4;
	private static final int RADIUS = 7;
	private static final int COOLDOWN = 20 * 6;

	private final int mHeal;

	public SoulRend(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Soul Rend");
		mInfo.mScoreboardId = "SoulRend";
		mInfo.mShorthandName = "SR";
		mInfo.mDescriptions.add("Getting a critical hit with a scythe heals you for 2 hp + 20% of the damage dealt. Cooldown: 6 seconds.");
		mInfo.mDescriptions.add("The healing increases to 4 hp + 20% of the damage dealt and nearby allies are healed as well.");
		mInfo.mLinkedSpell = Spells.SOUL_REND;
		mInfo.mCooldown = COOLDOWN;
		mHeal = getAbilityScore() == 1 ? HEAL_1 : HEAL_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			double heal = mHeal + event.getDamage() * PERCENT_HEAL;

			Location loc = event.getEntity().getLocation();
			mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.4f, 1.5f);
			mWorld.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.15f);
			if (getAbilityScore() > 1) {
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0);
				mWorld.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0);

				for (Player p : PlayerUtils.playersInRange(mPlayer, RADIUS, true)) {
					mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
					PlayerUtils.healPlayer(p, heal);
				}
			} else {
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0);
				mWorld.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0);

				mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
				PlayerUtils.healPlayer(mPlayer, heal);
			}

			putOnCooldown();
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())
				&& mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME) == null;
	}

}
