# Karatly: requirements and design

| | |
|---|---|
| Status | Draft 0.1, for review |
| Owner | Abhijit Saha |
| Last updated | 2026-11-23 |
| Scope | Personal project first; designed so it can become a product later |

---

## 1. Vision

Karatly tracks the precious metals a person owns (gold, silver, platinum, palladium) in every form they
hold them, values them at today's rates, and answers the question that existing trackers do not:

> **If I sell this today, how much money do I actually make, after charges, deductions and tax?**

Three numbers, always side by side:

1. **Market value**: what the holdings are worth at today's rate.
2. **True cost**: what was really paid: metal + making charges + tax + other charges − discount.
3. **Realisable value and true profit**: what would be received if sold today, after the buyer's
   deductions and estimated tax, minus the true cost.

## 2. Users

| Persona | Need |
|---|---|
| **Individual investor** (MVP: the owner himself) | One place for coins, jewellery, digital gold, ETFs and bonds; honest profit numbers; when to sell. |
| **Family portfolio keeper** (later) | Several portfolios (self, spouse, parents) under one login. |
| **Jeweller / dealer** (much later, B2B) | Buyback desk: quote a customer instantly from purity and weight. |

## 3. Glossary

| Term | Meaning |
|---|---|
| **Metal** | Gold (XAU), silver (XAG), platinum (XPT), palladium (XPD). |
| **Form** | How the metal is held: coin, bar, jewellery, digital gold, ETF, SGB, … (see §5). |
| **Purity** | Fraction of pure metal: 24K = 0.999, 22K = 0.916, 18K = 0.750; silver 999, 925. |
| **Gross / net weight** | Gross includes stones and other material; net is the metal alone. Value uses net weight × purity (fine weight). |
| **Lot** | One purchase of one holding. Sales consume lots. |
| **Rate** | Price of pure metal per gram in a currency on a date, from a source. |
| **Market rate** | Published rate for the day (e.g. IBJA in India). |
| **Buyback rate / deduction** | What a buyer actually pays: market rate minus a deduction (% or flat). |
| **True cost** | Everything paid to acquire a lot (cost basis). |
| **Cost basis method** | How a sale picks lots: FIFO (default), weighted average, or specific lots. |

## 4. Region and currency

- The user picks a **region** in their profile. The region sets defaults for: base currency, display units,
  rate source, tax rules, charge templates and which forms are offered.
- **Every transaction stores its own currency** and, when it differs from the base currency, the exchange
  rate on the transaction date. Reports convert to the base currency; history never changes when exchange
  rates move.
- MVP ships the **India** region (INR, grams and tola, Indian forms and tax). The design must not assume INR
  anywhere else.

## 5. Forms of holding

| Category | Forms | Quantity measured in | Priced by | Notes |
|---|---|---|---|---|
| Physical | Coin, bar/biscuit, jewellery, article/utensil | Weight | Metal rate × fine weight | Making charges, GST, hallmark fee; jewellery has stones. |
| Digital | Digital gold / silver (vault-backed, app-based) | Weight | Platform buy/sell rate | GST on purchase, buy–sell spread. |
| Exchange-traded | Gold/Silver ETF | Units | Market price / NAV | Brokerage and exchange charges. |
| Exchange-traded | Electronic Gold Receipt (EGR, India) | Weight | Exchange price | |
| Funds | Gold/Silver fund-of-funds, international precious-metal funds | Units | NAV | Expense ratio (information only). |
| Government | Sovereign Gold Bond (SGB) | Grams (units of 1 g) | Exchange price; RBI redemption price | 2.5% yearly interest; special tax treatment on maturity. |
| Government | Gold Monetisation Scheme deposit | Weight | Metal rate | Interest; lock-in. |
| Vault / international | Allocated / unallocated vault, bullion account, global ETF | Weight / units | Spot / market price | Storage fees. |
| Derivatives (later) | MCX futures and options | Contracts | Exchange price | Out of scope until v2+. |
| Encumbered | Metal pledged for a loan | Weight | Metal rate | Owned but not sellable; excluded from "sell today" suggestions. |

**Design consequence:** every holding has a `quantity_type` of **WEIGHT** or **UNITS**. Weight-based
holdings are valued from metal rates; unit-based holdings from an instrument price (NAV or market price).
Income (SGB interest, deposit interest) is recorded as its own transaction type.

## 6. Scope

### 6.1 MVP (personal use)

1. Sign in (single user is fine; data model is multi-user from day one).
2. Reference data: metals, purities, forms, units, regions, charge templates.
3. Daily rate fetch for gold, silver, platinum (and palladium) in INR per gram, stored as a history; manual
   rate entry when the feed is down.
