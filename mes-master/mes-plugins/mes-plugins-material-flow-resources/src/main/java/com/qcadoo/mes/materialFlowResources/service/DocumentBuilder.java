/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
 * <p>
 * This file is part of Qcadoo.
 * <p>
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.materialFlowResources.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.materialFlowResources.constants.*;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.security.api.UserService;
import com.qcadoo.view.api.utils.NumberGeneratorService;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class DocumentBuilder {

    private final DataDefinitionService dataDefinitionService;

    private final ResourceManagementService resourceManagementService;

    private final UserService userService;

    private final NumberGeneratorService numberGeneratorService;

    private final TranslationService translationService;

    private final ParameterService parameterService;

    private final Entity document;

    private final List<Entity> positions = Lists.newArrayList();

    DocumentBuilder(final DataDefinitionService dataDefinitionService, final ResourceManagementService resourceManagementService,
            final UserService userService, NumberGeneratorService numberGeneratorService,
            final TranslationService translationService, final ParameterService parameterService) {
        this.dataDefinitionService = dataDefinitionService;
        this.resourceManagementService = resourceManagementService;
        this.userService = userService;
        this.numberGeneratorService = numberGeneratorService;
        this.translationService = translationService;
        this.parameterService = parameterService;
        this.document = createDocument(userService, numberGeneratorService);
    }

    public Entity getDocument() {
        return document;
    }

    public DocumentBuilder receipt(final Entity locationTo) {
        document.setField(DocumentFields.LOCATION_TO, locationTo);
        document.setField(DocumentFields.TYPE, DocumentType.RECEIPT.getStringValue());
        return this;
    }

    public DocumentBuilder internalOutbound(Entity locationFrom) {
        document.setField(DocumentFields.LOCATION_FROM, locationFrom);
        document.setField(DocumentFields.TYPE, DocumentType.INTERNAL_OUTBOUND.getStringValue());
        return this;
    }

    public DocumentBuilder internalInbound(Entity locationTo) {
        document.setField(DocumentFields.LOCATION_TO, locationTo);
        document.setField(DocumentFields.TYPE, DocumentType.INTERNAL_INBOUND.getStringValue());
        return this;
    }

    public DocumentBuilder transfer(Entity locationTo, Entity locationFrom) {
        document.setField(DocumentFields.LOCATION_TO, locationTo);
        document.setField(DocumentFields.LOCATION_FROM, locationFrom);
        document.setField(DocumentFields.TYPE, DocumentType.TRANSFER.getStringValue());
        return this;
    }

    public DocumentBuilder release(Entity locationFrom) {
        document.setField(DocumentFields.LOCATION_FROM, locationFrom);
        document.setField(DocumentFields.TYPE, DocumentType.RELEASE.getStringValue());
        return this;
    }

    /**
     * Add position to document, use this method for outbound and transfer documents where additional attributes should not been
     * set.
     *
     * @param product
     * @param quantity
     * @return DocumentBuilder.this
     */
    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity) {
        return addPosition(product, quantity, null, null, null, null);
    }

    /**
     * Add position to document, use this method for outbound and transfer documents for locations with manual algorithm
     *
     * @param product
     * @param quantity
     * @param resource
     * @return DocumentBuilder.this
     */
    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity, final Entity resource) {
        return addPosition(product, quantity, null, null, null, null, resource);
    }

    /**
     * Add position to document, use this method for inbound documents where additional attributes are required sometimes.
     *
     * @param product
     * @param quantity
     * @param price
     * @param batch
     * @param expirationDate
     * @param productionDate
     * @return DocumentBuilder.this
     */
    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity, final BigDecimal price,
            final String batch, final Date productionDate, final Date expirationDate) {
        return addPosition(product, quantity, price, batch, productionDate, expirationDate, null);
    }

    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity, final BigDecimal price,
            final String batch, final Date productionDate, final Date expirationDate, final Entity resource) {
        Preconditions.checkArgument(product != null, "Product argument is required.");
        Preconditions.checkArgument(quantity != null, "Quantity argument is required.");

        Entity position = createPosition(product, quantity, price, batch, productionDate, expirationDate, resource);
        positions.add(position);
        return this;
    }

    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity, final BigDecimal givenQuantity,
            final String givenUnit, final BigDecimal conversion, final BigDecimal price, final String batch,
            final Date productionDate, final Date expirationDate, final Entity resource) {
        Preconditions.checkArgument(product != null, "Product argument is required.");
        Preconditions.checkArgument(quantity != null, "Quantity argument is required.");

        Entity position = createPosition(product, quantity, givenQuantity, givenUnit, conversion, price, batch, productionDate,
                expirationDate, resource);
        positions.add(position);
        return this;
    }

    public DocumentBuilder addPosition(final Entity product, final BigDecimal quantity, final BigDecimal givenQuantity,
            final String givenUnit, final BigDecimal conversion, final BigDecimal price, final String batch,
            final Date productionDate, final Date expirationDate, final Entity resource, final Entity storageLocation,
            final Entity palletNumber, final String typeOfPallet, final Entity additionalCode, final boolean isWaste) {
        Preconditions.checkArgument(product != null, "Product argument is required.");
        Preconditions.checkArgument(quantity != null, "Quantity argument is required.");

        Entity position = createPosition(product, quantity, givenQuantity, givenUnit, conversion, price, batch, productionDate,
                expirationDate, resource, storageLocation, palletNumber, typeOfPallet, additionalCode, isWaste);
        positions.add(position);
        return this;
    }

    /**
     * Creates position with given field values (with the same base and given unit)
     *
     * @param product
     * @param quantity
     * @param price
     * @param batch
     * @param expirationDate
     * @param productionDate
     * @param resource
     * @return Created position entity
     */
    public Entity createPosition(final Entity product, final BigDecimal quantity, final BigDecimal price, final String batch,
            final Date productionDate, final Date expirationDate, final Entity resource) {
        DataDefinition positionDD = dataDefinitionService
                .get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER, MaterialFlowResourcesConstants.MODEL_POSITION);
        Entity position = positionDD.create();

        position.setField(PositionFields.PRODUCT, product);
        position.setField(PositionFields.QUANTITY, quantity);
        position.setField(PositionFields.GIVEN_QUANTITY, quantity);
        position.setField(PositionFields.GIVEN_UNIT, product.getStringField(ProductFields.UNIT));
        position.setField(PositionFields.PRICE, price);
        position.setField(PositionFields.BATCH, batch);
        position.setField(PositionFields.PRODUCTION_DATE, productionDate);
        position.setField(PositionFields.EXPIRATION_DATE, expirationDate);
        position.setField(PositionFields.RESOURCE, resource);
        return position;
    }

    /**
     * Creates position with given field values (with different base unit and given unit)
     *
     * @param product
     * @param quantity
     * @param givenQuantity
     * @param givenUnit
     * @param price
     * @param batch
     * @param expirationDate
     * @param productionDate
     * @param resource
     * @return Created position entity
     */
    public Entity createPosition(final Entity product, final BigDecimal quantity, final BigDecimal givenQuantity,
            final String givenUnit, final BigDecimal conversion, final BigDecimal price, final String batch,
            final Date productionDate, final Date expirationDate, final Entity resource) {
        Entity position = createPosition(product, quantity, price, batch, productionDate, expirationDate, resource);
        position.setField(PositionFields.CONVERSION, conversion);
        position.setField(PositionFields.GIVEN_QUANTITY, givenQuantity);
        position.setField(PositionFields.GIVEN_UNIT, givenUnit);
        return position;
    }

    public Entity createPosition(final Entity product, final BigDecimal quantity, final BigDecimal givenQuantity,
            final String givenUnit, final BigDecimal conversion, final BigDecimal price, final String batch,
            final Date productionDate, final Date expirationDate, final Entity resource, final Entity storageLocation,
            final Entity palletNumber, final String typeOfPallet, final Entity additionalCode, final boolean isWaste) {
        Entity position = createPosition(product, quantity, givenQuantity, givenUnit, conversion, price, batch, productionDate,
                expirationDate, resource);
        position.setField(PositionFields.STORAGE_LOCATION, storageLocation);
        position.setField(PositionFields.PALLET_NUMBER, palletNumber);
        position.setField(PositionFields.TYPE_OF_PALLET, typeOfPallet);
        position.setField(PositionFields.ADDITIONAL_CODE, additionalCode);
        position.setField(PositionFields.WASTE, isWaste);
        return position;
    }

    /**
     * Add previously created position to document
     *
     * @param position
     * @return DocumentBuilder.this
     */
    public DocumentBuilder addPosition(final Entity position) {
        Preconditions.checkArgument(position != null, "Position argument is required.");
        positions.add(position);
        return this;
    }

    public DocumentBuilder setAccepted() {
        document.setField(DocumentFields.STATE, DocumentState.ACCEPTED.getStringValue());
        return this;
    }

    /**
     * Use this method to set document fields added by any extending plugins.
     *
     * @param field field name
     * @param value field value
     * @return this builder
     */
    public DocumentBuilder setField(String field, Object value) {
        document.setField(field, value);
        return this;
    }

    /**
     * Use this method to get document type.
     */
    public DocumentType getDocumentType() {
        return DocumentType.parseString(document.getStringField(DocumentFields.TYPE));
    }

    /**
     * Save document in database and creates resources if document is accepted.
     *
     * @return Created document entity.
     */
    public Entity build() {
        DataDefinition documentDD = dataDefinitionService
                .get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER, MaterialFlowResourcesConstants.MODEL_DOCUMENT);

        document.setField(DocumentFields.POSITIONS, positions);

        if (document.isValid() && DocumentState.of(document) == DocumentState.ACCEPTED) {
            createResources();
        }
        Entity savedDocument = documentDD.save(document);

        if (savedDocument.isValid() && DocumentState.ACCEPTED.getStringValue()
                .equals(savedDocument.getStringField(DocumentFields.STATE)) && buildConnectedPZDocument(savedDocument)) {
            ReceiptDocumentForReleaseHelper receiptDocumentForReleaseHelper = new ReceiptDocumentForReleaseHelper(
                    dataDefinitionService, resourceManagementService, userService, numberGeneratorService, translationService, parameterService);
            receiptDocumentForReleaseHelper.tryBuildConnectedPZDocument(savedDocument, false);
        }

        if (!savedDocument.isValid()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return savedDocument;
    }

    private boolean buildConnectedPZDocument(final Entity document) {
        if (document.getBooleanField(DocumentFields.CREATE_LINKED_PZ_DOCUMENT)
                && document.getBelongsToField(DocumentFields.LINKED_PZ_DOCUMENT_LOCATION) != null) {
            return true;
        }
        return false;
    }

    private void createResources() {
        DocumentType documentType = DocumentType.of(document);
        if (DocumentType.RECEIPT.equals(documentType) || DocumentType.INTERNAL_INBOUND.equals(documentType)) {
            resourceManagementService.createResourcesForReceiptDocuments(document);

        } else if (DocumentType.INTERNAL_OUTBOUND.equals(documentType) || DocumentType.RELEASE.equals(documentType)) {
            resourceManagementService.updateResourcesForReleaseDocuments(document);

        } else if (DocumentType.TRANSFER.equals(documentType)) {
            resourceManagementService.moveResourcesForTransferDocument(document);

        } else {
            throw new IllegalStateException("Unsupported document type");
        }
    }

    public Entity createDocument(UserService userService, NumberGeneratorService numberGeneratorService) {
        DataDefinition documentDD = dataDefinitionService
                .get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER, MaterialFlowResourcesConstants.MODEL_DOCUMENT);
        Entity newDocument = documentDD.create();
        newDocument.setField(DocumentFields.TIME, new Date());
        newDocument.setField(DocumentFields.USER, userService.getCurrentUserEntity().getId());
        newDocument.setField(DocumentFields.STATE, DocumentState.DRAFT.getStringValue());
        newDocument.setField(DocumentFields.POSITIONS, Lists.newArrayList());
        return newDocument;
    }
}
