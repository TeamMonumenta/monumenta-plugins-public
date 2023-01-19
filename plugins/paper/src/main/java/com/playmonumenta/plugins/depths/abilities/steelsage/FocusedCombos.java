package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public class FocusedCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Focused Combos";
	public static final double[] DAMAGE = {1.40, 1.45, 1.50, 1.55, 1.60, 2.00};
	public static final double BLEED_AMOUNT = 0.2;
	public static final int BLEED_DURATION = 20 * 3;

	public static final DepthsAbilityInfo<FocusedCombos> INFO =
		new DepthsAbilityInfo<>(FocusedCombos.class, ABILITY_NAME, FocusedCombos::new, DepthsTree.STEELSAGE, DepthsTrigger.COMBO)
			.displayItem(new ItemStack(Material.SPECTRAL_ARROW))
			.descriptions(FocusedCombos::getDescription);

	private int mComboCount = 0;

	public FocusedCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getDamager() instanceof Projectile proj && event.getType() == DamageType.PROJECTILE && EntityUtils.isAbilityTriggeringProjectile(proj, true)) {
			mComboCount++;

			if (mComboCount >= 3) {
				mComboCount = 0;
				EntityUtils.applyBleed(mPlugin, BLEED_DURATION, BLEED_AMOUNT, enemy);
				event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);

				Location playerLoc = mPlayer.getLocation();
				mPlayer.playSound(playerLoc, Sound.BLOCK_WEEPING_VINES_BREAK, SoundCategory.PLAYERS, 2, 0.8f);
				mPlayer.playSound(playerLoc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4f, 1.75f);
			}
			return true;
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Every third critical projectile shot deals ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]) + "x", color))
			.append(Component.text(" damage and applies " + StringUtils.multiplierToPercentage(BLEED_AMOUNT) + "% Bleed for " + BLEED_DURATION / 20 + " seconds."));
	}


}
