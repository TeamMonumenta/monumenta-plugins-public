package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class ManaLance extends Ability {

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255),
			1.0f);
	private static final int MANA_LANCE_STAGGER_DURATION = (int) (0.95 * 20);

	@Override
	public boolean cast(Player player) {
		int manaLance = getAbilityScore(player);
		ItemStack mainHand = player.getInventory().getItemInMainHand();

		int extraDamage = manaLance == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;

		Location loc = player.getEyeLocation();
		Vector dir = loc.getDirection();
		loc.add(dir);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < 8; i++) {
			loc.add(dir);

			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05, 0.05, 0.05, 0.025);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (loc.getBlock().getType().isSolid()) {
				loc.subtract(dir.multiply(0.5));
				mWorld.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125);
				mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 0.5)) {
				AbilityUtils.mageSpellshock(mPlugin, mob, extraDamage, player, MagicType.ARCANE);
				mob.addPotionEffect(
						new PotionEffect(PotionEffectType.SLOW, MANA_LANCE_STAGGER_DURATION, 10, true, false));
			}
		}
		PlayerUtils.callAbilityCastEvent(player, Spells.MANA_LANCE);
		putOnCooldown(player);
		mWorld.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.linkedSpell = Spells.MANA_LANCE;
		info.scoreboardId = "ManaLance";
		int cd = ScoreboardUtils.getScoreboardValue(player, info.scoreboardId) == 1 ? MANA_LANCE_1_COOLDOWN
				: MANA_LANCE_2_COOLDOWN;
		info.cooldown = cd;
		info.trigger = AbilityTrigger.RIGHT_CLICK;
		return info;
	}

	@Override
	public boolean runCheck(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		return !player.isSneaking() && InventoryUtils.isWandItem(mainHand)
				&& player.getGameMode() != GameMode.SPECTATOR;
	}

}
