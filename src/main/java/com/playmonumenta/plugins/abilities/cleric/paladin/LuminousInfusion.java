package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
* All attacks against undead deal +2/5 damage. Sneak and right-click while
* looking at the ground to charge your weapon with holy light for 15 seconds.
* Your next swing stuns non undead for 3s (slowness V weakness V) or deals +10
* damage to undead, and if it kills the undead, it explodes, dealing 10 damage
* to all mobs within 4 blocks. Cooldown 25/18s
*/

public class LuminousInfusion extends Ability {

	private static final String LUMINOUS_INFUSION_EXPIRATION_MESSAGE = "The light from your hands fades...";
	private static final int LUMINOUS_INFUSION_ACTIVATION_ANGLE = 75;
	private static final double LUMINOUS_INFUSION_RADIUS = 4;
	private static final int LUMINOUS_INFUSION_EXPLOSION_DAMAGE = 10;
	private static final int LUMINOUS_INFUSION_UNDEAD_DAMAGE = 10;
	private static final int LUMINOUS_INFUSION_1_PASSIVE_DAMAGE = 2;
	private static final int LUMINOUS_INFUSION_2_PASSIVE_DAMAGE = 5;
	private static final int LUMINOUS_INFUSION_SLOWNESS_DURATION = 3 * 20;
	private static final int LUMINOUS_INFUSION_WEAKNESS_DURATION = 3 * 20;
	private static final int LUMINOUS_INFUSION_SLOWNESS_LEVEL = 4;
	private static final int LUMINOUS_INFUSION_WEAKNESS_LEVEL = 4;
	private static final int LUMINOUS_INFUSION_MAX_DURATION = 15 * 20;
	private static final int LUMINOUS_INFUSION_1_COOLDOWN = 25 * 20;
	private static final int LUMINOUS_INFUSION_2_COOLDOWN = 18 * 20;

	private boolean mActive = false;

	public LuminousInfusion(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.LUMINOUS_INFUSION;
		mInfo.scoreboardId = "LuminousInfusion";
		mInfo.cooldown = getAbilityScore() == 1 ? LUMINOUS_INFUSION_1_COOLDOWN : LUMINOUS_INFUSION_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;

		/*
		 * NOTE! Because LuminousInfusion has two events (cast and damage), we
		 * need both events to trigger even when it is on cooldown. Therefor it
		 * needs to bypass the automatic cooldown check and manage cooldown
		 * itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean cast() {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			// On cooldown - can't cast it again yet
			return false;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (inMainHand == null || !mPlayer.isSneaking() || InventoryUtils.isBowItem(inMainHand)
		    || mPlayer.getLocation().getPitch() < LUMINOUS_INFUSION_ACTIVATION_ANGLE) {
			// Conditions not met - can't cast
			return false;
		}

		// Cast conditions met
		mActive = true;
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Holy energy radiates from your hands...");
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.65f);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1);
		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				t++;
				Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
				Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0);
				if (t >= LUMINOUS_INFUSION_MAX_DURATION || !mActive) {
					mActive = false;
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, LUMINOUS_INFUSION_EXPIRATION_MESSAGE);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);

		putOnCooldown();

		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		if (EntityUtils.isUndead(le)) {
			// Passive damage to undead from every hit, regardless of active
			int damage = getAbilityScore() == 1 ? LUMINOUS_INFUSION_1_PASSIVE_DAMAGE
			             : LUMINOUS_INFUSION_2_PASSIVE_DAMAGE;
			event.setDamage(event.getDamage() + damage);
		}

		if (mActive) {
			mActive = false;
			if (EntityUtils.isUndead(le)) {
				// Active damage to undead
				event.setDamage(event.getDamage() + LUMINOUS_INFUSION_UNDEAD_DAMAGE);

				new BukkitRunnable() {
					// Need to get this when launching runnable, before the mob
					// has died
					Location loc = le.getLocation();

					@Override
					public void run() {
						if (le.isDead()) {
							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, LUMINOUS_INFUSION_RADIUS)) {
								EntityUtils.damageEntity(mPlugin, e, LUMINOUS_INFUSION_EXPLOSION_DAMAGE, mPlayer);
								mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 100, 0.05f, 0.05f, 0.05f, 0.3);
								mWorld.spawnParticle(Particle.FLAME, loc, 75, 0.05f, 0.05f, 0.05f, 0.3);
								mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, 0.85f, 1.1f);
							}
						}
					}

				}.runTaskLater(mPlugin, 1);
			} else {
				// Active damage to non-undead
				le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, LUMINOUS_INFUSION_WEAKNESS_DURATION,
				                                    LUMINOUS_INFUSION_WEAKNESS_LEVEL, true, false));
				le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, LUMINOUS_INFUSION_SLOWNESS_DURATION,
				                                    LUMINOUS_INFUSION_SLOWNESS_LEVEL, true, false));
			}
		}
		return true;
	}
}