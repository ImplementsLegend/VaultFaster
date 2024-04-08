package implementslegend.mod.vaultfaster

import java.util.concurrent.Executors

val VAULT_GENERATION_EXECUTOR = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors())