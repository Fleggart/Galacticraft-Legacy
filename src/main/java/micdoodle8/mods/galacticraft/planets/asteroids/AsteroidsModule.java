/*
 * Copyright (c) 2023 Team Galacticraft
 *
 * Licensed under the MIT license.
 * See LICENSE file in the project root for details.
 */

package micdoodle8.mods.galacticraft.planets.asteroids;

import java.lang.reflect.Method;
import java.util.List;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.item.EnumExtendedInventorySlot;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import micdoodle8.mods.galacticraft.planets.GCPlanetDimensions;
import micdoodle8.mods.galacticraft.planets.GalacticraftPlanets;
import micdoodle8.mods.galacticraft.planets.IPlanetsModule;
import micdoodle8.mods.galacticraft.planets.asteroids.blocks.AsteroidBlocks;
import micdoodle8.mods.galacticraft.planets.asteroids.dimension.TeleportTypeAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.dimension.WorldProviderAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityEntryPod;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntitySmallAsteroid;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityTier3Rocket;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.player.AsteroidsPlayerHandler;
import micdoodle8.mods.galacticraft.planets.asteroids.event.AsteroidsEventHandler;
import micdoodle8.mods.galacticraft.planets.asteroids.items.AsteroidsItems;
import micdoodle8.mods.galacticraft.planets.asteroids.network.PacketSimpleAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.recipe.CanisterRecipes;
import micdoodle8.mods.galacticraft.planets.asteroids.recipe.RecipeManagerAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.schematic.SchematicTier3Rocket;
import micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamReceiver;
import micdoodle8.mods.galacticraft.planets.asteroids.world.gen.BiomeAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.world.gen.ChunkProviderAsteroids;
import micdoodle8.mods.galacticraft.planets.mars.MarsModule;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.RecipeSorter;

public class AsteroidsModule implements IPlanetsModule
{

    public static Planet                 planetAsteroids;

    public static AsteroidsPlayerHandler playerHandler;
    public static Fluid                  fluidMethaneGas;
    public static Fluid                  fluidOxygenGas;
    public static Fluid                  fluidNitrogenGas;
    public static Fluid                  fluidLiquidMethane;
    public static Fluid                  fluidLiquidOxygen;
    public static Fluid                  fluidLiquidNitrogen;
    public static Fluid                  fluidLiquidArgon;
    public static Fluid                  fluidAtmosphericGases;

