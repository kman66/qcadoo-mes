package com.warehousecorporation.warehouse.hooks;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.LookupComponent;
import com.warehousecorporation.warehouse.constants.PurchaseFields;
import com.warehousecorporation.warehouse.constants.WarehouseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Currency;
import java.util.Locale;

/**
 * Created by manterys on 30.11.2016.
 */

@Service
public class ViewPurchaseHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setDefaultCurrency(final ViewDefinitionState state){

        FieldComponent currency = (FieldComponent) state.getComponentByReference("purchaseCurrency");
        Locale defaultLocale = Locale.getDefault();
        Currency oCurrency = Currency.getInstance(defaultLocale);
        currency.setFieldValue(oCurrency.getCurrencyCode());
    }

    public void setProductDefaultUnit(final ViewDefinitionState viewDefinitionState, final ComponentState state,
                                      final String[] args){

        /*FieldComponent product = (FieldComponent) viewDefinitionState.getComponentByReference("purchaseProduct");
        FieldComponent productUnit = (FieldComponent) viewDefinitionState.getComponentByReference("productUnit");
        DataDefinition productDD = dataDefinitionService.get("basic", "product");
        Long productId = (Long) product.getFieldValue();

        if(productId != null){
            productUnit.setFieldValue(productDD.get(productId).getStringField("unit"));
        }*/

        LookupComponent lookupProduct = (LookupComponent) viewDefinitionState.getComponentByReference("purchaseProduct");
        FieldComponent productUnit = (FieldComponent) viewDefinitionState.getComponentByReference("productUnit");
        productUnit.setFieldValue(lookupProduct.getEntity().getStringField("unit"));
    }
}
