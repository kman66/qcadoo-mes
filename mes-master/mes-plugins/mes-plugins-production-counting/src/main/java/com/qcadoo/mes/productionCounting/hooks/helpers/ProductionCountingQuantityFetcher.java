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
package com.qcadoo.mes.productionCounting.hooks.helpers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.qcadoo.mes.basicProductionCounting.BasicProductionCountingService;
import com.qcadoo.mes.basicProductionCounting.constants.ProductionCountingQuantityFields;
import com.qcadoo.mes.productionCounting.constants.OrderFieldsPC;
import com.qcadoo.mes.productionCounting.constants.TypeOfProductionRecording;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchRestrictions;

@Service
public class ProductionCountingQuantityFetcher {

    @Autowired
    private BasicProductionCountingService basicProductionCountingService;

    public List<Entity> getQuantities(final Entity order, final Entity technologyOperationComponent) {
        Preconditions.checkArgument(order != null);
        String typeOfProdRecordingString = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);
        TypeOfProductionRecording typeOfProdRecording = TypeOfProductionRecording.parseString(typeOfProdRecordingString);
        if (typeOfProdRecording == TypeOfProductionRecording.CUMULATED) {
            return getAllQuantities(order);
        } else if (typeOfProdRecording == TypeOfProductionRecording.FOR_EACH) {
            return getQuantitiesForOperation(order, technologyOperationComponent);
        } else {
            // that should never happen
            return Lists.newArrayList();
        }
    }

    private List<Entity> getAllQuantities(final Entity order) {
        SearchCriteriaBuilder scb = basicProductionCountingService.getProductionCountingQuantityDD().find();
        scb.add(SearchRestrictions.belongsTo(ProductionCountingQuantityFields.ORDER, order));

        return scb.list().getEntities();
    }

    private List<Entity> getQuantitiesForOperation(final Entity order, final Entity technologyOperationComponent) {
        SearchCriteriaBuilder scb = basicProductionCountingService.getProductionCountingQuantityDD().find();
        scb.add(SearchRestrictions.belongsTo(ProductionCountingQuantityFields.ORDER, order));
        scb.add(SearchRestrictions.belongsTo(ProductionCountingQuantityFields.TECHNOLOGY_OPERATION_COMPONENT,
                technologyOperationComponent));

        return scb.list().getEntities();
    }

}