4. Record a **purchase** with the full charge breakdown (§7.1), live preview from the API.
5. Record a **sale** against lots (FIFO by default) with deductions (§7.2), live preview from the API.
6. Holdings: per metal, per form, per lot; market value, true cost, realisable value, unrealised P&L.
7. **Sell simulator**: "what if I sell X today", showing both metal-only and true profit.
8. Realised P&L list.
9. Physical forms + digital gold + ETF + SGB. Others follow in v1.

### 6.2 v1

- Tax-aware advisor board: best lots to sell, tax-timing nudge, break-even price per lot.
- Price alerts (target price, break-even reached).
- XIRR, CAGR, allocation charts, benchmark against a fixed deposit.
- Capital-gains report per financial year (export to CSV/PDF).
- Invoice attachment on purchases.
- Remaining forms (EGR, funds, GMS, vault, encumbered).

### 6.3 Product (later)

- Family portfolios, mobile app (React Native), invoice scanning (OCR), push / WhatsApp alerts, more
  regions, freemium subscription.

### 6.4 Out of scope

- Buying or selling metal through the app (no brokerage, no payments).
- Personalised investment advice. Karatly shows **scenarios and indicators**, not recommendations
  (see §11).

## 7. Calculation rules

All money and weight use exact decimals (`NUMERIC` in PostgreSQL, `BigDecimal` in Java). Rounding happens
only when a value is shown or stored as a final amount: money to the currency's minor units, half-up;
weight to 3 decimal places of a gram (1 mg). All calculations run in the API; the web and mobile apps
show what the API returns.

### 7.1 Purchase

Fields:

| Field | Type | Applies to | Default base |
|---|---|---|---|
| Metal, form, purity | Select | All | |
| Gross weight / net weight (+ unit) | Number | Weight-based | net = gross if not given |
| Quantity (units) | Number | Unit-based | |
| Rate | Per gram (or per unit) | All | Pre-filled with the day's rate, editable to match the invoice |
| Making charge | % **or** flat amount | Physical | % of metal value |
| Discount | % or flat | All | % of metal value + making |
| Tax | % | All | % of (metal value + making − discount) |
| Miscellaneous | % | All | % of metal value |
| Flat charges | Amount, list (hallmark, delivery, brokerage, …) | All | |
| Vendor, invoice number, notes | Text | All | |

Order of calculation (bases are configurable per charge template; these are the defaults):

```
metal value     = net weight in grams × rate per gram          (unit-based: quantity × price)
making          = making% × metal value          OR  flat making amount
discount        = discount% × (metal value + making)   OR  flat
taxable value   = metal value + making − discount
tax             = tax% × taxable value
misc            = misc% × metal value
true cost       = taxable value + tax + misc + flat charges
cost per gram   = true cost ÷ net weight
```

Purity is captured on the purchase and used to compare against a 24K/999 rate: when the entered rate is a
pure-metal rate, `metal value = net weight × purity × rate`. The form tells the user which rate is expected
(e.g. "22K rate" vs "24K rate").

**Charge templates** pre-fill these fields per region and form, e.g. India / jewellery: making 10%,
GST 3% on taxable value; India / ETF: brokerage flat, no GST; India / SGB: no GST.

### 7.2 Sale

| Field | Type | Notes |
|---|---|---|
| Quantity to sell | Weight or units | Must not exceed the unencumbered quantity held. |
| Lots | FIFO (default), specific lots | Weighted average is a reporting view. |
| Rate | Today's rate, pre-filled | **Only the sale day's rate is used.** Editable, because the buyer may quote differently. |
| Buyback deduction | % or flat | The buyer's cut below the market rate. |
| Melting / purity loss | % or weight | Jewellery tested and found below stated purity. |
| Stone / wastage deduction | Weight | Jewellery. |
| Selling charges | Flat amounts | Brokerage, platform fee. |

```
sale value        = sellable fine weight × rate                 (unit-based: units × price)
deductions        = buyback + melting/purity loss + stone/wastage + selling charges
net received      = sale value − deductions
cost of lots sold = Σ (true cost of each consumed lot × share of that lot sold)
true profit       = net received − cost of lots sold
metal-only profit = sale value − Σ (metal value of each consumed lot × share sold)
```

Selling part of a lot splits it: the sold share keeps a link to the sale; the remainder stays open with
the matching share of the cost.

### 7.3 Cost basis (pending decision D1)

**Assumed default:** the cost basis for profit and tax includes making charges, tax and other charges paid
(true cost). Both views are always shown: metal-only profit and true profit. To be confirmed with a CA
before the tax report ships.

### 7.4 Tax estimate (v1)

- Holding period is computed per lot from purchase date to sale date.
- Short-term vs long-term thresholds and rates are **data** (`tax_rule` table, per region, form and
  effective date), never code. Example rows for India at the time of writing: physical gold long-term after
  24 months at 12.5%; short-term at the user's slab rate (the user enters their slab). Verify before use.
