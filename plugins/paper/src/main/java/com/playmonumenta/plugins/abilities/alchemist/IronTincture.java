package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class IronTincture extends Ability {

	private static final int IRON_TINCTURE_THROW_COOLDOWN = 10 * 20;
	private static final int IRON_TINCTURE_USE_COOLDOWN = 50 * 20;
	private static final int IRON_TINCTURE_1_ABSORPTION = 8;
	private static final int IRON_TINCTURE_2_ABSORPTION = 12;
	private static final int IRON_TINCTURE_ABSORPTION_DURATION = 20 * 50;
	private static final int IRON_TINCTURE_TICK_PERIOD = 2;
	private static final double IRON_TINCTURE_VELOCITY = 0.7;

	private @Nullable AlchemistPotions mAlchemistPotions;

	public IronTincture(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Iron Tincture");
		mInfo.mLinkedSpell = ClassAbility.IRON_TINCTURE;
		mInfo.mScoreboardId = "IronTincture";
		mInfo.mShorthandName = "IT";
		mInfo.mDescriptions.add("Crouch and right-click to throw a tincture. If you walk over the tincture, gain 8 absorption health for 50 seconds, up to 8 absorption health. If an ally walks over it, or is hit by it, you both gain the effect. If it isn't grabbed before it disappears it will quickly come off cooldown. When another player grabs the tincture, you gain 2 Alchemist's Potions. When you grab the tincture, you gain 1 Alchemist's Potion. Cooldown: 50s.");
		mInfo.mDescriptions.add("Effect and effect cap increased to 12 absorption health.");
		mInfo.mCooldown = IRON_TINCTURE_USE_COOLDOWN; // Full duration cooldown
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.SPLASH_POTION, 1);

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && !ItemUtils.isSomeBow(mainHand) && !ItemUtils.isAlchemistItem(mainHand) && mainHand.getType() != Material.SPLASH_POTION && mainHand.getType() != Material.LINGERING_POTION && !mainHand.getType().isBlock();
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location loc = mPlayer.getEyeLocation();
		ItemStack itemTincture = new ItemStack(Material.SPLASH_POTION);
		ItemMeta tinctMeta = itemTincture.getItemMeta();
		tinctMeta.displayName(Component.text("Iron Tincture", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		itemTincture.setItemMeta(tinctMeta);
		// Add infinity enchantment so that potion injector cannot use it
		ItemStatUtils.addEnchantment(itemTincture, ItemStatUtils.EnchantmentType.INFINITY, 1);
		ItemUtils.setPlainName(itemTincture, "Iron Tincture");
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1, 0.15f);
		Item tincture = world.dropItem(loc, itemTincture);
		tincture.setPickupDelay(Integer.MAX_VALUE);

		Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
		vel.multiply(IRON_TINCTURE_VELOCITY);

		tincture.setVelocity(vel);
		tincture.setGlowing(true);

		mInfo.mCooldown = IRON_TINCTURE_USE_COOLDOWN;
		// Full duration cooldown - is shortened if not picked up
		putOnCooldown();

		new BukkitRunnable() {
			int mTinctureDecay = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.SPELL, tincture.getLocation(), 3, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);

				for (Player p : PlayerUtils.playersInRange(tincture.getLocation(), 1, true)) {
					// Prevent players from picking up their own tincture instantly
					if (p == mPlayer && tincture.getTicksLived() < 12) {
						continue;
					}

					world.playSound(tincture.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.85f);
					new PartialParticle(Particle.BLOCK_DUST, tincture.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData()).spawnAsPlayerActive(p);
					new PartialParticle(Particle.FIREWORKS_SPARK, tincture.getLocation(), 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(p);
					tincture.remove();

					execute(mPlayer);
					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharge();
					}

					if (p != mPlayer) {
						execute(p);
						if (mAlchemistPotions != null) {
							mAlchemistPotions.incrementCharge();
						}
					}

					mPlugin.mTimers.removeCooldown(mPlayer, mInfo.mLinkedSpell);
					putOnCooldown();

					this.cancel();
					return;
				}

				mTinctureDecay += IRON_TINCTURE_TICK_PERIOD;
				if (mTinctureDecay >= IRON_TINCTURE_THROW_COOLDOWN || !tincture.isValid() || tincture.isDead()) {
					tincture.remove();
					this.cancel();

					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.addCooldown(mPlayer, mInfo.mLinkedSpell, 0);
				}
			}

		}.runTaskTimer(mPlugin, 0, IRON_TINCTURE_TICK_PERIOD);
	}

	private void execute(Player player) {
		int absorption = getAbilityScore() == 1 ? IRON_TINCTURE_1_ABSORPTION : IRON_TINCTURE_2_ABSORPTION;

		AbsorptionUtils.addAbsorption(player, absorption, absorption, IRON_TINCTURE_ABSORPTION_DURATION);

		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
		new PartialParticle(Particle.FLAME, player.getLocation(), 30, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(player);
		new BukkitRunnable() {
			double mRotation = 0;
			double mY = 0.15;
			final double mRadius = 1.15;
			@Override
			public void run() {
				Location loc = player.getLocation();
				mRotation += 15;
				mY += 0.175;
				for (int i = 0; i < 3; i++) {
					double degree = Math.toRadians(mRotation + (i * 120));
					loc.add(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
					new PartialParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.05).minimumMultiplier(false).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.05, 0.05, 0.05, 0).minimumMultiplier(false).spawnAsPlayerActive(player);
					loc.subtract(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
