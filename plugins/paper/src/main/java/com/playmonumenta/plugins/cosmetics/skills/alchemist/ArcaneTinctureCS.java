package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneTinctureCS extends IronTinctureCS {

	public static final String NAME = "Arcane Tincture";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Enchanting a potion with protective spells",
			"provides a formidable and fast-acting line of",
			"defense for the traveling alchemist.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String tinctureName() {
		return "Arcane Tincture";
	}

	@Override
	public void onThrow(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 0.7f, 0.4f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 5.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_VEX_HURT, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 2.0f);
	}

	@Override
	public void onGroundEffect(Location location, Player caster, int twoTicks) {
		// sound
		if (twoTicks % 10 == 0) {
			location.getWorld().playSound(location, Sound.BLOCK_MEDIUM_AMETHYST_BUD_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
		}

		// particles
		Vector vec = VectorUtils.rotateYAxis(new Vector(0.4, 0, 0), twoTicks * 15);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, location.clone().add(vec).add(0, 0.4, 0))
			.manualTimeOverride(twoTicks)
			.directionalMode(true).delta(0, -0.25, 0).extra(1)
			.spawnAsPlayerActive(caster);
		new PPPeriodic(Particle.ENCHANTMENT_TABLE, location.clone().subtract(vec).add(0, 0.4, 0))
			.manualTimeOverride(twoTicks)
			.directionalMode(true).delta(0, -0.25, 0).extra(1)
			.spawnAsPlayerActive(caster);
		if (twoTicks > 0 && twoTicks % 5 == 0) {
			new PPPeriodic(Particle.WAX_OFF, location.clone().add(0, 0.3, 0))
				.manualTimeOverride(twoTicks)
				.delta(0.05, 0.1, 0.05)
				.spawnAsPlayerActive(caster);
		}
	}

	@Override
	public void tinctureExpireEffects(Location location, Player caster) {
		location.getWorld().playSound(location, Sound.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundCategory.PLAYERS, 1, 0.5f);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(0, 0.3, 0))
			.count(20).delta(0.05, 0.1, 0.05)
			.spawnAsPlayerActive(caster);
	}

	@Override
	public void pickupEffects(World world, Location location, Player p) {
		world.playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.6f, 1.8f);
		world.playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(location, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 1.7f);
		world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.5f, 2.0f);
		world.playSound(location, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.6f, 1.0f);
		new PartialParticle(Particle.BLOCK_CRACK, location, 50, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData()).spawnAsPlayerActive(p);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(0, 0.3, 0))
			.count(20).delta(0.05, 0.1, 0.05)
			.spawnAsPlayerActive(p);
	}

	@Override
	public void pickupEffectsForPlayer(Player player, Location tinctureLocation) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 1.25f);
		new PartialParticle(Particle.SPELL_INSTANT, tinctureLocation, 20, 0.25, 0.1, 0.25, 1).spawnAsPlayerActive(player);
		double radius = 1.15;
		new PPCircle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 0.2, 0), radius)
			.countPerMeter(8)
			.spawnAsPlayerActive(player);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				double rotStep = 20;
				double yStep = 0.175;
				double rotation = mT * rotStep;
				double y = 0.15 + yStep * mT;
				for (int i = 0; i < 3; i++) {
					double degree = rotation + (i * 120);
					new PPPeriodic(Particle.SCRAPE, player.getLocation().add(FastUtils.cosDeg(degree) * radius, y, FastUtils.sinDeg(degree) * radius))
						.manualTimeOverride(mT)
						.spawnAsPlayerActive(player);
					new PPPeriodic(Particle.ENCHANTMENT_TABLE, player.getLocation().add(FastUtils.cosDeg(degree - rotStep / 2) * radius, y - yStep / 2, FastUtils.sinDeg(degree - rotStep / 2) * radius))
						.manualTimeOverride(mT)
						.spawnAsPlayerActive(player);
				}

				if (y >= 1.8) {
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

}
