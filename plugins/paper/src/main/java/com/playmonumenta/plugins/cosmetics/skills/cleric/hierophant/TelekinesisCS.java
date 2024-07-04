package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TelekinesisCS extends EnchantedPrayerCS {

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

	private int mTicks = 0;
	private int mKillBoulder = 0;

	@Override
	public void onCast(Plugin plugin, Player player, World world, Location loc) {
		new PartialParticle(Particle.CRIT, player.getLocation(), 30, 1, 0, 1, 0.2).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.6f, 0.9f);
		world.playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, SoundCategory.PLAYERS, 1.4f, 1.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.0f, 1.1f);
		mTicks = 0;
		mKillBoulder = 0;
		new BukkitRunnable() {
			@Override
			public void run() {
				mTicks++;
				if (mKillBoulder == 1 || mTicks == 300) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		ItemDisplay display = loc.getWorld().spawn(loc.clone().add(0, 4, 0), ItemDisplay.class);
		display.setItemStack(DisplayEntityUtils.generateRPItem(Material.TUFF, "Telekinesis Boulder"));
		EntityUtils.setRemoveEntityOnUnload(display);
		display.setGlowing(true);
		display.setGlowColorOverride(Color.fromRGB(0, 180, 180));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), display::remove, 300);
		telekinesisBoulder(player, display);
	}

	@Override
	public void applyToPlayer(Player otherPlayer, Player user) {
		Location loc = otherPlayer.getLocation();
		World world = otherPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_AMBIENT, SoundCategory.PLAYERS, 1.1f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.9f, 0.9f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.0f, 2.0f);
		new PPCircle(Particle.SCRAPE, loc, 3).offset(Math.random()).delta(0, 10, 0).directionalMode(true)
			.extraRange(0.25, 1.55).ringMode(true).count(40).spawnAsPlayerActive(otherPlayer);
	}

	@Override
	public void onEffectTrigger(Player player, World world, Location loc, LivingEntity enemy, double effectSize) {
		Location playerLoc = player.getLocation().add(0, 4, 0);
		Location enemyLoc = enemy.getLocation().add(0, 0.5, 0);
		Vector dir = LocationUtils.getDirectionTo(enemyLoc, playerLoc);
		ParticleUtils.drawParticleLineSlash(playerLoc.add(enemyLoc).multiply(0.5), dir, 0, playerLoc.distance(enemyLoc), 0.1, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				new PartialParticle(Particle.SCULK_CHARGE_POP, lineLoc, 3, 0.1, 0.1, 0.01, 0.05).spawnAsPlayerActive(player);
				new PartialParticle(Particle.GLOW, lineLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
			});
		world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.8f);
		world.playSound(loc, Sound.ITEM_AXE_WAX_OFF, SoundCategory.PLAYERS, 1.8f, 0.9f);
		world.playSound(loc, Sound.BLOCK_MANGROVE_ROOTS_BREAK, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_HORSE_ARMOR, SoundCategory.PLAYERS, 1.2f, 0.8f);
		world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.4f, 0.8f);
		world.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.PLAYERS, 2.0f, 1.1f);
		new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, loc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(player);

		Material ground = enemy.getLocation().add(0, -0.5, 0).getBlock().getType();
		if (ground == Material.AIR) {
			ground = Material.TUFF;
		}
		BlockData block = ground.createBlockData();
		new PartialParticle(Particle.FALLING_DUST, loc.clone().add(0, 1, 0), 35, 0.5*effectSize, 0.5*effectSize, 0.5*effectSize, 1).data(block).spawnAsPlayerActive(player);
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, effectSize, 0, 8,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) ->
					new PartialParticle(Particle.BLOCK_CRACK, location, 1, 0, 1, 0, 0.25).data(block).directionalMode(true).spawnAsPlayerActive(player))
			)
		);
		mKillBoulder = 1;
	}

	@Override
	public void effectTick(Player player) {
		Location loc = player.getLocation().add(0, 4 + 0.5 * FastUtils.sinDeg(mTicks * 8), 0);
		new PartialParticle(Particle.GLOW, loc, 1, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 1, 0.3, 0.3, 0.3, 0).spawnAsPlayerActive(player);
	}

	public void telekinesisBoulder(Player player, ItemDisplay display) {
		new BukkitRunnable() {
			@Override
			public void run() {
				Location loc = player.getLocation().add(0, 4 + 0.5 * FastUtils.sinDeg(mTicks * 8), 0);
				display.teleport(loc);
				display.setRotation(mTicks * 4, 0);
				if (mKillBoulder == 1) {
					display.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
