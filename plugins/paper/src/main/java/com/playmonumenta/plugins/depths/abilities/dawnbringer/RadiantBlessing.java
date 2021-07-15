package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class RadiantBlessing extends DepthsAbility {

	public static final String ABILITY_NAME = "Radiant Blessing";
	private static final int HEALING_RADIUS = 18;
	private static final int COOLDOWN = 12 * 20;
	private static final double[] PERCENT_DAMAGE = {0.16, 0.20, 0.24, 0.28, 0.32};
	private static final int DURATION = 6 * 20;

	public RadiantBlessing(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SUNFLOWER;
		mTree = DepthsTree.SUNLIGHT;
		mInfo.mLinkedSpell = ClassAbility.RADIANT_BLESSING;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
		World world = mPlayer.getWorld();
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), HEALING_RADIUS, true)) {
			if (!p.equals(mPlayer)) {

				Location loc = p.getLocation();
				mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_OTHER,
				                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION, 0, true, true));
				mPlugin.mEffectManager.addEffect(p, ABILITY_NAME, new PercentDamageDealt(DURATION, PERCENT_DAMAGE[mRarity - 1]));
				world.spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				world.spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
			}
		}

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);

		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return DepthsUtils.isWeaponItem(mainHand) && mPlayer.isSneaking();
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to enchant players within " + HEALING_RADIUS + " blocks with Resistance 1 and " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(PERCENT_DAMAGE[rarity - 1]) + "%" + ChatColor.WHITE + " damage for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}
}

