# Data sources

The app ships one food database, `app/src/main/assets/foods.json`, built from the open datasets below.
Processed foods (prepared dishes, pastries, packaged drinks, flavoured/fortified dairy) are not bundled
from the Taiwan dataset; users can add their own foods in the app.

| Source | Files in `data/sources/` | License / terms |
|---|---|---|
| 文部科学省「日本食品標準成分表（八訂）」 | `jp_foods.json`, `jp_nutrients.json` | Free to use; secondary use must cite the source. |
| U.S. Department of Agriculture, Agricultural Research Service. FoodData Central. https://fdc.nal.usda.gov/ | `usda_foods.json` | Public domain (CC0 1.0). |
| 衛生福利部食品藥物管理署「食品營養成分資料集」 https://data.gov.tw/dataset/8543 | `tw_foods.json` | 政府資料開放授權條款-第1版 (compatible with CC BY 4.0). |

## Rebuilding

```bash
# 1. (only when TFDA publishes an update) download https://data.fda.gov.tw/data/opendata/export/20/json,
#    unzip it, then regenerate the Taiwan source file
python3 tools/convert_tfda.py path/to/20_x.json

# 2. merge, fix names, fill missing energy and de-duplicate -> app/src/main/assets/foods.json
python3 tools/build_foods.py
```

Then bump `FOOD_DATA_VERSION` in `FoodDbSeeder.kt` so installed apps re-sync the `foods` table.
`data/build_report.md` lists every rename, estimated value and removed duplicate.

Notes:
- Cross-source duplicates (same Chinese name) keep one record, priority Taiwan > Japan > USA.
- Every record has the same fields and the same 16 nutrient columns; a value the source does not provide is `null`.
- Energy and available carbohydrate missing in a source are estimated (Atwater 4/9/4; carbohydrate − fiber) only when the inputs are known, and listed in `estimatedNutrients`.
- All sources use one category set (the English names matched by the nutrition-scoring code).
- Taiwan samples taken in several months are merged into one record (official average when available).

## Attribution statement (顯名聲明)

衛生福利部食品藥物管理署 2025 食品營養成分資料集
此開放資料依政府資料開放授權條款 (Open Government Data License) 進行公眾釋出，使用者於遵守本條款各項規定之前提下，得利用之。
政府資料開放授權條款：https://data.gov.tw/license
