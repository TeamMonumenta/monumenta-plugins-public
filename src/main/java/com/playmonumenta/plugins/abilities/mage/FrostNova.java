package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class FrostNova extends Ability {

	private static final float FROST_NOVA_RADIUS = 6.0f;
	private static final int FROST_NOVA_1_DAMAGE = 3;
	private static final int FROST_NOVA_2_DAMAGE = 6;
	private static final int FROST_NOVA_EFFECT_LVL = 2;
	private static final int FROST_NOVA_COOLDOWN = 18 * 20;
	private static final int FROST_NOVA_DURATION = 8 * 20;
	
	@Override
	public boolean cast(Player player) {
		int frostNova = getAbilityScore(player);
		player.setFireTicks(0);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), FROST_NOVA_RADIUS)) {
			int extraDamage = frostNova == 1 ? FROST_NOVA_1_DAMAGE : FROST_NOVA_2_DAMAGE;
			AbilityUtils.mageSpellshock(mPlugin, mob, extraDamage, player, MagicType.ICE);

			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, FROST_NOVA_EFFECT_LVL, true, false));
			if (frostNova > 1) {
				EntityUtils.applyFreeze(mPlugin, FROST_NOVA_DURATION, mob);
			}

			mob.setFireTicks(0);
		}
		PlayerUtils.callAbilityCastEvent(player, Spells.FROST_NOVA);
		mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.FROST_NOVA, FROST_NOVA_COOLDOWN);

		Location loc = player.getLocation();
		mWorld.spawnParticle(Particle.SNOW_SHOVEL, loc.add(0, 1, 0), 400, 4, 1, 4, 0.001);
		mWorld.spawnParticle(Particle.CRIT_MAGIC, loc.add(0, 1, 0), 200, 4, 1, 4, 0.001);
		mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
		putOnCooldown(player);
		return true;
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.linkedSpell = Spells.FROST_NOVA;
		info.scoreboardId = "FrostNova";
		info.cooldown = FROST_NOVA_COOLDOWN;
		info.trigger = AbilityTrigger.LEFT_CLICK;
		return info;
	}
	
	@Override
	public boolean runCheck(Player player) {
		if (player.isSneaking()) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			return InventoryUtils.isWandItem(mainHand);
		}
		return false;
	}
	
}
