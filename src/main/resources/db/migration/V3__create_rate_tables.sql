-- Milestone M1: metal rates.
-- Copper joins the tracked metals; purities say how much pure metal an item holds; metal_rate keeps one
-- price per metal, currency, day and source, which doubles as the cache that saves API calls.

INSERT INTO metal (code, name, sort_order) VALUES ('XCU', 'Copper', 5);

-- How much pure metal an item contains. The value of an item = net weight × fineness × pure-metal rate.
CREATE TABLE purity (
    id          SMALLINT     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metal_code  CHAR(3)      NOT NULL REFERENCES metal (code),
    label       VARCHAR(20)  NOT NULL,
    fineness    NUMERIC(5, 4) NOT NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_purity_label    UNIQUE (metal_code, label),
    CONSTRAINT ck_purity_fineness CHECK (fineness > 0 AND fineness <= 1)
);

INSERT INTO purity (metal_code, label, fineness, sort_order) VALUES
    ('XAU', '24K (999)',      0.9990, 1),
    ('XAU', '24K (995)',      0.9950, 2),
    ('XAU', '22K (916)',      0.9160, 3),
    ('XAU', '18K (750)',      0.7500, 4),
    ('XAU', '14K (585)',      0.5850, 5),
    ('XAG', '999',            0.9990, 1),
    ('XAG', '925 (sterling)', 0.9250, 2),
    ('XPT', '999',            0.9990, 1),
    ('XPT', '950',            0.9500, 2),
    ('XPD', '999',            0.9990, 1),
    ('XPD', '950',            0.9500, 2);

-- One price per metal, currency, day and source. A fetch on the same day updates its row instead of
-- adding one. A MANUAL row (typed in, e.g. from IBJA) wins over fetched rows for that day.
CREATE TABLE metal_rate (
    id                   BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metal_code           CHAR(3)        NOT NULL REFERENCES metal (code),
    currency             CHAR(3)        NOT NULL,
    rate_date            DATE           NOT NULL,              -- the day in the app's time zone
    source               VARCHAR(20)    NOT NULL,
    spot_price_per_gram  NUMERIC(19, 6),                       -- international spot, pure metal; empty for MANUAL
    premium_percent      NUMERIC(7, 4)  NOT NULL DEFAULT 0,    -- import duty / local premium applied
    price_per_gram       NUMERIC(19, 6) NOT NULL,              -- the rate Karatly uses: spot × (1 + premium)
    priced_at            TIMESTAMPTZ    NOT NULL,              -- when the source priced it
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_metal_rate          UNIQUE (metal_code, currency, rate_date, source),
    CONSTRAINT ck_metal_rate_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_metal_rate_source   CHECK (source IN ('GOLDPRICE_DEV', 'GOLDAPI_IO', 'MANUAL')),
    CONSTRAINT ck_metal_rate_price    CHECK (price_per_gram > 0)
);

-- Premium over international spot per metal, e.g. Indian import duty. Applied when a rate is fetched.
CREATE TABLE rate_premium (
    metal_code       CHAR(3)       PRIMARY KEY REFERENCES metal (code),
    premium_percent  NUMERIC(7, 4) NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_rate_premium_range CHECK (premium_percent >= 0 AND premium_percent <= 100)
);

INSERT INTO rate_premium (metal_code) SELECT code FROM metal;

-- Every call to an outside rate API, so the free monthly quotas are never exceeded and a failing
-- provider is not retried on every page load.
CREATE TABLE rate_provider_call (
    id         BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider   VARCHAR(20)   NOT NULL,
    called_at  TIMESTAMPTZ   NOT NULL,
    requests   SMALLINT      NOT NULL,                         -- HTTP requests the fetch used
    success    BOOLEAN       NOT NULL,
    message    VARCHAR(500),
    CONSTRAINT ck_rate_provider_call_requests CHECK (requests > 0)
);

CREATE INDEX ix_rate_provider_call_recent ON rate_provider_call (provider, called_at DESC);
