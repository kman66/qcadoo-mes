package com.warehousecorporation.warehouse.hooks;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.security.api.SecurityService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.warehousecorporation.warehouse.constants.TransferFields;
import com.warehousecorporation.warehouse.constants.ResourceFields;
import com.warehousecorporation.warehouse.constants.WarehouseConstants;

@Service
public class ModelTransferHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityService securityService;

    public void setWorkersDatesAndResourceQuantity(final DataDefinition transferDD, final Entity transfer) {
        if("03correction".equals(transfer.getStringField(TransferFields.TYPE))){
			transfer.setField(TransferFields.REQUEST_WORKER, securityService.getCurrentUserName());
			transfer.setField(TransferFields.REQUEST_DATE, new Date());
			transfer.setField(TransferFields.CONFIRM_WORKER, securityService.getCurrentUserName());
			transfer.setField(TransferFields.CONFIRM_DATE, new Date());
			return;
		}
		
		if (transfer.getId() == null) {
            transfer.setField(TransferFields.REQUEST_WORKER, securityService.getCurrentUserName());
            transfer.setField(TransferFields.REQUEST_DATE, new Date());
        }
		
        if ("02done".equals(transfer.getStringField(TransferFields.STATUS))) {
            transfer.setField(TransferFields.CONFIRM_WORKER, securityService.getCurrentUserName());
            transfer.setField(TransferFields.CONFIRM_DATE, new Date());

            DataDefinition resourceDataDefinition = dataDefinitionService.get(WarehouseConstants.PLUGIN_IDENTIFIER, WarehouseConstants.MODEL_RESOURCE);

            Entity resource = transfer.getBelongsToField(TransferFields.RESOURCE);

            BigDecimal currentQuantity = (BigDecimal)resource.getField(ResourceFields.QUANTITY);
            BigDecimal transferQuantity = (BigDecimal)transfer.getField(TransferFields.QUANTITY);
            BigDecimal newQuantity;

            if ("02outgoing".equals(transfer.getStringField(TransferFields.TYPE))) {
                newQuantity = new BigDecimal(currentQuantity.doubleValue() - transferQuantity.doubleValue());
            } else {
                newQuantity = new BigDecimal(currentQuantity.doubleValue() + transferQuantity.doubleValue());
            }

			if (newQuantity.compareTo(BigDecimal.ZERO)>=0){
                resource.setField(ResourceFields.QUANTITY, newQuantity);
                resourceDataDefinition.save(resource);
            }
        }
    }

    public boolean checkIfHasEnoughtQuantity(final DataDefinition transferDD, final Entity transfer) {
        if ("02done".equals(transfer.getField(TransferFields.STATUS)) && "02outgoing".equals(transfer.getField(TransferFields.TYPE))) {

            Entity resource = transfer.getBelongsToField(TransferFields.RESOURCE);

            BigDecimal currentQuantity = (BigDecimal)resource.getField(ResourceFields.QUANTITY);
            BigDecimal transferQuantity = (BigDecimal)transfer.getField(TransferFields.QUANTITY);

            if (transferQuantity.compareTo(currentQuantity) > 0) {
                transfer.addError(transferDD.getField(TransferFields.QUANTITY), "warehouse.not.enought.resource.error");
                return false;
            }
        }

        return true;
    }
}