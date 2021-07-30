package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class CrushingEarth extends DepthsAbility {

	public static final String ABILITY_NAME = "Crushing Earth";
	private static final int COOLDOWN = 20 * 8;
	private static final double[] DAMAGE = {6, 7.5, 9, 10.5, 12};
	private static final int CAST_RANGE = 4;
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40};

	public CrushingEarth(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SHIELD;
		mTree = DepthsTree.EARTHBOUND;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.CRUSHING_EARTH;
	}

	@Override
	public void cast(Action action) {

		LivingEntity e = EntityUtils.getEntityAtCursor(mPlayer, CAST_RANGE, true, true, true);

		if (e == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				Location loc = mPlayer.getEyeLocation();
				Vector dir = loc.getDirection();
				World world = mPlayer.getWorld();

				LivingEntity applyE = e;

				if (EntityUtils.isHostileMob(applyE)) {

					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1, 0.9f);
					for (int i = 0; i < CAST_RANGE; i++) {
						loc.add(dir);
						world.spawnParticle(Particle.CRIT, loc, 5, 0.25, 0.25, 0.25, 0);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
						if (loc.distance(e.getEyeLocation()) < 1.25) {
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
							world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 1, 1.2f);
							break;
						}
					}
					EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], applyE);
					EntityUtils.damageEntity(mPlugin, e, DAMAGE[mRarity - 1], mPlayer);

					Location eLoc = applyE.getLocation().add(0, applyE.getHeight() / 2, 0);
					world.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
					world.spawnParticle(Particle.CRIT_MAGIC, loc, 30, 1, 1, 1, 0.25);
				}
				this.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offhand = mPlayer.getInventory().getItemInOffHand();
		return !mPlayer.isSneaking() && !isOnCooldown() && DepthsUtils.isWeaponItem(mainhand);
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while looking at an enemy within " + CAST_RANGE + " blocks to deal " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage and stun them for " + DepthsUtils.getRarityColor(rarity) + STUN_DURATION[rarity - 1] / 20.0 + ChatColor.WHITE + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}
}
