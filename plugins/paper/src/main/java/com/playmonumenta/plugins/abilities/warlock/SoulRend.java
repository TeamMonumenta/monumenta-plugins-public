package com.playmonumenta.plugins.abilities.warlock;

import java.util.NavigableSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class SoulRend extends Ability {

	private static final double PERCENT_HEAL = 0.2;
	private static final int HEAL_1 = 2;
	private static final int HEAL_2 = 4;
	private static final int RADIUS = 7;
	private static final int COOLDOWN = 20 * 6;

	private final int mHeal;

	private DarkPact mDarkPact;

	public SoulRend(Plugin plugin, Player player) {
		super(plugin, player, "Soul Rend");
		mInfo.mScoreboardId = "SoulRend";
		mInfo.mShorthandName = "SR";
		mInfo.mDescriptions.add("Attacking an enemy with a critical scythe attack heals you for 2 health and 20% of the melee damage dealt, capped at 10 total health. Cooldown: 6s.");
		mInfo.mDescriptions.add("Players within 7 blocks of you are now also healed. Flat healing is increased from 2 to 4 health.");
		mInfo.mLinkedSpell = ClassAbility.SOUL_REND;
		mInfo.mCooldown = COOLDOWN;
		mDisplayItem = new ItemStack(Material.POTION, 1);
		mHeal = getAbilityScore() == 1 ? HEAL_1 : HEAL_2;
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mDarkPact = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
			}
		});
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			double heal = mHeal + event.getDamage() * PERCENT_HEAL;

			Location loc = event.getEntity().getLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.4f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.15f);
			if (getAbilityScore() > 1) {
				world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0);
				world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0);
				world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0);
				NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
				if (mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME) != null) {
					if (mDarkPact.getAbilityScore() == 2) {
						int currPactDuration = darkPactEffects.last().getDuration();
						mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
						world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
						PlayerUtils.healPlayer(mPlayer, Math.min(10, mHeal));
						mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(currPactDuration, -1));
					}
				} else {
					world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
					PlayerUtils.healPlayer(mPlayer, Math.min(10, heal));
				}
				for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true)) {
					world.spawnParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
					PlayerUtils.healPlayer(p, Math.min(10, heal));
				}
			} else {
				world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0);
				world.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);
				world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0);

				world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
				NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
				if (mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME) != null) {
					if (mDarkPact.getAbilityScore() == 2) {
						int currPactDuration = darkPactEffects.last().getDuration();
						mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
						world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
						PlayerUtils.healPlayer(mPlayer, Math.min(10, mHeal));
						mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(currPactDuration, -1));
					}
				} else {
					world.spawnParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0);
					PlayerUtils.healPlayer(mPlayer, Math.min(10, heal));
				}
			}

			putOnCooldown();
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isFallingAttack(mPlayer) && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}
}