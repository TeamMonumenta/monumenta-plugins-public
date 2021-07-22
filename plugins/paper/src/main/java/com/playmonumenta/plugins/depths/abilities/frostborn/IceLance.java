package com.playmonumenta.plugins.depths.abilities.frostborn;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import net.md_5.bungee.api.ChatColor;

public class IceLance extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Lance";
	public static final double[] DAMAGE = {12.5, 15.0, 17.5, 20.0, 22.5};
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(194, 224, 249), 1.0f);
	private static final int COOLDOWN = 6 * 20;
	private static final int DURATION = 2 * 20;
	private static final double AMPLIFIER = 0.2;
	private static final int RANGE = 8;
	private static final double HITBOX_SIZE = 0.75;

	public IceLance(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SNOWBALL;
		mTree = DepthsTree.FROSTBORN;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.ICE_LANCE;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < RANGE; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);

			world.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.05, 0.05, 0.05, 0.025);
			world.spawnParticle(Particle.REDSTONE, bLoc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				world.spawnParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125);
				world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
					EntityUtils.applySlow(mPlugin, DURATION, AMPLIFIER, mob);
					EntityUtils.applyWeaken(mPlugin, DURATION, AMPLIFIER, mob);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f);
					iter.remove();
					mobs.remove(mob);
				}
			}
		}

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 2.0f);
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click to shoot an ice lance that travels " + RANGE + " blocks and pierces through mobs, dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage and applying " + DepthsUtils.roundPercent(AMPLIFIER) + "% slowness and weaken for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public boolean runCheck() {
		return (!mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}
}

