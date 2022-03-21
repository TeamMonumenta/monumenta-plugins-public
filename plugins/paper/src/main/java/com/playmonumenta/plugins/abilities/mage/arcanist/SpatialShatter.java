package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class SpatialShatter extends Ability {
	public static final String NAME = "Spatial Shatter";
	public static final ClassAbility ABILITY = ClassAbility.SPATIAL_SHATTER;
	public static final Particle.DustOptions COLOR_BLUE = new Particle.DustOptions(Color.fromRGB(16, 144, 192), 1.0f);

	public static final int DAMAGE_1 = 8;
	public static final int DAMAGE_2 = 13;
	public static final int SIZE = 4;
	public static final int DISTANCE = 8;
	public static final double REDUCTION_MULTIPLIER = 0.15;
	public static final int CAP_TICKS = (int)(1.5 * Constants.TICKS_PER_SECOND);
	public static final float KNOCKBACK = 0.3f;
	public static final double HITBOX = 0.55;
	public static final int COOLDOWN_TICKS = 6 * Constants.TICKS_PER_SECOND;

	private final int mLevelDamage;
	private final double mLevelReduction;
	private final int mLevelCap;

	public SpatialShatter(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "SpatialShatter";
		mInfo.mShorthandName = "SpSh";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand, pressing the swap key fires a burst of magic, instantly travelling up to %s blocks. If this projectile hits a solid block or enemy, it explodes, dealing %s magic damage to all enemies in a %s-block cube around it and reducing all your other skill cooldowns by %s%%, capped at %ss. Cooldown: %ss.",
				DISTANCE,
				DAMAGE_1,
				SIZE,
				StringUtils.multiplierToPercentage(REDUCTION_MULTIPLIER),
				StringUtils.ticksToSeconds(CAP_TICKS),
				StringUtils.ticksToSeconds(COOLDOWN_TICKS)
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
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);

		mLevelDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelReduction = REDUCTION_MULTIPLIER;
		mLevelCap = CAP_TICKS;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())) {
			event.setCancelled(true);
			if (!isTimerActive() && !mPlayer.isSneaking()) {
				putOnCooldown();

				Location loc = mPlayer.getEyeLocation();
				Vector direction = loc.getDirection();
				Vector shift = direction.normalize().multiply(HITBOX);
				BoundingBox box = BoundingBox.of(loc, HITBOX, HITBOX, HITBOX);
				box.shift(direction);

				World world = mPlayer.getWorld();
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.4f, 1.75f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.75f, 1.5f);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);

				Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, DISTANCE));

				for (double r = 0; r < DISTANCE; r += HITBOX) {
					Location bLoc = box.getCenter().toLocation(world);

					new PartialParticle(Particle.FIREWORKS_SPARK, bLoc, 5, 0.1, 0.1, 0.1, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_WITCH, bLoc, 5, 0, 0, 0, 0.5).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, bLoc, 20, 0.2, 0.2, 0.2, 0.1, COLOR_BLUE).spawnAsPlayerActive(mPlayer);

					if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
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
	}

	private void explode(Location loc) {
		double damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		boolean cdr = true;
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.1, COLOR_BLUE).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

		new PartialParticle(Particle.REDSTONE, loc, 125, 2.5, 2.5, 2.5, 0.25, COLOR_BLUE).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FALLING_DUST, loc, 150, 2.5, 2.5, 2.5, Material.LIGHT_BLUE_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, SIZE, mPlayer);
		for (LivingEntity mob : mobs) {
			if (cdr == true) {
				cdr = false;
				updateCooldowns(mLevelReduction);
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true);
			MovementUtils.knockAway(loc, mob, KNOCKBACK, KNOCKBACK, true);
		}
	}

	public void updateCooldowns(double percent) {
		for (Ability abil : AbilityManager.getManager().getPlayerAbilities(mPlayer).getAbilities()) {
			AbilityInfo info = abil.getInfo();
			if (info.mLinkedSpell == mInfo.mLinkedSpell) {
				continue;
			}
			int totalCD = info.mCooldown;
			int reducedCD = Math.min((int) (totalCD * percent), mLevelCap);
			mPlugin.mTimers.updateCooldown(mPlayer, info.mLinkedSpell, reducedCD);
		}
	}
}
