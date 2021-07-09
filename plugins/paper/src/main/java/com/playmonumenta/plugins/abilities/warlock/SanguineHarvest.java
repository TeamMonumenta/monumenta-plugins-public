package com.playmonumenta.plugins.abilities.warlock;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.SanguineMark;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.event.block.Action;


public class SanguineHarvest extends Ability {

	private static final int RANGE = 8;
	private static final int RADIUS_1 = 3;
	private static final int RADIUS_2 = 4;
	private static final int BLEED_LEVEL_1 = 1;
	private static final int BLEED_LEVEL_2 = 2;
	private static final double HEAL_PERCENT_1 = 0.05;
	private static final double HEAL_PERCENT_2 = 0.1;
	private static final int BLEED_DURATION = 10 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final double HITBOX_LENGTH = 0.55;

	private static final String SANGUINE_NAME = "SanguineEffect";
	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "SanguineHarvestTickRightClicked";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(179, 0, 0), 1.0f);

	private final int mRadius;
	private final int mBleedLevel;
	private final double mHealPercent;
	private int mRightClicks = 0;

	public SanguineHarvest(Plugin plugin, Player player) {
		super(plugin, player, "Sanguine Harvest");
		mInfo.mScoreboardId = "SanguineHarvest";
		mInfo.mShorthandName = "SH";
		mInfo.mDescriptions.add("Enemies you damage with an ability are afflicted with Bleed I for 10 seconds. Bleed gives mobs 10% Slowness and 10% Weaken per level if the mob is below 50% Max Health. Additionally, double right click while holding a scythe to fire a burst of darkness. This projectile travels up to 8 blocks and upon contact with a surface or an enemy, it explodes, knocking back and marking all mobs within 3 blocks of the explosion for a harvest. Any player that kills a marked mob is healed for 5% of max health. Cooldown: 20s.");
		mInfo.mDescriptions.add("Increase passive Bleed level to II, and increase the radius to 4 blocks.");
		mInfo.mLinkedSpell = ClassAbility.SANGUINE_HARVEST;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mRadius = getAbilityScore() == 1 ? RADIUS_1 : RADIUS_2;
		mHealPercent = getAbilityScore() == 1 ? HEAL_PERCENT_1 : HEAL_PERCENT_2;
		mBleedLevel = getAbilityScore() == 1 ? BLEED_LEVEL_1 : BLEED_LEVEL_2;
	}

	@Override
	public boolean runCheck() {
		return (ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand()) && !mPlayer.isSneaking());
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 5);
		}
		if (mRightClicks < 2) {
			return;
		} else {
			mRightClicks = 0;

			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);

			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.9f);

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, RANGE));

			for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);

				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075);
				world.spawnParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR);

				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					explode(bLoc);
					return;
				}

				Iterator<LivingEntity> iter = nearbyMobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							explode(bLoc);
							return;
						}
					}
				}
				box.shift(shift);
			}
		}
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0, 0, 0, 0.125);
		world.spawnParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.1, COLOR);

		world.spawnParticle(Particle.REDSTONE, loc, 75, mRadius, mRadius, mRadius, 0.25, COLOR);
		world.spawnParticle(Particle.FALLING_DUST, loc, 75, mRadius, mRadius, mRadius, Material.RED_CONCRETE.createBlockData());
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 55, mRadius, mRadius, mRadius, 0.25);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.3f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius, mPlayer);
		for (LivingEntity mob : mobs) {
			MovementUtils.knockAway(loc, mob, 0.2f, 0.2f);
			mPlugin.mEffectManager.addEffect(mob, SANGUINE_NAME, new SanguineMark(mHealPercent, 20 * 30));
		}
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity damagee = event.getDamaged();
		EntityUtils.applyBleed(mPlugin, BLEED_DURATION, mBleedLevel, damagee);
	}
}