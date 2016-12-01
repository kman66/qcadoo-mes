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
import com.warehousecorporation.warehouse.constants.TransferFields;
import com.warehousecorporation.warehouse.constants.ResourceFields;
import com.warehousecorporation.warehouse.constants.WarehouseConstants;

@Service
public class ModelResourceHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;
	
	public void createCorrectionTransfer(final DataDefinition resourceDD, final Entity resource){
 
		DataDefinition transferDataDefinition = dataDefinitionService.get(WarehouseConstants.PLUGIN_IDENTIFIER, WarehouseConstants.MODEL_RESOURCE);
		Entity correction = transferDataDefinition.create();
 
		correction.setField(TransferFields.RESOURCE, resource);
		correction.setField(TransferFields.QUANTITY, resource.getDecimalField(ResourceFields.QUANTITY));
		correction.setField(TransferFields.TYPE, TransferFields.TRANSFER_TYPE_CORRECTION);
		correction.setField(TransferFields.STATUS, TransferFields.TRANSFER_STATUS_CLOSED);
 
		transferDataDefinition.save(correction);
	}
}