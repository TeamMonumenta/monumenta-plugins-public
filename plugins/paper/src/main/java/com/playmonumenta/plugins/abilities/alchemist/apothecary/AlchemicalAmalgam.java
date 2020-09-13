package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Iterator;
import java.util.List;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class AlchemicalAmalgam extends Ability {

	private static final int AMALGAM_1_DAMAGE = 8;
	private static final int AMALGAM_2_DAMAGE = 16;
	private static final int AMALGAM_1_SHIELD = 2;
	private static final int AMALGAM_2_SHIELD = 3;
	private static final int AMALGAM_MAX_SHIELD = 12;
	private static final int AMALGAM_ABSORPTION_DURATION = 20 * 30;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int AMALGAM_MAX_DURATION = 20 * 2;
	private static final double AMALGAM_MOVE_SPEED = 0.25;
	private static final double AMALGAM_RADIUS = 1.5;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);

	private final int mDamage;
	private final int mShield;

	public AlchemicalAmalgam(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Alchemical Amalgam");
		mInfo.mScoreboardId = "Alchemical";
		mInfo.mShorthandName = "AAm";
		mInfo.mDescriptions.add("Shift left click with a Bow to shoot a mixture that deals 8 damage to every enemy touched and adds 2 absorption health to players (including yourself), lasting 30 seconds, maximum 12. After hitting a block or traveling 10 blocks, the mixture traces and returns to you, able to damage enemies and shield allies a second time. Cooldown: 30 seconds.");
		mInfo.mDescriptions.add("Absorption health added is increased to 3 and damage is increased to 16.");
		mInfo.mCooldown = 20 * 30;
		mInfo.mLinkedSpell = Spells.ALCHEMICAL_AMALGAM;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? AMALGAM_1_DAMAGE : AMALGAM_2_DAMAGE;
		mShield = getAbilityScore() == 1 ? AMALGAM_1_SHIELD : AMALGAM_2_SHIELD;
	}

	@Override
	public void cast(Action action) {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 1.25f);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);
		mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);

		AbsorptionUtils.addAbsorption(mPlayer, mShield, AMALGAM_MAX_SHIELD, AMALGAM_ABSORPTION_DURATION);
		putOnCooldown();

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			final BoundingBox mBox = BoundingBox.of(mLoc, AMALGAM_RADIUS, AMALGAM_RADIUS, AMALGAM_RADIUS);
			Vector mIncrement = mLoc.getDirection().multiply(AMALGAM_MOVE_SPEED);

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, AMALGAM_MOVE_SPEED * AMALGAM_MAX_DURATION + 2, mPlayer);
			List<Player> mPlayers = PlayerUtils.playersInRange(mPlayer, AMALGAM_MOVE_SPEED * AMALGAM_MAX_DURATION + 2, false);

			int mTicks = 0;
			int mReverseTick = 0;
			double mDegree = 0;
			boolean mReverse = false;

			@Override
			public void run() {
				mBox.shift(mIncrement);
				mLoc.add(mIncrement);
				Iterator<LivingEntity> mobIter = mMobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (mBox.overlaps(mob.getBoundingBox())) {
						/*
						 * Based on the ticks since reverse and the mob's noDamageTicks (11 to 20
						 * is iframes range), we can somewhat deduce if the noDamageTicks were
						 * triggered by the forward Amalgam hit and reset if necessary.
						 */
						if (mReverse && mReverseTick - mTicks + 10 <= mob.getNoDamageTicks()) {
							mob.setNoDamageTicks(0);
						}

						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.mLinkedSpell);
						mobIter.remove();
					}
				}
				Iterator<Player> playerIter = mPlayers.iterator();
				while (playerIter.hasNext()) {
					Player player = playerIter.next();
					if (mBox.overlaps(player.getBoundingBox())) {
						AbsorptionUtils.addAbsorption(player, mShield, AMALGAM_MAX_SHIELD, AMALGAM_ABSORPTION_DURATION);
						playerIter.remove();
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
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, APOTHECARY_LIGHT_COLOR);
					mWorld.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, APOTHECARY_DARK_COLOR);
				}
				mWorld.spawnParticle(Particle.SPELL_INSTANT, mLoc, 5, 0.35, 0.35, 0.35, 1);
				mWorld.spawnParticle(Particle.SPELL_WITCH, mLoc, 5, 0.35, 0.35, 0.35, 1);

				if (!mReverse && (LocationUtils.collidesWithSolid(mLoc, mLoc.getBlock()) || mTicks >= AMALGAM_MAX_DURATION)) {
					mMobs = EntityUtils.getNearbyMobs(mLoc, (0.3 + AMALGAM_MOVE_SPEED) * AMALGAM_MAX_DURATION + 2, mPlayer);
					mPlayers = PlayerUtils.playersInRange(mPlayer, (0.3 + AMALGAM_MOVE_SPEED) * AMALGAM_MAX_DURATION + 2, false);
					mReverse = true;
					mReverseTick = mTicks;
				}

				if (mReverse) {
					if (mTicks <= 0) {
						AbsorptionUtils.addAbsorption(mPlayer, mShield, AMALGAM_MAX_SHIELD, AMALGAM_ABSORPTION_DURATION);
						mWorld.playSound(mLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 2.4f);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), 8, 0.25, 0.5, 0.25, 0.5);
						mWorld.spawnParticle(Particle.SPELL, mPlayer.getLocation().add(0, 1, 0), 8, 0.35, 0.5, 0.35);
						mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 25, 0.35, 0.5, 0.35, APOTHECARY_LIGHT_COLOR);
						this.cancel();
					}

					// The mIncrement is calculated by the distance to the player divided by the number of increments left
					mIncrement = mPlayer.getEyeLocation().toVector().subtract(mLoc.toVector()).multiply(1.0 / mTicks);

					// To make the particles function without rewriting the particle code, manually calculate and set pitch and yaw
					double x = mIncrement.getX();
					double y = mIncrement.getY();
					double z = mIncrement.getZ();
					// As long as Z is nonzero, we won't get division by 0
					if (z == 0) {
						z = 0.0001;
					}
					float pitch = (float) Math.toDegrees(-Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))));
					float yaw = (float) Math.toDegrees(Math.atan(-x / z));
					if (z < 0) {
						yaw += 180;
					}
					mLoc.setPitch(pitch);
					mLoc.setYaw(yaw);

					mTicks--;
				} else {
					mTicks++;
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && InventoryUtils.isBowItem(inMainHand);
	}

}
