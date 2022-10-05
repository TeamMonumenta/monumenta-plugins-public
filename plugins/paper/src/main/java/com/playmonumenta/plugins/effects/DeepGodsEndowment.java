package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DeepGodsEndowment extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "DeepGodsEndowment";
	public static final String effectID = "DeepGodsEndowment";

	public static int HITS_NEEDED = 5;
	Random RANDOM = new Random();

	public static final String SOOTHING_SPEED_EFFECT_NAME = "SoothingCombosPercentSpeedEffect";
	private static final String EARTH_PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "EarthenCombosPercentDamageReceivedEffect";

	private int mCurrHits = 0;

	public DeepGodsEndowment(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		mCurrHits += 1;

		if (mCurrHits >= HITS_NEEDED) {
			mCurrHits = 0;
			int randInt = RANDOM.ints(0, 5).findFirst().getAsInt();

			switch (randInt) {
				case 0 -> soothingCombo(entity, enemy);
				case 1 -> earthenCombo(entity, enemy);
				case 2 -> volcanicCombo(entity, enemy);
				case 3 -> frigidCombo(entity, enemy);
				case 4 -> darkCombo(entity, enemy);
				default -> {
				}
			}
		}
	}

	public void soothingCombo(LivingEntity entity, LivingEntity enemy) {
		PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 2, 0, false, true);

		List<Player> players = PlayerUtils.playersInRange(entity.getLocation(), 12, true);

		for (Player p : players) {
			p.addPotionEffect(hasteEffect);
			Plugin.getInstance().mEffectManager.addEffect(p, SOOTHING_SPEED_EFFECT_NAME, new PercentSpeed(20 * 2, 0.1, SOOTHING_SPEED_EFFECT_NAME));
			entity.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
			entity.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001);
			entity.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
		}

		Location loc = entity.getLocation().add(0, 1, 0);
		entity.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
		entity.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
	}

	public void earthenCombo(LivingEntity entity, LivingEntity enemy) {
		Plugin.getInstance().mEffectManager.addEffect(entity, EARTH_PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(20 * 4, -.08));
		EntityUtils.applySlow(Plugin.getInstance(), 25, .99, enemy);

		Location loc = entity.getLocation().add(0, 1, 0);
		World world = entity.getWorld();
		Location entityLoc = enemy.getLocation();
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, 0.8f, 0.65f);
		world.playSound(loc, Sound.BLOCK_NETHER_BRICKS_BREAK, 0.8f, 0.45f);
		world.spawnParticle(Particle.CRIT_MAGIC, entityLoc.add(0, 1, 0), 10, 0.5, 0.2, 0.5, 0.65);
		world.spawnParticle(Particle.BLOCK_DUST, loc.add(0, 1, 0), 15, 0.5, 0.3, 0.5, 0.5, Material.PODZOL.createBlockData());
		world.spawnParticle(Particle.BLOCK_DUST, loc.add(0, 1, 0), 15, 0.5, 0.3, 0.5, 0.5, Material.ANDESITE.createBlockData());
	}

	public void volcanicCombo(LivingEntity entity, LivingEntity enemy) {
		Location location = enemy.getLocation();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, 4)) {
			EntityUtils.applyFire(Plugin.getInstance(), 60, mob, (Player) entity);
			DamageUtils.damage(entity, mob, DamageType.MAGIC, 6, null, true);
		}
		World world = entity.getWorld();
		for (int i = 0; i < 360; i += 45) {
			double rad = Math.toRadians(i);
			Location locationDelta = new Location(world, 4 / 2 * FastUtils.cos(rad), 0.5, 4 / 2 * FastUtils.sin(rad));
			location.add(locationDelta);
			world.spawnParticle(Particle.FLAME, location, 1);
			location.subtract(locationDelta);
		}
		world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.5f, 1);
	}

	public void frigidCombo(LivingEntity entity, LivingEntity enemy) {
		Location targetLoc = enemy.getLocation();
		World world = targetLoc.getWorld();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, 4)) {
			if (!(mob.getHealth() <= 0 || mob == null)) {
				world.spawnParticle(Particle.CRIT_MAGIC, mob.getLocation(), 25, .5, .2, .5, 0.65);
				EntityUtils.applySlow(Plugin.getInstance(), 2 * 20, 0.2, mob);
				DamageUtils.damage(entity, mob, DamageType.MAGIC, 2, null, true);
			}
		}

		Location playerLoc = entity.getLocation().add(0, 1, 0);
		world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.65f);
		world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.45f);
		world.spawnParticle(Particle.SNOW_SHOVEL, targetLoc, 25, .5, .2, .5, 0.65);
	}

	public void darkCombo(LivingEntity entity, LivingEntity enemy) {
		EntityUtils.applyVulnerability(Plugin.getInstance(), 20 * 3, 0.15, enemy);

		Location loc = entity.getLocation().add(0, 1, 0);
		((Player) entity).playSound(entity.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 0.5f);
		loc.getWorld().spawnParticle(Particle.SPELL_WITCH, enemy.getLocation(), 15, 0.5, 0.2, 0.5, 0.65);
		PotionUtils.applyPotion(entity, enemy,
			new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 0, true, false));
	}

	public static DeepGodsEndowment deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new DeepGodsEndowment(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Deep God's Endowment";
	}

	@Override
	public String toString() {
		return String.format("DeepGodsEndowment duration:%d", this.getDuration());
	}
}
