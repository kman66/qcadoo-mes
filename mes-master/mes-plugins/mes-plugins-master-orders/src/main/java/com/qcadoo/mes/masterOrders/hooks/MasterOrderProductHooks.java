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
package com.qcadoo.mes.masterOrders.hooks;

import com.qcadoo.mes.masterOrders.constants.MasterOrderFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderProductFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderType;
import com.qcadoo.mes.masterOrders.util.MasterOrderOrdersDataProvider;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.model.api.*;
import com.qcadoo.model.api.search.SearchRestrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MasterOrderProductHooks {

    @Autowired
    private MasterOrderOrdersDataProvider masterOrderOrdersDataProvider;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberService numberService;

    private Entity masterOrderProduct;

    public boolean onDelete(final DataDefinition masterOrderProductDD, final Entity masterOrderProduct) {
        return checkAssignedOrder(masterOrderProduct);
    }

    public void onView(final DataDefinition masterOrderProductDD, final Entity masterOrderProduct) {
        countCumulativeOrderQuantity(masterOrderProduct);
        fillRegisteredQuantity(masterOrderProduct);
        calculateLeftToRelease(masterOrderProduct);

    }

    private void calculateLeftToRelease(Entity masterOrderProduct) {
        BigDecimal value = BigDecimalUtils.convertNullToZero(masterOrderProduct.getDecimalField("masterOrderQuantity")).subtract(
                BigDecimalUtils.convertNullToZero(masterOrderProduct.getDecimalField("producedOrderQuantity")),
                numberService.getMathContext());
        if (BigDecimal.ZERO.compareTo(value) == 1) {
            value = BigDecimal.ZERO;
        }
        masterOrderProduct.setField(MasterOrderProductFields.LEFT_TO_RELASE, value);
    }

    private void fillRegisteredQuantity(Entity masterOrderProduct) {
        BigDecimal doneQuantity = masterOrderOrdersDataProvider.sumBelongingOrdersDoneQuantities(
                masterOrderProduct.getBelongsToField(MasterOrderProductFields.MASTER_ORDER),
                masterOrderProduct.getBelongsToField(MasterOrderFields.PRODUCT));
        masterOrderProduct.setField("producedOrderQuantity", doneQuantity);

    }

    private void countCumulativeOrderQuantity(final Entity masterOrderProduct) {
        if (masterOrderProduct.getId() == null) {
            return;
        }
        BigDecimal cumulativeQuantity = masterOrderOrdersDataProvider.sumBelongingOrdersPlannedQuantities(
                masterOrderProduct.getBelongsToField(MasterOrderProductFields.MASTER_ORDER),
                masterOrderProduct.getBelongsToField(MasterOrderFields.PRODUCT));
        masterOrderProduct.setField(MasterOrderFields.CUMULATED_ORDER_QUANTITY, cumulativeQuantity);
    }

    private boolean checkAssignedOrder(final Entity masterOrderProduct) {
        if (masterOrderProduct.getId() == null) {
            return true;
        }
        Entity masterOrder = masterOrderProduct.getBelongsToField(MasterOrderProductFields.MASTER_ORDER);
        if (MasterOrderType.of(masterOrder) != MasterOrderType.MANY_PRODUCTS) {
            return true;
        }
        Entity product = masterOrderProduct.getBelongsToField(MasterOrderProductFields.PRODUCT);
        long numOfBelongingOrdersMatchingProduct = masterOrderOrdersDataProvider.countBelongingOrders(masterOrder,
                SearchRestrictions.belongsTo(OrderFields.PRODUCT, product));

        if (numOfBelongingOrdersMatchingProduct > 0) {
            masterOrderProduct.addGlobalError("masterOrders.masterOrderProduct.delete.existsAssignedOrder");
            return false;
        }
        return true;
    }
}
