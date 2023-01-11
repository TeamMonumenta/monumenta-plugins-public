package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DarkCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Dark Combos";
	public static final double[] VULN_AMPLIFIER = {0.15, 0.1875, 0.225, 0.2625, 0.3, 0.375};
	public static final int DURATION = 20 * 3;

	public static final DepthsAbilityInfo<DarkCombos> INFO =
		new DepthsAbilityInfo<>(DarkCombos.class, ABILITY_NAME, DarkCombos::new, DepthsTree.SHADOWDANCER, DepthsTrigger.COMBO)
			.displayItem(new ItemStack(Material.FLINT))
			.descriptions(DarkCombos::getDescription, MAX_RARITY);

	private int mComboCount = 0;

	public DarkCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				EntityUtils.applyVulnerability(mPlugin, DURATION, VULN_AMPLIFIER[mRarity - 1], enemy);
				mComboCount = 0;

				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.6f, 0.5f);
				new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 15, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(mPlayer);
				PotionUtils.applyPotion(mPlayer, enemy,
					new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
			}
			return true;
		}
		return false;
	}

	private static String getDescription(int rarity) {
		return "Every third melee attack applies " + DepthsUtils.getRarityColor(rarity) + VULN_AMPLIFIER[rarity - 1] + ChatColor.WHITE + " vulnerability for " + DURATION / 20 + " seconds.";
	}


}

