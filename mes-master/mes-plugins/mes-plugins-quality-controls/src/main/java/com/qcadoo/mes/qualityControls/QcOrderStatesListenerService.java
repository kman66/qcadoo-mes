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
package com.qcadoo.mes.qualityControls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.messages.constants.StateMessageType;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.search.SearchResult;

@Service
public class QcOrderStatesListenerService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private ParameterService parameterService;

    public void onCompleted(final StateChangeContext stateChangeContext) {
        final Entity order = stateChangeContext.getOwner();
        if (isQualityControlAutoCheckEnabled() && !checkIfAllQualityControlsAreClosed(order)) {
            stateChangeContext.addMessage("qualityControls.qualityControls.not.closed", StateMessageType.FAILURE);
        }
    }

    private boolean isQualityControlAutoCheckEnabled() {
        Entity parameter = parameterService.getParameter();
        return parameter.getBooleanField("checkDoneOrderForQuality");
    }

    private boolean checkIfAllQualityControlsAreClosed(final Entity order) {
        if (order.getBelongsToField("technology") == null) {
            return true;
        }

        Object controlTypeField = order.getBelongsToField("technology").getField("qualityControlType");

        if (controlTypeField == null) {
            return false;
        } else {

            DataDefinition qualityControlDD = null;

            qualityControlDD = dataDefinitionService.get("qualityControls", "qualityControl");

            if (qualityControlDD == null) {
                return false;
            } else {
                SearchResult searchResult = qualityControlDD.find().add(SearchRestrictions.belongsTo("order", order))
                        .add(SearchRestrictions.eq("closed", false)).list();
                return (searchResult.getTotalNumberOfEntities() <= 0);
            }
        }
    }
}
