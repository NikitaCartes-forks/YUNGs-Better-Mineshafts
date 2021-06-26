package com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces;

import com.yungnickyoung.minecraft.bettermineshafts.world.BetterMineshaftStructure;
import com.yungnickyoung.minecraft.bettermineshafts.world.generator.BetterMineshaftGenerator;
import com.yungnickyoung.minecraft.bettermineshafts.world.generator.BetterMineshaftStructurePieceType;
import com.yungnickyoung.minecraft.yungsapi.world.BoundingBoxHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesHolder;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Arrays;
import java.util.Random;

public class SmallTunnelTurn extends MineshaftPiece {
    public enum TurnDirection {
        LEFT(0), RIGHT(1);

        private final int value;

        TurnDirection(int value) {
            this.value = value;
        }

        public static TurnDirection valueOf(int value) {
            return Arrays.stream(values())
                    .filter(dir -> dir.value == value)
                    .findFirst().get();
        }
    }

    private final TurnDirection turnDirection;
    private static final int
            SECONDARY_AXIS_LEN = 5,
            Y_AXIS_LEN = 5,
            MAIN_AXIS_LEN = 5;
    private static final int
            LOCAL_X_END = SECONDARY_AXIS_LEN - 1,
            LOCAL_Y_END = Y_AXIS_LEN - 1,
            LOCAL_Z_END = MAIN_AXIS_LEN - 1;

    public SmallTunnelTurn(ServerWorld world, NbtCompound compoundTag) {
        super(BetterMineshaftStructurePieceType.SMALL_TUNNEL_TURN, compoundTag);
        this.turnDirection = TurnDirection.valueOf(compoundTag.getInt("TurnDirection"));
    }

    public SmallTunnelTurn(int chunkPieceLen, Random random, BlockBox blockBox, Direction direction, BetterMineshaftStructure.Type type) {
        super(BetterMineshaftStructurePieceType.SMALL_TUNNEL_TURN, chunkPieceLen, type, blockBox);
        this.setOrientation(direction);
        this.turnDirection = random.nextBoolean() ? TurnDirection.LEFT : TurnDirection.RIGHT;
    }

    @Override
    protected void writeNbt(ServerWorld world, NbtCompound tag) {
        super.writeNbt(world, tag);
        tag.putInt("TurnDirection", this.turnDirection.value);
    }

    public static BlockBox determineBoxPosition(StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, Direction direction) {
        BlockBox blockBox = BoundingBoxHelper.boxFromCoordsWithRotation(x, y, z, SECONDARY_AXIS_LEN, Y_AXIS_LEN, MAIN_AXIS_LEN, direction);

        // The following func call returns null if this new blockbox does not intersect with any pieces in the list.
        // If there is an intersection, the following func call returns the piece that intersects.
        StructurePiece intersectingPiece = structurePiecesHolder.getIntersecting(blockBox);

        // Thus, this function returns null if blackBox intersects with an existing piece. Otherwise, we return blackbox
        return intersectingPiece != null ? null : blockBox;
    }

    @Override
    public void fillOpenings(StructurePiece structurePiece, StructurePiecesHolder structurePiecesHolder, Random random) {
        Direction direction = this.getFacing();
        if (direction == null) {
            return;
        }

        Direction nextDirection = this.turnDirection == TurnDirection.LEFT ? direction.rotateYCounterclockwise() : direction.rotateYClockwise();
        switch (nextDirection) {
            case NORTH:
            default:
                BetterMineshaftGenerator.generateAndAddSmallTunnelPiece(structurePiece, structurePiecesHolder, random, this.boundingBox.getMaxZ(), this.boundingBox.getMinX(), this.boundingBox.getMinY() - 1, nextDirection, chainLength);
                break;
            case SOUTH:
                BetterMineshaftGenerator.generateAndAddSmallTunnelPiece(structurePiece, structurePiecesHolder, random, this.boundingBox.getMinZ(), this.boundingBox.getMinX(), this.boundingBox.getMaxY() + 1, nextDirection, chainLength);
                break;
            case WEST:
                BetterMineshaftGenerator.generateAndAddSmallTunnelPiece(structurePiece, structurePiecesHolder, random, this.boundingBox.getMaxZ() - 1, this.boundingBox.getMinX(), this.boundingBox.getMaxY(), nextDirection, chainLength);
                break;
            case EAST:
                BetterMineshaftGenerator.generateAndAddSmallTunnelPiece(structurePiece, structurePiecesHolder, random, this.boundingBox.getMinZ() + 1, this.boundingBox.getMinX(), this.boundingBox.getMinY(), nextDirection, chainLength);
        }
    }

