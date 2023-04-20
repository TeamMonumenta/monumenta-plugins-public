package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GuardingBolt extends DepthsAbility {

	public static final String ABILITY_NAME = "Guarding Bolt";
	public static final int COOLDOWN = 24 * 20;
	private static final int RADIUS = 4;
	private static final int RANGE = 25;
	private static final int[] DAMAGE = {12, 14, 16, 18, 20, 24};
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40, 50};
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public static final DepthsAbilityInfo<GuardingBolt> INFO =
		new DepthsAbilityInfo<>(GuardingBolt.class, ABILITY_NAME, GuardingBolt::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.GUARDING_BOLT)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GuardingBolt::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.HORN_CORAL)
			.descriptions(GuardingBolt::getDescription);

	public GuardingBolt(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		World world = startLoc.getWorld();
		RayTraceResult result = world.rayTraceEntities(startLoc, dir, RANGE, 0.425,
			e -> e instanceof Player player && player != mPlayer && player.getGameMode() != GameMode.SPECTATOR);

		if (result != null && result.getHitEntity() instanceof Player targetPlayer) {
			if (ZoneUtils.hasZoneProperty(targetPlayer, ZoneUtils.ZoneProperty.LOOTROOM)) {
				return;
			}

			putOnCooldown();

			new PPLine(Particle.CLOUD, mPlayer.getEyeLocation(), targetPlayer.getEyeLocation())
				.countPerMeter(12)
				.delta(0.25)
				.spawnAsPlayerActive(mPlayer);

			for (int k = 0; k < 120; k++) {
				double x = FastUtils.randomDoubleInRange(-3, 3);
				double z = FastUtils.randomDoubleInRange(-3, 3);
				Location to = targetPlayer.getLocation().add(x, 0.15, z);
				Vector pdir = LocationUtils.getDirectionTo(to, targetPlayer.getLocation().add(0, 0.15, 0));
				new PartialParticle(Particle.CLOUD, targetPlayer.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4)).spawnAsPlayerActive(mPlayer);
			}

			for (int k = 0; k < 60; k++) {
				double x = FastUtils.randomDoubleInRange(-3, 3);
				double z = FastUtils.randomDoubleInRange(-3, 3);
				Location to = targetPlayer.getLocation().add(x, 0.15, z);
				Vector pdir = LocationUtils.getDirectionTo(to, targetPlayer.getLocation().add(0, 0.15, 0));
				new PartialParticle(Particle.REDSTONE, targetPlayer.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5), COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
			}

			Location userLoc = mPlayer.getLocation();
			Location targetLoc = targetPlayer.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
			if (userLoc.distance(targetLoc) > 1) {
				mPlayer.teleport(targetLoc);
				doDamage(targetLoc);
			}

			world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.75f, 0.9f);
		}
	}

	private void doDamage(Location location) {
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, location, 125, RADIUS, RADIUS, RADIUS, 3, COLOR_AQUA).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10).spawnAsPlayerActive(mPlayer);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, RADIUS);
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
			if (!EntityUtils.isBoss(enemy)) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], enemy);
			}

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Left click the air while sneaking and looking directly at a player within " + RANGE + " blocks to dash to their location. Mobs in a " + RADIUS + " block radius of the destination are dealt ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" magic damage, knocked back, and stunned for "))
			.append(Component.text(StringUtils.to2DP(STUN_DURATION[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}
}
