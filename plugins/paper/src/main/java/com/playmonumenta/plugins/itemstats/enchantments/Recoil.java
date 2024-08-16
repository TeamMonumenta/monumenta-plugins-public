package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Collection;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Recoil implements Enchantment {
	public static final String CHARM_VELOCITY = "Recoil Velocity";

	@Override
	public String getName() {
		return "Recoil";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RECOIL;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile proj) {
		if (EntityUtils.isAbilityTriggeringProjectile(proj, true)) {
			if (player.isSneaking()) {
				Material type = player.getInventory().getItemInMainHand().getType();
				if (player.getCooldown(type) < 10) {
					player.setCooldown(type, 10);
				}
			} else if (!ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !proj.getScoreboardTags().contains("NoRecoil")) {
				applyRecoil(player, level);
			}
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		ItemStack item = player.getInventory().getItemInMainHand();
		Collection<AttributeModifier> modifiers = item.getItemMeta().getAttributeModifiers(Attribute.GENERIC_ATTACK_SPEED);
		boolean hasCumbersome = ItemStatUtils.hasEnchantment(item, EnchantmentType.CUMBERSOME);
		if (event.getType() == DamageEvent.DamageType.MELEE
			&& (PlayerUtils.isFallingAttack(player) || (hasCumbersome && player.getCooledAttackStrength(0.5f) > 0.9))
			&& !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			&& modifiers != null) {
			applyRecoil(player, level);
		}
	}

	public static void applyRecoil(Player player, double level) {
		Vector velocity = NmsUtils.getVersionAdapter().getActualDirection(player).multiply(-0.5 * Math.sqrt(CharmManager.calculateFlatAndPercentValue(player, CHARM_VELOCITY, level)));
		velocity.setY(Math.max(0.1, velocity.getY()));
		player.setVelocity(velocity);
	}
}
