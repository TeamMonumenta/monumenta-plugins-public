package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VigilantMothdragonCS extends KeeperVirtueCS {
	// Ambient sounds are handled in the los entry

	public static final String NAME = "Vigilant Mothdragon";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Ishniran folklore suggests that these strange creatures are",
			"malevolent beings, harbingers of destruction and carnage,",
			"although it appears to be... relatively peaceful. Probably."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.PHANTOM_MEMBRANE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions HARMING = new Particle.DustTransition(Color.fromRGB(255, 153, 0), Color.fromRGB(221, 51, 51), 1.0f);
	private static final Particle.DustOptions SHIELDING = new Particle.DustOptions(Color.fromRGB(51, 221, 204), 1.0f);

	@Override
	public String getLosName() {
		return "VigilantMothdragon";
	}

	@Override
	public Component getComponentName() {
		return Component.text("Vigilant Mothdragon", NamedTextColor.AQUA);
	}

	@Override
	public void healPlayer(Player player, Player target, LivingEntity allay, int remainder) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_STRIDER_HURT, 0.5f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1.2f, 1f);
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.2f, 2f);
		}, remainder);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, remainder,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0.06, 0.06, 0.06).data(SHIELDING).spawnAsPlayerActive(player);
				new PartialParticle(Particle.SOUL_FIRE_FLAME, lineLoc, FastUtils.roundRandomly(0.4), 0.06, 0.06, 0.06, 0.02).spawnAsPlayerActive(player);
		});
	}

	@Override
	public void attackHeretic(Player player, LivingEntity target, LivingEntity allay, int remainder) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_STRIDER_HURT, 0.5f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1.2f, 1f);
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 1.3f);
		}, remainder);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, remainder,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, lineLoc, 1, 0.06, 0.06, 0.06).data(HARMING).spawnAsPlayerActive(player);
				new PartialParticle(Particle.SMALL_FLAME, lineLoc, FastUtils.roundRandomly(0.4), 0.06, 0.06, 0.06, 0.02).spawnAsPlayerActive(player);
		});
	}
}
