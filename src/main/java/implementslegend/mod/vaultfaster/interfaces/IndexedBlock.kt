package implementslegend.mod.vaultfaster.interfaces

interface IndexedBlock {
    val registryIndex:Int
    fun copyRegistryIndex(newIndex: Int)
}