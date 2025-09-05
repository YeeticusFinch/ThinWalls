package com.lerdorf.thinwalls;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		//this.getCommand("wall").setExecutor(this);

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
	    AttributeModifier modifier = new AttributeModifier(
	        UUID.randomUUID(),         // Unique ID for this modifier
	        "little_damage",           // Internal name
	        1,                        // Amount
	        AttributeModifier.Operation.ADD_NUMBER, // How it applies
	        EquipmentSlot.HAND         // Slot it applies to
	    );

	    // Apply modifier to the attribute
	    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
        chisel.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(key, chisel);
        recipe.shape(
        		" F",
        		"S "
        		);
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
            			if (comp.equalsIgnoreCase("block") || comp.equalsIgnoreCase("top") || comp.equalsIgnoreCase("side") || comp.equalsIgnoreCase("front"))
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
        	
        	NamespacedKey rkey = new NamespacedKey(this, mat.toString().toLowerCase()+"_paint_roller");
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
    	    AttributeModifier modifier = new AttributeModifier(
    	        UUID.randomUUID(),         // Unique ID for this modifier
    	        "zero_damage",           // Internal name
    	        0,                        // Amount
    	        AttributeModifier.Operation.ADD_NUMBER, // How it applies
    	        EquipmentSlot.HAND         // Slot it applies to
    	    );
    	    // Apply modifier to the attribute
    	    rmeta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
            rroller.setItemMeta(rmeta);

            ShapedRecipe rrecipe = new ShapedRecipe(rkey, rroller);
            rrecipe.shape(
            		"IWI",
            		" S "
            		);
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
    	for (int i = 0; i < str.length()-1; i++) {
    		if (i == 0) {
    			str = str.substring(0, 1).toUpperCase() + str.substring(1);
    		}
    		else if (str.charAt(i-1) == ' ') {
    			str = str.substring(0,i)+str.substring(i, i+1).toUpperCase() + str.substring(i+1);
    		}
    	}
    	return str;
    }
    
    @EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().discoverRecipes(recipes);
	}
	
    void checkRollerClick(PlayerInteractEvent event, ItemStack item) {
    	if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    	//event.getPlayer().sendMessage("Checking roller click");
    	NamespacedKey matKey = new NamespacedKey(this, "material");
    	if (!item.getItemMeta().getPersistentDataContainer().has(rollerKey, PersistentDataType.INTEGER)) return;
    	
    	//event.getPlayer().sendMessage("Used roller");
    	
    	if (event.getPlayer().hasCooldown(Material.WOODEN_SWORD)) return;
    	event.getPlayer().setCooldown(Material.WOODEN_SWORD, 10);
    	
    	//event.getPlayer().sendMessage("No Cooldown");
    	
    	if (!item.getItemMeta().getPersistentDataContainer().has(matKey, PersistentDataType.STRING)) return;
    	
    	//event.getPlayer().sendMessage("Has Mat key");
    	
    	Material mat = Material.getMaterial(item.getItemMeta().getPersistentDataContainer().get(matKey, PersistentDataType.STRING));
    	
    	Block block = event.getClickedBlock();
    	if (block == null || block.getType().isAir() || block.isLiquid()) return;
    	
    	//event.getPlayer().sendMessage("Clicked block");
    	
    	BlockFace face = event.getBlockFace();
    	Vector dir = face.getDirection();
    	Location loc = block.getLocation().add(0.5f, 0.5f, 0.5f).add(face.getDirection().multiply(0.5f));
    	if (face.getDirection().getX() < -0.01 || face.getDirection().getY() < -0.01 || face.getDirection().getZ() < -0.01)
    		loc.add(face.getDirection().multiply(1/16f));

    	for (BlockDisplay e : loc.getNearbyEntitiesByType(BlockDisplay.class, 0.2f)) {
    		if (e.isValid())
    			return;
    	}
    	
    	// Spawn BlockDisplay
        BlockDisplay display = block.getWorld().spawn(loc, BlockDisplay.class);
        display.setBlock(Bukkit.createBlockData(mat));
        display.addScoreboardTag("paint");
        Transformation t = new Transformation(
        	    new Vector3f(Math.abs(dir.getX()) > 0.1 ? 0f : -0.5f, Math.abs(dir.getY()) > 0.1 ? 0f : -0.5f, Math.abs(dir.getZ()) > 0.1 ? 0f : -0.5f),
        	    new AxisAngle4f(), // identity rotation
        	    new Vector3f((float) (1-Math.abs(dir.getX()*15f/16f)), (float) (1-Math.abs(dir.getY()*15f/16f)), (float)(1-Math.abs(dir.getZ()*15f/16f))),
        	    new AxisAngle4f()  // identity rotation
        	);

        display.setTransformation(t);
        
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 10, 0, 0, 0, 0, mat.createBlockData());
        loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.6f, 0.5f);
        
        item.damage(1, event.getPlayer());
        event.setCancelled(true);
    }
    
    void checkChiselClick(PlayerInteractEvent event, ItemStack item) {
    	NamespacedKey chiselKey = new NamespacedKey(plugin, "flint_chisel");
        if (!item.getItemMeta().getPersistentDataContainer().has(chiselKey, PersistentDataType.INTEGER)) return;
        
        if (event.getPlayer().hasCooldown(Material.WOODEN_SWORD)) return;
        event.getPlayer().setCooldown(Material.WOODEN_SWORD, 10);

        Collection<Entity> nearbyEntities = event.getPlayer().getEyeLocation().getNearbyEntities(5, 5, 5);
        Location point = event.getPlayer().getEyeLocation();
        Entity hitEntity = null;
        for (int i = 0; i < 9; i++) {
        	point.add(event.getPlayer().getEyeLocation().getDirection().multiply(0.5f));
        	for (Entity e : nearbyEntities) {
        		if (point.distance(e.getLocation()) < 0.3 && e.getScoreboardTags().contains("thin_wall")) {
        			hitEntity = e;
        			break;
        		}
        	}
        }
        
        if (hitEntity != null) {
        	// Shrink existing block entity
        	
        } else {

	        Block block = event.getClickedBlock();
	        if (block == null || block.isLiquid() || block.getType().isAir()) return;
	        Location blockLoc = block.getLocation();
	
	        // Replace the block with air
	        Material originalType = block.getType();
	        block.setType(Material.AIR);
	
	        // Spawn BlockDisplay
	        BlockDisplay display = block.getWorld().spawn(blockLoc, BlockDisplay.class);
	        display.setBlock(Bukkit.createBlockData(originalType));
	        display.addScoreboardTag("thin_wall");
	
	        float currentScale = 1.0f;
	        if (display.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("thinness:"))) {
	            String tag = display.getScoreboardTags().stream().filter(t -> t.startsWith("thinness:")).findFirst().orElse("thinness:1.0");
	            currentScale = Float.parseFloat(tag.split(":")[1]);
	            display.getScoreboardTags().remove(tag);
	        }
	
	        float newScale = currentScale - 0.2f;
	        if (newScale <= 0.2f) {
	            display.remove(); // too thin, delete
	            block.getWorld().playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
	            return;
	        }
	
	        display.addScoreboardTag("thinness:" + newScale);
	
	        // Shrink based on clicked face
	        Vector scale = new Vector(1, 1, 1);
	        Vector offset = new Vector(0, 0, 0);
	        
	        Bukkit.broadcastMessage(event.getBlockFace().name());
	
	        switch (event.getBlockFace()) {
	            case NORTH -> {
	                scale.setZ(newScale);
	                offset.setZ((1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.NORTH);
	                break;
	            }
	            case SOUTH -> {
	                scale.setZ(newScale);
	                offset.setZ(-(1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.SOUTH);
	                break;
	            }
	            case EAST -> {
	                scale.setX(newScale);
	                offset.setX(-(1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.EAST);
	                break;
	            }
	            case WEST -> {
	                scale.setX(newScale);
	                offset.setX((1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.WEST);
	                break;
	            }
	            case UP -> {
	                scale.setY(newScale);
	                offset.setY(-(1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.UP);
	                break;
	            }
	            case DOWN -> {
	                scale.setY(newScale);
	                offset.setY((1.0f - newScale) / 2);
	                placeTrapdoor(blockLoc, BlockFace.DOWN);
	                break;
	            }
	        }
	
	        Transformation t = new Transformation(
	        	    new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()),
	        	    new AxisAngle4f(), // identity rotation
	        	    new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ()),
	        	    new AxisAngle4f()  // identity rotation
	        	);
	
	        display.setTransformation(t);
	        event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onItemClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;
        
        checkChiselClick(event, item);
        checkRollerClick(event, item);
    }

    private void placeTrapdoor(Location loc, BlockFace facing) {
        Block block = loc.getBlock();
        block.setType(Material.IRON_TRAPDOOR, false);

        TrapDoor data = (TrapDoor) Bukkit.createBlockData(Material.IRON_TRAPDOOR);
        if (facing.isCartesian()) {
        	data.setFacing(facing);
            data.setHalf(Bisected.Half.BOTTOM);
            data.setOpen(true);
        }
        else {
        	data.setFacing(BlockFace.NORTH);
        	if (facing == BlockFace.UP)
        		data.setHalf(Bisected.Half.TOP);
        	else
        		data.setHalf(Bisected.Half.BOTTOM);
            data.setOpen(false);
        }
        data.setPowered(true); // disable redstone influence

        block.setBlockData(data);
    }

}