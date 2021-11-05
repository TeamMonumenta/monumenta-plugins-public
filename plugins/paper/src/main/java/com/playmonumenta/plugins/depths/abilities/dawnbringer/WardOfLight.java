package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class WardOfLight extends DepthsAbility {

	public static final String ABILITY_NAME = "Ward of Light";
	public static final double[] HEAL = {0.32, 0.4, 0.48, 0.56, 0.64, 0.8};
	private static final int HEALING_RADIUS = 12;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int COOLDOWN = 12 * 20;

	public WardOfLight(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.LANTERN;
		mTree = DepthsTree.SUNLIGHT;
		mInfo.mLinkedSpell = ClassAbility.WARD_OF_LIGHT;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {

		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		World world = mPlayer.getWorld();
		int count = 0;
		for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, HEALING_RADIUS, true)) {
			Vector toMobVector = p.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();

			// Only heal players in the correct direction
			// Don't heal players that have their class disabled (so it doesn't work on arena contenders)
			// Don't heal players with PvP enabled
			// If the source player was included (because PvP is on), heal them
			if (p.equals(mPlayer)
			    || (!p.getScoreboardTags().contains("disable_class")
			        && !AbilityManager.getManager().isPvPEnabled(mPlayer)
			        && (playerDir.dot(toMobVector) > HEALING_DOT_ANGLE
			        || p.getLocation().distance(mPlayer.getLocation()) < 2))) {

				AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) {
					PlayerUtils.healPlayer(p, maxHealth.getValue() * HEAL[mRarity - 1]);
				}

				Location loc = p.getLocation();
				world.spawnParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				world.spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
				mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);

				count++;
			}
		}

		if (count > 0) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 1.0f);

			ParticleUtils.explodingConeEffect(mPlugin, mPlayer, HEALING_RADIUS, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, HEALING_DOT_ANGLE);
			putOnCooldown();
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return DepthsUtils.isWeaponItem(mainHand) && !mPlayer.isSneaking();
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while holding a weapon and not sneaking to heal nearby players within " + HEALING_RADIUS + " blocks in front of you for " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(HEAL[rarity - 1]) + "%" + ChatColor.WHITE + " of their max health. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}
}

