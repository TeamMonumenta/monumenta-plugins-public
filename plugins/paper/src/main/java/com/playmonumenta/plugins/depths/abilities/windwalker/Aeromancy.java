package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Aeromancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Aeromancy";
	public static final double[] PLAYER_DAMAGE = {1.12, 1.15, 1.18, 1.21, 1.24, 1.3};
	public static final double[] MOB_DAMAGE = {1.056, 1.07, 1.084, 1.098, 1.112, 1.156};

	public static final DepthsAbilityInfo<Aeromancy> INFO =
		new DepthsAbilityInfo<>(Aeromancy.class, ABILITY_NAME, Aeromancy::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.FEATHER))
			.descriptions(Aeromancy::getDescription);

	public Aeromancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		event.setDamage(event.getDamage() * damageMultiplier(enemy));
		return false; // only changes event damage
	}

	private double damageMultiplier(Entity damagee) {
		double multiplier = 1;
		if (!PlayerUtils.isOnGround(mPlayer)) {
			multiplier *= PLAYER_DAMAGE[mRarity - 1];
		}
		if (!damagee.isOnGround()) {
			multiplier *= MOB_DAMAGE[mRarity - 1];
		}
		return multiplier;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("All damage you deal while airborne is multiplied by ")
			.append(Component.text(PLAYER_DAMAGE[rarity - 1], color))
			.append(Component.text(". Additionally, all damage you deal against airborne enemies is multiplied by "))
			.append(Component.text(MOB_DAMAGE[rarity - 1], color))
			.append(Component.text("."));
	}


}

