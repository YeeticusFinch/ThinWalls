package com.lerdorf.thinwalls;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.joml.Vector3f;
import org.joml.AxisAngle4f;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.block.data.Bisected;

public class ThinWalls extends JavaPlugin implements Listener, TabExecutor {

	private File configFile;
	private Map<String, Object> configValues;

	private Plugin plugin;

	public static Collection<NamespacedKey> recipes = new ArrayList<>();

	public void loadConfig() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // <-- use block style
		options.setIndent(2);
		options.setPrettyFlow(true);

		File pluginFolder = this.getDataFolder();
		if (!pluginFolder.exists())
			pluginFolder.mkdirs();

		configFile = new File(pluginFolder, "config.yml");

		Yaml yaml = new Yaml(options);

		// If file doesn't exist, create it with defaults
		if (!configFile.exists()) {
			configValues = new HashMap<>();
			// configValues.put("requireBothHandsEmpty", requireBothHandsEmpty);
			saveConfig(); // Save default config
		}

		try {
			String yamlAsString = Files.readString(configFile.toPath());
			configValues = (Map<String, Object>) yaml.load(yamlAsString);
			if (configValues == null)
				configValues = new HashMap<>();
		} catch (Exception e) {
			e.printStackTrace();
			configValues = new HashMap<>();
		}

		// Now parse and update values
		/*
		 * try { if (configValues.containsKey("requireBothHandsEmpty"))
		 * requireBothHandsEmpty = (boolean)configValues.get("requireBothHandsEmpty"); }
		 * catch (Exception e) { e.printStackTrace(); }
		 * configValues.put("requireBothHandsEmpty", requireBothHandsEmpty);
		 */

