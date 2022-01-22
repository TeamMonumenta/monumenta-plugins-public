package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Avalanche extends DepthsAbility {

	public static final String ABILITY_NAME = "Avalanche";
	public static final int[] DAMAGE = {30, 35, 40, 45, 50, 60};
	public static final int COOLDOWN_TICKS = 20 * 20;
	public static final int SLOW_DURATION = 3 * 20;
	public static final double SLOW_MODIFIER = 0.99;
	public static final int RADIUS = 10;
	private static final Particle.DustOptions ICE_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	public Avalanche(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SNOW_BLOCK;
		mTree = DepthsTree.FROSTBORN;
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.AVALANCHE;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);
		if (mPlayer != null && !isTimerActive()) {
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();

			HashSet<Location> iceToBreak = new HashSet<>(DepthsUtils.iceActive.keySet());
			iceToBreak.removeIf(l -> l.distance(loc) > RADIUS);
			iceToBreak.removeIf(l -> l.getBlock().getType() != DepthsUtils.ICE_MATERIAL);

			if (iceToBreak.size() == 0) {
				return;
			}

			putOnCooldown();

			world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.95f);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.95f);

			world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.75f);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);

			world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1f);

			List<LivingEntity> hitMobs = new ArrayList<LivingEntity>();

			//Shatter all nearby ice
			for (Location l : iceToBreak) {
				Location aboveLoc = l.clone().add(0.5, 1, 0.5);

				//Damage and root mobs
				for (LivingEntity mob : EntityUtils.getNearbyMobs(aboveLoc, 1.0)) {
					if (!hitMobs.contains(mob)) {
						EntityUtils.applySlow(mPlugin, SLOW_DURATION, SLOW_MODIFIER, mob);
						DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.mLinkedSpell);
						hitMobs.add(mob);
					}
				}
				mPlayer.getWorld().getBlockAt(l).setBlockData(DepthsUtils.iceActive.get(l));
				DepthsUtils.iceActive.remove(l);

				world.spawnParticle(Particle.REDSTONE, aboveLoc, 15, 0.5, 0.5, 0.5, ICE_PARTICLE_COLOR);
				world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, aboveLoc, 2, 0.5, 0.25, 0.5);
			}
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap hands to shatter all ice blocks within a radius of " + RADIUS + ", dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage to enemies on the shattered ice. Affected enemies are rooted for " + SLOW_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN_TICKS / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}

