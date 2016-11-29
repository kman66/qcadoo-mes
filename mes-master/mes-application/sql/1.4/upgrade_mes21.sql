-- Added field to PPS
-- last touched 01.06.2015 by kama

ALTER TABLE productionpershift_productionpershift ADD COLUMN orderfinishdate timestamp without time zone;

-- end


-- Changes in substitutes
-- last touched 12.05.2015 by kama

ALTER TABLE basic_substitutecomponent ADD COLUMN baseproduct_id bigint;
ALTER TABLE basic_substitutecomponent
  ADD CONSTRAINT substitutecomponent_baseproduct_fkey FOREIGN KEY (baseproduct_id)
      REFERENCES basic_product (id) DEFERRABLE;

-- end


-- Added 'is default' field to delivered products in company
-- last touched 14.05.2015 by kama


ALTER TABLE deliveries_companyproduct ADD COLUMN isdefault boolean;
ALTER TABLE deliveries_companyproduct ALTER COLUMN isdefault SET DEFAULT false;
ALTER TABLE deliveries_companyproductsfamily ADD COLUMN isdefault boolean;
ALTER TABLE deliveries_companyproductsfamily ALTER COLUMN isdefault SET DEFAULT false;

-- end


-- Table: warehouseminimalstate_warehouseminimumstate

-- DROP TABLE warehouseminimalstate_warehouseminimumstate;

CREATE TABLE warehouseminimalstate_warehouseminimumstate
(
  id bigint NOT NULL,
  product_id bigint,
  location_id bigint,
  minimumstate numeric(12,5),
  optimalorderquantity numeric(12,5),
  createdate timestamp without time zone,
  updatedate timestamp without time zone,
  createuser character varying(255),
  updateuser character varying(255),
  CONSTRAINT warehouseminimalstate_warehouseminimumstate_pkey PRIMARY KEY (id),
  CONSTRAINT warehouseminimumstate_product_fkey FOREIGN KEY (product_id)
      REFERENCES basic_product (id) DEFERRABLE,
  CONSTRAINT warehouseminimumstate_location_fkey FOREIGN KEY (location_id)
      REFERENCES materialflow_location (id) DEFERRABLE
);

CREATE TABLE warehouseminimalstate_warehouseminimumstatemulti
(
  id bigint NOT NULL,
  location_id bigint,
  minimumstate numeric(12,5),
  optimalorderquantity numeric(12,5),
  createdate timestamp without time zone,
  updatedate timestamp without time zone,
  createuser character varying(255),
  updateuser character varying(255),
  CONSTRAINT warehouseminimalstate_warehouseminimumstatemulti_pkey PRIMARY KEY (id),
  CONSTRAINT warehouseminimumstatemulti_location_fkey FOREIGN KEY (location_id)
      REFERENCES materialflow_location (id) DEFERRABLE
);

CREATE TABLE jointable_product_warehouseminimumstatemulti
(
  product_id bigint NOT NULL,
  warehouseminimumstatemulti_id bigint NOT NULL,
  CONSTRAINT jointable_product_warehouseminimumstatemulti_pkey PRIMARY KEY (warehouseminimumstatemulti_id, product_id),
  CONSTRAINT warehouseminimalstate_warehouseminimumstatemulti_product_fkey FOREIGN KEY (warehouseminimumstatemulti_id)
      REFERENCES warehouseminimalstate_warehouseminimumstatemulti (id) DEFERRABLE,
  CONSTRAINT basic_product_warehouseminimumstatemulti_fkey FOREIGN KEY (product_id)
      REFERENCES basic_product (id) DEFERRABLE
);

-- end


-- Added updating cost norms for products
-- last touched 28.05.2015 by kama

CREATE TABLE materialflowresources_costnormsgenerator
(
  id bigint NOT NULL,
  costssource character varying(255) DEFAULT '01mes'::character varying,
  productstoupdate character varying(255) DEFAULT '01all'::character varying,
  CONSTRAINT materialflowresources_costnormsgenerator_pkey PRIMARY KEY (id)
);