- SGB held to maturity and redeemed with RBI: capital gain exempt (rule row with rate 0).

### 7.5 Worked example (illustrative numbers)

Buy 10 g of 22K jewellery, rate ₹10,000/g (22K rate), making 10%, misc 0.5%, GST 3%:

| Line | Amount |
|---|---|
| Metal value | ₹1,00,000 |
| Making (10%) | ₹10,000 |
| GST 3% on ₹1,10,000 | ₹3,300 |
| Misc 0.5% of metal value | ₹500 |
| **True cost** | **₹1,13,800** (₹11,380/g) |

Sell later at ₹11,000/g (+10%), buyback deduction 3%:

| Line | Amount |
|---|---|
| Sale value | ₹1,10,000 |
| Buyback deduction (3%) | −₹3,300 |
| **Net received** | **₹1,06,700** |
| Metal-only profit | +₹10,000 |
| **True profit** | **−₹7,100** |

This example is the first acceptance test of the calculation engine.

## 8. User stories (MVP)

| # | Story | Acceptance criteria |
|---|---|---|
| US-1 | As a user I set my region so amounts, units and defaults fit where I live. | Region change updates base currency and defaults; existing transactions keep their own currency. |
| US-2 | As a user I see today's rate for each metal. | Rates refresh daily on a schedule; the page shows the rate's date and source; stale rates (> 1 business day) are flagged. |
| US-3 | As a user I enter a missing or corrected rate by hand. | Manual rates are marked as manual and win over fetched rates for that date. |
| US-4 | As a user I record a purchase with all its charges. | The form previews every line of §7.1 from the API while typing; the saved lot shows true cost and cost per gram; units are converted to grams on save. |
| US-5 | As a user I record a sale and see the profit. | Lots are picked FIFO unless I pick them; I cannot sell more than I hold; the preview shows net received, metal-only and true profit before I save. |
| US-6 | As a user I see my holdings. | Per metal, form and lot: quantity, true cost, market value, realisable value, unrealised P&L (amount and %). |
| US-7 | As a user I ask "what if I sell X today?". | Nothing is saved; result matches what US-5 would record with the same inputs. |
| US-8 | As a user I see what I have already sold. | List of sales with lots consumed and realised profit; totals per financial year. |
| US-9 | As a user I can correct or delete a transaction. | Edits are audited (who, when, before/after); deleting a purchase consumed by a sale is refused until the sale is removed. |

## 9. Data model (first cut)

Reference data:

| Table | Key columns |
|---|---|
| `metal` | `code` (XAU, XAG, XPT, XPD, XCU), name, active |
| `weight_unit` | `code` (G, MG, KG, OZT, TOLA), name, grams_per_unit |
| `purity` | id, metal_code, label (24K, 22K, 999, 925), fineness (0.999, 0.916…) |
| `holding_form` | `code` (COIN, BAR, JEWELLERY, DIGITAL, ETF, SGB, …), category, quantity_type (WEIGHT/UNITS) |
| `region` | `code` (IN…), base_currency, default_unit, rate_source |
| `charge_template` | region, form, charge lines with default % / flat and base |
| `tax_rule` | region, form, long_term_after_months, rates, effective_from / to |

Rates:

| Table | Key columns |
|---|---|
| `metal_rate` | metal, currency, rate_date, source (GOLDPRICE_DEV / GOLDAPI_IO / MANUAL), spot_price_per_gram, premium_percent, price_per_gram, priced_at, updated_at. One row per metal, currency, day and source; also the TTL cache (built in M1) |
| `rate_premium` | metal, premium_percent (premium over spot, e.g. import duty) |
| `rate_provider_call` | provider, called_at, requests, success, message (quota and retry guard) |
| `instrument_price` | instrument (ETF/fund/SGB symbol), currency, price, price_date, source |
| `fx_rate` | from, to, rate, rate_date |

Portfolio (every row has `user_id`):

| Table | Key columns |
|---|---|
| `app_user` | id, email, name, region, base_currency, slab_rate |
| `holding` | id, user, metal, form, purity, instrument (unit-based), description, encumbered |
| `transaction` | id, user, holding, type (BUY / SELL / INCOME / ADJUSTMENT), date, currency, fx_rate, quantity, gross_weight, net_weight, rate, vendor, invoice_no, notes, version |
| `transaction_charge` | transaction, kind (MAKING, TAX, MISC, DISCOUNT, FLAT, BUYBACK, MELTING, STONE, SELLING), mode (PERCENT/FLAT), percent, base, amount |
| `lot` | id, holding, buy transaction, original quantity, open quantity, true cost, metal value |
| `lot_allocation` | sell transaction, lot, quantity, cost share, metal value share |
| `audit_log` | entity, entity_id, action, before, after, at, user |

