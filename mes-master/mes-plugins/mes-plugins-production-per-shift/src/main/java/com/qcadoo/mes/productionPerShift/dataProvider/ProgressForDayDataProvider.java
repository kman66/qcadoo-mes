/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.productionPerShift.dataProvider;

import static com.qcadoo.model.api.search.SearchOrders.asc;
import static com.qcadoo.model.api.search.SearchRestrictions.and;
import static com.qcadoo.model.api.search.SearchRestrictions.belongsTo;
import static com.qcadoo.model.api.search.SearchRestrictions.eq;
import static com.qcadoo.model.api.search.SearchRestrictions.idEq;

import java.util.List;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.qcadoo.commons.functional.BiFunction;
import com.qcadoo.commons.functional.Fold;
import com.qcadoo.mes.orders.constants.TechnologyFieldsO;
import com.qcadoo.mes.productionPerShift.constants.ProductionPerShiftConstants;
import com.qcadoo.mes.productionPerShift.constants.ProgressForDayFields;
import com.qcadoo.mes.productionPerShift.constants.ProgressType;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyOperationComponentFields;
import com.qcadoo.mes.technologies.tree.domain.TechnologyOperationId;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityOpResult;
import com.qcadoo.model.api.search.JoinType;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchCriterion;
import com.qcadoo.model.api.search.SearchOrder;

@Service
public class ProgressForDayDataProvider {

    public static final SearchOrder[] DEFAULT_SEARCH_ORDER = new SearchOrder[] { asc(ProgressForDayFields.DAY), asc("id") };

    private static final List<String> MODEL_PATH_TO_ORDER = ImmutableList.of(ProgressForDayFields.TECHNOLOGY_OPERATION_COMPONENT,
            TechnologyOperationComponentFields.TECHNOLOGY, TechnologyFieldsO.ORDERS);

    private static final BiFunction<SearchCriteriaBuilder, String, SearchCriteriaBuilder> CREATE_SUB_QUERY = (acc, fieldName) -> acc
            .createCriteria(fieldName, fieldName + "_alias", JoinType.INNER);

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public List<Entity> findForOrder(final Entity order, final SearchOrder... searchOrders) {
        SearchCriteriaBuilder pfdCriteriaBuilder = getPfdDataDefinition().find();
        subCriteriaFor(pfdCriteriaBuilder, MODEL_PATH_TO_ORDER).add(idEq(order.getId()));
        for (SearchOrder searchOrder : searchOrders) {
            pfdCriteriaBuilder.addOrder(searchOrder);
        }
        return pfdCriteriaBuilder.list().getEntities();
    }

    public List<Entity> findForOperation(final Entity technologyOperation, final ProgressType progressType) {
        return findForOperation(technologyOperation, progressType == ProgressType.CORRECTED);
    }

    public List<Entity> findForOperation(final Entity technologyOperation, final boolean hasCorrections) {
        return find(
                and(eq(ProgressForDayFields.CORRECTED, hasCorrections),
                        belongsTo(ProgressForDayFields.TECHNOLOGY_OPERATION_COMPONENT, technologyOperation)),
                ProgressForDayDataProvider.DEFAULT_SEARCH_ORDER);
    }

    public Optional<Entity> findForOperationAndActualDate(final TechnologyOperationId tocId, final ProgressType progressType,
            final LocalDate day) {
        SearchCriteriaBuilder pfdCriteriaBuilder = getPfdDataDefinition().find();
        pfdCriteriaBuilder.add(eq(ProgressForDayFields.CORRECTED, progressType == ProgressType.CORRECTED));
        pfdCriteriaBuilder
                .add(belongsTo(ProgressForDayFields.TECHNOLOGY_OPERATION_COMPONENT, TechnologiesConstants.PLUGIN_IDENTIFIER,
                        TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT, tocId.get()));
        pfdCriteriaBuilder.add(eq(ProgressForDayFields.ACTUAL_DATE_OF_DAY, day.toDate()));
        return Optional.ofNullable(pfdCriteriaBuilder.setMaxResults(1).uniqueResult());
    }

    public List<Entity> find(final SearchCriterion criteria, final SearchOrder... searchOrders) {
        SearchCriteriaBuilder pfdCriteriaBuilder = getPfdDataDefinition().find();
        if (criteria != null) {
            pfdCriteriaBuilder.add(criteria);
        }
        for (SearchOrder searchOrder : searchOrders) {
            pfdCriteriaBuilder.addOrder(searchOrder);
        }
        return pfdCriteriaBuilder.list().getEntities();
    }

    public EntityOpResult delete(final Iterable<Long> ids) {
        if (Iterables.isEmpty(ids)) {
            return EntityOpResult.successfull();
        }
        return getPfdDataDefinition().delete(FluentIterable.from(ids).toArray(Long.class));
    }

    private SearchCriteriaBuilder subCriteriaFor(final SearchCriteriaBuilder scb, final List<String> path) {
        return Fold.fold(path, scb, CREATE_SUB_QUERY);
    }

    private DataDefinition getPfdDataDefinition() {
        return dataDefinitionService.get(ProductionPerShiftConstants.PLUGIN_IDENTIFIER,
                ProductionPerShiftConstants.MODEL_PROGRESS_FOR_DAY);
    }

}