CREATE TABLE materialflowresources_costnormslocation
(
  id bigint NOT NULL,
  costnormsgenerator_id bigint,
  location_id bigint,
  CONSTRAINT materialflowresources_costnormslocation_pkey PRIMARY KEY (id),
  CONSTRAINT costnormslocation_costnormsgenerator_fkey FOREIGN KEY (costnormsgenerator_id)
      REFERENCES materialflowresources_costnormsgenerator (id) DEFERRABLE,
  CONSTRAINT costnormslocation_location_fkey FOREIGN KEY (location_id)
      REFERENCES materialflow_location (id) DEFERRABLE
);

ALTER TABLE basic_product ADD COLUMN costnormsgenerator_id bigint;
ALTER TABLE basic_product
  ADD CONSTRAINT product_costnormsgenerator_fkey FOREIGN KEY (costnormsgenerator_id)
      REFERENCES materialflowresources_costnormsgenerator (id) DEFERRABLE;

-- end


-- Table: assignmenttoshift_staffassignmenttoshift
-- last touched 03.06.2015 by lupo

ALTER TABLE assignmenttoshift_staffassignmenttoshift ADD COLUMN masterorder_id bigint;

ALTER TABLE assignmenttoshift_staffassignmenttoshift
  ADD CONSTRAINT staffassignmenttoshift_masterorder_fkey FOREIGN KEY (masterorder_id)
      REFERENCES masterorders_masterorder (id) DEFERRABLE;

-- end


-- Table: cmmsmachineparts_machinepartattachment
-- last touched 08.06.2015 by kasi

CREATE TABLE cmmsmachineparts_machinepartattachment
(
  id bigint  NOT NULL,
  product_id bigint,
  attachment character varying(255),
  name character varying(255),
  size numeric(12,5),
  ext character varying(255),
  CONSTRAINT cmmsmachineparts_machinepartattachment_pkey PRIMARY KEY (id),
  CONSTRAINT machinepartattachment_product_fkey FOREIGN KEY (product_id)
      REFERENCES basic_product (id) DEFERRABLE
 );

-- end


-- add positivePurchasePrice to parameters

ALTER TABLE basic_parameter
        ADD COLUMN positivepurchaseprice boolean DEFAULT true;
-- end


-- QCADOOCLS-4011

ALTER TABLE orders_order
  ADD COLUMN  workplandelivered boolean DEFAULT false;

-- end


-- Table: cmmsmachineparts_machinepartattachment and jointables
-- last touched 15.06.2015 by kama

CREATE TABLE cmmsmachineparts_faulttype
(
  id bigint NOT NULL,
  name character varying(255),
  appliesto character varying(255) DEFAULT '01workstationOrSubassembly'::character varying,
  CONSTRAINT cmmsmachineparts_faulttype_pkey PRIMARY KEY (id)
);

CREATE TABLE jointable_faulttype_subassembly
(
  faulttype_id bigint NOT NULL,
  subassembly_id bigint NOT NULL,
  CONSTRAINT jointable_faulttype_subassembly_pkey PRIMARY KEY (subassembly_id, faulttype_id),
  CONSTRAINT jointable_faulttype_subassembly_faulttype_fkey FOREIGN KEY (faulttype_id)
      REFERENCES cmmsmachineparts_faulttype (id) DEFERRABLE,
  CONSTRAINT jointable_faulttype_subassembly_subassembly_fkey FOREIGN KEY (subassembly_id)
      REFERENCES basic_subassembly (id) DEFERRABLE
);

CREATE TABLE jointable_faulttype_workstation
(
  workstation_id bigint NOT NULL,
  faulttype_id bigint NOT NULL,
  CONSTRAINT jointable_faulttype_workstation_pkey PRIMARY KEY (faulttype_id, workstation_id),
  CONSTRAINT jointable_faulttype_workstation_faulttype_fkey FOREIGN KEY (faulttype_id)
      REFERENCES cmmsmachineparts_faulttype (id) DEFERRABLE,
  CONSTRAINT jointable_faulttype_workstation_workstation_fkey FOREIGN KEY (workstation_id)
      REFERENCES basic_workstation (id) DEFERRABLE
);

