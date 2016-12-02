package com.warehousecorporation.warehouse.hooks;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.validators.ErrorMessage;
import com.sun.javafx.collections.MappingChange;
import com.warehousecorporation.warehouse.constants.PurchaseFields;
import com.warehousecorporation.warehouse.constants.WarehouseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by manterys on 02.12.2016.
 */

@Service
public class PurchaseService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public Double getAvgPrice(){
        DataDefinition objDD = dataDefinitionService.get(WarehouseConstants.PLUGIN_IDENTIFIER, WarehouseConstants.MODEL_PURCHASE);
        Entity e = objDD.create();
        e.setField(PurchaseFields.UNIT, "testUnit");
        Entity newE = objDD.save(e);
        if(!newE.isValid()){
            Map<String, ErrorMessage> errors = newE.getErrors();
        }
        Entity entity = dataDefinitionService.get(WarehouseConstants.PLUGIN_IDENTIFIER, WarehouseConstants.MODEL_PURCHASE).find("select avg(price) as avg from #warehouse_purchase").uniqueResult();
        return (Double) entity.getField("avg");
    }
}
