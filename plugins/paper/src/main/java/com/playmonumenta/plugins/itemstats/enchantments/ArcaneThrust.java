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
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ArcaneThrust implements Enchantment {

	// For the enchantment
	private static final double WIDTH = 0.6d;
	private static final int THRUST_LENGTH = 3;
	// For the particles
	private static final float PARTICLE_SIZE = 0.6f;
	private static final float DELTA = 0.05f;
	private static final double SPACING = 0.25d;
	private static final Particle.DustOptions COLOR_1 = new Particle.DustOptions(Color.fromRGB(24, 216, 224), PARTICLE_SIZE);
	private static final Particle.DustOptions COLOR_2_START = new Particle.DustOptions(Color.fromRGB(93, 45, 135), PARTICLE_SIZE);
	private static final Particle.DustOptions COLOR_2_MIDDLE = new Particle.DustOptions(Color.fromRGB(37, 24, 68), PARTICLE_SIZE);
	private static final Particle.DustOptions COLOR_2_END = new Particle.DustOptions(Color.fromRGB(15, 13, 27), PARTICLE_SIZE);

	@Override
	public String getName() {
		return "Arcane Thrust";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ARCANE_THRUST;
	}

	@Override
	public double getPriorityAmount() {
		return 19;
		// after Hex Eater
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			if (player.getCooledAttackStrength(0.5f) > 0.9) {
				double damageMult = (value / (value + 1));
				double damage = 1 + (event.getFlatDamage() * damageMult);
				int decay = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.DECAY);

				Location loc = player.getEyeLocation();
				BoundingBox box = BoundingBox.of(loc, WIDTH, WIDTH, WIDTH);
				Vector dir = loc.getDirection();
				box.shift(dir);
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), 10, enemy);
				World world = player.getWorld();

				// Damage
				for (int i = 0; i < THRUST_LENGTH; i++) {
					box.shift(dir);
					for (LivingEntity mob : mobs) {
						if (box.overlaps(mob.getBoundingBox())) {
							// Deal damage.
							DamageUtils.damage(player, mob, DamageType.MELEE_ENCH, damage, ClassAbility.ARCANE_THRUST, false);
							// Deal knockback.
							MovementUtils.knockAway(player.getLocation(), mob, 0.25f, 0.25f);
						}
					}
				}

				// VFX: Particles
				new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 0, 0, 0, 2).spawnAsPlayerActive(player);
				if (decay == 0) {
					// I am so sorry for this line of code.
					ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, 0.8 * SPACING, 3,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, DELTA, DELTA, DELTA, COLOR_1)
							.spawnAsPlayerActive(player));
					new BukkitRunnable() {
						@Override
						public void run() {
							ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, SPACING, 4,
								(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, 2 * DELTA, 2 * DELTA, 2 * DELTA, COLOR_1)
									.spawnAsPlayerActive(player));
						}
					}.runTaskLater(plugin, 1);
					new BukkitRunnable() {
						@Override
						public void run() {
							ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, SPACING, 5,
								(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, 3 * DELTA, 3 * DELTA, 3 * DELTA, COLOR_1)
									.spawnAsPlayerActive(player));
						}
					}.runTaskLater(plugin, 2);
				} else {
					// Voidstained particles for Judgement of the Voidstained
					ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, 0.8 * SPACING, 3,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, DELTA, DELTA, DELTA, COLOR_2_START)
							.spawnAsPlayerActive(player));
					new BukkitRunnable() {
						@Override
						public void run() {
							ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, SPACING, 4,
								(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, 2 * DELTA, 2 * DELTA, 2 * DELTA, COLOR_2_MIDDLE)
									.spawnAsPlayerActive(player));
						}
					}.runTaskLater(plugin, 1);
					new BukkitRunnable() {
						@Override
						public void run() {
							ParticleUtils.drawParticleLineSlash(loc.clone().add(dir.clone().multiply(THRUST_LENGTH + 0.5)), dir, 0.0d, THRUST_LENGTH - 0.5, SPACING, 5,
								(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 3, 3 * DELTA, 3 * DELTA, 3 * DELTA, COLOR_2_END)
									.spawnAsPlayerActive(player));
						}
					}.runTaskLater(plugin, 2);
				}

				// SFX: Sounds
				Location playerLoc = player.getLocation();
				world.playSound(playerLoc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.4f);
				world.playSound(playerLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1.1f);
				world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 1.8f);
				world.playSound(playerLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 2.0f);
				world.playSound(playerLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.8f, 0.6f);
			}
		}
	}
}
