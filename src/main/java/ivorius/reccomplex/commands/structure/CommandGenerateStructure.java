/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure;

import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.mcopts.commands.SimpleCommand;
import ivorius.mcopts.commands.parameters.MCP;
import ivorius.mcopts.commands.parameters.Parameters;
import ivorius.mcopts.commands.parameters.expect.Expect;
import ivorius.mcopts.commands.parameters.expect.MCE;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.parameters.IvP;
import ivorius.reccomplex.commands.parameters.RCP;
import ivorius.reccomplex.commands.parameters.expect.IvE;
import ivorius.reccomplex.commands.parameters.expect.RCE;
import ivorius.reccomplex.operation.OperationRegistry;
import ivorius.reccomplex.utils.RCBlockAreas;
import ivorius.reccomplex.utils.RCStrings;
import ivorius.reccomplex.world.gen.feature.StructureGenerator;
import ivorius.reccomplex.world.gen.feature.structure.OperationGenerateStructure;
import ivorius.reccomplex.world.gen.feature.structure.Placer;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext.GenerateMaturity.FIRST;
import static ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext.GenerateMaturity.SUGGEST;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandGenerateStructure extends SimpleCommand
{
    public CommandGenerateStructure()
    {
        super(RCConfig.commandPrefix + "gen");
        permitFor(2);
    }

    @Override
    public void expect(Expect expect)
    {
        expect
                .then(RCE::structure).required()
                .then(IvE.surfacePos("x", "z"))
                .named("dimension", "d").then(MCE::dimension)
                .named("gen").then(RCE.generationType(p -> p.get(0)))
                .named("rotation", "r").then(MCE::rotation)
                .named("seed").words(RCE::randomString).descriptionU("seed")
                .flag("mirror", "m")
                .flag("select", "s")
                .flag("suggest", "t")
                ;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        Parameters parameters = Parameters.of(args, expect()::declare);

        String structureID = parameters.get(0).require();
        Structure<?> structure = parameters.get(0).to(RCP::structure).require();
        WorldServer world = parameters.get("dimension").to(MCP.dimension(server, sender)).require();
        AxisAlignedTransform2D transform = parameters.get(IvP.transform("rotation", "mirror")).optional().orElse(null);
        GenerationType generationType = parameters.get("gen").to(RCP.generationType(structure)).require();
        BlockSurfacePos pos = parameters.get(IvP.surfacePos("x", "z", sender.getPosition(), false)).require();
        String seed = parameters.get("seed").optional().orElse(null);
        boolean select = parameters.has("select");
        boolean suggest = parameters.has("suggest");

        Placer placer = generationType.placer();

        StructureGenerator<?> generator = new StructureGenerator<>(structure).world(world).generationInfo(generationType)
                .seed(RCStrings.seed(seed))
                .structureID(structureID).randomPosition(pos, placer).fromCenter(true)
                .maturity(suggest ? SUGGEST : FIRST)
                .transform(transform);

        if (structure instanceof GenericStructure && world == sender.getEntityWorld())
        {
            GenericStructure genericStructureInfo = (GenericStructure) structure;

            //noinspection unchecked
            if (!OperationRegistry.queueOperation(new OperationGenerateStructure(genericStructureInfo, generationType.id(), generator.transform(), generator.lowerCoord().orElse(null), false)
                    .withSeed(seed)
                    .withStructureID(structureID).prepare((Optional<GenericStructure.InstanceData>) generator.instanceData()), sender))
                return;
        }
        else
        {
            if (generator.generate() == null)
                throw RecurrentComplex.translations.commandException("commands.strucGen.noPlace");
        }

        if (select)
        {
            SelectionOwner owner = RCCommands.getSelectionOwner(sender, null, false);
            owner.setSelection(RCBlockAreas.from(generator.boundingBox().get()));
        }
    }
}