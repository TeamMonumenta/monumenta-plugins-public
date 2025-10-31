package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.itemstats.enchantments.Knockback.KB_VEL_PER_LEVEL;

public class SweepingEdge implements Enchantment {
	// Specify how much the Sweeping Edge hitbox expands (in ALL directions) compared to the hitbox of the enemy you hit.
	private static final double X_EXPANSION = 1.0;
	private static final double Y_EXPANSION = 0.25;
	private static final double Z_EXPANSION = 1.0;
	private static final double PLAYER_REACH = 3.0;
	private static final float KNOCKBACK_TRANSFER_COEFFICIENT = 0.8f;

	public static final double TRANSFER_COEFFICIENT = 0.2;

	@Override
	public String getName() {
		return "Sweeping Edge";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SWEEPING_EDGE;
	}

	@Override
	public double getPriorityAmount() {
		return 19;
		// after Hex Eater
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			&& !PlayerUtils.isFallingAttack(player)) {
			if (player.getCooledAttackStrength(0.5f) > 0.9) {
				// Compute damage.
				double damageMult = value * TRANSFER_COEFFICIENT;
				double damage = event.getFlatDamage() * damageMult;

				// Compute the bounding box - find the enemy's box, then expand it
				// If the enchantment DAMAGE stops working, check THIS SECTION
				BoundingBox box = enemy.getBoundingBox();
				box.expand(X_EXPANSION, Y_EXPANSION, Z_EXPANSION);

				Hitbox playerFootBox = new Hitbox.SphereHitbox(player.getLocation(), PLAYER_REACH);
				Hitbox playerEyeBox = new Hitbox.SphereHitbox(player.getEyeLocation(), PLAYER_REACH);

				// Find mobs which could be hit, only check ones which are nearby
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(enemy.getLocation(), 10, enemy);
				if (mobs.isEmpty()) {
					// If there are no other mobs in the area, don't bother running the rest of the code
					// Either that or you are hitting a massive mob with its centre more than 10 blocks away from you,
					// at which point I don't even want the enchantment to work
					return;
				}
				World world = player.getWorld();
				Location bLoc = box.getCenter().toLocation(world);

				// Get item stats
				int fire = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.FIRE_ASPECT);
				int ice = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ICE_ASPECT);
				int thunder = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.THUNDER_ASPECT);
				int decay = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.DECAY);
				int bleed = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.BLEEDING);
				int wind = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.WIND_ASPECT);
				int impact = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.IMPACT);
				int knockback = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.KNOCKBACK);


				// List of the locations of mobs hit by the sweep attack.
				List<Location> locMobsHit = new ArrayList<>(mobs.size());

				for (LivingEntity mob : mobs) {
					BoundingBox mobBox = mob.getBoundingBox();
					if (box.overlaps(mobBox)
						&& (playerFootBox.intersects(mobBox) || playerEyeBox.intersects(mobBox))
						&& mob != enemy) {
						// Find the location of the mob.
						// I am using the getCenter().toLocation method instead of directly getLocation() because I want to get the centre, not the feet.
						Location mobLoc = mobBox.getCenter().toLocation(world);
						mobLoc.subtract(bLoc);
						locMobsHit.add(mobLoc);
						// Deal damage.
						DamageUtils.damage(player, mob, DamageType.MELEE_ENCH, damage, ClassAbility.SWEEPING_EDGE, false);
						// Deal knockback.
						float speed = KNOCKBACK_TRANSFER_COEFFICIENT * (0.4f + KB_VEL_PER_LEVEL * (float) knockback);
						Vector vector = enemy.getLocation().clone().toVector().subtract(player.getLocation().toVector());
						vector.setY(0);
						if (vector.length() < 0.001) {
							vector = new Vector(0, KNOCKBACK_TRANSFER_COEFFICIENT * 0.4f, 0);
						} else {
							vector.normalize()
								.multiply(speed)
								.setY(KNOCKBACK_TRANSFER_COEFFICIENT * 0.4f);
						}
						// Knocks everything in the same direction as the central mob
						MovementUtils.knockAwayDirection(vector, mob, 0.5f, true, false);
					}
				}
				// Get the player location, to play sounds later
				Location playerLoc = player.getLocation().add(0, 1, 0);
				Vector playerToEnemy = bLoc.toVector();
				playerToEnemy.subtract(playerLoc.toVector());
				// Always show the sweep particle. Strictly this isn't the vanilla behaviour but I find it looks better.
				Location sweepLoc = playerLoc.clone().add(playerToEnemy.normalize().toLocation(world));
				new PartialParticle(Particle.SWEEP_ATTACK, sweepLoc).spawnFull();
				// Sounds: Only run if the list of mobs hit is non-empty.
				// Credit for SFX goes to mscr
				if (!locMobsHit.isEmpty()) {
					if (fire > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.9f);
						player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.0f);
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.5f, 1.0f);
					}
					if (ice > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.9f);
						player.playSound(player.getLocation(), Sound.ENTITY_TURTLE_DEATH_BABY, SoundCategory.PLAYERS, 1.0f, 0.5f);
						player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.PLAYERS, 1.0f, 1.3f);
						player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 1.5f, 2.0f);
					}
					if (thunder > 0) {
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.25f, 0.7f);
						player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 2.0f);
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.25f, 0.6f);
					}
					if (decay > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.9f);
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.14f, 0.75f);
						player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.2f, 0.5f);
					}
					if (bleed > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 0.9f);
						player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.4f, 1.8f);
						player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 0.4f, 1.8f);
					}
					if (wind > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.2f);
						player.playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 0.8f, 0.6f);
						player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 1.0f, 2.0f);
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.4f, 1.7f);
					}
					if (impact > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 0.7f);
						player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.4f, 1.6f);
						player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_HURT, SoundCategory.PLAYERS, 0.4f, 0.6f);
					}
					if (fire + ice + thunder + decay + bleed + wind + impact == 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.9f);
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 0.7f);
					}
				}
			}
		}
	}
}
