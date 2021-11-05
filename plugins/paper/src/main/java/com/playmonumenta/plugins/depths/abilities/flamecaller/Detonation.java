package com.playmonumenta.plugins.depths.abilities.flamecaller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class Detonation extends DepthsAbility {

	public static final String ABILITY_NAME = "Detonation";
	public static final int[] DAMAGE = {4, 5, 6, 7, 8, 10};
	public static final int DEATH_RADIUS = 8;
	public static final int DAMAGE_RADIUS = 2;

	public static String tree;

	public Detonation(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TNT;
		mTree = DepthsTree.FLAMECALLER;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Entity entity = event.getEntity();
		if (entity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) && entity.getName() != DummyDecoy.DUMMY_NAME) {
			return;
		}
		Location location = entity.getLocation();
		World world = mPlayer.getWorld();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, DAMAGE_RADIUS)) {
			world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, mob.getLocation().add(0, 1, 0), 2);
			EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell, true, true, true, true);
		}
		world.spawnParticle(Particle.EXPLOSION_LARGE, location.add(0, 0.5, 0), 1);
		world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location.add(0, 1, 0), 3);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1);
	}

	@Override
	public double entityDeathRadius() {
		return DEATH_RADIUS;
	}

	@Override
	public String getDescription(int rarity) {
		return "If an enemy dies within " + DEATH_RADIUS + " blocks of you it explodes, dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage in a " + DAMAGE_RADIUS + " block radius to other enemies. Bypasses iframes, and deaths from Detonation can trigger Detonation again.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}
