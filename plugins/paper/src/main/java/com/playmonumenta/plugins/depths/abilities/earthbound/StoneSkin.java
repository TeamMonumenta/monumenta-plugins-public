package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;

public class StoneSkin extends DepthsAbility {

	public static final String ABILITY_NAME = "Stone Skin";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "StoneSkinPercentDamageReceivedEffect";
	private static final double[] PERCENT_DAMAGE_RECEIVED = {-.15, -.18, -.22, -.26, -.30, -.38};
	private static final String KNOCKBACK_RESISTANCE_EFFECT_NAME = "StoneSkinPercentDamageReceivedEffect";
	private static final double[] KNOCKBACK_RESISTANCE = {0.4, 0.5, 0.6, 0.7, 0.8, 1.0};
	private static final int DURATION = 20 * 5;
	private static final int COOLDOWN = 20 * 12;

	public StoneSkin(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.POLISHED_ANDESITE;
		mTree = DepthsTree.EARTHBOUND;

		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.STONE_SKIN;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer == null) {
					return;
				}
				World world = mPlayer.getWorld();
				Location loc = mPlayer.getLocation();
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.2, 0, 0.2, 0.25);
				world.spawnParticle(Particle.BLOCK_DUST, loc, 20, 0.2, 0, 0.2, 0.25, Material.COARSE_DIRT.createBlockData());
				loc = loc.add(0, 1, 0);
				world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1.25f, 1.35f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.25f, 1.1f);
				world.spawnParticle(Particle.SPELL_INSTANT, loc, 35, 0.4, 0.4, 0.4, 0.25);
				mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED[mRarity - 1]));
				mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESISTANCE_EFFECT_NAME, new PercentKnockbackResist(DURATION, KNOCKBACK_RESISTANCE[mRarity - 1], KNOCKBACK_RESISTANCE_EFFECT_NAME));
			}
		}.runTaskLater(mPlugin, 1);

	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking() && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking to gain " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(-PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%" + ChatColor.WHITE + " resistance and " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(KNOCKBACK_RESISTANCE[rarity - 1]) + "%" + ChatColor.WHITE + " knockback resistance for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}
}
