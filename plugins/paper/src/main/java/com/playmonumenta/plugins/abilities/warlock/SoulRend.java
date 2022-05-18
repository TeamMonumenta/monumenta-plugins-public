package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class SoulRend extends Ability {

	private static final double PERCENT_HEAL = 0.2;
	private static final int HEAL_1 = 2;
	private static final int HEAL_2 = 4;
	private static final int RADIUS = 7;
	private static final int COOLDOWN = 20 * 8;
	private static final int HEAL_CAP = 10;
	private static final int ABSORPTION_CAP = 4;
	private static final int ABSORPTION_DURATION = 50;

	private final int mHeal;

	private @Nullable DarkPact mDarkPact;

	public SoulRend(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Soul Rend");
		mInfo.mScoreboardId = "SoulRend";
		mInfo.mShorthandName = "SR";
		mInfo.mDescriptions.add("Attacking an enemy with a critical scythe attack heals you for 2 health and 20% of the melee damage dealt, capped at 10 total health. Cooldown: 8s.");
		mInfo.mDescriptions.add("Players within 7 blocks of you are now also healed. Flat healing is increased from 2 to 4 health.");
		mInfo.mDescriptions.add("Healing above max health, as well as any healing from this skill that remains negated by Dark Pact, is converted into Absorption, up to 4 absorption health, for 2.5s.");
		mInfo.mLinkedSpell = ClassAbility.SOUL_REND;
		mInfo.mCooldown = COOLDOWN;
		mDisplayItem = new ItemStack(Material.POTION, 1);
		mHeal = isLevelOne() ? HEAL_1 : HEAL_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mDarkPact = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, DarkPact.class);
			});
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			double heal = mHeal + event.getDamage() * PERCENT_HEAL;
			heal = Math.min(HEAL_CAP, heal);

			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 0.4f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.15f);

			if (isLevelOne()) {
				new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 10, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 7, 0.75, 0.5, 0.75, 0.0).spawnAsPlayerActive(mPlayer);
			} else {
				new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 75, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 95, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 45, 3.5, 1.5, 3.5, 0.0).spawnAsPlayerActive(mPlayer);
			}

			new PartialParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(mPlayer);
			NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
			if (darkPactEffects != null) {
				if (mDarkPact != null && mDarkPact.isLevelTwo()) {
					int currPactDuration = darkPactEffects.last().getDuration();
					mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
					new PartialParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(mPlayer);
					double remainingHealth = EntityUtils.getMaxHealth(mPlayer) - mPlayer.getHealth();
					PlayerUtils.healPlayer(mPlugin, mPlayer, mHeal);
					mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(currPactDuration, -1));

					if (isEnhanced()) {
						double absorption = heal - Math.min(mHeal, remainingHealth);
						absorption = Math.min(ABSORPTION_CAP, absorption);
						AbsorptionUtils.addAbsorption(mPlayer, absorption, absorption, ABSORPTION_DURATION);
					}
				} else if (isEnhanced()) {
					double absorption = heal;
					absorption = Math.min(ABSORPTION_CAP, absorption);
					AbsorptionUtils.addAbsorption(mPlayer, absorption, absorption, ABSORPTION_DURATION);
				}
			} else {
				new PartialParticle(Particle.DAMAGE_INDICATOR, mPlayer.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(mPlayer);
				if (isEnhanced()) {
					double remainingHealth = EntityUtils.getMaxHealth(mPlayer) - mPlayer.getHealth();
					if (heal > remainingHealth) {
						double absorption = heal - remainingHealth;
						heal = remainingHealth;
						absorption = Math.min(ABSORPTION_CAP, absorption);
						AbsorptionUtils.addAbsorption(mPlayer, absorption, absorption, ABSORPTION_DURATION);
					}
				}
				PlayerUtils.healPlayer(mPlugin, mPlayer, heal);
			}

			if (isLevelTwo()) {
				for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true)) {
					new PartialParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.0).spawnAsPlayerActive(mPlayer);
					PlayerUtils.healPlayer(mPlugin, p, heal, mPlayer);
				}
			}

			putOnCooldown();
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && PlayerUtils.isFallingAttack(mPlayer) && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}
}
