package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.LucidRendBoss;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellLucidRendSlash extends SpellSlashAttack {
	private final ItemDisplay mSword;

	private final ParticlesList mCrossParticle;

	public final int mRecoverDuration;

	public SpellLucidRendSlash(Plugin plugin, LivingEntity boss, LucidRendBoss.Parameters parameters) {
		super(plugin, boss,
			parameters.COOLDOWN, parameters.DAMAGE, parameters.TELEGRAPH_DURATION, parameters.RADIUS, 270,
			270, parameters.ATTACK_NAME, parameters.RINGS, parameters.START_ANGLE, parameters.END_ANGLE,
			parameters.SPACING, Color.fromRGB(Integer.parseInt(parameters.START_HEX_COLOR, 16)), Color.fromRGB(Integer.parseInt(parameters.MID_HEX_COLOR, 16)), Color.fromRGB(Integer.parseInt(parameters.END_HEX_COLOR, 16)),
			false, false, parameters.FULL_ARC, parameters.HORIZONTAL_COLOR, new Vector(parameters.KB_X, parameters.KB_Y, parameters.KB_Z),
			parameters.KNOCK_AWAY, parameters.KBR_EFFECTIVENESS, parameters.FOLLOW_CASTER, false, parameters.HITBOX_SIZE,
			parameters.FORCED_PARTICLE_SIZE, parameters.DAMAGE_TYPE, parameters.SOUND_TELEGRAPH, parameters.SOUND_SLASH_START, parameters.SOUND_SLASH_TICK, parameters.SOUND_SLASH_END,
			false, 1, 10, false,
			parameters.MULTI_HIT, parameters.MULTIHIT_INTERVAL, parameters.RESPECT_IFRAMES);
		mSword = mBoss.getWorld().spawn(mBoss.getLocation(), ItemDisplay.class);
		EntityEquipment equipment = mBoss.getEquipment();
		if (equipment != null) {
			mSword.setItemStack(equipment.getItemInMainHand());
			equipment.setItemInMainHand(null);
		}
		mSword.setInterpolationDuration(5);
		mSword.setBrightness(new Display.Brightness(15, 15));
		mSword.setTransformation(new Transformation(
			new Vector3f(0, 0, 0),
			new AxisAngle4f(),
			new Vector3f(2, 2, 1),
			new AxisAngle4f())
		);
		mSword.setCustomNameVisible(false);
		EntityUtils.setRemoveEntityOnUnload(mSword);

		mBoss.addPassenger(mSword);

		mCrossParticle = parameters.CROSS_PARTICLE;

		// Jank
		mRecoverDuration = parameters.RECOVER_DURATION;
	}

	public void swordMatchRotation() {
		mSword.setInterpolationDelay(-1);
		Vector3f pitch = new Vector3f(0, 1, 0);
		Vector3f vec = new Vector3f(1, 0, 0);
		vec.rotateY((float) -Math.toRadians(mBoss.getYaw()));
		pitch.rotateAxis((float) Math.toRadians(mBoss.getPitch()), vec.x, vec.y, vec.z);
		mSword.setTransformation(new Transformation(
			new Vector3f(0, 0, 0),
			new AxisAngle4f((float) -Math.toRadians(mBoss.getYaw() + 90), pitch.x, pitch.y, pitch.z),
			new Vector3f(2, 2, 1),
			new AxisAngle4f())
		);
	}

	@Override
	public void run() {
		mSoundsTelegraph.play(mBoss.getLocation());

		transformSword(new Vector3f(), (float) -mStartAngle);
		// Jank

		EntityUtils.selfRoot(mBoss, mTelegraphDuration + mRecoverDuration);
		mBoss.setAI(false);

		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
					return;
				}

				Vector offset1 = VectorUtils.rotateYAxis(new Vector(1, 0.25, 0), mBoss.getYaw() + 90).multiply(mRadius);
				Vector offset2 = VectorUtils.rotateYAxis(new Vector(1, 0.25, 0), mBoss.getYaw() + 180).multiply(mRadius);
				mCrossParticle.spawn(mBoss, particle -> new PPLine(
						particle,
						mBoss.getLocation().add(offset1),
						mBoss.getLocation().subtract(offset1)
					)
				);
				mCrossParticle.spawn(mBoss, particle -> new PPLine(
						particle,
						mBoss.getLocation().add(offset2),
						mBoss.getLocation().subtract(offset2)
					)
				);

				// Slash Animation
				mSword.setInterpolationDuration(2);
				doSlash(selectAngle());

				// Finish Slash
				transformSword(new Vector3f(0.5f, -1, 0), (float) (-mEndAngle + mStartAngle));
				mSword.setInterpolationDuration(5);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.setAI(true), mRecoverDuration);
			}
		}.runTaskLater(mPlugin, mTelegraphDuration));
	}

	/**
	 * Rotate by Pitch
	 *
	 * @param translation transformation's *new* translation
	 * @param pitch       pitch in *degrees*!
	 */
	private void transformSword(Vector3f translation, float pitch) {
		mSword.setInterpolationDelay(-1);
		mSword.setTransformation(new Transformation(
			translation,
			mSword.getTransformation().getLeftRotation().rotateAxis((float) Math.toRadians(pitch), new Vector3f(0, 0, 1)),
			new Vector3f(2, 2, 1),
			new Quaternionf()
		));
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	public void removeSword() {
		mSword.remove();
	}
}
