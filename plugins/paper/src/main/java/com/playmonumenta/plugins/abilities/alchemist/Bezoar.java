package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class Bezoar extends Ability {
	private static final int FREQUENCY = 5;
	private static final int LINGER_TIME = 10 * 20;
	private static final int DEBUFF_REDUCTION = 10 * 20;
	private static final int HEAL_DURATION = 2 * 20;
	private static final double HEAL_PERCENT = 0.05;
	private static final int DAMAGE_DURATION = 8 * 20;
	private static final double DAMAGE_PERCENT = 0.15;

	private int mKills = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public Bezoar(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bezoar");
		mInfo.mLinkedSpell = ClassAbility.BEZOAR;
		mInfo.mScoreboardId = "Bezoar";
		mInfo.mShorthandName = "BZ";
		mInfo.mDescriptions.add("Every 5th mob killed within 16 blocks of the Alchemist spawns a Bezoar that lingers for 10s. Picking up a Bezoar will grant the Alchemist an additional Alchemist Potion, and will grant both the player who picks it up and the Alchemist a custom healing effect that regenerates 5% of max health every second for 2 seconds and reduces the duration of all current potion debuffs by 10s.");
		mInfo.mDescriptions.add("The Bezoar now additionally grants +15% damage from all sources for 8s.");
		mDisplayItem = new ItemStack(Material.LIME_CONCRETE, 1);

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
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
				new PartialParticle(Particle.FALLING_DUST, item.getLocation(), 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsPlayerActive(mPlayer);
				for (Player p : PlayerUtils.playersInRange(item.getLocation(), 1, true)) {
					if (p != mPlayer) {
						applyEffects(p);
					}
					applyEffects(mPlayer);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharge();
					}

					item.remove();

					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
					new PartialParticle(Particle.BLOCK_CRACK, item.getLocation(), 30, 0.15, 0.15, 0.15, 0.75F, Material.LIME_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.TOTEM, item.getLocation(), 20, 0, 0, 0, 0.35F).spawnAsPlayerActive(mPlayer);

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

	private void applyEffects(Player player) {
		if (mPlayer == null) {
			return;
		}

		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - DEBUFF_REDUCTION, 0), effect.getAmplifier()));
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(player);
		mPlugin.mEffectManager.addEffect(player, "BezoarHealing", new CustomRegeneration(HEAL_DURATION, maxHealth * HEAL_PERCENT, mPlayer, mPlugin));

		if (getAbilityScore() > 1) {
			mPlugin.mEffectManager.addEffect(player, "BezoarPercentDamageDealtEffect", new PercentDamageDealt(DAMAGE_DURATION, DAMAGE_PERCENT));
		}
	}

	public boolean shouldDrop() {
		return mKills >= FREQUENCY;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

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
