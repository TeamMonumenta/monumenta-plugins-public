package com.playmonumenta.plugins.depths.abilities.shadow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import net.md_5.bungee.api.ChatColor;

public class DarkCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Dark Combos";
	public static final double[] VULN_AMPLIFIER = {0.15, 0.1875, 0.225, 0.2625, 0.3, 0.375};
	public static final int DURATION = 20 * 3;

	private int mComboCount = 0;

	public DarkCombos(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.FLINT;
		mTree = DepthsTree.SHADOWS;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				EntityUtils.applyVulnerability(mPlugin, DURATION, VULN_AMPLIFIER[mRarity - 1], (LivingEntity) event.getEntity());
				mComboCount = 0;

				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 0.5f);
				loc.getWorld().spawnParticle(Particle.SPELL_WITCH, event.getEntity().getLocation(), 15, 0.5, 0.2, 0.5, 0.65);
				PotionUtils.applyPotion(mPlayer, (LivingEntity) event.getEntity(),
					new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
			}
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Every third melee attack applies " + DepthsUtils.getRarityColor(rarity) + VULN_AMPLIFIER[rarity - 1] + ChatColor.WHITE + " vulnerability for " + DURATION / 20 + " seconds.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.COMBO;
	}
}

