package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.BladeDanceCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class BladeDance extends Ability {

	private static final int DANCE_1_DAMAGE = 6;
	private static final int DANCE_2_DAMAGE = 9;
	private static final double SLOWNESS_AMPLIFIER = 1;
	private static final int SLOW_DURATION_1 = 2 * 20;
	private static final int SLOW_DURATION_2 = (int) (2.5 * 20);
	private static final int DANCE_RADIUS = 5;
	private static final float DANCE_KNOCKBACK_SPEED = 0.2f;
	private static final int INVULN_DURATION = 15;
	private static final int COOLDOWN_1 = 18 * 20;
	private static final int COOLDOWN_2 = 16 * 20;

	public static final String CHARM_DAMAGE = "Blade Dance Damage";
	public static final String CHARM_ROOT = "Blade Dance Root Duration";
	public static final String CHARM_RESIST = "Blade Dance Resistance Duration";
	public static final String CHARM_COOLDOWN = "Blade Dance Cooldown";
	public static final String CHARM_RADIUS = "Blade Dance Radius";

	public static final AbilityInfo<BladeDance> INFO =
		new AbilityInfo<>(BladeDance.class, "Blade Dance", BladeDance::new)
			.linkedSpell(ClassAbility.BLADE_DANCE)
			.scoreboardId("BladeDance")
			.shorthandName("BD")
			.actionBarColor(TextColor.color(150, 0, 0))
			.descriptions(
				String.format("When holding two swords, press the drop key to enter a defensive stance, " +
					              "parrying all attacks and becoming invulnerable for 0.75 seconds. " +
					              "Afterwards, unleash a powerful attack that deals %s melee damage to enemies in a %s block radius. " +
					              "Damaged enemies are rooted for %s seconds. Cooldown: %ss.",
					DANCE_1_DAMAGE,
					DANCE_RADIUS,
					SLOW_DURATION_1 / 20,
					COOLDOWN_1 / 20
				),
				String.format("The area attack now deals %s damage and roots for %ss. Cooldown: %ss.",
					DANCE_2_DAMAGE,
					SLOW_DURATION_2 / 20.0,
					COOLDOWN_2 / 20))
			.simpleDescription("Damage nearby mobs and become immune to damage for a short time.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BladeDance::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.STRING);


	private final double mDamage;
	private final int mSlowDuration;
	private boolean mIsActive = false;

	private final BladeDanceCS mCosmetic;

	public BladeDance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DANCE_1_DAMAGE : DANCE_2_DAMAGE);
		mSlowDuration = CharmManager.getDuration(player, CHARM_ROOT, (isLevelOne() ? SLOW_DURATION_1 : SLOW_DURATION_2));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BladeDanceCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, DANCE_RADIUS);

		World world = mPlayer.getWorld();
		mCosmetic.danceStart(mPlayer, world, mPlayer.getLocation());

		mIsActive = true;
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 1;
				Location loc = mPlayer.getLocation();
				mCosmetic.danceTick(mPlayer, world, loc, mTicks, radius);

				if (mTicks >= CharmManager.getDuration(mPlayer, CHARM_RESIST, INVULN_DURATION)) {
					mIsActive = false;

					mCosmetic.danceEnd(mPlayer, world, loc, radius);

					Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), radius);
					for (LivingEntity mob : hitbox.getHitMobs()) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
						MovementUtils.knockAway(mPlayer, mob, DANCE_KNOCKBACK_SPEED, true);

						if (!EntityUtils.isBoss(mob)) {
							EntityUtils.applySlow(mPlugin, mSlowDuration, SLOWNESS_AMPLIFIER, mob);
						}

						mCosmetic.danceHit(mPlayer, world, mob, mob.getLocation().add(0, 1, 0));
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		putOnCooldown();
		return true;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mIsActive && event.getType().isDefendable()) {
			event.setFlatDamage(0);
			event.setCancelled(true);
		}
	}
}
