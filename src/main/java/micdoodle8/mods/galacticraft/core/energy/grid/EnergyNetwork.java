/*
 * Copyright (c) 2023 Team Galacticraft
 *
 * Licensed under the MIT license.
 * See LICENSE file in the project root for details.
 */

package micdoodle8.mods.galacticraft.core.energy.grid;

import cofh.redstoneflux.api.IEnergyReceiver;
import ic2.api.energy.tile.IEnergySink;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.energy.IStrictEnergyAcceptor;
import micdoodle8.mods.galacticraft.api.transmission.NetworkType;
import micdoodle8.mods.galacticraft.api.transmission.grid.IElectricityNetwork;
import micdoodle8.mods.galacticraft.api.transmission.tile.IConductor;
import micdoodle8.mods.galacticraft.api.transmission.tile.IElectrical;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler;
import micdoodle8.mods.galacticraft.core.energy.EnergyUtil;
import micdoodle8.mods.galacticraft.core.tick.TickHandlerServer;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A universal network that works with multiple energy systems.
 *
 * @author radfast, micdoodle8, Calclavia, Aidancbrady
 */
public class EnergyNetwork implements IElectricityNetwork
{

    private boolean isMekLoaded = EnergyConfigHandler.isMekanismLoaded() && !EnergyConfigHandler.disableMekanismOutput;
    private boolean isRF1Loaded = EnergyConfigHandler.isRFAPIv1Loaded() && !EnergyConfigHandler.disableRFOutput;
    private boolean isRF2Loaded = EnergyConfigHandler.isRFAPIv2Loaded() && !EnergyConfigHandler.disableRFOutput;
    private boolean isIC2Loaded = EnergyConfigHandler.isIndustrialCraft2Loaded() && !EnergyConfigHandler.disableIC2Output;
    private boolean isFELoaded = !EnergyConfigHandler.disableFEOutput;

    public static int tickCount = 0;
    private int tickDone = -1;
    private float totalRequested = 0F;
    private float totalStorageExcess = 0F;
    private float totalEnergy = 0F;
    private float totalSent = 0F;
    private boolean doneScheduled = false;
    private boolean spamstop = false;
    private boolean loopPrevention = false;
    public int networkTierGC = 1;
    private int producersTierGC = 1;

    private List<Object> connectedAcceptors = new LinkedList<>();
    private List<EnumFacing> connectedDirections = new LinkedList<>();

    private Set<Object> availableAcceptors = new HashSet<>();
    private Map<Object, EnumFacing> availableconnectedDirections = new HashMap<>();

    private Map<Object, Float> energyRequests = new HashMap<>();
    private List<TileEntity> ignoreAcceptors = new LinkedList<>();

    private final Set<IConductor> conductors = new HashSet<>();

    private final static float ENERGY_STORAGE_LEVEL = 200F;

    @Override
    public Set<IConductor> getTransmitters()
    {
        return this.conductors;
    }

    @Override
    public float getRequest(TileEntity... ignoreTiles)
    {
        if (EnergyNetwork.tickCount != this.tickDone)
        {
            this.ignoreAcceptors.clear();
            this.ignoreAcceptors.addAll(Arrays.asList(ignoreTiles));
            this.doTickStartCalc();
        }
        return this.totalRequested - this.totalEnergy - this.totalSent;
    }

    @Override
    public float produce(float energy, boolean doReceive, int producerTier, TileEntity... ignoreTiles)
    {
        if (this.loopPrevention)
        {
            return energy;
        }

        if (energy > 0F)
        {
            if (EnergyNetwork.tickCount != this.tickDone)
            {
                this.tickDone = EnergyNetwork.tickCount;
                this.ignoreAcceptors.clear();
                this.ignoreAcceptors.addAll(Arrays.asList(ignoreTiles));
                this.producersTierGC = 1;
                this.doTickStartCalc();
            } else
            {
                this.ignoreAcceptors.addAll(Arrays.asList(ignoreTiles));
            }

            if (!this.doneScheduled && this.totalRequested > 0.0F)
            {
                TickHandlerServer.scheduleNetworkTick(this);
                this.doneScheduled = true;
            }

            float totalEnergyLast = this.totalEnergy;

            if (doReceive)
            {
                this.totalEnergy += Math.min(energy, this.totalRequested - totalEnergyLast);
                if (producerTier > this.producersTierGC)
                {
                    this.producersTierGC = producerTier;
                }
            }

            if (this.totalRequested >= totalEnergyLast + energy)
            {
                return 0F;
            }
            if (totalEnergyLast >= this.totalRequested)
            {
                return energy;
            }
            return totalEnergyLast + energy - this.totalRequested;
        }
        return energy;
    }