Rules: weights stored in grams `NUMERIC(18,6)`; money `NUMERIC(19,4)`; rates `NUMERIC(19,6)`; all
timestamps UTC; "today" decided by the user's time zone.

## 10. API (first cut, `/api/v1`)

| Method & path | Purpose |
|---|---|
| `GET /metals`, `/units`, `/purities`, `/forms`, `/regions` | Reference data |
| `GET /rates/latest?currency=INR` | Today's rate per metal (with date and source) |
| `GET /rates/history?metal=XAU&from=&to=` | Rate history for charts |
| `POST /rates` | Manual rate |
| `POST /quotes/purchase` | Calculate a purchase breakdown without saving |
| `POST /quotes/sale` | Calculate a sale (lots, deductions, profit) without saving |
| `GET/POST /holdings`, `GET /holdings/{id}` | Holdings with valuation |
| `POST /transactions/purchases`, `POST /transactions/sales` | Record a purchase / sale |
| `GET /transactions?type=&from=&to=` | Transaction list |
| `PUT/DELETE /transactions/{id}` | Correct / remove (audited) |
| `GET /portfolio/summary` | Totals: invested, market value, realisable value, P&L |
| `GET /reports/realised?fy=2026-27` | Realised gains per financial year |

Conventions (as in Duewise): RFC 7807 problem details with a stable `code` and per-field `errors`;
optimistic locking with a `version` field; paged lists; OpenAPI spec generated by springdoc and used to
generate the web client's types.

## 11. Non-functional requirements

| Area | Requirement |
|---|---|
| Accuracy | Exact decimals everywhere; the calculation engine has unit tests for every rule in §7, including the worked example. |
| Rate data | Provider behind a `RateProvider` interface; at least one fallback; free-tier API keys are never committed. Check the provider's licence before any public launch. |
| Security | Personal financial data: HTTPS only, passwords hashed (or sign-in with Google), every query filtered by `user_id`, audit log on edits, secrets in environment variables. |
| Privacy | Export all my data (CSV/JSON); delete my account. |
| Regulation | Advisor features are worded as scenarios and indicators, not advice. Before charging money, check whether SEBI investment-adviser rules apply. |
| Quality | CI on every push: build, tests, coverage gate (same as Duewise: 90% lines, 70% branches for the API). |
| Hosting | Runs locally with Docker Compose; MVP hosting on a free tier (Render / Oracle Cloud for the API, Vercel / Netlify for the web app, Neon / Supabase for PostgreSQL). |

## 12. Architecture

```
karatly-web (React, TS, Vite, MUI)  ─┐
karatly-mobile (React Native, later) ─┼──>  karatly-api (Java 21, Spring Boot)  ──>  PostgreSQL
                                      │        │
                                      │        └── scheduled rate fetch ──> rate provider(s)
                                      └── types generated from the API's OpenAPI spec
```

API packages (layered by type, then by feature, as in Duewise): `config`, `controller/<feature>`,
`dto/<feature>`, `entity/<feature>`, `repository/<feature>`, `service/<feature>`, `util`, `exception`.
Features: `reference`, `rate`, `portfolio` (holdings, transactions, lots), `quote` (calculation engine),
`valuation`, `report`, later `advisor` and `alert`.

## 13. Open decisions

| # | Decision | Proposal |
|---|---|---|
| D1 | Cost basis includes making, tax and charges? | Yes by default, show both views; confirm with a CA. |
| D2 | Rate provider for India | **Decided (M1):** goldprice.dev for gold, silver and copper; goldapi.io for platinum and palladium; both free plans. One `fetches-per-day` setting for all metals; TTL = 24 h / fetches-per-day, checked against the cached rate's updated_at. India gap: a configurable premium % per metal, and a manual (IBJA) rate wins for its day. |
| D3 | Sign-in | Email + password for MVP, Google sign-in in v1. |
| D4 | Rate the purchase form expects | Pure (24K/999) rate × purity, with an option to enter the purity-specific rate directly. |
| D5 | Hosting for the MVP | Local + tunnel first, then free tier. |

## 14. Milestones

| Milestone | Content |
|---|---|
| M0 | Skeletons, CI, this document (done when both repos build). |
| M1 | Rates: copper, purities, rate fetch from two providers with a TTL cache, premiums, manual rates, rate history. Region and user currency move to M4 with sign-in. |
| M2 | Purchases with charge breakdown + quote endpoint + holdings. |
| M3 | Sales with FIFO lots + sell simulator + realised P&L. |
| M4 | Web UI for M1–M3; MVP complete. |
| M5 | v1: advisor board, alerts, XIRR, reports. |
