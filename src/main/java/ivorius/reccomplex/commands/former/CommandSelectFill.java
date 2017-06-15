/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.former;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockStates;
import ivorius.ivtoolkit.math.IvShapeHelper;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.mcopts.commands.CommandExpecting;
import ivorius.mcopts.commands.parameters.*;
import ivorius.mcopts.commands.parameters.expect.Expect;
import ivorius.mcopts.commands.parameters.expect.MCE;
import ivorius.reccomplex.commands.parameters.expect.RCE;
import ivorius.reccomplex.commands.parameters.RCP;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSelectFill extends CommandExpecting implements CommandVirtual
{
    @Override
    public String getName()
    {
        return RCConfig.commandPrefix + "fill";
    }

    @Override
    public void expect(Expect expect)
    {
        expect.then(MCE::block).then(RCE::metadata)
                .named("shape", "s").any("cube", "sphere");
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender sender, String[] args) throws CommandException
    {
        Parameters parameters = Parameters.of(args, expect()::declare);

        Block dstBlock = parameters.get(0).to(MCP.block(sender)).require();
        int[] dstMeta = parameters.get(1).to(RCP::metadatas).optional().orElse(new int[1]);
        List<IBlockState> dst = IntStream.of(dstMeta).mapToObj(m -> BlockStates.fromMetadata(dstBlock, m)).collect(Collectors.toList());

        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(sender, null, true);
        RCCommands.assertSize(sender, selectionOwner);

        String shape = parameters.get("shape").optional().orElse("cube");

        BlockArea area = selectionOwner.getSelection();
        BlockPos p1 = area.getPoint1();
        BlockPos p2 = area.getPoint2();

        switch (shape)
        {
            case "cube":
                for (BlockPos pos : area)
                {
                    IBlockState state = dst.get(world.rand().nextInt(dst.size()));
                    world.setBlockState(pos, state, 2);
                }
                break;
            case "sphere":
            {
                double[] spheroidOrigin = new double[]{(p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5, (p1.getZ() + p2.getZ()) * 0.5};
                int[] areaSize = area.areaSize();
                double[] spheroidSize = new double[]{areaSize[0] * 0.5, areaSize[1] * 0.5, areaSize[2] * 0.5};

                for (BlockPos coord : area)
                {
                    double[] coordPoint = new double[]{coord.getX(), coord.getY(), coord.getZ()};
                    if (IvShapeHelper.isPointInSpheroid(coordPoint, spheroidOrigin, spheroidSize))
                    {
                        IBlockState state = dst.get(world.rand().nextInt(dst.size()));
                        world.setBlockState(coord, state, 2);
                    }
                }
                break;
            }
            default:
                throw new WrongUsageException(getUsage(sender));
        }

    }
}