		saveConfig(); // Ensure config is up to date
	}

	public void saveConfig() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // <-- use block style
		options.setIndent(2);
		options.setPrettyFlow(true);

		Yaml yaml = new Yaml(options);
		try (FileWriter writer = new FileWriter(configFile)) {
			yaml.dump(configValues, writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		plugin = this;
		rollerKey = new NamespacedKey(plugin, "piant_roller");
		getServer().getPluginManager().registerEvents(this, this);

		// this.getCommand("wall").setExecutor(this);

		loadConfig();
		saveConfig();

		registerChiselRecipe();
		registerRollerRecipe();

		/*
		 * new BukkitRunnable() {
		 * 
		 * @Override public void run() {
		 * 
		 * } }.runTaskTimer(this, 0L, 1L); // Run every 1 tick
		 */

		getLogger().info("ThinWalls enabled!");

	}

	@Override
	public void onDisable() {
		getLogger().info("ThinWalls disabled!");
	}

	public boolean rightClick(Player p, Block block) {
		if (p.isSneaking()) {
			// return false;
		} else {

		}
		return false;
	}

	private void registerChiselRecipe() {
		NamespacedKey key = new NamespacedKey(this, "flint_chisel");
		ItemStack chisel = new ItemStack(Material.WOODEN_SWORD);
		ItemMeta meta = chisel.getItemMeta();
		meta.setDisplayName("§7Flint Chisel");
		meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
		meta.setItemModel(NamespacedKey.fromString("tw:chisel"));
		// Create a modifier: +5 attack damage when in main hand
		AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), // Unique ID for this modifier
				"little_damage", // Internal name
				1, // Amount
				AttributeModifier.Operation.ADD_NUMBER, // How it applies
				EquipmentSlot.HAND // Slot it applies to
		);

		// Apply modifier to the attribute
		meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
		chisel.setItemMeta(meta);

		ShapedRecipe recipe = new ShapedRecipe(key, chisel);
		recipe.shape(" F", "S ");
		recipe.setIngredient('F', Material.FLINT);
		recipe.setIngredient('S', Material.STICK);
		Bukkit.addRecipe(recipe);
		recipes.add(key);
	}

	NamespacedKey rollerKey = null;

	private void registerRollerRecipe() {

		for (int i = 0; i < Resources.matArray.length; i++) {
			Material mat = Resources.matArray[i];
			String[] matComp = mat.toString().toLowerCase().strip().split("_");
			String texture = null;
			for (String temp : Resources.textureArray) {
				boolean isTexture = true;
				for (String comp : matComp) {
					if (!temp.toLowerCase().contains(comp.toLowerCase())) {
						isTexture = false;
						break;
					}
				}
				if (isTexture) {
					texture = temp;
					break;
				}
			}
			if (texture == null) {
				for (String temp : Resources.textureArray) {
					boolean isTexture = true;
					for (String comp : matComp) {
						if (comp.equalsIgnoreCase("block") || comp.equalsIgnoreCase("top")
								|| comp.equalsIgnoreCase("side") || comp.equalsIgnoreCase("front"))
							if (!temp.toLowerCase().contains(comp.toLowerCase())) {
								isTexture = false;
								break;
							}
					}
					if (isTexture) {
						texture = temp;
						break;
					}
				}
			}
			if (texture == null)
				texture = Resources.textureArray[i].strip();

			NamespacedKey rkey = new NamespacedKey(this, mat.toString().toLowerCase() + "_paint_roller");
			NamespacedKey matKey = new NamespacedKey(this, "material");
			ItemStack rroller = new ItemStack(Material.WOODEN_SWORD);
			Damageable rmeta = (Damageable) rroller.getItemMeta();
			rmeta.setMaxDamage(64);
			rmeta.setDisplayName("§7" + toFancyString(mat.toString()) + " Paint Roller");
			rmeta.getPersistentDataContainer().set(rollerKey, PersistentDataType.INTEGER, 1);
			rmeta.getPersistentDataContainer().set(rkey, PersistentDataType.INTEGER, 1);
			rmeta.getPersistentDataContainer().set(matKey, PersistentDataType.STRING, mat.toString());
			List<String> lore = new ArrayList<String>();
			lore.add(ChatColor.WHITE + mat.toString().toLowerCase());
			rmeta.setLore(lore);
			rmeta.setItemModel(NamespacedKey.fromString("tw:" + texture + "_paint_roller"));
			// Create a modifier: +5 attack damage when in main hand
			AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), // Unique ID for this modifier
					"zero_damage", // Internal name
					0, // Amount
					AttributeModifier.Operation.ADD_NUMBER, // How it applies
					EquipmentSlot.HAND // Slot it applies to
			);
			// Apply modifier to the attribute
			rmeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
			rroller.setItemMeta(rmeta);

			ShapedRecipe rrecipe = new ShapedRecipe(rkey, rroller);
			rrecipe.shape("IWI", " S ");
			rrecipe.setIngredient('W', mat);
			rrecipe.setIngredient('S', Material.STICK);
			rrecipe.setIngredient('I', Material.IRON_NUGGET);
			Bukkit.addRecipe(rrecipe);
			recipes.add(rkey);
		}
	}

	// Transforms a string like this: "wooden_sword" --> "Wooden Sword"
	public String toFancyString(String str) {
		str = str.toLowerCase().strip().replace('_', ' ');
		for (int i = 0; i < str.length() - 1; i++) {
			if (i == 0) {
				str = str.substring(0, 1).toUpperCase() + str.substring(1);
			} else if (str.charAt(i - 1) == ' ') {
				str = str.substring(0, i) + str.substring(i, i + 1).toUpperCase() + str.substring(i + 1);
			}
		}
		return str;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().discoverRecipes(recipes);
	}

	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent event) {
		Vector dir = event.getDirection().getDirection();
		List<BlockDisplay> moved = new ArrayList<BlockDisplay>();
		for (Block block : event.getBlocks()) {
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
				if (display.getScoreboardTags().contains("paint")) {
					display.teleport(display.getLocation().add(dir));
					moved.add(display);
				}
				else if (display.getScoreboardTags().contains("thin_wall")) {
					display.teleport(display.getLocation().add(dir));
					moved.add(display);
				}
			}
			
			loc = loc.add(dir);
			for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
				if (display.getScoreboardTags().contains("paint") && !moved.contains(display)) {
					//display.teleport(display.getLocation().add(dir));
					display.remove();
				}
			}
		}
	}
	
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent event) {
		Vector dir = event.getDirection().getDirection();
		List<BlockDisplay> moved = new ArrayList<BlockDisplay>();
		for (Block block : event.getBlocks()) {
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
				if (display.getScoreboardTags().contains("paint") && isAttached(display, block)) {
					display.teleport(display.getLocation().add(dir));
					moved.add(display);
				}
				else if (display.getScoreboardTags().contains("thin_wall")) {
					display.teleport(display.getLocation().add(dir));
					moved.add(display);
				}
			}

			loc = loc.add(dir);
			for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
				if (display.getScoreboardTags().contains("paint") && !moved.contains(display)) {
					//display.teleport(display.getLocation().add(dir));
					display.remove();
				}
			}
		}
	}
	
	public Block getPaintedBlock(BlockDisplay display) {
		if (display.getScoreboardTags().contains("paint")) {
			for (String tag : display.getScoreboardTags()) {
				if (tag.contains("dir:")) {
					String dir = tag.substring(tag.indexOf(':') + 1);
					BlockFace face = BlockFace.valueOf(dir);
					Location loc = display.getLocation().getBlock().getLocation().subtract(face.getDirection());
					return loc.getBlock();
				}
			}
		}
		return null;
	}
	
	public boolean isAttached(BlockDisplay display, Block block) {
		if (display.getScoreboardTags().contains("paint")) {
			Block paintedBlock = getPaintedBlock(display);
			return paintedBlock.getX() == block.getX() && paintedBlock.getY() == block.getY() && paintedBlock.getZ() == block.getZ();
		} else {
			Block paintedBlock = display.getLocation().getBlock();
			return paintedBlock.getX() == block.getX() && paintedBlock.getY() == block.getY() && paintedBlock.getZ() == block.getZ();
		}
	}
	
	@EventHandler
	public void onRedstone(BlockRedstoneEvent event) {
	    Block block = event.getBlock();

	    if (block.getType() == Material.IRON_TRAPDOOR) {
	        // If this trapdoor is one of ours (thin wall hitbox)
	    	Location loc = block.getLocation().add(0.5, 0.5, 0.5);
	    	for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
				if (display.getScoreboardTags().contains("thin_wall") && isAttached(display, block)) {
					event.setNewCurrent(event.getOldCurrent());
				}
			}
	    }
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation().add(0.5, 0.5, 0.5);

		for (BlockDisplay display : loc.getNearbyEntitiesByType(BlockDisplay.class, 1)) {
			if (display.getScoreboardTags().contains("paint") && isAttached(display, block)) {
				display.remove();
			}
			if (display.getScoreboardTags().contains("thin_wall") && isAttached(display, block)) {
				display.remove();
				if (block.getType() == Material.IRON_TRAPDOOR) {
					event.setDropItems(false);
				}
			}
		}
	}

	void checkRollerClick(PlayerInteractEvent event, ItemStack item) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		// event.getPlayer().sendMessage("Checking roller click");
		NamespacedKey matKey = new NamespacedKey(this, "material");
		if (!item.getItemMeta().getPersistentDataContainer().has(rollerKey, PersistentDataType.INTEGER))
			return;

		// event.getPlayer().sendMessage("Used roller");

		if (event.getPlayer().hasCooldown(Material.WOODEN_SWORD))
			return;
		event.getPlayer().setCooldown(Material.WOODEN_SWORD, 10);

		// event.getPlayer().sendMessage("No Cooldown");

		if (!item.getItemMeta().getPersistentDataContainer().has(matKey, PersistentDataType.STRING))
			return;

		// event.getPlayer().sendMessage("Has Mat key");

		Material mat = Material
				.getMaterial(item.getItemMeta().getPersistentDataContainer().get(matKey, PersistentDataType.STRING));

		Block block = event.getClickedBlock();
		if (block == null || block.getType().isAir() || block.isLiquid())
			return;

		// event.getPlayer().sendMessage("Clicked block");

		BlockFace face = event.getBlockFace();
		Vector dir = face.getDirection();
		Location loc = block.getLocation().add(0.5f, 0.5f, 0.5f).add(face.getDirection().multiply(0.5f));
		if (face.getDirection().getX() < -0.01 || face.getDirection().getY() < -0.01
				|| face.getDirection().getZ() < -0.01)
			loc.add(face.getDirection().multiply(1 / 16f));

		for (BlockDisplay e : loc.getNearbyEntitiesByType(BlockDisplay.class, 0.2f)) {
			if (e.isValid())
				return;
		}

		// Spawn BlockDisplay
		BlockDisplay display = block.getWorld().spawn(loc, BlockDisplay.class);
		display.addScoreboardTag("dir:"+face.toString());
		display.setBlock(Bukkit.createBlockData(mat));
		display.addScoreboardTag("paint");
		Transformation t = new Transformation(
				new Vector3f(Math.abs(dir.getX()) > 0.1 ? 0f : -0.5f, Math.abs(dir.getY()) > 0.1 ? 0f : -0.5f,
						Math.abs(dir.getZ()) > 0.1 ? 0f : -0.5f),
				new AxisAngle4f(), // identity rotation
				new Vector3f((float) (1 - Math.abs(dir.getX() * 15f / 16f)),
						(float) (1 - Math.abs(dir.getY() * 15f / 16f)), (float) (1 - Math.abs(dir.getZ() * 15f / 16f))),
				new AxisAngle4f() // identity rotation
		);

		display.setTransformation(t);

		loc.getWorld().spawnParticle(Particle.BLOCK, loc, 10, 0, 0, 0, 0, mat.createBlockData());
		loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.6f, 0.5f);

		item.damage(1, event.getPlayer());
		event.setCancelled(true);
	}

	Set<Material> UNCHISELABLE = Set.of(Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
			Material.ANCIENT_DEBRIS, Material.NETHERITE_BLOCK, Material.REINFORCED_DEEPSLATE, Material.BARRIER,
			Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.WATER, Material.LAVA, Material.AIR,
			Material.CAVE_AIR, Material.VOID_AIR);

	boolean isFullBlock(Block block) {
		
		boolean isFullCube = true;
		
		Material type = block.getType();
		
		// Not solid? Immediately not full
	    if (!type.isSolid()) {
	    	isFullCube = false;
	    } else {
	
		    String name = type.name();
	
		    // filter common non-full categories
		    if (name.contains("SLAB") || name.contains("STAIRS") 
		        || name.contains("DOOR") || name.contains("TRAPDOOR")) {
		        isFullCube = false;
		    }
	    }
	    
		//Bukkit.broadcastMessage(block.getType() + " full cube --> " + isFullCube);
		
		return isFullCube;
	}

	boolean isChisel(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return false;
		NamespacedKey chiselKey = new NamespacedKey(plugin, "flint_chisel");
		return item.getItemMeta().getPersistentDataContainer().has(chiselKey, PersistentDataType.INTEGER);
	}

	void checkChiselClick(PlayerInteractEvent event, ItemStack item) {

		if (!isChisel(item))
			return;

		if (event.getPlayer().hasCooldown(Material.WOODEN_SWORD))
			return;
		event.getPlayer().setCooldown(Material.WOODEN_SWORD, 10);

		//event.getPlayer().sendMessage("Chisel right click");

		Entity hitEntity = null;
		Vector dir = event.getPlayer().getEyeLocation().getDirection().normalize();
		Location eye = event.getPlayer().getEyeLocation();

		// Search nearby BlockDisplays
		for (BlockDisplay d : eye.getWorld().getNearbyEntitiesByType(BlockDisplay.class, eye, 5)) {
		    if (!d.getScoreboardTags().contains("thin_wall")) continue;

		    // Get transformation
		    Transformation t = d.getTransformation();
		    Vector3f scale = t.getScale();
		    Vector3f offset = t.getTranslation();

		    // Base center (BlockDisplay’s position is usually block center)
		    Vector base = d.getLocation().toVector();

		    // Compute half extents (original cube is 1x1x1)
		    double hx = scale.x / 2.0;
		    double hy = scale.y / 2.0;
		    double hz = scale.z / 2.0;

		    // Apply offset (translation relative to entity pivot)
		    Vector center = base.clone().add(new Vector(offset.x, offset.y, offset.z));

		    // Construct our own bounding box
		    BoundingBox box = new BoundingBox(
		        center.getX() - hx, center.getY() - hy, center.getZ() - hz,
		        center.getX() + hx, center.getY() + hy, center.getZ() + hz
		    );

		    // Ray trace against this box
		    if (box.rayTrace(eye.toVector(), dir, 5.0) != null) {
		        hitEntity = d;
		        break;
		    }
		}
		
		if (hitEntity == null) {
			Block block = event.getClickedBlock();
			if (block != null && block.getType() == Material.IRON_TRAPDOOR) {
				Collection<Entity> nearbyEntities = block.getLocation().add(0.5f, 0.5f, 0.5f).getNearbyEntitiesByType(BlockDisplay.class, 1);
				for (Entity e : nearbyEntities) {
					if (e.getScoreboardTags().contains("thin_wall")) {
						hitEntity = e;
						break;
					}
				}
			}
		}

		if (hitEntity != null) {
			//event.getPlayer().sendMessage("Hit entity!");
			BlockDisplay display = (BlockDisplay) hitEntity;

			// Read block material
			Material originalType = display.getBlock().getMaterial();

			// Get current scale
			Transformation old = display.getTransformation();
			Vector3f scaleVec = old.getScale();
			Vector3f transVec = old.getTranslation();

			// Detect axis from tag, or infer if missing
			String axisTag = display.getScoreboardTags().stream().filter(tag -> tag.startsWith("chisel_axis:"))
					.findFirst().orElse(null);

			//event.getPlayer().sendMessage("Axis tag: " + axisTag);

			String axis;
			if (axisTag != null) {
				axis = axisTag.split(":")[1];
			} else {
				// Infer axis: whichever dimension < 1.0 means it was chiselled there
				if (scaleVec.x < 0.999f)
					axis = "X";
				else if (scaleVec.y < 0.999f)
					axis = "Y";
				else
					axis = "Z";
				display.addScoreboardTag("chisel_axis:" + axis);
			}

			// Figure out current thickness
			float currentScale = switch (axis) {
			case "X" -> scaleVec.x;
			case "Y" -> scaleVec.y;
			default -> scaleVec.z;
			};

			// Reduce thickness
			float newScale = currentScale - 0.15f;
			if (newScale <= 0.25f) {
				display.getWorld().playSound(display.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
				display.getLocation().getBlock().setType(Material.AIR);
				display.remove(); // too thin, delete
				return;
			}

			// Apply new scale & offset
			setThinWallTransform(display, event.getBlockFace(), newScale);

			// Feedback
			display.getWorld().playSound(display.getLocation(), Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f);
			display.getWorld().spawnParticle(Particle.BLOCK, display.getLocation(), 10, 0.1, 0.1, 0.1,
					originalType.createBlockData());

			event.setCancelled(true);
			return;
		} else {
			//event.getPlayer().sendMessage("Hit block");
			Block block = event.getClickedBlock();
			if (block == null)
				return;
			if (UNCHISELABLE.contains(block.getType())) {
				event.getPlayer().sendMessage(ChatColor.RED + "You can't chisel this block!");
				return;
			}
			if (block.isLiquid() || block.getType().isAir() || !isFullBlock(block))
				return;
			
			//event.getPlayer().sendMessage("Block is valid");

			Location loc = block.getLocation();
			Material mat = block.getType();

			// Replace with BlockDisplay
			block.setType(Material.AIR);
			BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class);
			display.setBlock(Bukkit.createBlockData(mat));
			display.addScoreboardTag("thin_wall");

			// Start scale at full, shrink on clicked face
			float newScale = 0.8f; // first shrink
			
			setThinWallTransform(display, event.getBlockFace(), newScale);

			placeTrapdoor(loc, event.getBlockFace());

			event.setCancelled(true);
		}
	}
	
	public void setThinWallTransform(BlockDisplay display, BlockFace face, float newScale) {
		Vector3f scale = new Vector3f(1.05f, 1.05f, 1.05f);
		Vector3f offset = new Vector3f(-0.025f, -0.025f, -0.025f);
		Bukkit.broadcastMessage(face.toString());
		switch (face) {
			case SOUTH -> {
				scale.z = newScale;
				//offset.z = (1 - newScale) / 2;
				offset.z -= 0.05f;
				display.addScoreboardTag("chisel_axis:Z");
			}
			case NORTH -> {
				scale.z = newScale;
				offset.z += (1 - newScale) + 0.05f;
				display.addScoreboardTag("chisel_axis:Z");
			}
			case WEST -> {
				scale.x = newScale;
				offset.x += (1 - newScale) + 0.05f;
				display.addScoreboardTag("chisel_axis:X");
			}
			case EAST -> {
				scale.x = newScale;
				offset.x -= 0.05f;
				//offset.x = (1 - newScale) / 2;
				display.addScoreboardTag("chisel_axis:X");
			}
			case DOWN -> {
				scale.y = newScale;
				offset.y += (1 - newScale)+0.05f;
				display.addScoreboardTag("chisel_axis:Y");
			}
			case UP -> {
				scale.y = newScale;
				//offset.y += (1 - newScale) / 2;
				offset.y -= 0.05f;
				display.addScoreboardTag("chisel_axis:Y");
			}
		}
		
		Transformation t = new Transformation(offset, new AxisAngle4f(), scale, new AxisAngle4f());
		display.setTransformation(t);
	}

	public void checkChiselLeftClick(PlayerInteractEvent event, ItemStack item) {

		if (!isChisel(item))
			return;

		if (event.getPlayer().hasCooldown(Material.WOODEN_SWORD))
			return;
		event.getPlayer().setCooldown(Material.WOODEN_SWORD, 10);

		//event.getPlayer().sendMessage("Chisel left click");

		Entity hitEntity = null;
		Vector dir = event.getPlayer().getEyeLocation().getDirection().normalize();
		Location eye = event.getPlayer().getEyeLocation();

		// Search nearby BlockDisplays
		for (BlockDisplay d : eye.getWorld().getNearbyEntitiesByType(BlockDisplay.class, eye, 5)) {
		    if (!d.getScoreboardTags().contains("thin_wall") && !d.getScoreboardTags().contains("paint")) continue;

		    // Get transformation
		    Transformation t = d.getTransformation();
		    Vector3f scale = t.getScale();
		    Vector3f offset = t.getTranslation();

		    // Base center (BlockDisplay’s position is usually block center)
		    Vector base = d.getLocation().toVector();

		    // Compute half extents (original cube is 1x1x1)
		    double hx = scale.x / 2.0;
		    double hy = scale.y / 2.0;
		    double hz = scale.z / 2.0;

		    // Apply offset (translation relative to entity pivot)
		    Vector center = base.clone().add(new Vector(offset.x, offset.y, offset.z));

		    // Construct our own bounding box
		    BoundingBox box = new BoundingBox(
		        center.getX() - hx, center.getY() - hy, center.getZ() - hz,
		        center.getX() + hx, center.getY() + hy, center.getZ() + hz
		    );

		    // Ray trace against this box
		    if (box.rayTrace(eye.toVector(), dir, 5.0) != null) {
		        hitEntity = d;
		        break;
		    }
		}

		if (hitEntity == null) {
			Block block = event.getClickedBlock();
			if (block != null && block.getType() == Material.IRON_TRAPDOOR) {
				Collection<Entity> nearbyEntities = block.getLocation().add(0.5f, 0.5f, 0.5f).getNearbyEntitiesByType(BlockDisplay.class, 1);
				for (Entity e : nearbyEntities) {
					if (e.getScoreboardTags().contains("thin_wall")) {
						hitEntity = e;
						break;
					}
				}
			}
			else if (block != null) {
			    BlockFace face = event.getBlockFace();
			    Location loc = block.getLocation().add(0.5f, 0.5f, 0.5f).add(face.getDirection().multiply(0.5f));
			    if (face.getDirection().getX() < -0.01 || face.getDirection().getY() < -0.01
			            || face.getDirection().getZ() < -0.01)
			        loc.add(face.getDirection().multiply(1 / 16f));

			    for (BlockDisplay e : loc.getNearbyEntitiesByType(BlockDisplay.class, 0.2f)) {
			        if (e.isValid() && e.getScoreboardTags().contains("paint")) {
			            hitEntity = e;
			            break;
			        }
			    }
			}
		}

		if (hitEntity != null && hitEntity instanceof BlockDisplay display) {
			if (!hitEntity.getScoreboardTags().contains("thin_wall"))
				return;
			//event.getPlayer().sendMessage("Hit entity with left click");

			Vector3f scale = display.getTransformation().getScale();
			boolean canDrop = scale.x >= 0.5f && scale.y >= 0.5f && scale.z >= 0.5f;

			Material mat = display.getBlock().getMaterial();
			display.getLocation().getBlock().setType(Material.AIR);
			display.remove();
			//event.getPlayer().sendMessage("Removing blockdisplay");

			if (canDrop) {
				event.getPlayer().getWorld().dropItemNaturally(display.getLocation(), new ItemStack(mat));
			}
			

			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onItemClick(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || item.getItemMeta() == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
			checkChiselLeftClick(event, item);
			return;
		}

		checkChiselClick(event, item);
		checkRollerClick(event, item);
	}

	private void placeTrapdoor(Location loc, BlockFace facing) {
		Block block = loc.getBlock();
		block.setType(Material.IRON_TRAPDOOR, false);

		TrapDoor data = (TrapDoor) Bukkit.createBlockData(Material.IRON_TRAPDOOR);
		if (facing == BlockFace.EAST || facing == BlockFace.SOUTH || facing == BlockFace.NORTH || facing == BlockFace.WEST) {
			data.setFacing(facing);
			data.setHalf(Bisected.Half.BOTTOM);
			data.setOpen(true);
		} else {
			data.setFacing(BlockFace.NORTH);
			if (facing == BlockFace.DOWN)
				data.setHalf(Bisected.Half.TOP);
			else
				data.setHalf(Bisected.Half.BOTTOM);
			data.setOpen(false);
		}
		data.setPowered(true); // disable redstone influence

		block.setBlockData(data);
	}

}