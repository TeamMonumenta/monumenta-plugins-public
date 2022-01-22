package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;

public class BrambleShell extends DepthsAbility {

	public static final String ABILITY_NAME = "Bramble Shell";
	public static final double[] BRAMBLE_DAMAGE = {6, 7.5, 9, 10.5, 12, 15};

	public BrambleShell(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SWEET_BERRIES;
		mTree = DepthsTree.EARTHBOUND;
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (mPlayer != null && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE) && !event.isCancelled() || event.isBlocked()) {
			Location loc = source.getLocation();
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.BLOCK_CRACK, loc.add(0, source.getHeight() / 2, 0), 25, 0.5, 0.5, 0.5, 0.125, Bukkit.createBlockData(Material.SWEET_BERRY_BUSH));
			world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1, 0.8f);

			DamageUtils.damage(mPlayer, source, DamageType.MELEE_SKILL, BRAMBLE_DAMAGE[mRarity - 1], mInfo.mLinkedSpell, true);
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Whenever an enemy deals melee or projectile damage to you, they take " + DepthsUtils.getRarityColor(rarity) + BRAMBLE_DAMAGE[rarity - 1] + ChatColor.WHITE + " melee damage.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}
}
