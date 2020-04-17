package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class HungeringVortex extends Ability {

	private static final int HUNGERING_VORTEX_DURATION = 8 * 20;
	private static final int HUNGERING_VORTEX_COOLDOWN = 20 * 18;
	private static final int HUNGERING_VORTEX_RADIUS = 8;
	private static final int HUNGERING_VORTEX_1_SLOWNESS_AMPLIFIER = 0;
	private static final int HUNGERING_VORTEX_2_SLOWNESS_AMPLIFIER = 1;
	private static final int HUNGERING_VORTEX_1_ABSORPTION_AMPLIFIER = 0;
	private static final int HUNGERING_VORTEX_2_ABSORPTION_AMPLIFIER = 1;
	private static final int HUNGERING_VORTEX_1_EXTRA_DAMAGE = 1;
	private static final int HUNGERING_VORTEX_2_EXTRA_DAMAGE = 2;
	private static final int HUNGERING_VORTEX_1_EXTRA_DAMAGE_MAX = 6;
	private static final int HUNGERING_VORTEX_2_EXTRA_DAMAGE_MAX = 12;

	private final int mSlownessAmplifier;
	private final int mAbsorptionAmplifier;
	private final int mExtraDamage;
	private final int mExtraDamageMax;

	private int mActiveBonus = 0;

	public HungeringVortex(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Hungering Vortex");
		mInfo.scoreboardId = "HungeringVortex";
		mInfo.mShorthandName = "HV";
		mInfo.mDescriptions.add("Right-clicking while shifted while looking down pulls all mobs in a 8 block radius towards you, afflicting them with Slowness I for 8s. This draws the aggro of all mobs pulled and increases your melee damage by 1 for every affected enemy up to a maximum of 6 bonus damage for 8s. In addition, you gain Absorption I for 8 seconds on activation. This skill only goes on cooldown if at least one mob is affected. Cooldown: 18s.");
		mInfo.mDescriptions.add("Slowness is increased to II, Absorption is increased to II, and melee damage increased by 2 for each affected enemy, up to a maximum of 12.");
		mInfo.linkedSpell = Spells.HUNGERING_VORTEX;
		mInfo.cooldown = HUNGERING_VORTEX_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.ignoreCooldown = true;
		mSlownessAmplifier = getAbilityScore() == 1 ? HUNGERING_VORTEX_1_SLOWNESS_AMPLIFIER : HUNGERING_VORTEX_2_SLOWNESS_AMPLIFIER;
		mAbsorptionAmplifier = getAbilityScore() == 1 ? HUNGERING_VORTEX_1_ABSORPTION_AMPLIFIER : HUNGERING_VORTEX_2_ABSORPTION_AMPLIFIER;
		mExtraDamage = getAbilityScore() == 1 ? HUNGERING_VORTEX_1_EXTRA_DAMAGE : HUNGERING_VORTEX_2_EXTRA_DAMAGE;
		mExtraDamageMax = getAbilityScore() == 1 ? HUNGERING_VORTEX_1_EXTRA_DAMAGE_MAX : HUNGERING_VORTEX_2_EXTRA_DAMAGE_MAX;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() + mActiveBonus);
		}
		return true;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)
				|| !mPlayer.isSneaking() || mPlayer.getLocation().getPitch() < 50) {
			return;
		}

		float velocity = mPlayer.getLocation().getBlock().isLiquid() ? 0.04f : 0.055f;

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), HUNGERING_VORTEX_RADIUS, mPlayer);
		for (LivingEntity mob : mobs) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, HUNGERING_VORTEX_DURATION, mSlownessAmplifier));
			if (mob instanceof Mob) {
				((Mob)mob).setTarget(mPlayer);
			}
		}

		// Cancel ability particles and cooldown if nothing is targeted
		if (mobs == null || mobs.size() == 0) {
			return;
		}

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.25f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 0.75f);
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 200, 3.5, 3.5, 3.5, 1);
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.ABSORPTION, HUNGERING_VORTEX_DURATION, mAbsorptionAmplifier, true, true));

		// Gradual pull on mobs
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t += 2;
				for (LivingEntity mob : mobs) {
					// Release suction on hit mobs for half a second
					if (mob.getNoDamageTicks() > mob.getMaximumNoDamageTicks() - 10) {
						MovementUtils.pullTowards(mPlayer, mob, velocity);
						// This means MovementUtils is being screwy and I'm too lazy to change MovementUtils
						if (mob.getVelocity().getY() > 0.4) {
							mob.setVelocity(mob.getVelocity().setY(-0.1));
						}
					}
				}
				if (t > HUNGERING_VORTEX_DURATION) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		// Creates a fast-spiraling helix.
		new BukkitRunnable() {
			Location loc = mPlayer.getLocation();
			double rotation = 0;
			double radius = HUNGERING_VORTEX_RADIUS;

			@Override
			public void run() {
				for (int j = 0; j < 5; j++) {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(rotation + (72 * i));
						loc.add(Math.cos(radian1) * radius, 0.5, Math.sin(radian1) * radius);
						mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
						mPlayer.getWorld().spawnParticle(Particle.PORTAL, loc, 5, 0.1, 0.1, 0.1, 0);
						loc.subtract(Math.cos(radian1) * radius, 0.5, Math.sin(radian1) * radius);
					}
					rotation += 8;
					radius -= 0.25;
					if (radius <= 0) {
						this.cancel();
						return;
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		mActiveBonus = Math.min(mExtraDamageMax, mobs.size() * mExtraDamage);

		new BukkitRunnable() {
			@Override
			public void run() {
				mActiveBonus = 0;
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "The power of your Vortex fades away...");
				this.cancel();
			}

		}.runTaskLater(mPlugin, HUNGERING_VORTEX_DURATION);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return (mainHand == null || mainHand.getType() != Material.BOW) &&
		       (offHand == null || offHand.getType() != Material.BOW) && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());

	}

}
