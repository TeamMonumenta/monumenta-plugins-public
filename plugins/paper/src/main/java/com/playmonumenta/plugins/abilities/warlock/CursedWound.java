package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;



public class CursedWound extends Ability {

	private static final int CURSED_WOUND_DOT_DAMAGE = 1;
	private static final int CURSED_WOUND_DOT_PERIOD = 20;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final double CURSED_WOUND_DAMAGE = 0.05;
	private static final double CURSED_WOUND_1_CAP = 0.15;
	private static final double CURSED_WOUND_2_CAP = 0.3;
	private static final int CURSED_WOUND_EXTENDED_DURATION = 2 * 20;
	private static final String DOT_EFFECT_NAME = "CursedWoundDamageOverTimeEffect";

	private Ability[] mAbilities = {};

	public CursedWound(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Cursed Wound");
		mInfo.mScoreboardId = "CursedWound";
		mInfo.mShorthandName = "CW";
		mInfo.mDescriptions.add("Attacking an enemy with a critical scythe attack passively afflicts it and all enemies in a 3-block cube around it with 1 damage every second for 6s. Your melee attacks passively deal 3% more damage per ability on cooldown, capped at +15% damage.");
		mInfo.mDescriptions.add("Critical attacks now also extend all enemies' debuffs (except Stun, Silence, and Paralysis) by 2s. Damage cap is increased from 15% to 30%.");
		mDisplayItem = new ItemStack(Material.GOLDEN_SWORD, 1);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mAbilities = Stream.of(AmplifyingHex.class, CholericFlames.class, GraspingClaws.class, SoulRend.class,
				                       SanguineHarvest.class, MelancholicLament.class, DarkPact.class, VoodooBonds.class,
				                       JudgementChain.class, HauntingShades.class, WitheringGaze.class)
					.map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c)).toArray(Ability[]::new);
			});
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			double cursedWoundCap = getAbilityScore() == 1 ? CURSED_WOUND_1_CAP : CURSED_WOUND_2_CAP;
			BlockData fallingDustData = Material.ANVIL.createBlockData();
			World world = mPlayer.getWorld();
			if (EntityUtils.isHostileMob(enemy)) {
				world.spawnParticle(Particle.FALLING_DUST, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 3,
					(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, fallingDustData);
				world.spawnParticle(Particle.SPELL_MOB, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 6,
					(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, 0);

				int cooldowns = 0;
				for (Ability ability : mAbilities) {
					if (ability != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().mLinkedSpell)) {
						cooldowns++;
					}
				}

				event.setDamage(event.getDamage() * (1 + Math.min(cooldowns * CURSED_WOUND_DAMAGE, cursedWoundCap)));
			}

			if (PlayerUtils.isFallingAttack(mPlayer)) {
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.75f);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), CURSED_WOUND_RADIUS, mPlayer)) {
					world.spawnParticle(Particle.FALLING_DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 3,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, fallingDustData);
					world.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 6,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, 0);
					mPlugin.mEffectManager.addEffect(mob, DOT_EFFECT_NAME, new CustomDamageOverTime(CURSED_WOUND_DURATION, CURSED_WOUND_DOT_DAMAGE, CURSED_WOUND_DOT_PERIOD, mPlayer, null, Particle.SQUID_INK));
					if (getAbilityScore() > 1) {
						//Bleed interaction
						if (EntityUtils.isBleeding(mPlugin, mob)) {
							EntityUtils.setBleedTicks(mPlugin, mob, EntityUtils.getBleedTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						//Custom slow effect interaction
						if (EntityUtils.isSlowed(mPlugin, mob)) {
							EntityUtils.setSlowTicks(mPlugin, mob, EntityUtils.getSlowTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						//Custom weaken interaction
						if (EntityUtils.isWeakened(mPlugin, mob)) {
							EntityUtils.setWeakenTicks(mPlugin, mob, EntityUtils.getWeakenTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, mob)) {
							PotionEffect effect = mob.getPotionEffect(effectType);
							if (effect != null) {
								mob.removePotionEffect(effectType);
								// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
								mob.addPotionEffect(new PotionEffect(effectType, effect.getDuration() + CURSED_WOUND_EXTENDED_DURATION, effect.getAmplifier()));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}
}
