package com.carslab.crm.finances.domain.ports.fixedcosts

import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfiguration
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfigurationId

interface BreakevenConfigurationRepository {

    /**
     * Zapisuje konfigurację break-even
     */
    fun save(configuration: BreakevenConfiguration): BreakevenConfiguration

    /**
     * Znajduje konfigurację po identyfikatorze
     */
    fun findById(id: BreakevenConfigurationId): BreakevenConfiguration?

    /**
     * Znajduje aktywną konfigurację dla firmy
     */
    fun findActiveConfiguration(): BreakevenConfiguration?

    /**
     * Znajduje wszystkie konfiguracje
     */
    fun findAll(): List<BreakevenConfiguration>

    /**
     * Usuwa konfigurację
     */
    fun deleteById(id: BreakevenConfigurationId): Boolean

    /**
     * Dezaktywuje wszystkie konfiguracje
     */
    fun deactivateAll(): Int
}