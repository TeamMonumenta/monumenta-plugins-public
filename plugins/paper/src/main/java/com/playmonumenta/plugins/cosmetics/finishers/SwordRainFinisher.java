package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

public class SwordRainFinisher implements EliteFinisher {

	public static final String NAME = "Sword Rain";

	public static class Weapon {
		private String mName;
		private Material mMaterial;

		public Weapon(String name, Material material) {
			this.mName = name;
			this.mMaterial = material;
		}

		public String getName() {
			return mName;
		}

		public Material getMaterial() {
			return mMaterial;
		}
	}

	public static final Weapon[] ITEMS = {
		new Weapon("Abisso Ancestrale", Material.STONE_SWORD),
		new Weapon("Harrakfar's Claw", Material.IRON_SWORD),
		new Weapon("Cutter of Eons", Material.DIAMOND_SWORD),
		new Weapon("Stormblessed Greatspear", Material.TRIDENT),
		new Weapon("Veinshredder", Material.IRON_HOE),
		new Weapon("Hollow Gladius", Material.STONE_SWORD),
		new Weapon("The Vedha's Soulcrusher", Material.WOODEN_AXE),
		new Weapon("Tactician's Axe", Material.IRON_AXE),
		new Weapon("Shroombane", Material.IRON_AXE),
		new Weapon("Blade of Reclamation", Material.IRON_SWORD),
		new Weapon("Regulated Cogblade", Material.IRON_SWORD),
		new Weapon("Wrench of the Perfect Atrium", Material.IRON_AXE),
		new Weapon("Air-Purified Thresher", Material.IRON_HOE),
		new Weapon("Will of the Codex", Material.IRON_SWORD),
		new Weapon("Nalatia's Galeblade", Material.IRON_SWORD),
		new Weapon("Threadreaper", Material.IRON_HOE),
		new Weapon("Death's Echo", Material.WOODEN_HOE),
		new Weapon("Hycenea's Will", Material.IRON_HOE),
		new Weapon("Twisted Pike", Material.TRIDENT),
		new Weapon("Epoch Hammer", Material.IRON_AXE),
		new Weapon("Crescent of Dominion", Material.STONE_HOE),
		new Weapon("Resonant Scythe", Material.IRON_HOE),
		new Weapon("Hammer of Legacy", Material.STONE_AXE),
		new Weapon("Fleshbreaker", Material.IRON_AXE),
		new Weapon("Soul of Ishnir", Material.GOLDEN_HOE),
		new Weapon("Sword of the Exiled", Material.DIAMOND_SWORD),
		new Weapon("Frost Giant's Crescent", Material.IRON_HOE),
		new Weapon("Frost Giant's Crusher", Material.IRON_AXE),
		new Weapon("Frost Giant's Greatsword", Material.IRON_SWORD),
		new Weapon("Energized Maelstrom", Material.IRON_SWORD),
		new Weapon("Firecoral Lance", Material.TRIDENT),
		new Weapon("Phlogiston Fissure", Material.IRON_HOE),
		new Weapon("Marauder's Haze", Material.GOLDEN_HOE),
		new Weapon("Treasured Rapier", Material.IRON_SWORD),
		new Weapon("Unwavering Will", Material.IRON_SWORD),
		new Weapon("Swoard", Material.WOODEN_SWORD),
		new Weapon("Demon's Scar", Material.STONE_HOE),
		new Weapon("Devouring Wind", Material.GOLDEN_AXE),
		new Weapon("Harmony's End", Material.STONE_SWORD),
		new Weapon("Tournament Longsword", Material.IRON_SWORD),
		new Weapon("Phoenix's Gaze", Material.GOLDEN_AXE),
		new Weapon("Waverider", Material.TRIDENT),
		new Weapon("Shipwrecker's Sabre", Material.IRON_SWORD),
		new Weapon("Thrashing Tide", Material.IRON_HOE),
		new Weapon("Hrimnir's Incisor", Material.IRON_HOE),
		new Weapon("Titan Spear", Material.TRIDENT),
		new Weapon("Entcrusher", Material.WOODEN_AXE),
		new Weapon("Bloody Fang", Material.IRON_HOE),
		new Weapon("Edge of Tiferet", Material.WOODEN_SWORD),
		new Weapon("Beastblight Machete", Material.IRON_SWORD),
		new Weapon("Slipstrike", Material.STONE_AXE),
		new Weapon("Stormborn Javelin", Material.TRIDENT),
		new Weapon("Busturus", Material.IRON_SWORD),
		new Weapon("Hurricane Halberd", Material.STONE_AXE),
		new Weapon("Mutiny", Material.IRON_HOE),
		new Weapon("Phantom Cutlass", Material.IRON_SWORD),
		new Weapon("Gold Cursed Polearm", Material.TRIDENT),
		new Weapon("Oar", Material.WOODEN_AXE),
		new Weapon("Blasphemer's Sickle", Material.IRON_HOE),
		new Weapon("Forlorn Flames", Material.GOLDEN_AXE),
		new Weapon("Putrid Maw", Material.IRON_SWORD),
		new Weapon("Chainbreaker's Blight", Material.IRON_SWORD),
		new Weapon("Mirrorshard Scythe", Material.IRON_HOE),
		new Weapon("Dutiful Blade", Material.STONE_SWORD),
		new Weapon("Erebus", Material.STONE_AXE),
		new Weapon("Soulreaper", Material.GOLDEN_HOE),
		new Weapon("Autumn Wind", Material.GOLDEN_AXE),
		new Weapon("Harmonic Wrath", Material.IRON_SWORD),
		new Weapon("Spearpoint of Baelle", Material.TRIDENT),
		new Weapon("Gillman's Claw", Material.IRON_HOE),
		new Weapon("Iridescence", Material.TRIDENT),
		new Weapon("Silverweight Axe", Material.IRON_AXE),
		new Weapon("Amaranth Blade", Material.IRON_SWORD),
		new Weapon("Archeologist's Hammer", Material.STONE_AXE),
		new Weapon("Gambler's Cane", Material.IRON_HOE),
		new Weapon("True Ice Splinter", Material.IRON_SWORD),
		new Weapon("Dissolvi Ombre", Material.STONE_SWORD),
		new Weapon("Hydraal", Material.TRIDENT),
		new Weapon("Ice Reaper", Material.IRON_HOE),
		new Weapon("Mage Hunter's Blade", Material.IRON_SWORD),
		new Weapon("Oasis Scimitar", Material.IRON_SWORD),
		new Weapon("Wrath of the Mountains", Material.IRON_AXE),
		new Weapon("Gargantuan Harpoon", Material.TRIDENT),
		new Weapon("Hammer of the Architect", Material.IRON_AXE),
		new Weapon("Siege Axe", Material.IRON_AXE),
		new Weapon("Soul Spear", Material.TRIDENT),
		new Weapon("Swordfish", Material.IRON_SWORD),
		new Weapon("Twilight Razor", Material.GOLDEN_HOE),
		new Weapon("Webripper", Material.IRON_SWORD),
		new Weapon("Thunder and Tempest", Material.GOLDEN_SWORD),
		new Weapon("Eclipse Splitter", Material.IRON_SWORD),
		new Weapon("Frodian Saving Grace", Material.IRON_AXE),
		new Weapon("Ichimonji", Material.TRIDENT),
		new Weapon("Iron Pipe", Material.IRON_HOE),
		new Weapon("Longtime Sunshine", Material.IRON_HOE),
		new Weapon("Luck of the Draw", Material.GOLDEN_SWORD),
		new Weapon("Midas Tooth", Material.GOLDEN_HOE),
		new Weapon("Mitten of Madness", Material.GOLDEN_HOE),
		new Weapon("Moon's Shadow", Material.STONE_SWORD),
		new Weapon("Nightfall", Material.IRON_SWORD),
		new Weapon("Plaguebaron's Scythe", Material.WOODEN_HOE),
		new Weapon("Sunrise", Material.IRON_SWORD),
		new Weapon("Wilt", Material.IRON_SWORD),
		new Weapon("Amina's Axe", Material.IRON_AXE),
		new Weapon("Apotheosis Spear (Fire)", Material.TRIDENT),
		new Weapon("Apotheosis Spear (Ice)", Material.TRIDENT),
		new Weapon("Apotheosis Spear (Thunder)", Material.TRIDENT),
		new Weapon("Calvarium", Material.DIAMOND_AXE),
		new Weapon("Celsian Heraldic Blade", Material.IRON_SWORD),
		new Weapon("Monshee's Judgement", Material.GOLDEN_SWORD),
		new Weapon("Myriad's Rapier", Material.GOLDEN_SWORD),
		new Weapon("Rootstrike", Material.STONE_SWORD),
		new Weapon("Stablemaster's Prod", Material.IRON_SWORD),
		new Weapon("Brutal Longsword", Material.STONE_SWORD),
		new Weapon("Chillwind Battleaxe", Material.IRON_AXE),
		new Weapon("Coldsteel Edge", Material.IRON_HOE),
		new Weapon("Everlasting Falchion", Material.GOLDEN_SWORD),
		new Weapon("Gleaming Edge", Material.IRON_SWORD),
		new Weapon("Lumberjack's Axe", Material.IRON_AXE),
		new Weapon("Perilous Zweihander", Material.IRON_SWORD),
		new Weapon("Reaper's Razor", Material.IRON_HOE),
		new Weapon("Seahunter", Material.TRIDENT),
		new Weapon("Silver Warhammer", Material.IRON_AXE),
		new Weapon("Spirit Hammer", Material.IRON_AXE),
		new Weapon("Spirit Skiver", Material.IRON_HOE),
		new Weapon("Spore's Bane", Material.IRON_SWORD),
		new Weapon("Thunderclap Battleaxe", Material.GOLDEN_AXE),
		new Weapon("Daybreaker", Material.STONE_SWORD),
		new Weapon("Ironcore Machete", Material.IRON_SWORD),
		new Weapon("Monsters' Bane", Material.STONE_AXE),
		new Weapon("Necrotic Scythe", Material.IRON_HOE),
		new Weapon("Obsidian Blade", Material.STONE_SWORD),
		new Weapon("Recalling Trident", Material.TRIDENT),
		new Weapon("Silverwind Scythe", Material.IRON_HOE),
		new Weapon("Skullcrusher", Material.IRON_AXE),
		new Weapon("Starbound Warhammer", Material.IRON_AXE),
		new Weapon("Sword of Ages", Material.STONE_SWORD),
		new Weapon("Thunderhead", Material.IRON_AXE),
		new Weapon("Thunderous Claymore", Material.IRON_SWORD),
		new Weapon("Vulture Khopesh", Material.IRON_HOE),
		new Weapon("Acute Webslasher", Material.STONE_SWORD),
		new Weapon("Blazing Hammer", Material.STONE_AXE),
		new Weapon("Bluesteel Longsword", Material.IRON_SWORD),
		new Weapon("Chitincrush", Material.STONE_AXE),
		new Weapon("Cursed Hammer", Material.IRON_AXE),
		new Weapon("Drowned Trident", Material.TRIDENT),
		new Weapon("Frosted Sabre", Material.IRON_SWORD),
		new Weapon("Gale Shortsword", Material.IRON_SWORD),
		new Weapon("Gravemaking Scythe", Material.IRON_HOE),
		new Weapon("Logfeller", Material.IRON_AXE),
		new Weapon("Rusted Sickle", Material.IRON_HOE),
		new Weapon("Steelscale Maul", Material.IRON_AXE),
		new Weapon("Storm Dagger", Material.IRON_SWORD),
		new Weapon("Basaltic Battleaxe", Material.STONE_AXE),
		new Weapon("Biding Saber", Material.STONE_SWORD),
		new Weapon("Frozen Maul", Material.IRON_AXE),
		new Weapon("Graceful Broadsword", Material.STONE_SWORD),
		new Weapon("Ironcast Tomahawk", Material.IRON_AXE),
		new Weapon("Keen Shortsword", Material.IRON_SWORD),
		new Weapon("Prismarine Pike", Material.TRIDENT),
		new Weapon("Raider's Scythe", Material.IRON_HOE),
		new Weapon("Seawater Razor", Material.IRON_HOE),
		new Weapon("Slayer's Maul", Material.STONE_AXE),
		new Weapon("Steel Woodcutter", Material.IRON_AXE),
		new Weapon("Thunder Estoc", Material.GOLDEN_SWORD),
		new Weapon("Wispblade", Material.WOODEN_SWORD),
		new Weapon("Arachnope Axe", Material.STONE_AXE),
		new Weapon("Coldfire Axe", Material.STONE_AXE),
		new Weapon("Creeper's Bane", Material.WOODEN_SWORD),
		new Weapon("Driftwood's Gambit", Material.WOODEN_SWORD),
		new Weapon("Frozen Gladius", Material.STONE_SWORD),
		new Weapon("Hunter's Edge", Material.IRON_HOE),
		new Weapon("Inevitable Truth", Material.STONE_AXE),
		new Weapon("Iron Battleaxe", Material.IRON_AXE),
		new Weapon("Iron Broadsword", Material.IRON_SWORD),
		new Weapon("Iron Scythe", Material.IRON_HOE),
		new Weapon("Spearfisher", Material.TRIDENT),
		new Weapon("Thunder Rapier", Material.GOLDEN_SWORD),
		new Weapon("Myrahg's Pestilence", Material.TRIDENT),
		new Weapon("Jungle's Requiem", Material.WOODEN_SWORD),
		new Weapon("Roots' Repose", Material.WOODEN_HOE),
		new Weapon("Verdant Crusher", Material.WOODEN_AXE),
		new Weapon("Harvestman's Scythe", Material.STONE_HOE),
		new Weapon("Lightbringer", Material.TRIDENT),
		new Weapon("Threadwarped Tkaa", Material.STONE_AXE),
		new Weapon("Pyreborne Spear", Material.TRIDENT),
		new Weapon("Arachnidruid Cutlass", Material.GOLDEN_SWORD),
		new Weapon("Frostbite Scythe", Material.STONE_HOE),
		new Weapon("Lunatic's Respite", Material.STONE_AXE),
		new Weapon("Rosethorn Blade", Material.STONE_SWORD),
		new Weapon("Slicing Wind", Material.WOODEN_SWORD),
		new Weapon("Smouldering Flame", Material.GOLDEN_SWORD),
		new Weapon("Vermin's End", Material.STONE_SWORD),
		new Weapon("Corrupted Geomantic Dagger", Material.GOLDEN_SWORD),
		new Weapon("Corrupted Scalawag's Hatchet", Material.STONE_AXE),
		new Weapon("Corrupted Watcher's Sword", Material.STONE_SWORD),
		new Weapon("Tkaa of C'Axtal", Material.STONE_AXE),
		new Weapon("Coldsteel Blunt", Material.IRON_HOE),
		new Weapon("Fading Wispblade", Material.WOODEN_SWORD),
		new Weapon("Lustrous Edge", Material.STONE_SWORD),
		new Weapon("Squid Hunter", Material.TRIDENT),
		new Weapon("Thawed Frozen Mallet", Material.IRON_AXE),
		new Weapon("Sickle of the Maker", Material.STONE_HOE),
		new Weapon("Telum Immoriel", Material.STONE_SWORD),
		new Weapon("Poison Ivy", Material.STONE_SWORD),
		new Weapon("Sleepwalker's Sickle", Material.STONE_HOE),
		new Weapon("Acrid Wrath", Material.STONE_AXE),
		new Weapon("Alchemist's Insight", Material.STONE_HOE),
		new Weapon("Prismatic Blade", Material.STONE_SWORD),
		new Weapon("Doom's Edge", Material.GOLDEN_SWORD),
		new Weapon("Reaper's Harvest", Material.GOLDEN_HOE),
		new Weapon("Soulcrusher", Material.STONE_AXE),
		new Weapon("Blightbane", Material.TRIDENT),
		new Weapon("Arachnobane", Material.GOLDEN_AXE),
		new Weapon("The Ravager", Material.STONE_AXE),
		new Weapon("Wildthrasher", Material.STONE_SWORD),
		new Weapon("Ashheart Dagger", Material.STONE_SWORD),
		new Weapon("Spiritspark Flint", Material.STONE_AXE),
		new Weapon("Sacrificial Blade", Material.STONE_SWORD),
		new Weapon("Entomology Scalpal", Material.STONE_SWORD),
		new Weapon("Medicine Stick", Material.WOODEN_HOE),
		new Weapon("Earthbound Runeblade", Material.STONE_SWORD),
		new Weapon("Forest's Reaper", Material.STONE_HOE),
		new Weapon("Seasoaked Runeblade", Material.STONE_SWORD),
		new Weapon("Stormborn Runeblade", Material.STONE_SWORD),
		new Weapon("Velara Crusher", Material.STONE_AXE),
		new Weapon("Clearcut", Material.GOLDEN_AXE),
		new Weapon("Light of Salvation", Material.GOLDEN_SWORD),
		new Weapon("Loci Offering", Material.STONE_SWORD),
		new Weapon("Abomination Splinter", Material.STONE_SWORD),
		new Weapon("Axtan Blade", Material.STONE_SWORD),
		new Weapon("Bandit's Dagger", Material.STONE_SWORD),
		new Weapon("Cavewalker Scythe", Material.STONE_HOE),
		new Weapon("Jaguartooth Cleaver", Material.STONE_AXE),
		new Weapon("Blighted Scythe", Material.STONE_HOE),
		new Weapon("Civit Dagger", Material.STONE_SWORD),
		new Weapon("Completely Normal Sword", Material.STONE_SWORD),
		new Weapon("Cracked Blade de Vie", Material.STONE_SWORD),
		new Weapon("Cutting Breeze", Material.WOODEN_SWORD),
		new Weapon("Lacuna", Material.TRIDENT),
		new Weapon("Lingering Flame", Material.GOLDEN_SWORD),
		new Weapon("Plagueborne Axe", Material.WOODEN_AXE),
		new Weapon("Vermin's Scourge", Material.STONE_SWORD),
		new Weapon("Voltaic Edge", Material.GOLDEN_SWORD),
		new Weapon("Blackroot Branch", Material.WOODEN_SWORD),
		new Weapon("Highwatch Pike", Material.TRIDENT),
		new Weapon("Entropic Razor", Material.STONE_HOE),
		new Weapon("Frodian Keyblade", Material.STONE_SWORD),
		new Weapon("Ruby's Rose", Material.STONE_HOE),
		new Weapon("Chef's Knife", Material.STONE_SWORD),
		new Weapon("Destroyed Ancient Blade", Material.STONE_SWORD),
		new Weapon("Geomantic Dagger", Material.GOLDEN_SWORD),
		new Weapon("Glass Dagger", Material.GOLDEN_SWORD),
		new Weapon("Hallowed Gladius", Material.GOLDEN_SWORD),
		new Weapon("Murano's Dagger", Material.WOODEN_SWORD),
		new Weapon("Scalawag's Hatchet", Material.STONE_AXE),
		new Weapon("Silver Knight's Failure", Material.IRON_SWORD),
		new Weapon("Silver Knight's Hammer", Material.STONE_AXE),
		new Weapon("Watcher's Sword", Material.STONE_SWORD),
		new Weapon("Blizzard Blade", Material.STONE_SWORD),
		new Weapon("Blessed Axe", Material.STONE_AXE),
		new Weapon("Crushing Mace", Material.STONE_AXE),
		new Weapon("Eternal Crescent", Material.WOODEN_SWORD),
		new Weapon("Executioner's Scythe", Material.STONE_HOE),
		new Weapon("Macuahuitl", Material.STONE_SWORD),
		new Weapon("Molten Rapier", Material.STONE_SWORD),
		new Weapon("Nereid Harpoon", Material.TRIDENT),
		new Weapon("Phoenix Axe", Material.STONE_AXE),
		new Weapon("Polished Gladius", Material.STONE_SWORD),
		new Weapon("Soulhammer", Material.STONE_AXE),
		new Weapon("Versatile Cutlass", Material.STONE_SWORD),
		new Weapon("Battle Axe", Material.STONE_AXE),
		new Weapon("Deforester", Material.WOODEN_AXE),
		new Weapon("Divine Cleaver", Material.STONE_AXE),
		new Weapon("Druidic Broadsword", Material.WOODEN_SWORD),
		new Weapon("Falling Comet", Material.WOODEN_AXE),
		new Weapon("Godwood Sword", Material.WOODEN_SWORD),
		new Weapon("Masterwork Sabre", Material.STONE_SWORD),
		new Weapon("Nest's Bane", Material.STONE_SWORD),
		new Weapon("Officer's Blade", Material.STONE_SWORD),
		new Weapon("Rebel's Scythe", Material.STONE_HOE),
		new Weapon("Salburic Scythe", Material.STONE_HOE),
		new Weapon("Crusader's Sword", Material.WOODEN_SWORD),
		new Weapon("Flamewreath Splinter", Material.WOODEN_SWORD),
		new Weapon("Jagged Cleaver", Material.STONE_AXE),
		new Weapon("Light Scimitar", Material.WOODEN_SWORD),
		new Weapon("Living Thorn", Material.WOODEN_SWORD),
		new Weapon("Meteor Hammer", Material.WOODEN_AXE),
		new Weapon("Ruffian's Scythe", Material.STONE_HOE),
		new Weapon("Steelwood Blade", Material.WOODEN_SWORD),
		new Weapon("Tempered Mace", Material.WOODEN_AXE),
		new Weapon("Thief's Dagger", Material.WOODEN_SWORD),
		new Weapon("Woodsman's Axe", Material.STONE_AXE),
		new Weapon("Ashen Broadsword", Material.WOODEN_SWORD),
		new Weapon("Honed Swiftwood Axe", Material.WOODEN_AXE),
		new Weapon("Peasant's Scythe", Material.STONE_HOE),
		new Weapon("Priest's Stake", Material.WOODEN_SWORD),
		new Weapon("Smoldering Mace", Material.WOODEN_AXE),
		new Weapon("Soldier's Blade", Material.STONE_SWORD),
		new Weapon("Tempered Ironwood Blade", Material.WOODEN_SWORD),
		new Weapon("Versatile Axe", Material.WOODEN_AXE),
		new Weapon("Versatile Knife", Material.WOODEN_SWORD),
		new Weapon("Hunter's Stake", Material.WOODEN_SWORD),
		new Weapon("Ironwood Blade", Material.WOODEN_SWORD),
		new Weapon("Oaken Broadsword", Material.WOODEN_SWORD),
		new Weapon("Rough Dagger", Material.WOODEN_SWORD),
		new Weapon("Scorching Splinter", Material.WOODEN_SWORD),
		new Weapon("Smoldering Hatchet", Material.WOODEN_AXE),
		new Weapon("Squire's Hammer", Material.WOODEN_AXE),
		new Weapon("Swiftwood Axe", Material.WOODEN_AXE),
		new Weapon("Weak Scythe", Material.STONE_HOE),
		new Weapon("Novice's Battleaxe", Material.WOODEN_AXE),
		new Weapon("Novice's Scythe", Material.WOODEN_HOE),
		new Weapon("Athena", Material.DIAMOND_SWORD),
		new Weapon("Drinker of Mercy", Material.IRON_AXE),
		new Weapon("Malakut's Scorn", Material.GOLDEN_HOE),
		new Weapon("Rinascita", Material.STONE_SWORD),
		new Weapon("Calvoarium", Material.DIAMOND_AXE),
		new Weapon("Hightide Moonshine", Material.IRON_HOE),
		new Weapon("Bloodthirsty Croissant", Material.IRON_SWORD),
		new Weapon("Chunchunmaru", Material.IRON_SWORD),
		new Weapon("Soulblighter's Sacrifice", Material.TRIDENT),
		new Weapon("Divine Harvestman's Scythe", Material.STONE_HOE),
		new Weapon("Divine Rosethorn Blade", Material.STONE_SWORD),
		new Weapon("Divine Scalawag's Hatchet", Material.STONE_AXE),
		new Weapon("Divine Watcher's Sword", Material.STONE_SWORD),
		new Weapon("Ari's Ionic Buster", Material.STONE_SWORD),
		new Weapon("Render's Ruthless Claw", Material.STONE_HOE),
		new Weapon("Arcane Confection", Material.IRON_HOE),
		new Weapon("Holiday's End", Material.STONE_SWORD),
		new Weapon("Ice Sickle", Material.STONE_HOE),
		new Weapon("Magma Cake", Material.IRON_HOE),
		new Weapon("Pappermint Punisher", Material.GOLDEN_SWORD),
		new Weapon("Pole Star", Material.TRIDENT),
		new Weapon("Sketched Fleshbreaker", Material.IRON_AXE),
		new Weapon("Sketched Lightbringer", Material.TRIDENT),
		new Weapon("Sketched Sword of the Exiled", Material.DIAMOND_SWORD),
		new Weapon("Replica Pumpkin Spythe", Material.STONE_HOE),
		new Weapon("Coven Scythe", Material.IRON_HOE),
		new Weapon("Dueling Hammer", Material.IRON_AXE),
		new Weapon("Oaksteel Blade", Material.IRON_SWORD),
		new Weapon("Fungal Spear", Material.TRIDENT),
		new Weapon("Shade Hunter's Scythe", Material.IRON_HOE),
		new Weapon("Witch's Bane", Material.IRON_AXE),
		new Weapon("Silver Blade", Material.IRON_SWORD),
		new Weapon("Tuathan Trident", Material.TRIDENT),
		new Weapon("Falling Leaves", Material.IRON_HOE),
		new Weapon("Frosted Scythe", Material.IRON_HOE),
		new Weapon("Dendweller's Spear", Material.TRIDENT),
		new Weapon("Braveheart Hammer", Material.IRON_AXE),
		new Weapon("Bloodbinder", Material.IRON_AXE),
		new Weapon("Holy Relic Blade", Material.IRON_SWORD),
		new Weapon("Slayer of Fears", Material.IRON_SWORD),
		new Weapon("Windborn Scythe", Material.STONE_HOE)
	};

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		for (int i = 0; i < 3; i++) {
			int offset = 5 * (3 - i);
			new BukkitRunnable() {
				int mTicks = 0;
				ArmorStand mSwordStand = createSword(loc);
				double mFallSpeed = 0.8;
				@Override
				public void run() {
					if (mTicks == 0) {
						loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.25f, Constants.NotePitches.F11);
					}
					if (mSwordStand.getLocation().clone().add(0, 0.4, 0).getBlock().isEmpty()) {
						if (mSwordStand.getLocation().clone().add(0, -mFallSpeed + 0.3, 0).getBlock().isEmpty()) {
							mSwordStand.teleport(mSwordStand.getLocation().clone().add(0, -mFallSpeed, 0));
						} else {
							mSwordStand.teleport(mSwordStand.getLocation().clone().add(0, -mFallSpeed / 2, 0));
						}
					}
					if (mTicks >= 30 + offset) {
						mSwordStand.remove();
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 5 * i, 1);
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND_SWORD;
	}

	public ArmorStand createSword(Location loc) {
		ArmorStand swordStand = loc.getWorld().spawn(loc.clone().add(FastUtils.RANDOM.nextDouble(4) - 2, 5, FastUtils.RANDOM.nextDouble(4) - 2), ArmorStand.class);
		swordStand.setVisible(false);
		swordStand.setMarker(true);
		swordStand.setCollidable(false);
		int randIdx = FastUtils.RANDOM.nextInt(ITEMS.length);
		ItemStack sword = new ItemStack(ITEMS[randIdx].getMaterial());
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.displayName(Component.text(ITEMS[randIdx].getName()));
		sword.setItemMeta(swordMeta);
		ItemUtils.setPlainTag(sword);
		swordStand.getEquipment().setItemInMainHand(sword);
		swordStand.setRightArmPose(new EulerAngle(Math.PI / 2.0, FastUtils.RANDOM.nextDouble() * Math.PI, 0));
		return swordStand;
	}
}
