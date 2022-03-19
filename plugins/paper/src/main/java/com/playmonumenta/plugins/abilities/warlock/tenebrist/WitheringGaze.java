package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
	private static final String DOT_EFFECT_NAME = "WitheringGazeDamageOverTimeEffect";

	private final int mDOTDuration;

	public WitheringGaze(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Withering Gaze");
		mInfo.mScoreboardId = "WitheringGaze";
		mInfo.mShorthandName = "WG";
		mInfo.mDescriptions.add("Sprint left-clicking unleashes a 9 block long cone in the direction the player is facing. Enemies in its path are stunned for 3 seconds (elites and bosses are given 30% Slowness instead) and dealt 1 damage every half second for 6 seconds. Cooldown: 30s.");
		mInfo.mDescriptions.add("Your damage over time lasts for 8 seconds. Cooldown: 20s.");
		mInfo.mLinkedSpell = ClassAbility.WITHERING_GAZE;
		mInfo.mCooldown = getAbilityScore() == 1 ? WITHERING_GAZE_1_COOLDOWN : WITHERING_GAZE_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.WITHER_ROSE, 1);
		mDOTDuration = getAbilityScore() == 1 ? WITHERING_GAZE_DOT_DURATION_1 : WITHERING_GAZE_DOT_DURATION_2;
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
		new BukkitRunnable() {
			double mT = 0;
			double mDamageRange = 1.15;
			double mR = 1;

			@Override
			public void run() {

				mT += 1;
				Vector vec;
				for (double degree = 0; degree < 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mR, 0, FastUtils.sin(radian1) * mR);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					new PartialParticle(Particle.SPELL_WITCH, l, 3, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_MOB, l, 3, 0.15, 0.15, 0.15, 0).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 2, 0.15, 0.15, 0.15, 0.05).spawnAsPlayerActive(mPlayer);
				}
				mR += 0.55;

				for (Entity e : player.getNearbyEntities(mDamageRange, mDamageRange * 2, mDamageRange)) {
					if (EntityUtils.isHostileMob(e)) {
						Vector eVec = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
						if (direction.dot(eVec) > 0.4) {
							LivingEntity le = (LivingEntity) e;
							if (EntityUtils.isElite(le) || EntityUtils.isBoss(le) || ((e instanceof Player) && AbilityManager.getManager().isPvPEnabled((Player)e))) {
								EntityUtils.applySlow(mPlugin, WITHERING_GAZE_STUN_DURATION, 0.3, le);
							} else {
								EntityUtils.applyStun(mPlugin, WITHERING_GAZE_STUN_DURATION, le);
							}
							mPlugin.mEffectManager.addEffect(le, DOT_EFFECT_NAME, new CustomDamageOverTime(mDOTDuration, WITHERING_GAZE_DOT_DAMAGE, WITHERING_GAZE_DOT_PERIOD, mPlayer, null, Particle.SQUID_INK));
						}
					}
				}

				mDamageRange += 1;
				loc.add(direction.clone().multiply(0.75));
				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
				}

				if (mT >= 9) {
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
