package com.playmonumenta.plugins.depths.abilities.flamecaller;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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

public class Fireball extends DepthsAbility {

	public static final String ABILITY_NAME = "Fireball";
	private static final int COOLDOWN = 6 * 20;
	private static final int DISTANCE = 8;
	private static final double[] DAMAGE = {10, 12.5, 15, 17.5, 20, 25};
	private static final int RADIUS = 3;
	private static final int FIRE_TICKS = 3 * 20;

	public Fireball(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.FIREBALL;
		mDisplayItem = Material.FIREWORK_STAR;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		world.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < DISTANCE; i++) {
			loc.add(dir);

			if (loc.getBlock().getType().isSolid() || EntityUtils.getNearbyMobs(loc, 1).size() > 0) {
				explode(loc);

				return;
			}
		}

		explode(loc);
	}

	private void explode(Location loc) {
		World world = loc.getWorld();

		world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0);
		world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 1.5, 1.5, 1.5, 0);
		world.spawnParticle(Particle.FLAME, loc, 25, 1.5, 1.5, 1.5, 0);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
			EntityUtils.applyFire(mPlugin, FIRE_TICKS, e, mPlayer);
			EntityUtils.damageEntity(mPlugin, e, DAMAGE[mRarity - 1], mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click to summon a " + RADIUS + " block radius fireball at the location you are looking, up to " + DISTANCE + " blocks away. The fireball deals " + DepthsUtils.getRarityColor(rarity) + (float)DAMAGE[rarity - 1] + ChatColor.WHITE + " damage and sets enemies ablaze for " + FIRE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}
