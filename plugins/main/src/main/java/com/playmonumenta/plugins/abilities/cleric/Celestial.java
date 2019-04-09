package com.playmonumenta.plugins.abilities.cleric;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Celestial extends Ability {

	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	private static final String CELESTIAL_1_TAGNAME = "Celestial_1";
	private static final String CELESTIAL_2_TAGNAME = "Celestial_2";
	private static final double CELESTIAL_1_DAMAGE_MULTIPLIER = 1.20;
	private static final double CELESTIAL_2_DAMAGE_MULTIPLIER = 1.35;

	public Celestial(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.CELESTIAL_BLESSING;
		mInfo.scoreboardId = "Celestial";
		mInfo.cooldown = CELESTIAL_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		int celestial = getAbilityScore();

		World world = mPlayer.getWorld();
		int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;
		String tagName = celestial == 1 ? CELESTIAL_1_TAGNAME : CELESTIAL_2_TAGNAME;

		List<Player> affectedPlayers = PlayerUtils.getNearbyPlayers(mPlayer, CELESTIAL_RADIUS, true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class")
				|| p.hasMetadata(CELESTIAL_1_TAGNAME)
				|| p.hasMetadata(CELESTIAL_2_TAGNAME));

		// Give these players the metadata tag that boosts their damage
		for (Player p : affectedPlayers) {
			p.setMetadata(tagName, new FixedMetadataValue(mPlugin, 0));
			p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.02);
			Location loc = p.getLocation();
			world.spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 100, 2.0, 0.75, 2.0, 0.001);
			world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.5f);
		}

		// Run a task later to remove the metadata tag after time has elapsed
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player p : affectedPlayers) {
					p.removeMetadata(tagName, mPlugin);
					p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() - 0.02);
				}
			}
		}.runTaskLater(mPlugin, duration);

		return true;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
			return mPlayer.isSneaking();
		}
		return false;
	}

	/**
	 * A static method that is run every time a player does damage (regardless of class) to
	 * check for this ability.
	 */
	public static void modifyDamage(Player player, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(CELESTIAL_1_TAGNAME)) {
			event.setDamage(event.getDamage() * CELESTIAL_1_DAMAGE_MULTIPLIER);
		} else if (player.hasMetadata(CELESTIAL_2_TAGNAME)) {
			event.setDamage(event.getDamage() * CELESTIAL_2_DAMAGE_MULTIPLIER);
		}
	}
}
