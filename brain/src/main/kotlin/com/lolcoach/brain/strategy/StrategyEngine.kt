package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.Strategy

class StrategyEngine(
    private val strategies: MutableList<Strategy> = mutableListOf()
) {
    init {
        if (strategies.isEmpty()) {
            strategies.add(EarlyGameStrategy())
            strategies.add(VisionMacroStrategy())
            strategies.add(ChampSelectStrategy())
            strategies.add(AramStrategy())
        }
    }

    fun allStrategies(): List<Strategy> = strategies.toList()

    fun addStrategy(strategy: Strategy) {
        strategies.add(strategy)
    }

    fun removeStrategy(strategy: Strategy) {
        strategies.remove(strategy)
    }
}
