package com.carslab.crm.production.modules.visits.infrastructure.persistence.specification

import com.carslab.crm.production.modules.visits.application.queries.models.VisitSearchCriteria
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity
import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientEntity
import com.carslab.crm.production.modules.vehicles.infrastructure.entity.VehicleEntity
import jakarta.persistence.criteria.*
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component

@Component
class VisitFilterSpecificationBuilder {

    fun buildSpecification(criteria: VisitSearchCriteria): Specification<VisitEntity> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), criteria.companyId))

            criteria.status?.let { status ->
                predicates.add(cb.equal(root.get<Any>("status"), status))
            }

            criteria.startDate?.let { startDate ->
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate))
            }

            criteria.endDate?.let { endDate ->
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate))
            }

            criteria.title?.let { title ->
                predicates.add(cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%"))
            }

            addClientFilters(root, cb, criteria, predicates)
            addVehicleFilters(root, cb, criteria, predicates)
            addServiceFilters(root, cb, criteria, predicates)

            cb.and(*predicates.toTypedArray())
        }
    }

    private fun addClientFilters(
        root: Root<VisitEntity>,
        cb: CriteriaBuilder,
        criteria: VisitSearchCriteria,
        predicates: MutableList<Predicate>
    ) {
        criteria.clientName?.let { clientName ->
            val clientSubquery = cb.createQuery().subquery(Long::class.java)
            val clientRoot = clientSubquery.from(ClientEntity::class.java)

            val firstNamePredicate = cb.like(cb.lower(clientRoot.get("firstName")), "%${clientName.lowercase()}%")
            val lastNamePredicate = cb.like(cb.lower(clientRoot.get("lastName")), "%${clientName.lowercase()}%")
            val fullNamePredicate = cb.like(
                cb.lower(cb.concat(cb.concat(clientRoot.get("firstName"), " "), clientRoot.get("lastName"))),
                "%${clientName.lowercase()}%"
            )

            clientSubquery.select(clientRoot.get("id"))
                .where(
                    cb.and(
                        cb.equal(clientRoot.get<Long>("companyId"), criteria.companyId),
                        cb.or(firstNamePredicate, lastNamePredicate, fullNamePredicate)
                    )
                )

            predicates.add(root.get<Long>("clientId").`in`(clientSubquery))
        }
        
        criteria.clientId?.let { clientId ->
            predicates.add(cb.equal(root.get<Long>("clientId"), clientId.toLong()))
        }
    }

    private fun addVehicleFilters(
        root: Root<VisitEntity>,
        cb: CriteriaBuilder,
        criteria: VisitSearchCriteria,
        predicates: MutableList<Predicate>
    ) {
        if (criteria.licensePlate != null || criteria.make != null || criteria.model != null) {
            val vehicleSubquery = cb.createQuery().subquery(Long::class.java)
            val vehicleRoot = vehicleSubquery.from(VehicleEntity::class.java)
            val vehiclePredicates = mutableListOf<Predicate>()

            vehiclePredicates.add(cb.equal(vehicleRoot.get<Long>("companyId"), criteria.companyId))

            criteria.licensePlate?.let { licensePlate ->
                vehiclePredicates.add(cb.like(cb.lower(vehicleRoot.get("licensePlate")), "%${licensePlate.lowercase()}%"))
            }

            criteria.make?.let { make ->
                vehiclePredicates.add(cb.like(cb.lower(vehicleRoot.get("make")), "%${make.lowercase()}%"))
            }

            criteria.model?.let { model ->
                vehiclePredicates.add(cb.like(cb.lower(vehicleRoot.get("model")), "%${model.lowercase()}%"))
            }

            vehicleSubquery.select(vehicleRoot.get("id"))
                .where(cb.and(*vehiclePredicates.toTypedArray()))

            predicates.add(root.get<Long>("vehicleId").`in`(vehicleSubquery))
        }
    }

    private fun addServiceFilters(
        root: Root<VisitEntity>,
        cb: CriteriaBuilder,
        criteria: VisitSearchCriteria,
        predicates: MutableList<Predicate>
    ) {
        if (criteria.serviceName != null || !criteria.serviceIds.isNullOrEmpty() ||
            criteria.minPrice != null || criteria.maxPrice != null) {

            val serviceSubquery = cb.createQuery().subquery(Long::class.java)
            val serviceRoot = serviceSubquery.from(VisitServiceEntity::class.java)
            val servicePredicates = mutableListOf<Predicate>()

            criteria.serviceName?.let { serviceName ->
                servicePredicates.add(cb.like(cb.lower(serviceRoot.get("name")), "%${serviceName.lowercase()}%"))
            }

            criteria.serviceIds?.takeIf { it.isNotEmpty() }?.let { serviceIds ->
                servicePredicates.add(serviceRoot.get<String>("serviceId").`in`(serviceIds))
            }

            if (criteria.minPrice != null || criteria.maxPrice != null) {
                val totalPriceSubquery = cb.createQuery().subquery(java.math.BigDecimal::class.java)
                val totalPriceRoot = totalPriceSubquery.from(VisitServiceEntity::class.java)

                totalPriceSubquery.select(cb.sum(totalPriceRoot.get("finalPrice")))
                    .where(cb.equal(totalPriceRoot.get<Long>("visitId"), serviceRoot.get<Long>("visitId")))

                criteria.minPrice?.let { minPrice ->
                    servicePredicates.add(cb.greaterThanOrEqualTo(totalPriceSubquery, minPrice))
                }

                criteria.maxPrice?.let { maxPrice ->
                    servicePredicates.add(cb.lessThanOrEqualTo(totalPriceSubquery, maxPrice))
                }
            }

            if (servicePredicates.isNotEmpty()) {
                serviceSubquery.select(serviceRoot.get("visitId"))
                    .where(cb.and(*servicePredicates.toTypedArray()))

                predicates.add(root.get<Long>("id").`in`(serviceSubquery))
            }
        }
    }
}