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
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DarkCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Dark Combos";
	public static final double[] VULN_AMPLIFIER = {0.15, 0.1875, 0.225, 0.2625, 0.3, 0.375};
	public static final int DURATION = 20 * 3;

	public static final DepthsAbilityInfo<DarkCombos> INFO =
		new DepthsAbilityInfo<>(DarkCombos.class, ABILITY_NAME, DarkCombos::new, DepthsTree.SHADOWDANCER, DepthsTrigger.COMBO)
			.displayItem(Material.FLINT)
			.descriptions(DarkCombos::getDescription);

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

				playSounds(mPlayer.getWorld(), mPlayer.getLocation());
				new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 15, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(mPlayer);
				PotionUtils.applyPotion(mPlayer, enemy,
					new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
			}
			return true;
		}
		return false;
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.6f);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Every third melee attack applies ")
			       .append(Component.text(StringUtils.multiplierToPercentage(VULN_AMPLIFIER[rarity - 1]) + "%", color))
			       .append(Component.text(" vulnerability for " + DURATION / 20 + " seconds."));
	}


}

