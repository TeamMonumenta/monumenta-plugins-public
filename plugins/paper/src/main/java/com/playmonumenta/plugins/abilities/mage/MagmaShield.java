package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
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



public class MagmaShield extends Ability {

	public static final String NAME = "Magma Shield";
	public static final ClassAbility ABILITY = ClassAbility.MAGMA_SHIELD;

	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 12;
	public static final int SIZE = 6;
	public static final int FIRE_SECONDS = 4;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.5f;
	// 70° on each side of look direction for XZ-plane (flattened Y),
	// so 140° angle of effect
	public static final int ANGLE = 70;
	public static final int COOLDOWN_SECONDS = 12;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	private final int mLevelDamage;

	private boolean mHasBlizzard;

	public MagmaShield(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "Magma";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add(
			String.format(
				"While sneaking, right-clicking with a wand summons a torrent of flames, dealing %s magic damage to all enemies in front of you within a %s-block cube around you, setting them on fire for %ss, and knocking them away. The damage ignores iframes. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				FIRE_SECONDS,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s.",
				DAMAGE_1,
				DAMAGE_2
			)
		);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.MAGMA_CREAM, 1);

		mLevelDamage = getAbilityScore() == 2 ? DAMAGE_2 : DAMAGE_1;

		mHasBlizzard = false;
		if (ServerProperties.getClassSpecializationsEnabled()) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				mHasBlizzard = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Blizzard.class) != null;
			});
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();

		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		Vector flattenedLookDirection = mPlayer.getEyeLocation().getDirection().setY(0);
		for (LivingEntity potentialTarget : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SIZE, mPlayer)) {
			Vector flattenedTargetVector = potentialTarget.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0);
			if (
				VectorUtils.isAngleWithin(
					flattenedLookDirection,
					flattenedTargetVector,
					ANGLE
				)
			) {
				EntityUtils.applyFire(mPlugin, FIRE_TICKS, potentialTarget, mPlayer);
				DamageUtils.damage(mPlayer, potentialTarget, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, false);
				MovementUtils.knockAway(mPlayer, potentialTarget, KNOCKBACK, true);
			}
		}

		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				}

				if (mRadius >= SIZE + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer != null && mPlayer.isSneaking() && ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand()) && !(mHasBlizzard && mPlayer.getLocation().getPitch() < Blizzard.ANGLE)) {
			return true;
		}
		return false;
	}
}