    public void tickEnd()
    {
        this.doneScheduled = false;
        this.loopPrevention = true;

        if (this.totalEnergy > 0F)
        {
            this.doTickStartCalc();

            if (this.totalRequested > 0F)
            {
                this.totalSent = this.doProduce();
                if (this.totalSent < this.totalEnergy)
                {
                    this.totalEnergy -= this.totalSent;
                } else
                {
                    this.totalEnergy = 0F;
                }
            } else
            {
                this.totalEnergy = 0F;
            }
        } else
        {
            this.totalEnergy = 0F;
        }

        this.loopPrevention = false;
    }

    private void doTickStartCalc()
    {
        this.tickDone = EnergyNetwork.tickCount;
        this.totalSent = 0F;
        this.refreshAcceptors();

        if (!EnergyUtil.initialisedIC2Methods)
        {
            EnergyUtil.initialiseIC2Methods();
        }

        if (this.conductors.size() == 0)
        {
            return;
        }

        this.loopPrevention = true;

        this.availableAcceptors.clear();
        this.availableconnectedDirections.clear();
        this.energyRequests.clear();
        this.totalRequested = 0.0F;
        this.totalStorageExcess = 0F;

        if (!this.connectedAcceptors.isEmpty())
        {
            float e;
            final Iterator<EnumFacing> acceptorDirection = this.connectedDirections.iterator();
            for (Object acceptor : this.connectedAcceptors)
            {
                EnumFacing sideFrom = acceptorDirection.next();

                if (!this.ignoreAcceptors.contains(acceptor) && !this.availableAcceptors.contains(acceptor))
                {
                    e = 0.0F;

                    if (acceptor instanceof IElectrical)
                    {
                        e = ((IElectrical) acceptor).getRequest(sideFrom);
                    } else if (isMekLoaded && acceptor instanceof IStrictEnergyAcceptor)
                    {
                        e = (float) (((IStrictEnergyAcceptor) acceptor).acceptEnergy(sideFrom, 1000000D, true) / EnergyConfigHandler.TO_MEKANISM_RATIO);
                    } else if (isIC2Loaded && acceptor instanceof IEnergySink)
                    {
                        double result = 0;
                        try
                        {
                            result = (Double) EnergyUtil.demandedEnergyIC2.invoke(acceptor);
                        } catch (Exception ex)
                        {
                            if (ConfigManagerCore.enableDebug)
                            {
                                ex.printStackTrace();
                            }
                        }
                        result = Math.min(result, this.networkTierGC * 128D);
                        e = (float) result / EnergyConfigHandler.TO_IC2_RATIO;
                    } else if (isRF2Loaded && acceptor instanceof IEnergyReceiver)
                    {
                        e = ((IEnergyReceiver) acceptor).receiveEnergy(sideFrom, Integer.MAX_VALUE, true) / EnergyConfigHandler.TO_RF_RATIO;
                    } else if (isFELoaded && acceptor instanceof net.minecraftforge.energy.IEnergyStorage)
                    {
                        net.minecraftforge.energy.IEnergyStorage forgeEnergy = (net.minecraftforge.energy.IEnergyStorage) acceptor;
                        if (forgeEnergy.canReceive())
                        {
                            e = forgeEnergy.receiveEnergy(Integer.MAX_VALUE, true) / EnergyConfigHandler.TO_RF_RATIO;
                        }
                    }

                    if (e > 0.0F)
                    {
                        this.availableAcceptors.add(acceptor);
                        this.availableconnectedDirections.put(acceptor, sideFrom);
                        this.energyRequests.put(acceptor, e);
                        this.totalRequested += e;
                        if (e > EnergyNetwork.ENERGY_STORAGE_LEVEL)
                        {
                            this.totalStorageExcess += e - EnergyNetwork.ENERGY_STORAGE_LEVEL;
                        }
                    }
                }
            }
        }

        this.loopPrevention = false;
    }

