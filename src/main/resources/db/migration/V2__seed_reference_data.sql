-- Metals Karatly tracks, with their ISO 4217 codes.
INSERT INTO metal (code, name, sort_order) VALUES
    ('XAU', 'Gold',      1),
    ('XAG', 'Silver',    2),
    ('XPT', 'Platinum',  3),
    ('XPD', 'Palladium', 4);

-- Weight units. Troy ounce: 31.1034768 g exactly (international definition).
-- Tola: 11.6638038 g, the customary Indian unit (180 grains), used by Indian jewellers.
INSERT INTO weight_unit (code, name, symbol, grams_per_unit, sort_order) VALUES
    ('G',    'Gram',       'g',    1,            1),
    ('MG',   'Milligram',  'mg',   0.001,        2),
    ('KG',   'Kilogram',   'kg',   1000,         3),
    ('OZT',  'Troy ounce', 'oz t', 31.1034768,   4),
    ('TOLA', 'Tola',       'tola', 11.6638038,   5);
