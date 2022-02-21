package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class RadiantBlessing extends DepthsAbility {

	public static final String ABILITY_NAME = "Radiant Blessing";
	private static final int HEALING_RADIUS = 18;
	private static final int COOLDOWN = 22 * 20;
	private static final double[] PERCENT_DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.3};
	private static final int DURATION = 10* 20;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "RadiantBlessingPercentDamageReceivedEffect";
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;

	public RadiantBlessing(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.SUNFLOWER;
		mTree = DepthsTree.SUNLIGHT;
		mInfo.mLinkedSpell = ClassAbility.RADIANT_BLESSING;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location userLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		for (Player p : PlayerUtils.playersInRange(userLoc, HEALING_RADIUS, true)) {
			Location loc = p.getLocation();
			mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED));
			mPlugin.mEffectManager.addEffect(p, ABILITY_NAME, new PercentDamageDealt(DURATION, PERCENT_DAMAGE[mRarity - 1]));
			world.spawnParticle(Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
			world.spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		}

		world.playSound(userLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		world.playSound(userLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);

		putOnCooldown();
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
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return DepthsUtils.isWeaponItem(mainHand) && mPlayer.isSneaking();
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to enchant players within " + HEALING_RADIUS + " blocks, including yourself, with " + (int) DepthsUtils.roundPercent(-PERCENT_DAMAGE_RECEIVED) + "% resistance and " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(PERCENT_DAMAGE[rarity - 1]) + "%" + ChatColor.WHITE + " melee damage for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
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