    private Fluid registerFluid(String fluidName, int density, int viscosity, int temperature, boolean gaseous)
    {
        Fluid returnFluid = FluidRegistry.getFluid(fluidName);
        if (returnFluid == null)
        {
            ResourceLocation texture = new ResourceLocation(GalacticraftPlanets.TEXTURE_PREFIX + "blocks/fluids/" + fluidName);
            FluidRegistry.registerFluid(new Fluid(fluidName, texture, texture).setDensity(density).setViscosity(viscosity).setTemperature(temperature).setGaseous(gaseous));
            returnFluid = FluidRegistry.getFluid(fluidName);
        }
        return returnFluid;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        AsteroidsModule.planetAsteroids = new Planet("asteroids").setParentSolarSystem(GalacticraftCore.solarSystemSol);

        playerHandler = new AsteroidsPlayerHandler();
        MinecraftForge.EVENT_BUS.register(playerHandler);
        AsteroidsEventHandler eventHandler = new AsteroidsEventHandler();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        RecipeSorter.register("galacticraftplanets:canisterRecipe", CanisterRecipes.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");

        registerFluid("methane", 1, 11, 295, true);
        registerFluid("atmosphericgases", 1, 13, 295, true);
        registerFluid("liquidmethane", 450, 120, 109, false);
        // Data source for liquid methane:
        // http://science.nasa.gov/science-news/science-at-nasa/2005/25feb_titan2/
        registerFluid("liquidoxygen", 1141, 140, 90, false);
        registerFluid("liquidnitrogen", 808, 130, 90, false);
        registerFluid("nitrogen", 1, 12, 295, true);
        registerFluid("carbondioxide", 2, 20, 295, true);
        registerFluid("argon", 1, 4, 295, true);
        registerFluid("liquidargon", 900, 100, 87, false);
        registerFluid("helium", 1, 1, 295, true);
        AsteroidsModule.fluidMethaneGas = FluidRegistry.getFluid("methane");
        AsteroidsModule.fluidAtmosphericGases = FluidRegistry.getFluid("atmosphericgases");
        AsteroidsModule.fluidLiquidMethane = FluidRegistry.getFluid("liquidmethane");
        AsteroidsModule.fluidLiquidOxygen = FluidRegistry.getFluid("liquidoxygen");
        AsteroidsModule.fluidOxygenGas = FluidRegistry.getFluid("oxygen");
        AsteroidsModule.fluidLiquidNitrogen = FluidRegistry.getFluid("liquidnitrogen");
        AsteroidsModule.fluidLiquidArgon = FluidRegistry.getFluid("liquidargon");
        AsteroidsModule.fluidNitrogenGas = FluidRegistry.getFluid("nitrogen");

        AsteroidBlocks.initBlocks();
        AsteroidBlocks.registerBlocks();
        AsteroidBlocks.setHarvestLevels();

        AsteroidsItems.initItems();

        AsteroidsModule.planetAsteroids.setBiomeInfo(BiomeAsteroids.asteroid);
        // This enables Endermen on Asteroids in Asteroids Challenge mode
        ((BiomeAsteroids) BiomeAsteroids.asteroid).resetMonsterListByMode(ConfigManagerCore.challengeMobDropsAndSpawning);
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
        AsteroidBlocks.oreDictRegistration();
        AsteroidsItems.oreDictRegistrations();

        this.registerMicroBlocks();
        SchematicRegistry.registerSchematicRecipe(new SchematicTier3Rocket());

        GalacticraftCore.packetPipeline.addDiscriminator(7, PacketSimpleAsteroids.class);

        this.registerEntities();

        RecipeManagerAsteroids.loadCompatibilityRecipes();

        AsteroidsModule.planetAsteroids.setDimensionInfo(ConfigManagerAsteroids.dimensionIDAsteroids, WorldProviderAsteroids.class).setTierRequired(3);
        AsteroidsModule.planetAsteroids.setRelativeDistanceFromCenter(new CelestialBody.ScalableDistance(1.375F, 1.375F)).setRelativeOrbitTime(45.0F).setPhaseShift((float) (Math.random() * (2 * Math.PI)));
        AsteroidsModule.planetAsteroids.setBodyIcon(new ResourceLocation(Constants.ASSET_PREFIX, "textures/gui/celestialbodies/asteroid.png"));
        AsteroidsModule.planetAsteroids.setAtmosphere(new AtmosphereInfo(false, false, false, -1.5F, 0.05F, 0.0F));
        AsteroidsModule.planetAsteroids.addChecklistKeys("equip_oxygen_suit", "thermal_padding");

        GalaxyRegistry.register(AsteroidsModule.planetAsteroids);
        GalacticraftRegistry.registerTeleportType(WorldProviderAsteroids.class, new TeleportTypeAsteroids());

        GalacticraftRegistry.registerGear(Constants.GEAR_ID_THERMAL_PADDING_T1_HELMET, EnumExtendedInventorySlot.THERMAL_HELMET, new ItemStack(AsteroidsItems.thermalPadding, 1, 0));
        GalacticraftRegistry.registerGear(Constants.GEAR_ID_THERMAL_PADDING_T1_CHESTPLATE, EnumExtendedInventorySlot.THERMAL_CHESTPLATE, new ItemStack(AsteroidsItems.thermalPadding, 1, 1));
        GalacticraftRegistry.registerGear(Constants.GEAR_ID_THERMAL_PADDING_T1_LEGGINGS, EnumExtendedInventorySlot.THERMAL_LEGGINGS, new ItemStack(AsteroidsItems.thermalPadding, 1, 2));
        GalacticraftRegistry.registerGear(Constants.GEAR_ID_THERMAL_PADDING_T1_BOOTS, EnumExtendedInventorySlot.THERMAL_BOOTS, new ItemStack(AsteroidsItems.thermalPadding, 1, 3));
    }

    @Override
    public void postInit(FMLPostInitializationEvent event)
    {
        GCPlanetDimensions.ASTEROIDS = WorldUtil.getDimensionTypeById(ConfigManagerAsteroids.dimensionIDAsteroids);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event)
    {
        ChunkProviderAsteroids.reset();
    }

    @Override
    public void serverInit(FMLServerStartedEvent event)
    {
    }

    @Override
    public void getGuiIDs(List<Integer> idList)
    {
    }

    @Override
    public Object getGuiElement(Side side, int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        return null;
    }

    private void registerEntities()
    {
        this.registerCreatures();
        this.registerNonMobEntities();
        this.registerTileEntities();
    }

    private void registerCreatures()
    {

    }

    private void registerNonMobEntities()
    {
        MarsModule.registerGalacticraftNonMobEntity(EntitySmallAsteroid.class, "small_asteroid", 150, 3, true);
        MarsModule.registerGalacticraftNonMobEntity(EntityTier3Rocket.class, "rocket_t3", 150, 1, false);
        MarsModule.registerGalacticraftNonMobEntity(EntityEntryPod.class, "entry_pod", 150, 1, true);
    }

    private void registerMicroBlocks()
    {
        try
        {
            Class<?> clazz = Class.forName("codechicken.microblock.MicroMaterialRegistry");
            if (clazz != null)
            {
                Method registerMethod = null;
                Method[] methodz = clazz.getMethods();
                for (Method m : methodz)
                {
                    if (m.getName().equals("registerMaterial"))
                    {
                        registerMethod = m;
                        break;
                    }
                }
                Class<?> clazzbm = Class.forName("codechicken.microblock.BlockMicroMaterial");
                registerMethod.invoke(null, clazzbm.getConstructor(Block.class, int.class).newInstance(AsteroidBlocks.blockBasic, 0), "tile.asteroids_block.asteroid_rock_0");
                registerMethod.invoke(null, clazzbm.getConstructor(Block.class, int.class).newInstance(AsteroidBlocks.blockBasic, 1), "tile.asteroids_block.asteroid_rock_1");
                registerMethod.invoke(null, clazzbm.getConstructor(Block.class, int.class).newInstance(AsteroidBlocks.blockBasic, 2), "tile.asteroids_block.asteroid_rock_2");
                registerMethod.invoke(null, clazzbm.getConstructor(Block.class, int.class).newInstance(AsteroidBlocks.blockDenseIce, 0), "tile.dense_ice");
            }
        }
        catch (Exception e)
        {}
    }

    private void registerTileEntities()
    {
        register(TileEntityBeamReceiver.class, "gc_beam_receiver");
    }

    @Override
    public Configuration getConfiguration()
    {
        return ConfigManagerAsteroids.config;
    }

    @Override
    public void syncConfig()
    {
        ConfigManagerAsteroids.syncConfig(false, false);
    }
}