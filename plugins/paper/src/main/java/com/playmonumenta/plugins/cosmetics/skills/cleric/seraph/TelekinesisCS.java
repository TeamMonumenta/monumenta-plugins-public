package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class TelekinesisCS extends EtherealAscensionCS {

	public static final String NAME = "Telekinesis";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The earth rumbles from your pull...",
			"The flow of life bows to your will.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.WARPED_ROOTS;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private @Nullable ItemDisplay mBoulder = null;
	private int mDegree = 0;

	@Override
	public void onLaunch(Player player, World world, Location loc) {
		mDegree = 0;
		if (mBoulder != null) {
			mBoulder.remove();
		}
		mBoulder = spawnTelekinesisBoulder(player);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0f, 0.7f);
		world.playSound(loc, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.6f, 0.9f);
		world.playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, SoundCategory.PLAYERS, 1.4f, 1.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.0f, 1.1f);
	}

	@Override
	public void tickEffect(Player player, Location loc, double hoverHeight) {
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 1, 2, 1, 2).spawnAsPlayerPassive(player);
		for (int i = 0; i < 3; i++) {
			Location loc2 = player.getLocation().add(3 * FastUtils.sinDeg(mDegree + 120 * i), 0.5 + 0.35 * FastUtils.sinDeg(mDegree + 120 * i), 3 * FastUtils.cosDeg(mDegree + 120 * i));
			new PartialParticle(Particle.GLOW, loc2, 1, 0, 1, 0, 0.25).directionalMode(true).spawnAsPlayerPassive(player);
		}
		mDegree += 4 % 360;
	}

	@Override
	public void orbShoot(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.4f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.5f, 0.8f);
	}

	@Override
	public void orbTrail(Player player, Location loc, Location startLoc) {
		new PartialParticle(Particle.SCULK_CHARGE_POP, loc, 2, 0.1, 0.1, 0.01, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void orbImpact(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.BLOCK_MANGROVE_ROOTS_BREAK, SoundCategory.PLAYERS, 1.6f, 0.5f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.1f, 0.6f);
		world.playSound(loc, Sound.ENTITY_HORSE_ARMOR, SoundCategory.PLAYERS, 1.0f, 0.9f);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SCULK_CHARGE_POP, loc, 12, 0.25, 0.5, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, loc, 12, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(player);

		Material ground = LocationUtils.fallToGround(loc, 0).add(0, -0.5, 0).getBlock().getType();
		if (ground == Material.AIR) {
			ground = Material.TUFF;
		}
		BlockData block = ground.createBlockData();
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 0, 4,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.BLOCK_CRACK, location, 1, 0, 1, 0, 0.25).data(block).directionalMode(true).spawnAsPlayerActive(player))
			)
		);
	}

	@Override
	public void orbBuff(Player player, World world, Location loc, List<Player> hitPlayers, double radius) {
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.6f, 2.0f);
		new PPCircle(Particle.SCRAPE, loc, radius).offset(Math.random()).delta(0, 10, 0).directionalMode(true)
			.extraRange(0.25, 1.55).ringMode(true).count(40).spawnAsPlayerActive(player);
	}

	@Override
	public void forceEndAscension(Player player, World world, Location loc) {
		world.playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, SoundCategory.PLAYERS, 1.0f, 0.9f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
	}

	@Override
	public void ascensionEnd(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.6f, 1.0f);
		if (mBoulder != null) {
			mBoulder.remove();
		}
	}

	public ItemDisplay spawnTelekinesisBoulder(Player player) {
		Location loc = player.getLocation().add(0, 3.5 + 0.75 * FastUtils.sinDeg(mDegree), 0);
		ItemDisplay display = loc.getWorld().spawn(loc.clone().add(0, 4, 0), ItemDisplay.class);
		display.setItemStack(DisplayEntityUtils.generateRPItem(Material.TUFF, "Telekinesis Boulder"));
		EntityUtils.setRemoveEntityOnUnload(display);
		GlowingManager.startGlowing(display, Color.fromRGB(0, 180, 180), -1, 0, p -> p == player);

		new BukkitRunnable() {
			@Override
			public void run() {
				Location loc = player.getLocation().add(0, 3.5 + 0.35 * FastUtils.sinDeg(0.75 * mDegree), 0);
				display.teleport(loc);
				display.setRotation(mDegree, 0);

				if (!player.isOnline() || player.isDead() || AbilityUtils.isSilenced(player) || display.isDead() || !display.isValid()) {
					display.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
		return display;
	}
}
