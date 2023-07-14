package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ShieldBashCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class ShieldBash extends Ability {

	private static final int SHIELD_BASH_DAMAGE = 8;
	private static final int SHIELD_BASH_STUN = 20 * 1;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 8;
	private static final int SHIELD_BASH_2_RADIUS = 3;
	private static final int SHIELD_BASH_RANGE = 4;
	private static final int ENHANCEMENT_BLOCKING_DURATION = 20;
	private static final double ENHANCEMENT_CDR = 0.5;

	public static final String CHARM_DAMAGE = "Shield Bash Damage";
	public static final String CHARM_DURATION = "Shield Bash Duration";
	public static final String CHARM_COOLDOWN = "Shield Bash Cooldown";
	public static final String CHARM_RADIUS = "Shield Bash Radius";
	public static final String CHARM_RANGE = "Shield Bash Range";
	public static final String CHARM_PARRY_DURATION = "Shield Bash Parry Duration";
	public static final String CHARM_CDR = "Shield Bash Cooldown Reduction";

	public static final AbilityInfo<ShieldBash> INFO =
		new AbilityInfo<>(ShieldBash.class, "Shield Bash", ShieldBash::new)
			.linkedSpell(ClassAbility.SHIELD_BASH)
			.scoreboardId("ShieldBash")
			.shorthandName("SB")
			.descriptions(
				"Block while looking at an enemy within 4 blocks and not sneaking to deal 8 melee damage, stun for 1 second, and taunt the targeted enemy. " +
					"Elites and bosses are rooted instead of stunned. Cooldown: 8s.",
				"Additionally, apply damage, stun, and taunt to all enemies in a 3 block radius from the enemy you are looking at.",
				"Blocking damage with a shield within 1s of beginning to block refreshes 50% of this skill's cooldown.")
			.simpleDescription("Bash mobs with your shield, stunning and taunting them.")
			.cooldown(SHIELD_BASH_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.IRON_DOOR);

	private boolean mIsEnhancementUsed = true;

	private final ShieldBashCS mCosmetic;

	public ShieldBash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldBashCS());
	}

	@Override
	public void blockWithShieldEvent() {
		if (isOnCooldown() || mPlayer.isSneaking()) {
			return;
		}

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, SHIELD_BASH_RANGE);
		LivingEntity mob = EntityUtils.getHostileEntityAtCursor(mPlayer, range);

		if (mob == null) {
			return;
		}

		Location mobLoc = mob.getEyeLocation();
		Location eyeLoc = mPlayer.getEyeLocation();
		World world = eyeLoc.getWorld();
		mCosmetic.onCast(mPlayer, world, eyeLoc, mobLoc);

		bash(mob, ClassAbility.SHIELD_BASH);
		if (isLevelTwo()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mob), CharmManager.getRadius(mPlayer, CHARM_RADIUS, SHIELD_BASH_2_RADIUS));
			for (LivingEntity le : hitbox.getHitMobs(mob)) {
				bash(le, ClassAbility.SHIELD_BASH_AOE);
			}
		}

		if (isEnhanced()) {
			mIsEnhancementUsed = false;
		}
		putOnCooldown();
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If:
		// isEnhanced,
		// Player has blocked within the parry
		// Event has been successfully blockedbyshield
		// And ShieldBash is on CD...
		if (isEnhanced() && !mIsEnhancementUsed && event.isBlockedByShield() && isOnCooldown() && mPlayer.getHandRaisedTime() < CharmManager.getDuration(mPlayer, CHARM_PARRY_DURATION, ENHANCEMENT_BLOCKING_DURATION)) {
			int newCooldown = (int) (getModifiedCooldown() * (ENHANCEMENT_CDR + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CDR)));
			mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.SHIELD_BASH, newCooldown);
			mCosmetic.onParry(mPlayer.getWorld(), mPlayer.getLocation());

			mIsEnhancementUsed = true;
		}
	}

	private void bash(LivingEntity le, ClassAbility ca) {
		DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_BASH_DAMAGE), ca, true, true);
		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SHIELD_BASH_STUN);
		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, duration, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, duration, le);
		}
		if (le instanceof Mob mob) {
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mob.isDead() || !mob.isValid() || mT > duration * 2) {
						this.cancel();
					} else if (!EntityUtils.isStunned(mob)) {
						EntityUtils.applyTaunt(mob, mPlayer, false);
						this.cancel();
					}

					mT++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

}
