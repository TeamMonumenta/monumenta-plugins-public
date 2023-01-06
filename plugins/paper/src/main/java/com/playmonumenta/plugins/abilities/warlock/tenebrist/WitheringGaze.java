package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class WitheringGaze extends Ability {

	private static final int WITHERING_GAZE_STUN_DURATION = 3 * 20;
	private static final int WITHERING_GAZE_DOT_DURATION_1 = 6 * 20;
	private static final int WITHERING_GAZE_DOT_DURATION_2 = 8 * 20;
	private static final int WITHERING_GAZE_DOT_PERIOD = 10;
	private static final int WITHERING_GAZE_DOT_DAMAGE = 1;
	private static final int WITHERING_GAZE_1_COOLDOWN = 20 * 30;
	private static final int WITHERING_GAZE_2_COOLDOWN = 20 * 20;
	private static final int WITHERING_GAZE_RANGE = 9;
	private static final double ANGLE = 65;
	private static final String DOT_EFFECT_NAME = "WitheringGazeDamageOverTimeEffect";

	public static final String CHARM_STUN = "Withering Gaze Stun Duration";
	public static final String CHARM_COOLDOWN = "Withering Gaze Cooldown";
	public static final String CHARM_RANGE = "Withering Gaze Range";
	public static final String CHARM_DOT = "Withering Gaze Dot Duration";
	public static final String CHARM_DAMAGE = "Withering Gaze Damage";

	public static final AbilityInfo<WitheringGaze> INFO =
		new AbilityInfo<>(WitheringGaze.class, "Withering Gaze", WitheringGaze::new)
			.linkedSpell(ClassAbility.WITHERING_GAZE)
			.scoreboardId("WitheringGaze")
			.shorthandName("WG")
			.descriptions(
				("Pressing the drop key while not sneaking and holding a scythe unleashes a %s block long cone in the direction the player is facing. " +
					 "Enemies in its path are stunned for %s seconds (elites and bosses are given 100%% Slowness instead) " +
					 "and dealt %s damage every half second for %s seconds. Cooldown: %ss.")
					.formatted(WITHERING_GAZE_RANGE, StringUtils.ticksToSeconds(WITHERING_GAZE_STUN_DURATION), WITHERING_GAZE_DOT_DAMAGE,
						StringUtils.ticksToSeconds(WITHERING_GAZE_DOT_DURATION_1), StringUtils.ticksToSeconds(WITHERING_GAZE_1_COOLDOWN)),
				"Your damage over time lasts for %s seconds. Cooldown: %ss."
					.formatted(StringUtils.ticksToSeconds(WITHERING_GAZE_DOT_DURATION_2), StringUtils.ticksToSeconds(WITHERING_GAZE_2_COOLDOWN)))
			.cooldown(WITHERING_GAZE_1_COOLDOWN, WITHERING_GAZE_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WitheringGaze::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.WITHER_ROSE, 1));

	private final int mDOTDuration;

	public WitheringGaze(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDOTDuration = CharmManager.getDuration(player, CHARM_DOT, (isLevelOne() ? WITHERING_GAZE_DOT_DURATION_1 : WITHERING_GAZE_DOT_DURATION_2));
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		Location loc = mPlayer.getLocation().add(0, 0.65, 0); // the Y height is higher so that the skill doesn't get stomped by half slabs
		Vector direction = loc.getDirection().setY(0).normalize();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1f, 1f);
		double radius = CharmManager.getRadius(mPlayer, CHARM_RANGE, WITHERING_GAZE_RANGE);
		new BukkitRunnable() {
			double mT = 0;
			double mDamageRange = 1.15;
			double mR = 1;

			@Override
			public void run() {

				for (double degree = 0; degree < 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					Vector vec = new Vector(FastUtils.cos(radian1) * mR, 0, FastUtils.sin(radian1) * mR);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					new PartialParticle(Particle.SPELL_WITCH, l, 3, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_MOB, l, 3, 0.15, 0.15, 0.15, 0).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 2, 0.15, 0.15, 0.15, 0.05).spawnAsPlayerActive(mPlayer);
				}
				mR += 0.55;

				Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mDamageRange, 0), 2 * mDamageRange, mDamageRange, Math.toRadians(ANGLE));
				int stunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN, WITHERING_GAZE_STUN_DURATION);
				for (LivingEntity e : hitbox.getHitMobs()) {
					if (EntityUtils.isElite(e) || EntityUtils.isBoss(e)) {
						EntityUtils.applySlow(mPlugin, stunDuration, 1.0, e);
					} else {
						EntityUtils.applyStun(mPlugin, stunDuration, e);
					}
					mPlugin.mEffectManager.addEffect(e, DOT_EFFECT_NAME, new CustomDamageOverTime(mDOTDuration, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, WITHERING_GAZE_DOT_DAMAGE), WITHERING_GAZE_DOT_PERIOD, mPlayer, null));
				}

				mDamageRange += 1;
				loc.add(direction.clone().multiply(0.75));
				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
				}

				mT += 1;
				if (mT >= radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

}
