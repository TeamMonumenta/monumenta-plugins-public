package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class Icebreaker extends DepthsAbility {

	public static final String ABILITY_NAME = "Icebreaker";
	public static final double[] ICE_DAMAGE = {1.24, 1.28, 1.32, 1.36, 1.40, 1.48};
	public static final double[] EFFECT_DAMAGE = {1.12, 1.14, 1.16, 1.18, 1.20, 1.24};

	public Icebreaker(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.TUBE_CORAL_FAN;
		mTree = DepthsTree.FROSTBORN;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		event.setDamage(event.getDamage() * getMultiplier(enemy));
		return false; // only changes event damage
	}

	private boolean isOnIce(LivingEntity entity) {
		Location loc = entity.getLocation();
		return DepthsUtils.isIce(loc.getBlock().getRelative(BlockFace.DOWN).getType()) && DepthsUtils.iceActive.containsKey(loc.getBlock().getRelative(BlockFace.DOWN).getLocation());
	}

	private double getMultiplier(LivingEntity entity) {
		List<PotionEffectType> e = PotionUtils.getNegativeEffects(mPlugin, entity);
		if (isOnIce(entity)) {
			return ICE_DAMAGE[mRarity - 1];
		} else if (e.size() > 0 || EntityUtils.isStunned(entity) || EntityUtils.isParalyzed(mPlugin, entity) || EntityUtils.isBleeding(mPlugin, entity)
				|| EntityUtils.isSlowed(mPlugin, entity) || EntityUtils.isWeakened(mPlugin, entity) || EntityUtils.isSilenced(entity) || EntityUtils.vulnerabilityMult(entity) > 1
				|| entity.getFireTicks() > 0 || Inferno.hasInferno(mPlugin, entity) || EntityUtils.hasDamageOverTime(mPlugin, entity)) {
			return EFFECT_DAMAGE[mRarity - 1];
		}
		return 1;
	}

	@Override
	public String getDescription(int rarity) {
		return "Damage you deal to mobs that are on ice is multiplied by " + DepthsUtils.getRarityColor(rarity) + ICE_DAMAGE[rarity - 1] + ChatColor.WHITE + ". Damage you deal to mobs that are debuffed but not on ice is multiplied by " + DepthsUtils.getRarityColor(rarity) + EFFECT_DAMAGE[rarity - 1] + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}
}

