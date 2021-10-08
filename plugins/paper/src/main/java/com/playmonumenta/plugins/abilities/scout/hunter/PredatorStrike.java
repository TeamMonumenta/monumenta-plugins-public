package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
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
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;


public class PredatorStrike extends Ability {

	private static final int COOLDOWN_1 = 20 * 18;
	private static final int COOLDOWN_2 = 20 * 14;
	private static final double DISTANCE_SCALE_1 = 0.1;
	private static final double DISTANCE_SCALE_2 = 0.15;
	private static final int MAX_RANGE = 30;
	private static final int MAX_DAMAGE_RANGE = 12;
	private static final double EXPLODE_RADIUS = 0.75;
	private static final double HITBOX_LENGTH = 0.5;

	private boolean mActive = false;
	private double mDistanceScale;

	public PredatorStrike(Plugin plugin, Player player) {
		super(plugin, player, "Predator Strike");
		mInfo.mLinkedSpell = ClassAbility.PREDATOR_STRIKE;
		mInfo.mScoreboardId = "PredatorStrike";
		mInfo.mShorthandName = "PrS";
		mInfo.mDescriptions.add("Left-clicking with a bow while not sneaking will prime a Predator Strike that unprimes after 5s. When you fire a critical arrow, it will instantaneously travel in a straight line for up to 30 blocks or until it hits an enemy or block and damages enemies in a 0.75 block radius. This ability deals 100% of your projectile damage (including bonuses from other skills) increased by 10% for every block of distance from you and the target (up to 12 blocks, or 220% total). Hit targets contribute to Sharpshooter stacks. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage now increases 15% for each block of distance (up to 280%). Cooldown: 14s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SPECTRAL_ARROW, 1);
		mDistanceScale = getAbilityScore() == 1 ? DISTANCE_SCALE_1 : DISTANCE_SCALE_2;
	}

	@Override
	public void cast(Action action) {
		if (!mPlayer.isSneaking() && !mActive && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isSomeBow(mainHand)) {
				Player player = mPlayer;
				mActive = true;
				World world = mPlayer.getWorld();
				world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1, 1.0f);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 0.75, 0), 1, 0.25, 0, 0.25, 0);
						if (!mActive || mTicks >= 20 * 5) {
							mActive = false;
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mActive && arrow.isCritical()) {
			putOnCooldown();
			arrow.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			mActive = false;

			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 0.8f);

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, MAX_RANGE));

			for (double r = 0; r < MAX_RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);

				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075);
				world.spawnParticle(Particle.FLAME, bLoc, 2, 0.2, 0.2, 0.2, 0.1);

				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					explode(bLoc);
					return true;
				}

				Iterator<LivingEntity> iter = nearbyMobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							explode(bLoc);
							return true;
						}
					}
				}
				box.shift(shift);
			}
		}
		return true;
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();

		// Damage calculation - include Proj Attribute, Focus, Enchants, Teammate buffs (Blessing/Thurible), and Sharpshooter
		double damage = EntityUtils.getProjSkillDamage(mPlayer, mPlugin, true, loc) * (2 + mDistanceScale * Math.min(mPlayer.getLocation().distance(loc), MAX_DAMAGE_RANGE));

		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 45, EXPLODE_RADIUS, EXPLODE_RADIUS, EXPLODE_RADIUS, 0.125);
		world.spawnParticle(Particle.FLAME, loc, 12, EXPLODE_RADIUS, EXPLODE_RADIUS, EXPLODE_RADIUS, 0.1);

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.7f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.7f);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, EXPLODE_RADIUS, mPlayer);
		for (LivingEntity mob : mobs) {
			MovementUtils.knockAway(loc, mob, 0.25f, 0.25f);
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.SHADOWS, true, mInfo.mLinkedSpell);
		}
		Sharpshooter.addStacks(mPlugin, mPlayer, mobs.size());
	}
}