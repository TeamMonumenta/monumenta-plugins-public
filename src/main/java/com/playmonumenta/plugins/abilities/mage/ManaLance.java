package com.playmonumenta.plugins.abilities.mage;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ManaLance extends Ability {

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);
	private static final int MANA_LANCE_STAGGER_DURATION = 20;

	public ManaLance(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.MANA_LANCE;
		mInfo.scoreboardId = "ManaLance";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? MANA_LANCE_1_COOLDOWN : MANA_LANCE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		int manaLance = getAbilityScore();

		int extraDamage = manaLance == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;
		boolean hasSpellShock = AbilityManager.getManager().getPlayerAbility(mPlayer, Spellshock.class) != null;

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < 8; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(mWorld);

			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.05, 0.05, 0.05, 0.025);
			mWorld.spawnParticle(Particle.REDSTONE, bLoc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				mWorld.spawnParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125);
				mWorld.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					if (hasSpellShock) {
						Spellshock.spellDamageMob(mPlugin, mob, extraDamage, mPlayer, MagicType.ARCANE);
					}
					Spellshock.addStaticToMob(mob, mPlayer);
					if (!EntityUtils.isBoss(mob) && !mob.getScoreboardTags().contains(Constants.PLAYER_DISABLE_PVP_TAG))
						mob.addPotionEffect(
						    new PotionEffect(PotionEffectType.SLOW, MANA_LANCE_STAGGER_DURATION, 10, true, false));
					iter.remove();
				}
			}
		}
		PlayerUtils.callAbilityCastEvent(mPlayer, Spells.MANA_LANCE);
		putOnCooldown();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && InventoryUtils.isWandItem(mainHand)
		       && mPlayer.getGameMode() != GameMode.SPECTATOR;
	}

}