    @Override
    public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator generator, Random random, BlockBox box, ChunkPos pos, BlockPos blockPos) {
        // Don't spawn if liquid in this box or if in ocean biome
        if (this.isTouchingLiquid(world, box)) return false;
        if (this.isInOcean(world, 0, 0) || this.isInOcean(world, LOCAL_X_END, LOCAL_Z_END)) return false;

        Direction direction = this.getFacing();

        // Randomize blocks
        this.chanceReplaceNonAir(world, box, random, this.getReplacementRate(), 0, 1, 0, LOCAL_X_END, LOCAL_Y_END, LOCAL_Z_END, getMainSelector());

        // Randomize floor
        this.chanceReplaceNonAir(world, box, random, this.getReplacementRate(), 0, 0, 0, LOCAL_X_END, 0, LOCAL_Z_END, getFloorSelector());

        // Fill with air
        this.fill(world, box, 1, 1, 0, LOCAL_X_END - 1, LOCAL_Y_END - 1, LOCAL_Z_END - 1, AIR);

        // Fill in any air in floor with main block
        this.replaceAir(world, box, 1, 0, 0, LOCAL_X_END - 1, 0, LOCAL_Z_END, getMainBlock());

        // Rails
        this.fill(world, box, 2, 1, 0, 2, 1, 1, Blocks.RAIL.getDefaultState());

        if (this.turnDirection == TurnDirection.LEFT) {
            if (direction == Direction.NORTH || direction == Direction.EAST) {
                generateLeftTurn(world, box);
            } else {
                generateRightTurn(world, box);
            }
        } else {
            if (direction == Direction.NORTH || direction == Direction.EAST) {
                generateRightTurn(world, box);
            } else {
                generateLeftTurn(world, box);
            }
        }

        // Decorations
        this.addBiomeDecorations(world, box, random, 0, 0, 0, LOCAL_X_END, LOCAL_Y_END - 1, LOCAL_Z_END);
        this.addVines(world, box, random, 1, 0, 1, LOCAL_X_END - 1, LOCAL_Y_END, LOCAL_Z_END - 1);

        return true;
    }

    private void generateLeftTurn(StructureWorldAccess world, BlockBox box) {
        this.fill(world, box, 0, 1, 1, 0, LOCAL_Y_END - 1, LOCAL_Z_END - 1, AIR);
        this.fill(world, box, 0, 0, 0, 0, 0, LOCAL_Z_END - 1, getMainBlock());
        this.fill(world, box, 2, 1, 2, 2, 1, 2, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.SOUTH_WEST));
        this.fill(world, box, 0, 1, 2, 1, 1, 2, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
    }

    private void generateRightTurn(StructureWorldAccess world, BlockBox box) {
        this.fill(world, box, LOCAL_X_END, 1, 1, LOCAL_X_END, LOCAL_Y_END - 1, LOCAL_Z_END - 1, AIR);
        this.fill(world, box, LOCAL_X_END, 0, 0, LOCAL_X_END, 0, LOCAL_Z_END - 1, getMainBlock());
        this.fill(world, box, 2, 1, 2, 2, 1, 2, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.SOUTH_EAST));
        this.fill(world, box, LOCAL_X_END - 1, 1, 2, LOCAL_X_END, 1, 2, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
    }
}
