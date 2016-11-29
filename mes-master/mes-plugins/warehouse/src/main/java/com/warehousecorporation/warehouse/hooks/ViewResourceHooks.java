package com.warehousecorporation.warehouse.hooks;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;

@Service
public class ViewResourceHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setResourceInitialQuantity(final ViewDefinitionState state) {

        ComponentState quantity = (ComponentState) state.getComponentByReference("quantity");
        if(quantity.getFieldValue() == null) {
            quantity.setFieldValue(0);
        }
    }
}