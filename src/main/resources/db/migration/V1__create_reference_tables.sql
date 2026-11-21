-- Reference (lookup) tables. Holdings and rates point at these instead of storing free text,
-- so a metal or unit is defined once and every row stays consistent.
-- More reference tables (purity, holding_form, region, charge_template, tax_rule) follow with the
-- features that need them; see docs/requirements-and-design.md, section 9.

CREATE TABLE metal (
    code        CHAR(3)     PRIMARY KEY,           -- ISO 4217 metal code: XAU, XAG, XPT, XPD. Never changes.
    name        VARCHAR(30) NOT NULL,
    sort_order  SMALLINT    NOT NULL DEFAULT 0,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_metal_code CHECK (code ~ '^X[A-Z]{2}$')
);

-- Every weight is stored in grams. A unit only says how many grams one of it is, so converting is a
-- single multiplication and adding a unit needs no code change.
CREATE TABLE weight_unit (
    code            VARCHAR(10)    PRIMARY KEY,
    name            VARCHAR(30)    NOT NULL,
    symbol          VARCHAR(10)    NOT NULL,
    grams_per_unit  NUMERIC(20, 10) NOT NULL,
    sort_order      SMALLINT       NOT NULL DEFAULT 0,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_weight_unit_code  CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_weight_unit_grams CHECK (grams_per_unit > 0)
);
