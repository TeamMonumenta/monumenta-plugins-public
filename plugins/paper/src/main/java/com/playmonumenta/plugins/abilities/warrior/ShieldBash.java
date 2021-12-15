package com.playmonumenta.plugins.abilities.warrior;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;



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

	public ShieldBash(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Shield Bash");
		mInfo.mLinkedSpell = ClassAbility.SHIELD_BASH;
		mInfo.mScoreboardId = "ShieldBash";
		mInfo.mShorthandName = "SB";
		mInfo.mDescriptions.add("Block while looking at an enemy within 4 blocks to deal 5 damage, stun for 1 second, and taunt. Elites and bosses are rooted instead of stunned. Cooldown: 8s.");
		mInfo.mDescriptions.add("Additionally, apply damage, stun, and taunt to all enemies in a 2 block radius from the enemy you are looking at.");
		mInfo.mCooldown = SHIELD_BASH_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.IRON_DOOR, 1);
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer != null && mPlayer.isHandRaised()) {
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
									bash(mob, damage);
								} else {
									for (LivingEntity le : EntityUtils.getNearbyMobs(mob.getLocation(), SHIELD_BASH_2_RADIUS)) {
										bash(le, damage);
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

	private void bash(LivingEntity le, int damage) {
		EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, SHIELD_BASH_STUN, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, SHIELD_BASH_STUN, le);
		}
		if (le instanceof Mob mob) {
			mob.setTarget(mPlayer);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !ItemUtils.isSomeBow(mainHand) && (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD);
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return ShieldBashCooldownEnchantment.class;
	}
}