    private float doProduce()
    {
        float sent = 0.0F;

        if (!this.availableAcceptors.isEmpty())
        {
            float energyNeeded = this.totalRequested;
            float energyAvailable = this.totalEnergy;
            float reducor = 1.0F;
            float energyStorageReducor = 1.0F;

            if (energyNeeded > energyAvailable)
            {
                energyNeeded -= this.totalStorageExcess;
                if (energyNeeded > energyAvailable)
                {
                    energyStorageReducor = 0F;
                    reducor = energyAvailable / energyNeeded;
                } else
                {
                    energyStorageReducor = (energyAvailable - energyNeeded) / this.totalStorageExcess;
                }
            }

            float currentSending;
            float sentToAcceptor;
            int tierProduced = Math.min(this.producersTierGC, this.networkTierGC);

            Object debugTE = null;
            try
            {
                for (Object tileEntity : this.availableAcceptors)
                {
                    debugTE = tileEntity;
                    if (sent >= energyAvailable)
                    {
                        break;
                    }

                    currentSending = this.energyRequests.get(tileEntity);

                    if (currentSending > EnergyNetwork.ENERGY_STORAGE_LEVEL)
                    {
                        currentSending = EnergyNetwork.ENERGY_STORAGE_LEVEL + (currentSending - EnergyNetwork.ENERGY_STORAGE_LEVEL) * energyStorageReducor;
                    }

                    currentSending *= reducor;

                    if (currentSending > energyAvailable - sent)
                    {
                        currentSending = energyAvailable - sent;
                    }

                    EnumFacing sideFrom = this.availableconnectedDirections.get(tileEntity);

                    if (tileEntity instanceof IElectrical)
                    {
                        sentToAcceptor = ((IElectrical) tileEntity).receiveElectricity(sideFrom, currentSending, tierProduced, true);
                    } else if (isMekLoaded && tileEntity instanceof IStrictEnergyAcceptor)
                    {
                        sentToAcceptor =
                            (float) ((IStrictEnergyAcceptor) tileEntity).acceptEnergy(sideFrom, currentSending * EnergyConfigHandler.TO_MEKANISM_RATIO, false) / EnergyConfigHandler.TO_MEKANISM_RATIO;
                    } else if (isIC2Loaded && tileEntity instanceof IEnergySink)
                    {
                        double energySendingIC2 = currentSending * EnergyConfigHandler.TO_IC2_RATIO;
                        if (energySendingIC2 >= 1D)
                        {
                            double result = 0;
                            try
                            {
                                if (EnergyUtil.voltageParameterIC2)
                                {
                                    result = (Double) EnergyUtil.injectEnergyIC2.invoke(tileEntity, sideFrom.getOpposite(), energySendingIC2, 120D);
                                } else
                                {
                                    result = (Double) EnergyUtil.injectEnergyIC2.invoke(tileEntity, sideFrom.getOpposite(), energySendingIC2);
                                }
                            } catch (Exception ex)
                            {
                                if (ConfigManagerCore.enableDebug)
                                {
                                    ex.printStackTrace();
                                }
                            }
                            sentToAcceptor = currentSending - (float) result / EnergyConfigHandler.TO_IC2_RATIO;
                            if (sentToAcceptor < 0F)
                            {
                                sentToAcceptor = 0F;
                            }
                        } else
                        {
                            sentToAcceptor = 0F;
                        }
                    } else if (isRF2Loaded && tileEntity instanceof IEnergyReceiver)
                    {
                        final int currentSendinginRF =
                            (currentSending >= Integer.MAX_VALUE / EnergyConfigHandler.TO_RF_RATIO) ? Integer.MAX_VALUE : (int) (currentSending * EnergyConfigHandler.TO_RF_RATIO);
                        sentToAcceptor = ((IEnergyReceiver) tileEntity).receiveEnergy(sideFrom, currentSendinginRF, false) / EnergyConfigHandler.TO_RF_RATIO;
                    } else if (isFELoaded && tileEntity instanceof net.minecraftforge.energy.IEnergyStorage)
                    {
                        final int currentSendinginRF =
                            (currentSending >= Integer.MAX_VALUE / EnergyConfigHandler.TO_RF_RATIO) ? Integer.MAX_VALUE : (int) (currentSending * EnergyConfigHandler.TO_RF_RATIO);
                        sentToAcceptor = ((net.minecraftforge.energy.IEnergyStorage) tileEntity).receiveEnergy(currentSendinginRF, false) / EnergyConfigHandler.TO_RF_RATIO;
                    } else
                    {
                        sentToAcceptor = 0F;
                    }

                    if (sentToAcceptor / currentSending > 1.002F && sentToAcceptor > 0.01F)
                    {
                        if (!this.spamstop)
                        {
                            GalacticraftCore.logger.info("Energy network: acceptor took too much energy, offered " + currentSending + ", took " + sentToAcceptor + ". " + tileEntity.toString());
                            this.spamstop = true;
                        }
                        sentToAcceptor = currentSending;
                    }

                    sent += sentToAcceptor;
                }
            } catch (Exception e)
            {
                GalacticraftCore.logger.error("DEBUG Energy network loop issue, please report this");
                if (debugTE instanceof TileEntity)
                {
                    GalacticraftCore.logger.error("Problem was likely caused by tile in dim " + GCCoreUtil.getDimensionID(((TileEntity) debugTE).getWorld()) + " at " + ((TileEntity) debugTE).getPos() + " Type:"
                        + debugTE.getClass().getSimpleName());
                }
            }
        }

        if (EnergyNetwork.tickCount % 200 == 0)
        {
            this.spamstop = false;
        }

        float returnvalue = sent;
        if (returnvalue > this.totalEnergy)
        {
            returnvalue = this.totalEnergy;
        }
        if (returnvalue < 0F)
        {
            returnvalue = 0F;
        }
        return returnvalue;
    }

