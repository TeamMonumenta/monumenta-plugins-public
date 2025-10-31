package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.seraph.KeeperVirtue;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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

public class KeeperVirtueCS implements CosmeticSkill {
	// Ambient sounds are handled in the los entry

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.KEEPER_VIRTUE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_RELIC;
	}

	public String getLosName() {
		return "KeeperVirtue";
	}

	public Component getComponentName() {
		return Component.text("Keeper Virtue", NamedTextColor.GOLD);
	}

	public NamedTextColor getGlowColor(double percentHealth) {
		return percentHealth >= (double) 1 / 3 ? NamedTextColor.GOLD : NamedTextColor.RED;
	}

	public void changeModeCast(Allay allay, boolean toActive) {
		if (toActive) {
			allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1.2f, 1f);
		} else {
			allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.2f, 1f);
		}
	}

	public ItemStack getHeldItem(KeeperVirtue.VirtueMode mode) {
		ItemStack item;
		switch (mode) {
			case ACTIVE_GENERIC, ACTIVE_COMBAT ->
				item = DisplayEntityUtils.generateRPItem(Material.STICK, "Staff of the Soulseeker");
			case ACTIVE_SUPPORT ->
				item = DisplayEntityUtils.generateRPItem(Material.SPLASH_POTION, "Potion of Redemption");
			default -> item = DisplayEntityUtils.generateRPItem(Material.POTION, "Salazar's Breath");
		}
		return item;
	}

	public void healPlayer(Player player, Player target, LivingEntity allay) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ALLAY_HURT, 0.2f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.8f), 2);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, 2,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.WAX_ON, lineLoc, 1, 0.08, 0.08, 0.08, 2).spawnAsPlayerActive(player));
	}

	public void attackHeretic(Player player, LivingEntity target, LivingEntity allay) {
		Location loc = allay.getEyeLocation();
		allay.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2f);
		allay.getWorld().playSound(loc, Sound.ENTITY_ALLAY_HURT, 0.2f, 1f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.3f), 2);

		Vector vec = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(target), loc);
		ParticleUtils.drawParticleLineSlash(loc.clone().add(LocationUtils.getEntityCenter(target)).multiply(0.5), vec, 0, 0.5 * vec.length(), 0.135, 2,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.CRIT_MAGIC, lineLoc, 1, 0.06, 0.06, 0.06, 0.1).spawnAsPlayerActive(player));
	}

	public void allayOnDeath(LivingEntity allay, Location loc) {
		allay.getWorld().playSound(loc, Sound.ENTITY_ALLAY_DEATH, 1f, 1f);
	}
}
