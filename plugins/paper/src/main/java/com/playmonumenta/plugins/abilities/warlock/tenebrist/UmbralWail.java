package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.VectorUtils;


public class UmbralWail extends Ability {

	private static final Particle.DustOptions COLOR_BLACK = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final Particle.DustOptions COLOR_BLUE = new Particle.DustOptions(Color.fromRGB(0, 0, 77), 1.0f);

	private static final int COOLDOWN = 20 * 20;
	private static final int SPHERE_RADIUS = 2;
	private static final int MAX_DURATION = 20;
	private static final double MOVE_SPEED = 0.5;
	private static final int DURATION_1 = 5 * 20;
	private static final int DURATION_2 = 8 * 20;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 16;

	private final int mDamage;
	private final int mDuration;

	public UmbralWail(Plugin plugin, Player player) {
		super(plugin, player, "Umbral Wail");
		mInfo.mLinkedSpell = ClassAbility.UMBRAL_WAIL;
		mInfo.mScoreboardId = "UmbralWail";
		mInfo.mShorthandName = "UW";
		mInfo.mDescriptions.add("Right-click while sprinting and holding a scythe to unleash a chilling shadow in the direction you are looking. This shadow travels for 10 blocks and deals 10 damage to all mobs it passes through, afflicting them with 5s of Silence. Silence prevents mobs from using spells, but does not work on Bosses. Cooldown: 20s.");
		mInfo.mDescriptions.add("The damage done by the shadow is increased to 16, and the Silence duration is increased to 8s.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mDuration = getAbilityScore() == 1 ? DURATION_1 : DURATION_2;
	}

	@Override
	public void cast(Action action) {
		World world = mPlayer.getWorld();

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			final BoundingBox mBox = BoundingBox.of(mLoc, SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS);
			Vector mIncrement = mLoc.getDirection().multiply(MOVE_SPEED);
			List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, 12, mPlayer);

			int mTicks = 0;
			double mDegree = 0;

			@Override
			public void run() {
				mBox.shift(mIncrement);
				mLoc.add(mIncrement);
				Iterator<LivingEntity> mobIter = mMobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (mBox.overlaps(mob.getBoundingBox())) {
						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
						if (!EntityUtils.isBoss(mob)) {
							EntityUtils.applySilence(mPlugin, mDuration, mob);
						}
						mobIter.remove();
					}
				}

				mDegree += 12;
				Vector vec;
				for (int i = 0; i < 2; i++) {
					double radian1 = Math.toRadians(mDegree + (i * 180));
					vec = new Vector(FastUtils.cos(radian1) * 0.325, 0, FastUtils.sin(radian1) * 0.325);
					vec = VectorUtils.rotateXAxis(vec, -mLoc.getPitch() + 90);
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(vec);
					world.spawnParticle(Particle.REDSTONE, l, 7, 0.15, 0.15, 0.15, 0.15, COLOR_BLACK);
					world.spawnParticle(Particle.REDSTONE, l, 7, 0.15, 0.15, 0.15, 0.15, COLOR_BLUE);
					world.spawnParticle(Particle.REDSTONE, l, 10, SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS, 0.15, COLOR_BLACK);
					world.spawnParticle(Particle.REDSTONE, l, 10, SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS, COLOR_BLUE);
				}
				world.spawnParticle(Particle.REDSTONE, mLoc, 7, 0.35, 0.35, 0.35, 1, COLOR_BLACK);
				world.spawnParticle(Particle.REDSTONE, mLoc, 7, 0.35, 0.35, 0.35, 1, COLOR_BLUE);
				world.spawnParticle(Particle.REDSTONE, mLoc, 7, SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS, 1, COLOR_BLACK);
				world.spawnParticle(Particle.REDSTONE, mLoc, 7, SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS, 1, COLOR_BLUE);

				if (mTicks >= MAX_DURATION) {
					this.cancel();
				}

				mTicks++;
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FOX_SCREECH, 2.5f, 0.2f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FOX_SCREECH, 2.5f, 0.15f);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && InventoryUtils.isScytheItem(mHand);
	}

}