    public void refreshWithChecks()
    {
        int tierfound = Integer.MAX_VALUE;
        Iterator<IConductor> it = this.conductors.iterator();
        while (it.hasNext())
        {
            IConductor conductor = it.next();

            if (conductor == null)
            {
                it.remove();
                continue;
            }

            TileEntity tile = (TileEntity) conductor;
            World world = tile.getWorld();
            if (tile.isInvalid() || world == null || !world.isBlockLoaded(tile.getPos()))
            {
                it.remove();
                continue;
            }

            if (conductor != world.getTileEntity(tile.getPos()))
            {
                it.remove();
                continue;
            }

            if (conductor.getTierGC() < tierfound)
            {
                tierfound = conductor.getTierGC();
            }

            if (conductor.getNetwork() != this)
            {
                conductor.setNetwork(this);
                conductor.onNetworkChanged();
            }
        }

        if (tierfound == Integer.MAX_VALUE)
        {
            tierfound = 1;
        }
        this.networkTierGC = tierfound;
    }

    @Override
    public void refresh()
    {
        int tierfound = Integer.MAX_VALUE;
        Iterator<IConductor> it = this.conductors.iterator();
        while (it.hasNext())
        {
            IConductor conductor = it.next();

            if (conductor == null)
            {
                it.remove();
                continue;
            }

            TileEntity tile = (TileEntity) conductor;
            World world = tile.getWorld();
            if (tile.isInvalid() || world == null)
            {
                it.remove();
                continue;
            }

            if (conductor.getTierGC() < tierfound)
            {
                tierfound = conductor.getTierGC();
            }

            if (conductor.getNetwork() != this)
            {
                conductor.setNetwork(this);
                conductor.onNetworkChanged();
            }
        }

        if (tierfound == Integer.MAX_VALUE)
        {
            tierfound = 1;
        }
        this.networkTierGC = tierfound;
    }

