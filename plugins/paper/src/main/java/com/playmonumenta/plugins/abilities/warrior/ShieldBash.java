package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class ShieldBash extends Ability {

	private static final int SHIELD_BASH_DAMAGE = 8;
	private static final int SHIELD_BASH_STUN = 20;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 8;
	private static final int SHIELD_BASH_2_RADIUS = 3;
	private static final int SHIELD_BASH_RANGE = 4;
	private static final int ENHANCEMENT_BLOCKING_DURATION = 20;
	private static final double ENHANCEMENT_CDR = 0.5;

	public static final String CHARM_DAMAGE = "Shield Bash Damage";
	public static final String CHARM_DURATION = "Shield Bash Duration";
	public static final String CHARM_COOLDOWN = "Shield Bash Cooldown";
	public static final String CHARM_KNOCKBACK = "Shield Bash Knockback";
	public static final String CHARM_RADIUS = "Shield Bash Radius";
	public static final String CHARM_RANGE = "Shield Bash Range";
	public static final String CHARM_PARRY_DURATION = "Shield Bash Parry Duration";
	public static final String CHARM_CDR = "Shield Bash Cooldown Reduction";

	public static final AbilityInfo<ShieldBash> INFO =
		new AbilityInfo<>(ShieldBash.class, "Shield Bash", ShieldBash::new)
			.linkedSpell(ClassAbility.SHIELD_BASH)
			.scoreboardId("ShieldBash")
			.shorthandName("SB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Bash mobs with your shield, stunning and taunting them.")
			.cooldown(SHIELD_BASH_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.IRON_DOOR);

	private final double mRange;
	private final double mRadius;
	private final double mDamage;
	private final double mKnockback;
	private final int mStunDuration;
	private final int mParryDuration;
	private final double mCDR;

	private boolean mIsEnhancementUsed = true;
	private @Nullable CounterStrike mCounterStrike;

	private final ShieldBashCS mCosmetic;

	public ShieldBash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, SHIELD_BASH_RANGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, SHIELD_BASH_2_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_BASH_DAMAGE);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SHIELD_BASH_STUN);
		mParryDuration = CharmManager.getDuration(mPlayer, CHARM_PARRY_DURATION, ENHANCEMENT_BLOCKING_DURATION);
		mCDR = ENHANCEMENT_CDR + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CDR);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, 0.35f);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldBashCS());
		Bukkit.getScheduler().runTask(plugin, () -> {
			mCounterStrike = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, CounterStrike.class);
		});
	}

	@Override
	public void blockWithShieldEvent() {
		if (isOnCooldown() || mPlayer.isSneaking()) {
			return;
		}

		LivingEntity mob = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange);
		if (mob == null) {
			return;
		}

		Location mobLoc = mob.getEyeLocation();
		Location eyeLoc = mPlayer.getEyeLocation();
		World world = eyeLoc.getWorld();
		mCosmetic.onCast(mPlayer, world, eyeLoc, mobLoc);

		bash(mob, ClassAbility.SHIELD_BASH);
		if (isLevelTwo()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mob), mRadius);
			for (LivingEntity le : hitbox.getHitMobs(mob)) {
				mCosmetic.onHitSurroundingMobs(mPlayer, world, eyeLoc, le.getEyeLocation());
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
		if (isEnhanced() && !mIsEnhancementUsed && event.isBlockedByShield() && isOnCooldown() && mPlayer.getHandRaisedTime() < mParryDuration) {
			int newCooldown = (int) (getModifiedCooldown() * mCDR);
			mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.SHIELD_BASH, newCooldown);
			mCosmetic.onParry(mPlayer.getWorld(), mPlayer.getLocation());

			mIsEnhancementUsed = true;
		}
	}

	private void bash(LivingEntity le, ClassAbility ca) {
		DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, mDamage, ca, true, false);
		if (mKnockback != 0) {
			MovementUtils.knockAway(mPlayer, le, (float) mKnockback);
		}
		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, mStunDuration, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, mStunDuration, le);
		}
		if (le instanceof Mob mob) {
			if (mCounterStrike != null) {
				mCounterStrike.onTaunt(mob);
			}

			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mob.isDead() || !mob.isValid() || mT > mStunDuration * 2) {
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

	private static Description<ShieldBash> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addCustomTrigger("Shield Block")
			.addDashedLine()
			.addLine("Damage, taunt, and stun a target")
			.addLine("mob in front of you.")
			.addLine("(Elites/Bosses are rooted instead)")
			.addLine()
			.addStat("Damage: %d (m)")
				.statValues(stat(a -> a.mDamage, SHIELD_BASH_DAMAGE))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunDuration, SHIELD_BASH_STUN))
			.addStat("Range: %r")
				.statValues(stat(a -> a.mRange, SHIELD_BASH_RANGE))
			.addStat("Cooldown: %t")
				.statValues(cooldown(SHIELD_BASH_COOLDOWN))
			.addDashedLine();
	}

	private static Description<ShieldBash> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Shield Bash* now hits all").styles(UNDERLINED)
			.addLine("mobs near the target mob.")
			.addLine()
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, SHIELD_BASH_2_RADIUS))
			.addDashedLine();
	}

	private static Description<ShieldBash> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Blocking an attack within %t of")
				.statValues(stat(a -> a.mParryDuration, ENHANCEMENT_BLOCKING_DURATION))
			.addLine("raising your shield reduces")
			.addLine("*Shield Bash*'s cooldown.").styles(UNDERLINED)
			.addLine()
			.addStat("Cooldown Reduction: %p")
				.statValues(stat(a -> a.mCDR, ENHANCEMENT_CDR))
			.addDashedLine();
	}
}
