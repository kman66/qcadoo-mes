package com.warehousecorporation.warehouse.hooks;

import com.google.common.base.Optional;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.LookupComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
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

    public void setDefaultCurrency(final ViewDefinitionState view){

        Optional<FieldComponent> currency = view.tryFindComponentByReference("purchaseCurrency");
        if(currency.isPresent()){
//            Locale defaultLocale = LocaleContextHolder.getLocale();   // nie dziala
            Locale defaultLocale = Locale.getDefault();
            Currency oCurrency = Currency.getInstance(defaultLocale);
            currency.get().setFieldValue(oCurrency.getCurrencyCode());
        }
    }

    public void setProductDefaultUnit(final ViewDefinitionState viewDefinitionState, final ComponentState state,
                                      final String[] args){

//        LookupComponent lookupProduct = (LookupComponent) viewDefinitionState.getComponentByReference("purchaseProduct");
//        FieldComponent productUnit = (FieldComponent) viewDefinitionState.getComponentByReference("productUnit");
        Optional<LookupComponent> lookupProduct = viewDefinitionState.tryFindComponentByReference("purchaseProduct");
        Optional<FieldComponent> productUnit = viewDefinitionState.tryFindComponentByReference("productUnit");

        if(lookupProduct.isPresent() && productUnit.isPresent()){
            productUnit.get().setFieldValue(lookupProduct.get().getEntity().getStringField(ProductFields.UNIT));
        }
    }
}
