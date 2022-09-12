package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public class Taunt extends DepthsAbility {

	public static final String ABILITY_NAME = "Taunt";
	private static final int COOLDOWN = 20 * 18;
	private static final double[] ABSORPTION = {1, 1.25, 1.5, 1.75, 2, 2.5};
	private static final int CAST_RANGE = 12;
	private static final int MAX_ABSORB = 6;
	private static final int ABSORPTION_DURATION = 20 * 8;

	public Taunt(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.GOLDEN_CHESTPLATE;
		mTree = DepthsTree.EARTHBOUND;

		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.TAUNT;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, CAST_RANGE);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		if (mobs.size() > 0) {
			putOnCooldown();

			// add rarity% absorption for each affected mob, up to 6
			AbsorptionUtils.addAbsorption(mPlayer, Math.min(mobs.size(), MAX_ABSORB) * ABSORPTION[mRarity - 1], MAX_ABSORB * ABSORPTION[mRarity - 1], ABSORPTION_DURATION);
			for (LivingEntity le : mobs) {
				EntityUtils.applyTaunt(mPlugin, le, mPlayer);
				world.spawnParticle(Particle.BLOCK_DUST, le.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData());
				world.spawnParticle(Particle.FIREWORKS_SPARK, le.getLocation(), 30, 0.1, 0.1, 0.1, 0.2);
			}
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1, 1.2f);
			world.spawnParticle(Particle.BLOCK_DUST, mPlayer.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData());
			world.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 30, 0.1, 0.1, 0.1, 0.2);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !isOnCooldown() && mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to have all enemies within " + CAST_RANGE + " blocks target you, and you gain " + DepthsUtils.getRarityColor(rarity) + ABSORPTION[rarity - 1] + ChatColor.WHITE +
				" absorption for every enemy (up to " + MAX_ABSORB + " enemies) afflicted, for " + ABSORPTION_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}
}
