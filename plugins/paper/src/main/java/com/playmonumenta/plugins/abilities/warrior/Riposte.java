package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;



public class Riposte extends Ability {

	private static final int RIPOSTE_1_COOLDOWN = 15 * 20;
	private static final int RIPOSTE_2_COOLDOWN = 12 * 20;
	private static final int RIPOSTE_SWORD_DURATION = 2 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	private @Nullable BukkitRunnable mSwordTimer = null;

	public Riposte(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Riposte");
		mInfo.mLinkedSpell = ClassAbility.RIPOSTE;
		mInfo.mScoreboardId = "Obliteration";
		mInfo.mShorthandName = "Rip";
		mInfo.mDescriptions.add("While wielding a sword or axe, you block a melee attack that would have hit you. Cooldown: 15s.");
		mInfo.mDescriptions.add("Cooldown lowered to 12s and if you block an attack with Riposte's effect while holding a sword, your next sword attack within 2s deals double damage. If you block with Riposte's effect while holding an axe, the attacking mob is stunned for 3s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? RIPOSTE_1_COOLDOWN : RIPOSTE_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!isTimerActive()
			    && source != null
			    && event.getType() == DamageType.MELEE
			    && !event.isBlocked()
			    && mPlayer != null) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isAxe(mainHand) || ItemUtils.isSword(mainHand)) {
				MovementUtils.knockAway(mPlayer, source, RIPOSTE_KNOCKBACK_SPEED, true);
				if (getAbilityScore() > 1) {
					if (ItemUtils.isSword(mainHand)) {
						if (mSwordTimer == null) {
							mSwordTimer = new BukkitRunnable() {
								int mTimer = 0;

								@Override
								public void run() {
									if (mTimer >= RIPOSTE_SWORD_DURATION) {
										this.cancel();
										mSwordTimer = null;
										return;
									}
									mTimer += 5;
								}
							};
						}
						mSwordTimer.runTaskTimer(mPlugin, 0, 5);

					} else if (ItemUtils.isAxe(mainHand)) {
						EntityUtils.applyStun(mPlugin, RIPOSTE_AXE_DURATION, source);
					}
				}

				World world = mPlayer.getWorld();
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.8f);
				Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation().add(0, 1, 0), source.getLocation().add(0, source.getHeight() / 2, 0));
				Location loc = mPlayer.getLocation().add(0, 1, 0).subtract(dir);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT, loc, 75, 0.1, 0.1, 0.1, 0.6).spawnAsPlayerActive(mPlayer);
				putOnCooldown();
				event.setCancelled(true);
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && mPlayer != null) {
			if (ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
				if (mSwordTimer != null && !mSwordTimer.isCancelled()) {
					event.setDamage(event.getDamage() * 2);
					mSwordTimer.cancel();
					mSwordTimer = null;
				}
			}
		}
		return false; // prevents multiple applications itself by clearing mSwordTimer
	}

}
