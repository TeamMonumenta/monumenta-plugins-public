package com.playmonumenta.plugins.abilities.warrior;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;

public class ShieldBash extends Ability {

	public static class ShieldBashCooldownEnchantment extends BaseAbilityEnchantment {
		public ShieldBashCooldownEnchantment() {
			super("Shield Bash Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	public static class ShieldBashDamageEnchantment extends BaseAbilityEnchantment {
		public ShieldBashDamageEnchantment() {
			super("Shield Bash Damage", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final int SHIELD_BASH_DAMAGE = 5;
	private static final int SHIELD_BASH_STUN = 20 * 1;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 8;
	private static final int SHIELD_BASH_2_RADIUS = 2;
	private static final int SHIELD_BASH_RANGE = 4;

	public ShieldBash(Plugin plugin, Player player) {
		super(plugin, player, "Shield Bash");
		mInfo.mLinkedSpell = Spells.SHIELD_BASH;
		mInfo.mScoreboardId = "ShieldBash";
		mInfo.mShorthandName = "SB";
		mInfo.mDescriptions.add("Block while looking at an enemy within 4 blocks to deal 5 damage and stun them for 1 second. Cooldown: 8s.");
		mInfo.mDescriptions.add("Additionally, deal 5 damage and stun for 1 second all enemies in a 2 block radius from the enemy you are looking at.");
		mInfo.mCooldown = SHIELD_BASH_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer.isHandRaised()) {
					mInfo.mCooldown = (int) ShieldBashCooldownEnchantment.getCooldown(mPlayer, SHIELD_BASH_COOLDOWN, ShieldBashCooldownEnchantment.class);
					int damage = (int) ShieldBashDamageEnchantment.getExtraDamage(mPlayer, ShieldBashDamageEnchantment.class) + SHIELD_BASH_DAMAGE;
					Location eyeLoc = mPlayer.getEyeLocation();
					Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), SHIELD_BASH_RANGE);
					ray.mThroughBlocks = false;
					ray.mThroughNonOccluding = false;

					RaycastData data = ray.shootRaycast();

					List<LivingEntity> mobs = data.getEntities();
					if (mobs != null && !mobs.isEmpty()) {
						World world = mPlayer.getWorld();
						for (LivingEntity mob : mobs) {
							if (mob.isValid() && !mob.isDead() && EntityUtils.isHostileMob(mob)) {
								Location mobLoc = mob.getEyeLocation();
								world.spawnParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25);
								world.spawnParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25);
								world.spawnParticle(Particle.CLOUD, mobLoc, 5, 0.15, 0.5, 0.15, 0);
								world.playSound(eyeLoc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 1);
								world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);

								if (getAbilityScore() == 1) {
									EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
									if (EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)) {
										mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SHIELD_BASH_STUN, 6));
									} else {
										EntityUtils.applyStun(mPlugin, SHIELD_BASH_STUN, mob);
									}
								} else {
									for (LivingEntity le : EntityUtils.getNearbyMobs(mob.getLocation(), SHIELD_BASH_2_RADIUS)) {
										EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
										if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
											le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SHIELD_BASH_STUN, 6));
										} else {
											EntityUtils.applyStun(mPlugin, SHIELD_BASH_STUN, le);
										}
									}
								}

								putOnCooldown();
								break;
							}
						}
					}
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD;
	}
}
