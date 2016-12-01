package com.warehousecorporation.warehouse.hooks;

import com.google.common.base.Optional;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.components.GridComponent;
import com.warehousecorporation.warehouse.constants.ResourceFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.view.api.ViewDefinitionState;

import java.util.List;

@Service
public class ViewResourceHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setResourceInitialQuantity(final ViewDefinitionState view) {
        Optional<GridComponent>  resourceListGrid = view.tryFindComponentByReference("grid");

        if(resourceListGrid.isPresent()){
            List<Entity> entities =  resourceListGrid.get().getEntities();

            for(Entity e : entities){
                if(e.getDecimalField(ResourceFields.QUANTITY) == null){
                    e.setField(ResourceFields.QUANTITY, Long.valueOf(0));
                }
            }

            resourceListGrid.get().setEntities(entities);
        }
    }
}