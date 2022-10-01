package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class ShieldBash extends Ability {

	private static final int SHIELD_BASH_DAMAGE = 5;
	private static final int SHIELD_BASH_STUN = 20 * 1;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 8;
	private static final int SHIELD_BASH_2_RADIUS = 2;
	private static final int SHIELD_BASH_RANGE = 4;
	private static final int ENHANCEMENT_BLOCKING_DURATION = 10;

	public static final String CHARM_DAMAGE = "Shield Bash Damage";
	public static final String CHARM_DURATION = "Shield Bash Duration";
	public static final String CHARM_COOLDOWN = "Shield Bash Cooldown";
	public static final String CHARM_RADIUS = "Shield Bash Radius";
	public static final String CHARM_RANGE = "Shield Bash Range";

	private boolean mIsEnhancementUsed = true;

	public ShieldBash(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Shield Bash");
		mInfo.mLinkedSpell = ClassAbility.SHIELD_BASH;
		mInfo.mScoreboardId = "ShieldBash";
		mInfo.mShorthandName = "SB";
		mInfo.mDescriptions.add("Block while looking at an enemy within 4 blocks to deal 5 melee damage, stun for 1 second, and taunt. Elites and bosses are rooted instead of stunned. Cooldown: 8s.");
		mInfo.mDescriptions.add("Additionally, apply damage, stun, and taunt to all enemies in a 2 block radius from the enemy you are looking at.");
		mInfo.mDescriptions.add("Blocking damage with a shield within 0.5s of beginning to block refreshes 50% of this skill's cooldown.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, SHIELD_BASH_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.IRON_DOOR, 1);
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		// This timer makes sure that the player actually blocked instead of some other right click interaction
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer != null && mPlayer.isHandRaised()) {
					// Normal Shield bash cast
					if (!isTimerActive()) {
						Location eyeLoc = mPlayer.getEyeLocation();
						Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), (int) CharmManager.getRadius(mPlayer, CHARM_RANGE, SHIELD_BASH_RANGE));
						ray.mThroughBlocks = false;
						ray.mThroughNonOccluding = false;

						RaycastData data = ray.shootRaycast();

						List<LivingEntity> mobs = data.getEntities();
						if (mobs != null && !mobs.isEmpty()) {
							World world = mPlayer.getWorld();
							for (LivingEntity mob : mobs) {
								if (mob.isValid() && !mob.isDead() && EntityUtils.isHostileMob(mob)) {
									Location mobLoc = mob.getEyeLocation();
									new PartialParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
									new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
									new PartialParticle(Particle.CLOUD, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(mPlayer);
									world.playSound(eyeLoc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 1);
									world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);

									bash(mob, mInfo.mLinkedSpell);
									if (isLevelTwo()) {
										for (LivingEntity le : EntityUtils.getNearbyMobs(mob.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, SHIELD_BASH_2_RADIUS), mob)) {
											bash(le, ClassAbility.SHIELD_BASH_AOE);
										}
									}

									if (isEnhanced()) {
										mIsEnhancementUsed = false;
									}
									putOnCooldown();
									break;
								}
							}
						}
					}
				}
				this.cancel();
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If:
		// isEnhanced,
		// Player has blocked within the parry (0.5 seconds)
		// Event has been successfully blockedbyshield
		// And ShieldBash is on CD...
		if (mPlayer != null) {
			if (isEnhanced() && mPlayer.getHandRaisedTime() < ENHANCEMENT_BLOCKING_DURATION && event.isBlockedByShield() && isTimerActive() && !mIsEnhancementUsed) {
				// Reduce cooldown by half of shield bash's CD.
				mPlugin.mTimers.updateCooldown(mPlayer, mInfo.mLinkedSpell, getModifiedCooldown() / 2);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 2);

				mIsEnhancementUsed = true;
				// mPlayer.sendMessage("Shield bash CD reduced!");
			}
		}
	}

	private void bash(LivingEntity le, ClassAbility ca) {
		DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_BASH_DAMAGE), ca, true, true);
		int duration = SHIELD_BASH_STUN + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, duration, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, duration, le);
		}
		if (le instanceof Mob mob) {
			mob.setTarget(mPlayer);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !ItemUtils.isShootableItem(mainHand) && (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD);
	}
}