    private void refreshAcceptors()
    {
        this.connectedAcceptors.clear();
        this.connectedDirections.clear();

        this.refreshWithChecks();

        try
        {
            LinkedList<IConductor> conductorsCopy = new LinkedList<>();
            conductorsCopy.addAll(this.conductors);
            for (IConductor conductor : conductorsCopy)
            {
                EnergyUtil.setAdjacentPowerConnections((TileEntity) conductor, this.connectedAcceptors, this.connectedDirections);
            }
        } catch (Exception e)
        {
            GalacticraftCore.logger.error("GC Aluminium Wire: Error when testing whether another mod's tileEntity can accept energy.");
            e.printStackTrace();
        }
    }

    @Override
    public IElectricityNetwork merge(IElectricityNetwork network)
    {
        if (network != null && network != this)
        {
            Set<IConductor> thisNetwork = this.conductors;
            Set<IConductor> thatNetwork = network.getTransmitters();
            if (thisNetwork.size() < thatNetwork.size())
            {
                thatNetwork.addAll(thisNetwork);
                network.refresh();
                this.destroy();
                return network;
            }
            thisNetwork.addAll(thatNetwork);
            this.refresh();
            if (network instanceof EnergyNetwork)
            {
                ((EnergyNetwork) network).destroy();
            }
        }

        return this;
    }

    private void destroy()
    {
        this.conductors.clear();
        this.connectedAcceptors.clear();
        this.availableAcceptors.clear();
        this.totalEnergy = 0F;
        this.totalRequested = 0F;
        TickHandlerServer.removeNetworkTick(this);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    public void split(IConductor splitPoint)
    {
        if (splitPoint instanceof TileEntity)
        {
            this.getTransmitters().remove(splitPoint);
            splitPoint.setNetwork(null);

            if (this.getTransmitters().size() > 1)
            {
                World world = ((TileEntity) splitPoint).getWorld();

                if (this.getTransmitters().size() > 0)
                {
                    TileEntity[] nextToSplit = new TileEntity[6];
                    boolean[] toDo = {true, true, true, true, true, true};
                    TileEntity tileEntity;

                    BlockPos pos = ((TileEntity) splitPoint).getPos();

                    for (int j = 0; j < 6; j++)
                    {
                        switch (j)
                        {
                            case 0:
                                tileEntity = world.getTileEntity(pos.down());
                                break;
                            case 1:
                                tileEntity = world.getTileEntity(pos.up());
                                break;
                            case 2:
                                tileEntity = world.getTileEntity(pos.north());
                                break;
                            case 3:
                                tileEntity = world.getTileEntity(pos.south());
                                break;
                            case 4:
                                tileEntity = world.getTileEntity(pos.west());
                                break;
                            case 5:
                                tileEntity = world.getTileEntity(pos.east());
                                break;
                            default:
                                tileEntity = null;
                                break;
                        }

                        if (tileEntity instanceof IConductor && ((IConductor) tileEntity).canConnect(EnumFacing.byIndex(j ^ 1), NetworkType.POWER))
                        {
                            nextToSplit[j] = tileEntity;
                        } else
                        {
                            toDo[j] = false;
                        }
                    }

                    for (int i1 = 0; i1 < 6; i1++)
                    {
                        if (toDo[i1])
                        {
                            TileEntity connectedBlockA = nextToSplit[i1];
                            NetworkFinder finder = new NetworkFinder(world, new BlockVec3(connectedBlockA), new BlockVec3(pos));
                            List<IConductor> partNetwork = finder.exploreNetwork();

                            for (int i2 = i1 + 1; i2 < 6; i2++)
                            {
                                TileEntity connectedBlockB = nextToSplit[i2];

                                if (toDo[i2])
                                {
                                    if (partNetwork.contains(connectedBlockB))
                                    {
                                        toDo[i2] = false;
                                    }
                                }
                            }

                            EnergyNetwork newNetwork = new EnergyNetwork();
                            newNetwork.getTransmitters().addAll(partNetwork);
                            newNetwork.refreshWithChecks();
                        }
                    }

                    this.destroy();
                }
            }
            else if (this.getTransmitters().size() == 0)
            {
                this.destroy();
            }
        }
    }

    @Override
    public String toString()
    {
        return "EnergyNetwork[" + this.hashCode() + "|Wires:" + this.getTransmitters().size() + "|Acceptors:" + this.connectedAcceptors.size() + "]";
    }
}