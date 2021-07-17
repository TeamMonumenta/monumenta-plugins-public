package com.playmonumenta.plugins.abilities.warrior;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;



public class Riposte extends Ability {
	public static class RiposteCooldownEnchantment extends BaseAbilityEnchantment {
		public RiposteCooldownEnchantment() {
			super("Riposte Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final int RIPOSTE_1_COOLDOWN = 12 * 20;
	private static final int RIPOSTE_2_COOLDOWN = 10 * 20;
	private static final int RIPOSTE_SWORD_DURATION = 2 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	private BukkitRunnable mSwordTimer = null;

	public Riposte(Plugin plugin, Player player) {
		super(plugin, player, "Riposte");
		mInfo.mLinkedSpell = ClassAbility.RIPOSTE;
		mInfo.mScoreboardId = "Obliteration";
		mInfo.mShorthandName = "Rip";
		mInfo.mDescriptions.add("While wielding a sword or axe, you block a melee attack that would have hit you. Cooldown: 12s.");
		mInfo.mDescriptions.add("Cooldown lowered to 10s and if you block an attack with Riposte's effect while holding a sword, your next sword attack within 2s deals double damage. If you block with Riposte's effect while holding an axe, the attacking mob is stunned for 3s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? RIPOSTE_1_COOLDOWN : RIPOSTE_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (!isTimerActive()) {
			if (!AbilityUtils.isBlocked(event)) {
				LivingEntity damager = (LivingEntity) event.getDamager();
				if (event.getCause() == DamageCause.ENTITY_ATTACK
						 && !(damager instanceof Guardian)) {
					ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
					MovementUtils.knockAway(mPlayer, damager, RIPOSTE_KNOCKBACK_SPEED);

					if (ItemUtils.isAxe(mainHand) || ItemUtils.isSword(mainHand)) {
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
								if (!EntityUtils.isBoss(damager)) {
									EntityUtils.applyStun(mPlugin, RIPOSTE_AXE_DURATION, damager);
								}
							}
						}

						World world = mPlayer.getWorld();
						world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
						world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.8f);
						Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation().add(0, 1, 0), damager.getLocation().add(0, damager.getHeight() / 2, 0));
						Location loc = mPlayer.getLocation().add(0, 1, 0).subtract(dir);
						world.spawnParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001);
						world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1);
						world.spawnParticle(Particle.CRIT, loc, 75, 0.1, 0.1, 0.1, 0.6);
						int cooldown = getAbilityScore() == 1 ? RIPOSTE_1_COOLDOWN : RIPOSTE_2_COOLDOWN;
						putOnCooldown();
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		Player player = (Player) event.getDamager();
		if (ItemUtils.isSword(player.getInventory().getItemInMainHand())) {
			if (mSwordTimer != null && !mSwordTimer.isCancelled()) {
				event.setDamage(event.getDamage() * 2);
				mSwordTimer.cancel();
				mSwordTimer = null;
			}
		}

		return true;
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return RiposteCooldownEnchantment.class;
	}
}