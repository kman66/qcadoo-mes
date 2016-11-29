-- Added factories (DUR)
-- last touched 27.04.2015 by kama 

CREATE TABLE basic_factory
(
  id bigint NOT NULL,
  "number" character varying(255),
  name character varying(1024),
  city character varying(255),
  active boolean DEFAULT true,
  CONSTRAINT basic_factory_pkey PRIMARY KEY (id)
);

ALTER TABLE basic_division ADD COLUMN factory_id bigint;
ALTER TABLE basic_division
  ADD CONSTRAINT division_factory_fkey FOREIGN KEY (factory_id)
      REFERENCES basic_factory (id) DEFERRABLE;

-- end

-- Scripts for DUR
-- last touched 04.05.2015 by kama

CREATE TABLE jointable_company_workstation
(
  company_id bigint NOT NULL,
  workstation_id bigint NOT NULL,
  CONSTRAINT jointable_company_workstation_pkey PRIMARY KEY (workstation_id, company_id),
  CONSTRAINT jointable_company_workstation_company_fkey FOREIGN KEY (company_id)
      REFERENCES basic_company (id) DEFERRABLE,
  CONSTRAINT jointable_company_workstation_workstation_fkey FOREIGN KEY (workstation_id)
      REFERENCES basic_workstation (id) DEFERRABLE
);

ALTER TABLE basic_workstation ADD COLUMN division_id bigint;
ALTER TABLE basic_workstation
  ADD CONSTRAINT workstation_division_fkey FOREIGN KEY (division_id)
      REFERENCES basic_division (id) DEFERRABLE;
ALTER TABLE basic_workstation ADD COLUMN serialnumber character varying(255);
ALTER TABLE basic_workstation ADD COLUMN udtnumber character varying(255);
ALTER TABLE basic_workstation ADD COLUMN series character varying(255);
ALTER TABLE basic_workstation ADD COLUMN producer character varying(255);
ALTER TABLE basic_workstation ADD COLUMN productiondate date;
ALTER TABLE basic_workstation ADD COLUMN wnknumber character varying(255);

CREATE TABLE basic_workstationattachment
(
  id bigint NOT NULL,
  workstation_id bigint,
  attachment character varying(255),
  name character varying(255),
  size numeric(12,5),
  ext character varying(255),
  CONSTRAINT basic_workstationattachment_pkey PRIMARY KEY (id),
  CONSTRAINT workstationattachment_workstation_fkey FOREIGN KEY (workstation_id)
      REFERENCES basic_workstation (id) DEFERRABLE
);

CREATE TABLE jointable_division_productionline
(
  productionline_id bigint NOT NULL,
  division_id bigint NOT NULL,
  CONSTRAINT jointable_division_productionline_pkey PRIMARY KEY (division_id, productionline_id),
  CONSTRAINT jointable_division_productionline_productionline_fkey FOREIGN KEY (productionline_id)
      REFERENCES productionlines_productionline (id) DEFERRABLE,
  CONSTRAINT jointable_division_productionline_division_fkey FOREIGN KEY (division_id)
      REFERENCES basic_division (id) DEFERRABLE
);

CREATE TABLE basic_subassembly
(
  id bigint NOT NULL,
  "number" character varying(255),
  name character varying(1024),
  workstationtype_id bigint,
  workstation_id bigint,
  serialnumber character varying(255),
  series character varying(255),
  producer character varying(255),
  productiondate date,
  lastrepairsdate date,
  active boolean DEFAULT true,
  CONSTRAINT basic_subassembly_pkey PRIMARY KEY (id),
  CONSTRAINT subassembly_workstationtype_fkey FOREIGN KEY (workstationtype_id)
      REFERENCES basic_workstationtype (id) DEFERRABLE,
  CONSTRAINT subassembly_workstation_fkey FOREIGN KEY (workstation_id)
      REFERENCES basic_workstation (id) DEFERRABLE
);

CREATE TABLE jointable_company_subassembly
(
  company_id bigint NOT NULL,
  subassembly_id bigint NOT NULL,
  CONSTRAINT jointable_company_subassembly_pkey PRIMARY KEY (subassembly_id, company_id),
  CONSTRAINT jointable_company_subassembly_company_fkey FOREIGN KEY (company_id)
      REFERENCES basic_company (id) DEFERRABLE,
  CONSTRAINT jointable_company_subassembly_subassembly_fkey FOREIGN KEY (subassembly_id)
      REFERENCES basic_subassembly (id) DEFERRABLE
);

CREATE TABLE basic_subassemblyattachment
(
  id bigint NOT NULL,
  subassembly_id bigint,
  attachment character varying(255),
  name character varying(255),
  size numeric(12,5),
  ext character varying(255),
  CONSTRAINT basic_subassemblyattachment_pkey PRIMARY KEY (id),
  CONSTRAINT subassemblyattachment_subassembly_fkey FOREIGN KEY (subassembly_id)
      REFERENCES basic_subassembly (id) DEFERRABLE
);

ALTER TABLE basic_workstationtype ADD COLUMN subassembly boolean;

ALTER TABLE basic_division ADD COLUMN active boolean;
ALTER TABLE basic_division ALTER COLUMN active SET DEFAULT true;
ALTER TABLE basic_division ADD COLUMN comment character varying(2048);

CREATE TABLE productionlines_factorystructureelement
(
  id bigint NOT NULL,
  "number" character varying(255),
  name character varying(255),
  parent_id bigint,
  priority integer,
  nodenumber character varying(255),
  entitytype character varying(255) DEFAULT 'factory'::character varying,
  current boolean DEFAULT false,
  subassembly_id bigint,
  workstation_id bigint,
  CONSTRAINT productionlines_factorystructureelement_pkey PRIMARY KEY (id),
  CONSTRAINT factorystructureelement_workstation_fkey FOREIGN KEY (workstation_id)
      REFERENCES basic_workstation (id) DEFERRABLE,
  CONSTRAINT factorystructureelement_parent_fkey FOREIGN KEY (parent_id)
      REFERENCES productionlines_factorystructureelement (id) DEFERRABLE,
  CONSTRAINT factorystructureelement_subassembly_fkey FOREIGN KEY (subassembly_id)
      REFERENCES basic_subassembly (id) DEFERRABLE
);

-- Table: basic_shifttimetableexception
-- last touched 13.05.2015 by pako

ALTER TABLE basic_shifttimetableexception ADD COLUMN relatestoprevday boolean;
ALTER TABLE basic_shifttimetableexception ALTER COLUMN relatestoprevday SET DEFAULT false;

-- end

-- ALTER TABLE technologies_technology
-- last touched 21.05.2015 by kasi
ALTER TABLE technologies_technology ADD COLUMN typeofproductionrecording character varying(255);
ALTER TABLE technologies_technology ADD COLUMN justone boolean;
ALTER TABLE technologies_technology ADD COLUMN generateproductionrecordnumberfromordernumber boolean;
ALTER TABLE technologies_technology ADD COLUMN allowtoclose boolean;
ALTER TABLE technologies_technology ADD COLUMN registerquantityoutproduct boolean;
ALTER TABLE technologies_technology ADD COLUMN autocloseorder boolean;
ALTER TABLE technologies_technology ADD COLUMN registerpiecework boolean;
ALTER TABLE technologies_technology ADD COLUMN registerquantityinproduct boolean;
ALTER TABLE technologies_technology ADD COLUMN registerproductiontime boolean;

--end

