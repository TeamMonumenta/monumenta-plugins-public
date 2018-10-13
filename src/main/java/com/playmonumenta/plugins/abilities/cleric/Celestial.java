package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Celestial extends Ability {

	public static final int CELESTIAL_1_FAKE_ID = 100361;
	public static final int CELESTIAL_2_FAKE_ID = 100362;
	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	public static final String CELESTIAL_1_TAGNAME = "Celestial_1";
	public static final String CELESTIAL_2_TAGNAME = "Celestial_2";
	private static final double CELESTIAL_1_DAMAGE_MULTIPLIER = 1.20;
	private static final double CELESTIAL_2_DAMAGE_MULTIPLIER = 1.35;
	
	public Celestial(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.CELESTIAL_BLESSING;
		mInfo.scoreboardId = "Celestial";
		mInfo.cooldown = CELESTIAL_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}
	
	@Override
	public boolean cast() {
		int celestial = getAbilityScore();
		
		World world = mPlayer.getWorld();
		Spells fakeID = celestial == 1 ? Spells.CELESTIAL_FAKE_1 : Spells.CELESTIAL_FAKE_2;
		int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;

		for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, CELESTIAL_RADIUS, true)) {
			mPlugin.mTimers.AddCooldown(p.getUniqueId(), fakeID, duration);

			p.setMetadata(celestial == 1 ? CELESTIAL_1_TAGNAME : CELESTIAL_2_TAGNAME, new FixedMetadataValue(mPlugin, 0));

			Location loc = p.getLocation();
			world.spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 100, 2.0, 0.75, 2.0, 0.001);
			world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.5f);
		}
		
		return true;
	}
	
	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
