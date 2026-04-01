package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class WindBombCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WIND_BOMB;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	public void onThrow(Plugin plugin, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1f, 1.4f);
		world.playSound(loc, Sound.ENTITY_VEX_HURT, SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.5f, 0.4f);

		Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.8f), 2);
	}

	public void aerial(Player player, LivingEntity bomb, int ticks, int maxDuration) {
		Location center = LocationUtils.getEntityCenter(bomb);
		World world = bomb.getWorld();

		new PPPeriodic(Particle.FALLING_DUST, center).count(2).data(Material.WHITE_CONCRETE.createBlockData()).spawnAsPlayerActive(player);
		new PPPeriodic(Particle.CLOUD, center).count(2).extra(0.1).spawnAsPlayerActive(player);

		if (ticks % 20 == 0) {
			world.playSound(center, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 0.3f, 0.6f);
			world.playSound(center, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.PLAYERS, 0.5f, 1.4f);
		}
	}

	public void onExplode(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.7f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.4f, 0.4f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.5f, 0.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f);

		new PPCircle(Particle.CLOUD, loc, radius)
			.extra(0.125)
			.count(150)
			.spawnAsPlayerActive(player);
	}

	public void onVortexSpawn(Player player, World world, Location loc, double enhancePullDuration) {
		new PartialParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1.6f, 0.5f);
				mTicks += 10;
				if (mTicks >= enhancePullDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 10);
	}

	public void onVortexTick(Player player, Location loc, double radius, int tick) {
		new PartialParticle(Particle.CLOUD, loc, 3, 1, 2, 1).spawnAsPlayerActive(player);

		if (tick % 10 == 0) {
			new BukkitRunnable() {
				int mTicks = 1;

				@Override
				public void run() {
					double multiplier = Math.pow((4 - mTicks) / 3.0, 1.6);

					Location newLoc = loc.clone().subtract(new Vector(0, mTicks / 2.0, 0));

					new PPCircle(Particle.FIREWORKS_SPARK, newLoc, radius * multiplier)
						.count(30)
						.directionalMode(true).rotateDelta(true)
						.delta(-multiplier, 0, 0.15)
						.extra(0.75)
						.extraVariance(0.1)
						.spawnAsPlayerActive(player);

					if (++mTicks > 3) {
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	public String getBase64Head() {
		return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDY2YjEwYmY2ZWUyY2Q3ZTNhYzk2ZDk3NDllYTYxNmFhOWM3MzAzMGJkY2FlZmZhZWQyNDllNTVjODQ5OTRhYyJ9fX0=";
	}

	public Team getTeam() {
		return ScoreboardUtils.getExistingTeamOrCreate("unpushable_white", NamedTextColor.WHITE);
	}

	// Shouldn't need to override this
	public void modify(LivingEntity bomb, Plugin plugin, int size) {
		ItemDisplay bombHead = DisplayEntityUtils.spawnItemDisplayWithBase64Head(bomb.getLocation(), getBase64Head());
		if (bombHead == null) {
			return;
		}
		getTeam().addEntity(bombHead);
		bombHead.setGlowing(true);
		bombHead.setTransformation(new Transformation(
			new Vector3f(),
			new AxisAngle4f(),
			new Vector3f(size, size, size),
			new AxisAngle4f()));
		bomb.addPassenger(bombHead);
		EntityUtils.setRemoveEntityOnUnload(bombHead);
		new BukkitRunnable() {
			private float mYaw = 0f;

			@Override
			public void run() {
				if (bombHead.isDead() || !bombHead.isValid()) {
					this.cancel();
					return;
				}
				if (!bomb.isValid()) {
					bombHead.remove();
					this.cancel();
					return;
				}
				bombHead.setRotation(mYaw, 0f);
				mYaw = (mYaw + 12f) % 360;
			}
		}.runTaskTimer(plugin, 0, 1);
	}
}
