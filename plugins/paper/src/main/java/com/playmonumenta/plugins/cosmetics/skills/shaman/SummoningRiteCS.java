package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SummoningRiteCS extends TotemicProjectionCS implements GalleryCS {

	public static final String NAME = "Summoning Rite";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Summoning rituals practiced by the Sages of",
			"the Nightmare would call forth twisted beasts",
			"from deeper layers within the dreamscape.",
			"If this is what they call a \"nightmare\"...",
			"what unspeakable places do those come from?"
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_QUARTZ_ORE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			|| player.getGameMode() == GameMode.CREATIVE;
	}

	private static final Particle.DustTransition CRIMSON = new Particle.DustTransition(Color.fromRGB(200, 0, 0), Color.fromRGB(0, 0, 0), 1.25f);
	private static final Particle.DustTransition CLEANSING_COLOR = new Particle.DustTransition(Color.fromRGB(0, 87, 255), Color.fromRGB(0, 0, 0), 1.25f);
	private static final Particle.DustTransition FLAME_COLOR = new Particle.DustTransition(Color.fromRGB(240, 102, 0), Color.fromRGB(0, 0, 0), 1.25f);
	private static final Particle.DustTransition LIGHTNING_COLOR = new Particle.DustTransition(Color.fromRGB(255, 255, 102), Color.fromRGB(0, 0, 0), 1.25f);
	private static final Particle.DustTransition WHIRLWIND_COLOR = new Particle.DustTransition(Color.fromRGB(204, 255, 255), Color.fromRGB(0, 0, 0), 1.25f);
	private static final Particle.DustTransition DECAYED_COLOR = new Particle.DustTransition(Color.fromRGB(5, 120, 5), Color.fromRGB(0, 0, 0), 1.25f);

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void totemCast(Player player, Projectile proj, String totemName) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.5f, 1.8f);
		player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 0.9f);
		player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.4f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!proj.isValid() || mTicks >= 100) {
					this.cancel();
				} else {
					Location projLoc = proj.getLocation();
					switch (totemName) {
						case "CleansingTotem" -> {
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).data(CLEANSING_COLOR).spawnAsPlayerActive(player);
							new PartialParticle(Particle.DOLPHIN, projLoc).delta(0.25).extra(1).spawnAsPlayerActive(player);
						}
						case "FlameTotem" -> {
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).data(FLAME_COLOR).spawnAsPlayerActive(player);
							new PartialParticle(Particle.CRIMSON_SPORE, projLoc).delta(0.25).extra(1).spawnAsPlayerActive(player);
						}
						case "LightningTotem" -> {
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).data(LIGHTNING_COLOR).spawnAsPlayerActive(player);
							new PartialParticle(Particle.CRIT, projLoc).delta(0.25).extra(0.2).spawnAsPlayerActive(player);
						}
						case "WhirlwindTotem" -> {
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).data(WHIRLWIND_COLOR).spawnAsPlayerActive(player);
							new PartialParticle(Particle.WHITE_ASH, projLoc).delta(0.25).extra(1).spawnAsPlayerActive(player);
						}
						case "DecayedTotem" -> {
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).data(DECAYED_COLOR).spawnAsPlayerActive(player);
							new PartialParticle(Particle.SPORE_BLOSSOM_AIR, projLoc).delta(0.25).extra(1).spawnAsPlayerActive(player);
						}
						default -> {
						}
					}
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void projectionCast(Player player, Projectile proj, @Nullable List<LivingEntity> totems) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.5f, 1.8f);
		player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 0.9f);
		player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.9f, 1.8f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!proj.isValid() || mTicks >= 100) {
					this.cancel();
				} else {
					Location projLoc = proj.getLocation();
					new PartialParticle(Particle.CRIMSON_SPORE, projLoc).extra(1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.ENCHANTMENT_TABLE, projLoc).extra(0.5).spawnAsPlayerActive(player);
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, projLoc).extra(1).data(CRIMSON).spawnAsPlayerActive(player);
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void projectionCollision(Player player, Location dropCenter, double radius, List<LivingEntity> totems) {
		dropCenter.subtract(0, LocationUtils.distanceToGround(dropCenter, -64, 1), 0);
		switch (totems.size()) {
			case 2, 3 ->
				player.getWorld().playSound(dropCenter, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.75f, 1.4f);
			case 4 ->
				player.getWorld().playSound(dropCenter, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.7f, 1.4f);
			default ->
				player.getWorld().playSound(dropCenter, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 1.4f);
		}

		player.getWorld().playSound(dropCenter, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.45f, 1.35f);
		player.getWorld().playSound(dropCenter, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.3f, 1.6f);
		player.getWorld().playSound(dropCenter, Sound.ENTITY_STRIDER_HAPPY, SoundCategory.PLAYERS, 0.5f, 1.0f);
		player.getWorld().playSound(dropCenter, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.6f, 0.8f);

		new PPCircle(Particle.END_ROD, dropCenter.clone().add(0, 0.1, 0), 0.1 * radius).countPerMeter(5).spawnAsPlayerActive(player);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, dropCenter, 0.5 * radius).countPerMeter(8).delta(0.06).data(CRIMSON).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Vector dir = new Vector(radius * 0.4, 0, 0);
				for (int i = 0; i < 3; i++) {
					Vector dir2 = VectorUtils.rotateTargetDirection(dir, i * 120 + 40 * mTicks, 0);
					new PPLine(Particle.WAX_OFF, dropCenter.clone().add(dir2), dropCenter.clone().add(VectorUtils.rotateTargetDirection(dir2, 120, 0))).countPerMeter(4).delta(dir2.getX(), 0, dir2.getZ()).extra(0.1).directionalMode(true).spawnAsPlayerActive(player);
				}
				mTicks++;
				if (mTicks >= 3) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
	}

	@Override
	public void projectionAOE(Player player, Location dropCenter, double radius) {
		dropCenter.subtract(0, LocationUtils.distanceToGround(dropCenter, -64, 1), 0);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PPCircle(Particle.DUST_COLOR_TRANSITION, dropCenter, radius * (0.75 + 0.25 * mTicks)).countPerMeter(8).delta(0.06).data(CRIMSON).spawnAsPlayerActive(player);
				new PPCircle(Particle.CRIMSON_SPORE, dropCenter, radius * (0.75 + 0.25 * mTicks)).countPerMeter(0.1).delta(0.15).spawnAsPlayerActive(player);
				mTicks++;
				if (mTicks >= 2) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
	}
}
