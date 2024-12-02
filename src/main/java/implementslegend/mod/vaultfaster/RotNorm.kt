package implementslegend.mod.vaultfaster

import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation

data class RotNorm private constructor(val rotation:Int, val invertedNormal:Int,val unused:Unit){
    constructor(rotation:Int,normal:Int):this(rotation and 0b11, normal and 0b1,Unit)
}
operator fun RotNorm.plus(other: RotNorm): RotNorm =
    RotNorm(rotation + other.rotation
            +if(invertedNormal == 0 && (rotation and 1)==1 && other.invertedNormal==1)2 else 0//not sure why is this correct but it it correct
        , invertedNormal + other.invertedNormal)

fun RotateMirror.toRotNorm(): RotNorm = when(mirror){
    Mirror.NONE -> when(rotation){
        Rotation.NONE -> RotNorm(0, 0)
        Rotation.CLOCKWISE_90 -> RotNorm(1, 0)
        Rotation.CLOCKWISE_180 -> RotNorm(2, 0)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(3, 0)
    }
    Mirror.FRONT_BACK -> when(rotation){
        Rotation.NONE -> RotNorm(2, 1)
        Rotation.CLOCKWISE_90 -> RotNorm(1, 1)
        Rotation.CLOCKWISE_180 -> RotNorm(0, 1)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(3, 1)
    }
    Mirror.LEFT_RIGHT -> when(rotation){
        Rotation.NONE -> RotNorm(0, 1)
        Rotation.CLOCKWISE_90 -> RotNorm(3, 1)
        Rotation.CLOCKWISE_180 -> RotNorm(2, 1)
        Rotation.COUNTERCLOCKWISE_90 -> RotNorm(1, 1)
    }
}

fun RotNorm.toRotateMirror(): RotateMirror = RotateMirror(
    when (rotation) {
        0 -> Rotation.NONE
        1 -> Rotation.CLOCKWISE_90
        2 -> Rotation.CLOCKWISE_180
        3 -> Rotation.COUNTERCLOCKWISE_90
        else -> throw IllegalStateException("what the hell?")
    }, if (invertedNormal == 0) Mirror.NONE else if ((rotation and 1) == 0) Mirror.LEFT_RIGHT else Mirror.FRONT_BACK
)