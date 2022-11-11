package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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

	private final int mDOTDuration;

	public WitheringGaze(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Withering Gaze");
		mInfo.mScoreboardId = "WitheringGaze";
		mInfo.mShorthandName = "WG";
		mInfo.mDescriptions.add("Sprint left-clicking unleashes a 9 block long cone in the direction the player is facing. " +
			                        "Enemies in its path are stunned for 3 seconds (elites and bosses are given 100% Slowness instead) " +
			                        "and dealt 1 damage every half second for 6 seconds. Cooldown: 30s.");
		mInfo.mDescriptions.add("Your damage over time lasts for 8 seconds. Cooldown: 20s.");
		mInfo.mLinkedSpell = ClassAbility.WITHERING_GAZE;
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, isLevelOne() ? WITHERING_GAZE_1_COOLDOWN : WITHERING_GAZE_2_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.WITHER_ROSE, 1);
		mDOTDuration = CharmManager.getExtraDuration(player, CHARM_DOT) + (isLevelOne() ? WITHERING_GAZE_DOT_DURATION_1 : WITHERING_GAZE_DOT_DURATION_2);
	}

	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		if (player == null) {
			return;
		}
		Location loc = player.getLocation().add(0, 0.65, 0); // the Y height is higher so that the skill doesn't get stomped by half slabs
		Vector direction = loc.getDirection().setY(0).normalize();
		World world = player.getWorld();
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

				Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(player).add(0, -mDamageRange, 0), 2 * mDamageRange, mDamageRange, Math.toRadians(ANGLE));
				for (LivingEntity e : hitbox.getHitMobs()) {
					if (EntityUtils.isElite(e) || EntityUtils.isBoss(e) || ((e instanceof Player p) && AbilityManager.getManager().isPvPEnabled(p))) {
						EntityUtils.applySlow(mPlugin, WITHERING_GAZE_STUN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_STUN), 1.0, e);
					} else {
						EntityUtils.applyStun(mPlugin, WITHERING_GAZE_STUN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_STUN), e);
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

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && ItemUtils.isHoe(mHand);
	}
}
