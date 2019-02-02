package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Crouch while throwing an Alchemist Potion and you'll instead consume 4
 * potions and buff yourself. You gain 20s of Regen I, Strength I, Speed I, but
 * when the effect ends you take 2 hearts of damage (bypasses armor but can't
 * kill the player). At level 2 you also gain Resistance I and Haste I (Cooldown
 * 30s)
 *
 * TODO: Particle effects need flair
 */

public class AdrenalSerum extends Ability {
	private static final int ADRENAL_SERUM_COOLDOWN = 30 * 20;
	private static final int ADRENAL_SERUM_DURATION = 20 * 20;
	private static final int ADRENAL_SERUM_POTIONS_CONSUMED = 4;
	private static final double ADRENAL_SERUM_DAMAGE = 4;
	private static final Particle.DustOptions ADRENAL_SERUM_COLOR = new Particle.DustOptions(Color.fromRGB(185, 0, 0), 1.0f);

	public AdrenalSerum(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ADRENAL_SERUM;
		mInfo.scoreboardId = "AdrenalSerum";
		mInfo.cooldown = ADRENAL_SERUM_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.testForItemWithName(inMainHand, "Alchemist's Potion")) {
			Inventory inv = mPlayer.getInventory();
			ItemStack firstFoundPotStack = null;
			int potCount = 0;
			for (ItemStack item : inv.getContents()) {
				if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
					if (firstFoundPotStack == null) {
						firstFoundPotStack = item;
					}
					potCount += item.getAmount();
				}
			}
			return potCount > ADRENAL_SERUM_POTIONS_CONSUMED && mPlayer.isSneaking();
		}
		return false;
	}

	@Override
	public boolean cast() {
		Inventory inv = mPlayer.getInventory();
		int amountConsumed = 0;
		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				if (amountConsumed >= 3) {
					break;
				}
				if (item.getAmount() >= 3) {
					item.setAmount(item.getAmount() - 3);
					break;
				} else {
					for (int i = 0; i < item.getAmount(); i++) {
						item.setAmount(item.getAmount() - 1);
						amountConsumed++;
						if (item.getAmount() <= 0 || amountConsumed >= 3) {
							break;
						}
					}
				}
			}
		}

		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 30, 0.75f, 0.25f, 0.75f, 0.5f); //Rudimentary effects
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1f, 1.15f);
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, ADRENAL_SERUM_DURATION, 0, true, true));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, ADRENAL_SERUM_DURATION, 0, true, true));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ADRENAL_SERUM_DURATION, 0, true, true));
		if (getAbilityScore() == 2) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, ADRENAL_SERUM_DURATION, 0, true, true));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, ADRENAL_SERUM_DURATION, 0, true, true));
		}

		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				t++;
				mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, ADRENAL_SERUM_COLOR);

				if (t > ADRENAL_SERUM_DURATION) {
					if (mPlayer.getHealth() > ADRENAL_SERUM_DAMAGE + 1) {
						mPlayer.damage(ADRENAL_SERUM_DAMAGE);
					} else {
						mPlayer.damage(mPlayer.getHealth() - 1);
					}
					this.cancel();
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1);
					BlockData fallingDustData = Material.NETHER_WART_BLOCK.createBlockData();
					mWorld.spawnParticle(Particle.FALLING_DUST, mPlayer.getLocation().add(0, 1, 0), 25, 0.25, 0.45, 0.25, fallingDustData);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
		return true;
	}
}
