package com.playmonumenta.plugins.abilities.warlock;

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.UmbralWail;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;



public class CursedWound extends Ability {
	public static class CursedWoundDamageEnchantment extends BaseAbilityEnchantment {
		public CursedWoundDamageEnchantment() {
			super("Cursed Wound Damage", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final double CURSED_WOUND_DAMAGE = 0.05;
	private static final double CURSED_WOUND_1_CAP = 0.15;
	private static final double CURSED_WOUND_2_CAP = 0.3;
	private static final int CURSED_WOUND_EXTENDED_DURATION = 2 * 20;

	public CursedWound(Plugin plugin, Player player) {
		super(plugin, player, "Cursed Wound");
		mInfo.mScoreboardId = "CursedWound";
		mInfo.mShorthandName = "CW";
		mInfo.mDescriptions.add("Attacking an enemy with a critical scythe attack passively afflicts it and all enemies in a 3-block cube around it with Wither 2 for 6s. Your melee attacks passively deal 3% more damage per ability on cooldown, capped at +15% damage.");
		mInfo.mDescriptions.add("Critical attacks now also extend all enemies' debuffs (except Stun, Silence, and Confusion) by 2s. Damage cap is increased from 15% to 30%.");
		mDisplayItem = new ItemStack(Material.GOLDEN_SWORD, 1);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			double cursedWoundCap = getAbilityScore() == 1 ? CURSED_WOUND_1_CAP : CURSED_WOUND_2_CAP;
			LivingEntity damagee = (LivingEntity) event.getEntity();
			BlockData fallingDustData = Material.ANVIL.createBlockData();
			World world = mPlayer.getWorld();
			if (EntityUtils.isHostileMob(damagee)) {
				world.spawnParticle(Particle.FALLING_DUST, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 3,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, fallingDustData);
				world.spawnParticle(Particle.SPELL_MOB, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 6,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, 0);
				// *TO DO* - Move to constructor
				Ability[] abilities = new Ability[12];
				abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
				abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, CholericFlames.class);
				abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
				abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
				abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, SanguineHarvest.class);
				abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, MelancholicLament.class);
				abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
				abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, VoodooBonds.class);
				abilities[8] = AbilityManager.getManager().getPlayerAbility(mPlayer, JudgementChain.class);
				abilities[9] = AbilityManager.getManager().getPlayerAbility(mPlayer, HauntingShades.class);
				abilities[10] = AbilityManager.getManager().getPlayerAbility(mPlayer, WitheringGaze.class);
				abilities[11] = AbilityManager.getManager().getPlayerAbility(mPlayer, UmbralWail.class);

				int cooldowns = 0;
				for (Ability ability : abilities) {
					if (ability != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().mLinkedSpell)) {
						cooldowns++;
					}
				}

				event.setDamage(event.getDamage() * (1 + CursedWoundDamageEnchantment.getExtraPercentDamage(mPlayer, CursedWoundDamageEnchantment.class, (float) Math.min(cooldowns * CURSED_WOUND_DAMAGE, cursedWoundCap))));
				CustomDamageEvent customDamageEvent = new CustomDamageEvent(mPlayer, damagee, 0, null);
				Bukkit.getPluginManager().callEvent(customDamageEvent);
			}

			if (PlayerUtils.isFallingAttack(mPlayer)) {
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.75f);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), CURSED_WOUND_RADIUS, mPlayer)) {
					world.spawnParticle(Particle.FALLING_DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 3,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, fallingDustData);
					world.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 6,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, 0);
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));
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
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}
}