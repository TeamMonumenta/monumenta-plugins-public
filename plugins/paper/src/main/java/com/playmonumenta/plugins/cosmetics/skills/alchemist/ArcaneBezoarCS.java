package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneBezoarCS extends BezoarCS {

	public static final String NAME = "Arcane Bezoar";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Extracting certain parts of creatures' dead bodies",
			"allows alchemists to create an invigorating concoction.");
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
	public ItemStack bezoarItem(boolean philosophersStone) {
		if (philosophersStone) {
			return super.bezoarItem(true);
		}
		ItemStack item = new ItemStack(Material.GILDED_BLACKSTONE);
		ItemUtils.modifyMeta(item, meta -> {
			meta.displayName(Component.text("Arcane Bezoar", NamedTextColor.WHITE)
				                 .decoration(TextDecoration.ITALIC, false));
		});
		ItemUtils.setPlainName(item, "Arcane Bezoar");
		return item;
	}

	@Override
	public void periodicBezoarEffects(Player player, Location loc, int tick, boolean philosophersStone) {
		if (tick % 2 == 0) {
			Vector vec = VectorUtils.rotateYAxis(new Vector(0.4, 0, 0), tick * 7.5);
			new PPPeriodic(Particle.ENCHANTMENT_TABLE, loc.clone().add(vec).add(0, 0.4, 0))
				.manualTimeOverride(tick / 2)
				.directionalMode(true).delta(0, -0.25, 0).extra(1)
				.spawnAsPlayerActive(player);
			new PPPeriodic(Particle.ENCHANTMENT_TABLE, loc.clone().subtract(vec).add(0, 0.4, 0))
				.manualTimeOverride(tick / 2)
				.directionalMode(true).delta(0, -0.25, 0).extra(1)
				.spawnAsPlayerActive(player);
		}
		if (tick % 10 == 0) {
			new PPPeriodic(Particle.WAX_OFF, loc.clone().add(0, 0.4, 0))
				.manualTimeOverride(tick)
				.delta(0.1, 0.1, 0.1)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void pickupEffects(Player player, Location loc, boolean philosophersStone) {

		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 0.3, 0))
			.count(20).delta(0.1, 0.1, 0.1)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 10, 0.25, 0.1, 0.25, 1).spawnAsPlayerActive(player);

	}

	@Override
	public void targetEffects(Player player, Location loc, boolean philosophersStone) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.5f, 2);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.4f, 1.5f);

		// Symbol: Cancer ♋
		double bigRadius = 1;
		double smallRadius = 0.3;
		float forwardYaw = player.getLocation().getYaw();
		Location center = player.getLocation().add(0, 0.4, 0);
		Vector right = VectorUtils.rotationToVector(forwardYaw - 90, 0).multiply(bigRadius - smallRadius);
		// small circles
		new PPCircle(Particle.ENCHANTMENT_TABLE, center.clone().add(right), smallRadius)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.directionalMode(true).delta(0, -0.2, 0).extra(1)
			.spawnAsPlayerActive(player)
			.location(center.clone().subtract(right))
			.spawnAsPlayerActive(player);
		// accent particle in center of small circles
		new PartialParticle(Particle.SCRAPE, player.getLocation().add(0, 0.2, 0).add(right))
			.spawnAsPlayerActive(player)
			.location(player.getLocation().add(0, 0.2, 0).clone().subtract(right))
			.spawnAsPlayerActive(player);

		// tails/big circle parts
		new PPCircle(Particle.ENCHANTMENT_TABLE, center, bigRadius)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.directionalMode(true).delta(0, -0.2, 0).extra(1)
			.arcDegree(forwardYaw, forwardYaw + 120)
			.spawnAsPlayerActive(player)
			.arcDegree(forwardYaw + 180, forwardYaw + 180 + 120)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void expireEffects(Player player, Location loc, boolean philosophersStone) {
		loc.getWorld().playSound(loc, Sound.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundCategory.PLAYERS, 1, 0.5f);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 0.4, 0))
			.count(20).delta(0.1, 0.1, 0.1)
			.spawnAsPlayerActive(player);
	}

}
