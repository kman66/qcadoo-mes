package com.warehousecorporation.warehouse.hooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.security.api.SecurityService;
import com.warehousecorporation.warehouse.constants.PurchaseFields;

@Service
public class ModelPurchaseHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityService securityService;
    
    public boolean checkIfProductWithPriceAlreadyExists(final DataDefinition purchaseDD, final Entity purchase){
    	SearchCriteriaBuilder scb = purchaseDD.find();
        scb.add(SearchRestrictions.belongsTo(PurchaseFields.PRODUCT, purchase));
        scb.add(SearchRestrictions.eq(PurchaseFields.PRICE, purchase.getDecimalField(PurchaseFields.PRICE)));

        if(purchase.getId() != null){
            scb.add(SearchRestrictions.idNe(purchase.getId()));
        }
        
        int result = scb.list().getTotalNumberOfEntities();
        
        if(result > 0){
            purchase.addError(purchaseDD.getField(PurchaseFields.PRODUCT), "warehouse.already.the.same.product.with.price");
            return false;
        }

    	return true;
    }
}
