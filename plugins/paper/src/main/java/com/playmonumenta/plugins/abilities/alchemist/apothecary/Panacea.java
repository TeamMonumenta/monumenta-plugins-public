package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;



public class Panacea extends Ability {

	private static final double PANACEA_1_DAMAGE_FRACTION = 0.8;
	private static final double PANACEA_2_DAMAGE_FRACTION = 1.6;
	private static final int PANACEA_1_SHIELD = 2;
	private static final int PANACEA_2_SHIELD = 3;
	private static final int PANACEA_MAX_SHIELD = 12;
	private static final int PANACEA_ABSORPTION_DURATION = 20 * 24;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int PANACEA_MAX_DURATION = 20 * 2;
	private static final double PANACEA_MOVE_SPEED = 0.25;
	private static final double PANACEA_RADIUS = 1.5;
	private static final int PANACEA_1_SLOW_TICKS = (int) (1.5 * 20);
	private static final int PANACEA_2_SLOW_TICKS = 2 * 20;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);
	private static final int COOLDOWN = 24 * 20;

	private double mDamage;
	private final int mShield;
	private int mSlowTicks;

	public Panacea(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Panacea");
		mInfo.mScoreboardId = "Panacea";
		mInfo.mShorthandName = "Pn";
		mInfo.mDescriptions.add("Shift left click with a Bow to shoot a mixture that deals 80% of your potion damage and applies 100% Slow for 1.5s to every enemy touched and adds 2 absorption health to other players, lasting 24 seconds, maximum 12. After hitting a block or traveling 10 blocks, the mixture traces and returns to you, able to damage enemies and shield allies a second time. Cooldown: 24s.");
		mInfo.mDescriptions.add("Absorption health added is increased to 3, damage is increased to 160% of your potion damage, and Slow duration is increased to 2s.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.PANACEA;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = 0;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			AlchemistPotions alchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			if (alchemistPotions != null) {
				mDamage = alchemistPotions.getDamage() * (getAbilityScore() == 1 ? PANACEA_1_DAMAGE_FRACTION : PANACEA_2_DAMAGE_FRACTION);
			}
		});
		mSlowTicks = getAbilityScore() == 1 ? PANACEA_1_SLOW_TICKS : PANACEA_2_SLOW_TICKS;
		mShield = getAbilityScore() == 1 ? PANACEA_1_SHIELD : PANACEA_2_SHIELD;
		mDisplayItem = new ItemStack(Material.PURPLE_CONCRETE_POWDER, 1);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 1.25f);
		world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);
		world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1);

		putOnCooldown();

		FixedMetadataValue playerItemStats = new FixedMetadataValue(mPlugin, new ItemStatManager.PlayerItemStats(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer)));

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			final BoundingBox mBox = BoundingBox.of(mLoc, PANACEA_RADIUS, PANACEA_RADIUS, PANACEA_RADIUS);
			Vector mIncrement = mLoc.getDirection().multiply(PANACEA_MOVE_SPEED);

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, PANACEA_MOVE_SPEED * PANACEA_MAX_DURATION + 2, mPlayer);
			List<Player> mPlayers = PlayerUtils.otherPlayersInRange(mPlayer, PANACEA_MOVE_SPEED * PANACEA_MAX_DURATION + 2, true);

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

						DamageEvent damageEvent = new DamageEvent(mob, mPlayer, mPlayer, DamageType.MAGIC, mInfo.mLinkedSpell, mDamage);
						damageEvent.setDelayed(true);
						damageEvent.setPlayerItemStat(playerItemStats);
						DamageUtils.damage(damageEvent, false, true, null);

						EntityUtils.applySlow(mPlugin, mSlowTicks, 1, mob);
						mobIter.remove();
					}
				}
				Iterator<Player> playerIter = mPlayers.iterator();
				while (playerIter.hasNext()) {
					Player player = playerIter.next();
					if (mBox.overlaps(player.getBoundingBox())) {
						if (player != mPlayer) {
							AbsorptionUtils.addAbsorption(player, mShield, PANACEA_MAX_SHIELD, PANACEA_ABSORPTION_DURATION);
						}
						playerIter.remove();
					}
				}

				mDegree += 12;
				Vector vec;
				for (int i = 0; i < 2; i++) {
					double radian1 = Math.toRadians(mDegree + (i * 180));
					vec = new Vector(FastUtils.cos(radian1) * 0.325, 0, FastUtils.sin(radian1) * 0.325);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch() - 90);
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(vec);
					world.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, APOTHECARY_LIGHT_COLOR);
					world.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 0.1, 0.1, APOTHECARY_DARK_COLOR);
				}
				world.spawnParticle(Particle.SPELL_INSTANT, mLoc, 5, 0.35, 0.35, 0.35, 1);
				world.spawnParticle(Particle.SPELL_WITCH, mLoc, 5, 0.35, 0.35, 0.35, 1);

				if (!mReverse && (!mLoc.isChunkLoaded() || LocationUtils.collidesWithSolid(mLoc, mLoc.getBlock()) || mTicks >= PANACEA_MAX_DURATION)) {
					mMobs = EntityUtils.getNearbyMobs(mLoc, (0.3 + PANACEA_MOVE_SPEED) * PANACEA_MAX_DURATION + 2, mPlayer);
					mPlayers = PlayerUtils.otherPlayersInRange(mPlayer, (0.3 + PANACEA_MOVE_SPEED) * PANACEA_MAX_DURATION + 2, true);
					mReverse = true;
					mReverseTick = mTicks;
				}

				if (mReverse) {
					if (mTicks <= 0) {
						world.playSound(mLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 2.4f);
						world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), 8, 0.25, 0.5, 0.25, 0.5);
						world.spawnParticle(Particle.SPELL, mPlayer.getLocation().add(0, 1, 0), 8, 0.35, 0.5, 0.35);
						world.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 25, 0.35, 0.5, 0.35, APOTHECARY_LIGHT_COLOR);
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
		if (mPlayer == null) {
			return false;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && ItemUtils.isSomeBow(inMainHand);
	}
}
