package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.seraph.KeeperVirtue;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

	@Override
	public String getLosName() {
		return "VigilantMothdragon";
	}

	@Override
	public Component getComponentName() {
		return Component.text("Vigilant Mothdragon", NamedTextColor.AQUA);
	}

	@Override
	public NamedTextColor getGlowColor(double percentHealth) {
		return percentHealth >= (double) 1 / 3 ? NamedTextColor.AQUA : NamedTextColor.RED;
	}

	@Override
	public void changeModeCast(Allay allay, boolean toActive) {
		if (toActive) {
			allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_STRIDER_HAPPY, 1.2f, 1.6f);
		} else {
			allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_STRIDER_RETREAT, 1.2f, 1.2f);
		}
	}

	@Override
	public ItemStack getHeldItem(KeeperVirtue.VirtueMode mode) {
		ItemStack item;
		switch (mode) {
			case ACTIVE_GENERIC, ACTIVE_COMBAT -> item = DisplayEntityUtils.generateRPItem(Material.BOW, "Aleph");
			case ACTIVE_SUPPORT ->
				item = DisplayEntityUtils.generateRPItem(Material.PINK_STAINED_GLASS, "Tesseract of Balance");
			default -> item = DisplayEntityUtils.generateRPItem(Material.POTION, "Ocean's Gate");
		}
		return item;
	}

	@Override
	public void healPlayer(Player player, Player target, LivingEntity allay) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_STRIDER_HURT, 0.5f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.8f), 2);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, 2,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.WAX_OFF, lineLoc, 1, 0.08, 0.08, 0.08, 2).spawnAsPlayerActive(player));
	}

	@Override
	public void attackHeretic(Player player, LivingEntity target, LivingEntity allay) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_STRIDER_HURT, 0.5f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.3f), 2);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, 2,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.CRIT_MAGIC, lineLoc, 1, 0.06, 0.06, 0.06, 0.1).spawnAsPlayerActive(player));
	}

	@Override
	public void allayOnDeath(LivingEntity allay, Location loc) {
		allay.getWorld().playSound(loc, Sound.ENTITY_STRIDER_DEATH, 1.5f, 1f);
	}
}
