package com.playmonumenta.plugins.abilities.alchemist;

import java.util.EnumSet;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentRegeneration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Bezoar extends Ability {
	private static final int FREQUENCY = 5;
	private static final int LINGER_TIME = 10 * 20;
	private static final int DEBUFF_REDUCTION = 10 * 20;
	private static final int HEAL_DURATION = 2 * 20;
	private static final double HEAL_PERCENT = 0.05;
	private static final int DAMAGE_DURATION = 8 * 20;
	private static final double DAMAGE_PERCENT = 0.15;
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.CUSTOM
	);

	private int mKills = 0;

	public Bezoar(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bezoar");
		mInfo.mLinkedSpell = ClassAbility.BEZOAR;
		mInfo.mScoreboardId = "Bezoar";
		mInfo.mShorthandName = "BZ";
		mInfo.mDescriptions.add("Every 5th mob killed within 16 blocks of the Alchemist spawns a Bezoar that lingers for 10s. Picking up a Bezoar will grant the Alchemist an additional Alchemist Potion, and will grant both the player who picks it up and the Alchemist a custom healing effect that regenerates 5% of max health every second for 2 seconds and reduces the duration of all current potion debuffs by 10s.");
		mInfo.mDescriptions.add("The Bezoar now additionally grants +15% ability damage for 8s.");
		mDisplayItem = new ItemStack(Material.LIME_CONCRETE, 1);
	}

	public void dropBezoar(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills = 0;
		World world = event.getEntity().getWorld();
		Location loc = event.getEntity().getLocation().add(0, 0.25, 0);
		ItemStack itemBezoar = new ItemStack(Material.LIME_CONCRETE);
		ItemMeta bezoarMeta = itemBezoar.getItemMeta();
		bezoarMeta.displayName(Component.text("Bezoar", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		itemBezoar.setItemMeta(bezoarMeta);
		ItemUtils.setPlainName(itemBezoar, "Bezoar");
		Item item = world.dropItemNaturally(loc, itemBezoar);
		item.setGlowing(true);
		item.setPickupDelay(Integer.MAX_VALUE);

		new BukkitRunnable() {
			int mT = 0;
			final BlockData mFallingDustData = Material.LIME_CONCRETE.createBlockData();
			@Override
			public void run() {
				if (mPlayer == null) {
					return;
				}
				mT++;
				world.spawnParticle(Particle.FALLING_DUST, item.getLocation(), 1, 0.2, 0.2, 0.2, mFallingDustData);
				//Other player
				for (Player p : PlayerUtils.playersInRange(item.getLocation(), 1, true)) {
					if (!Objects.equals(p.getName(), mPlayer.getName())) {
						for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, p)) {
							PotionEffect effect = p.getPotionEffect(effectType);
							if (effect != null) {
								p.removePotionEffect(effectType);
								// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
								p.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - DEBUFF_REDUCTION, 0), effect.getAmplifier()));
							}
						}

						double maxHealth = EntityUtils.getMaxHealth(p);
						//if the player has the effect refresh the duration
						if (mPlugin.mEffectManager.hasEffect(p, "BezoarHealing")) {
							mPlugin.mEffectManager.clearEffects(p, "BezoarHealing");
						}
						mPlugin.mEffectManager.addEffect(p, "BezoarHealing", new PercentRegeneration(HEAL_DURATION, maxHealth * HEAL_PERCENT));

						if (getAbilityScore() > 1) {
							mPlugin.mEffectManager.addEffect(p, "BezoarAbilityDamage", new PercentDamageDealt(DAMAGE_DURATION, DAMAGE_PERCENT, AFFECTED_DAMAGE_CAUSES));
						}
					}
					//Alchemist
					for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, mPlayer)) {
						PotionEffect effect = mPlayer.getPotionEffect(effectType);
						if (effect != null) {
							mPlayer.removePotionEffect(effectType);
							// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
							mPlayer.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - DEBUFF_REDUCTION, 0), effect.getAmplifier()));
						}
					}

					double maxHealth = EntityUtils.getMaxHealth(mPlayer);
					if (mPlugin.mEffectManager.hasEffect(mPlayer, "BezoarHealing")) {
						mPlugin.mEffectManager.clearEffects(mPlayer, "BezoarHealing");
					}
					mPlugin.mEffectManager.addEffect(mPlayer, "BezoarHealing", new PercentRegeneration(HEAL_DURATION, maxHealth * HEAL_PERCENT));

					if (getAbilityScore() > 1) {
						mPlugin.mEffectManager.addEffect(mPlayer, "BezoarAbilityDamage", new PercentDamageDealt(DAMAGE_DURATION, DAMAGE_PERCENT, AFFECTED_DAMAGE_CAUSES));
					}

					AlchemistPotions alchemistPotion = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
					if (alchemistPotion != null) {
						alchemistPotion.incrementCharge();
					}

					item.remove();

					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
					world.spawnParticle(Particle.BLOCK_CRACK, item.getLocation(), 30, 0.15, 0.15, 0.15, 0.75F, Material.LIME_CONCRETE.createBlockData());
					world.spawnParticle(Particle.TOTEM, item.getLocation(), 20, 0, 0, 0, 0.35F);

					this.cancel();
					break;
				}
				if (mT >= LINGER_TIME || item.isDead()) {
					this.cancel();
					item.remove();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void incrementKills() {
		mKills++;
	}

	public boolean shouldDrop() {
		return mKills >= FREQUENCY;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills++;
		if (shouldDrop()) {
			dropBezoar(event, shouldGenDrops);
		}
	}

	@Override
	public double entityDeathRadius() {
		return 16;
	}

}