CREATE TABLE jointable_faulttype_workstationtype
(
  workstationtype_id bigint NOT NULL,
  faulttype_id bigint NOT NULL,
  CONSTRAINT jointable_faulttype_workstationtype_pkey PRIMARY KEY (faulttype_id, workstationtype_id),
  CONSTRAINT jointable_faulttype_workstationtype_workstationtype_fkey FOREIGN KEY (workstationtype_id)
      REFERENCES basic_workstationtype (id) DEFERRABLE,
  CONSTRAINT jointable_faulttype_workstationtype_faulttype_fkey FOREIGN KEY (faulttype_id)
      REFERENCES cmmsmachineparts_faulttype (id) DEFERRABLE
);

-- end


-- Table: basic_parameter
-- last touched 16.06.2015 by lupo

ALTER TABLE basic_parameter ADD COLUMN sameordernumber boolean;
ALTER TABLE basic_parameter ALTER COLUMN sameordernumber SET DEFAULT false;

-- end


-- QCADOOCLS-4053

CREATE OR REPLACE FUNCTION update_ordersupplies_columnforcoverages() RETURNS VOID AS $$  DECLARE row record; BEGIN IF EXISTS (SELECT * FROM ordersupplies_columnforcoverages WHERE identifier = 'isSubcontracted') THEN EXECUTE 'DELETE FROM ordersupplies_columnforcoverages WHERE identifier = ''isSubcontracted'';'; END IF; IF EXISTS (SELECT * FROM ordersupplies_columnforcoverages WHERE identifier = 'isPurchased') THEN EXECUTE 'DELETE FROM ordersupplies_columnforcoverages WHERE identifier = ''isPurchased'';'; END IF; IF NOT EXISTS (SELECT * FROM ordersupplies_columnforcoverages WHERE identifier = 'produceQuantity') THEN EXECUTE 'INSERT INTO ordersupplies_columnforcoverages (identifier, name, description, columnfiller, alignment, succession) VALUES (''produceQuantity'', ''orderSupplies.columnForCoverages.name.value.produceQuantity'', ''orderSupplies.columnForCoverages.description.value.produceQuantity'', ''com.qcadoo.mes.orderSupplies.columnExtension.OrderSuppliesColumnFiller'', ''02right'', 11);';END IF; EXECUTE 'update ordersupplies_columnforcoverages set alignment=''02right'' where identifier=''productUnit'';'; END; $$ LANGUAGE 'plpgsql';

SELECT * FROM update_ordersupplies_columnforcoverages();

DROP FUNCTION update_ordersupplies_columnforcoverages();

-- end


-- Table: assignmenttoshift_assignmenttoshift
-- last touched 22.06.2015 by lupo

ALTER TABLE assignmenttoshift_assignmenttoshift ADD COLUMN externalnumber integer;

ALTER TABLE assignmenttoshift_assignmenttoshift ADD COLUMN externalsynchronized boolean;
ALTER TABLE assignmenttoshift_assignmenttoshift ALTER COLUMN externalsynchronized SET DEFAULT true;

ALTER TABLE assignmenttoshift_assignmenttoshift ADD COLUMN showlaststatechangeresult boolean;
ALTER TABLE assignmenttoshift_assignmenttoshift ALTER COLUMN showlaststatechangeresult SET DEFAULT false;

ALTER TABLE assignmenttoshift_assignmenttoshift ADD COLUMN laststatechangefails boolean;
ALTER TABLE assignmenttoshift_assignmenttoshift ALTER COLUMN laststatechangefails SET DEFAULT false;

ALTER TABLE assignmenttoshift_assignmenttoshift ADD COLUMN laststatechangefailcause character varying(2048);

UPDATE assignmenttoshift_assignmenttoshift SET externalsynchronized = TRUE, showlaststatechangeresult = FALSE, laststatechangefails = false;

-- end



-- Alter type of filename column in workplans
-- last touched 16.06.2015 by kama

ALTER TABLE workplans_workplan ALTER COLUMN filename TYPE character varying(1024);

-- end

ALTER TABLE basic_product ADD COLUMN producer_id bigint;
ALTER TABLE basic_product
 ADD CONSTRAINT product_company_fkey FOREIGN KEY (producer_id)
     REFERENCES basic_company (id) DEFERRABLE;

ALTER TABLE basic_product ADD COLUMN machinepart boolean;

ALTER TABLE basic_product ADD COLUMN drawingnumber character varying(255);

ALTER TABLE basic_product ADD COLUMN catalognumber character varying(255);