package com.warehousecorporation.warehouse.hooks;

import com.qcadoo.view.api.ComponentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.view.api.ViewDefinitionState;

@Service
public class ViewResourceHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setResourceInitialQuantity(final ViewDefinitionState state) {

        ComponentState quantity = (ComponentState) state.getComponentByReference("quantity");

        if(quantity != null){
            if(quantity.getFieldValue() == null) {
                quantity.setFieldValue(0);
            }
        }
    }
}