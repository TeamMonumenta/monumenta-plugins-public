package com.playmonumenta.plugins.depths.abilities.earthbound;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
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

		Location eyeLoc = mPlayer.getEyeLocation();
		Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), CAST_RANGE);
		ray.mThroughBlocks = false;
		ray.mThroughNonOccluding = false;

		RaycastData data = ray.shootRaycast();

		List<LivingEntity> mobs = data.getEntities();
		if (mobs != null && !mobs.isEmpty()) {
			World world = mPlayer.getWorld();
			for (LivingEntity mob : mobs) {
				if (mob.isValid() && !mob.isDead() && EntityUtils.isHostileMob(mob)) {
					Location mobLoc = mob.getEyeLocation();
					world.spawnParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25);
					world.spawnParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25);
					world.spawnParticle(Particle.SPIT, mobLoc, 5, 0.15, 0.5, 0.15, 0);
					world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
					world.playSound(eyeLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.0f);

					EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], mob);
					EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer);

					putOnCooldown();
					break;
				}
			}
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
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
