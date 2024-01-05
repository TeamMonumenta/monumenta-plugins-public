package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.SiriusContagion;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellBlightedBolts extends Spell {
	private static final int COOLDOWN = 5 * 20;
	private static final int DAMAGE = 20;
	private static final int LIFESPAWN = 10 * 20;
	private static final double SPEED = 0.5;
	private static final double TURNRADIUS = Math.PI / 8;
	private static final int HITBOXLENGTH = 1;
	public static final int RADIUS = 5;
	public static final String BLIGHTEDBOLTTAG = "BLIGHTEDBOLTSCONATGION";
	public static final int BLIGHTEDBOLTCONTAGIONDURATION = 5 * 20;
	public static final int CONATGIONDAMAGE = 30;
	public static final int SAFEDURATION = 15 * 20;
	private boolean mOnCooldown;
	private Plugin mPlugin;
	private Sirius mSirius;
	private PassiveStarBlightConversion mConvertor;


	public SpellBlightedBolts(Plugin plugin, Sirius sirius, PassiveStarBlightConversion convertor) {
		mPlugin = plugin;
		mSirius = sirius;
		mConvertor = convertor;
		mOnCooldown = false;
	}

	private void bullet(Player target) {
		//spawn animation once model is done
		new BukkitRunnable() {
			//Start point of projectiles
			final Location mLocation = mSirius.mBoss.getLocation();
			final BoundingBox mHitbox = BoundingBox.of(mLocation, HITBOXLENGTH / 2.0f, HITBOXLENGTH / 2.0f, HITBOXLENGTH / 2.0f);
			final Player mTarget = target;
			final Vector mBaseDir = mTarget.getLocation().clone().subtract(mLocation).toVector().normalize();
			int mTicks = 0;
			Vector mDirection = VectorUtils.rotateTargetDirection(
				mBaseDir, 0, 25);

			@Override
			public void run() {
				mTicks++;
				if (mTicks == 1) {
					mSirius.mBoss.getWorld().playSound(mLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 2);
				}
				if (mTarget != null) {
					Vector newDirection = mTarget.getEyeLocation().subtract(mLocation).toVector();
					newDirection.normalize();
					if (!Double.isFinite(newDirection.getX())) {
						newDirection = mDirection;
					}
					double newAngle = Math.acos(Math.max(-1, Math.min(1, mDirection.dot(newDirection))));
					if (newAngle < TURNRADIUS) {
						mDirection = newDirection;
					} else {
						double halfEndpointDistance = FastUtils.sin(newAngle / 2);
						if (halfEndpointDistance != 0) {
							double scalar = (halfEndpointDistance + FastUtils.sin(TURNRADIUS - newAngle / 2)) / (2 * halfEndpointDistance);
							Vector newerDirection = mDirection.clone().add(newDirection.subtract(mDirection).multiply(scalar)).normalize();
							if (Double.isFinite(newerDirection.getX())) {
								mDirection = newerDirection;
							}
						}
					}
				}
				if (!Double.isFinite(mDirection.getX())) {
					mDirection = new Vector(0, 1, 0);
				}
				Vector shift = mDirection.clone().multiply(SPEED);
				mLocation.add(shift);
				mHitbox.shift(shift);
				new PartialParticle(Particle.REDSTONE, mLocation, 5).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 0.9f)).delta(0.1).spawnAsBoss();
				Block block = mLocation.getBlock();
				if (!block.isLiquid() && mHitbox.overlaps(block.getBoundingBox())) {
					mConvertor.convertColumn(block.getX(), block.getZ());
				}
				for (Player player : PlayerUtils.playersInRange(mLocation, HITBOXLENGTH + 2, true)) {
					if (mHitbox.overlaps(player.getBoundingBox())) {
						hitaction(mSirius.mBoss.getWorld(), player, mLocation);
						this.cancel();
						return;
					}
				}

				if (mTicks > LIFESPAWN) {
					new PPExplosion(Particle.REDSTONE, mLocation).count(15).delta(2).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 0.9f)).spawnAsBoss();
					mConvertor.convertSphere(2, mLocation);
					this.cancel();
				}


			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		List<Player> pList = mSirius.getPlayersInArena(false);
		Collections.shuffle(pList);
		int shotCount = pList.size() / 3 + 1;
		List<Player> targets = new ArrayList<>();
		World world = mSirius.mBoss.getWorld();
		Location loc = mSirius.mBoss.getLocation();
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.7f, 2f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 1.2f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 0.6f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.3f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 0.4f, 1.8f);

		for (Player p : pList) {
			if (!AbilityUtils.isStealthed(p)) {
				targets.add(p);
			}
		}
		for (int i = 0; i < shotCount && i < targets.size(); i++) {
			bullet(targets.get(i));
		}

	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void hitaction(World world, Player p, Location hitloc) {
		//play some sounds
		world.playSound(hitloc, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 0.5f, 0.4f);
		world.playSound(hitloc, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.6f, 0.4f);
		world.playSound(hitloc, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 0.6f, 0.6f);
		world.playSound(hitloc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 0.6f, 1.4f);
		new PPExplosion(Particle.WAX_OFF, hitloc).count(20).spawnAsBoss();
		DamageUtils.damage((LivingEntity) mSirius.mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, "Starblight Infection");
		if (com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.getActiveEffect(p, SpellBlightedBolts.BLIGHTEDBOLTTAG) == null
			&& com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.getActiveEffect(p, "BlightProtection") == null) {
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, SpellBlightedBolts.BLIGHTEDBOLTTAG, new SiriusContagion(SpellBlightedBolts.BLIGHTEDBOLTCONTAGIONDURATION, SpellBlightedBolts.BLIGHTEDBOLTTAG));
		}
	}
}
