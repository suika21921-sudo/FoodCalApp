#!/usr/bin/env python3
"""Convert the Taiwan FDA food nutrition open dataset into data/sources/tw_foods.json.

Source: 衛生福利部食品藥物管理署「食品營養成分資料集」 (data.gov.tw dataset 8543, InfoId=20)
Download: https://data.fda.gov.tw/data/opendata/export/20/json  (a zip containing one JSON file)
License: 政府資料開放授權條款-第1版 (Open Government Data License, version 1.0), compatible with CC BY 4.0.

Usage:
    python3 tools/convert_tfda.py path/to/20_x.json [data/sources/tw_foods.json]
    python3 tools/build_foods.py          # then rebuild app/src/main/assets/foods.json

The raw file is a long table (one row per sample x nutrient). This script pivots it to one
record per sample and skips processed foods (prepared dishes, pastries, packaged drinks,
flavoured/fortified dairy) — users can add those themselves as needed.

Samples taken in different months ("全脂鮮乳(6月取樣)", ...) are collapsed into one record: the
official average sample is used when TFDA publishes one, otherwise the monthly samples are averaged.
"""
import json
import re
import sys
from collections import OrderedDict

EXCLUDED_CATEGORIES = {"加工調理食品及其他類", "糕餅點心類", "飲料類"}
# Within kept categories, drop obviously processed variants.
EXCLUDED_NAME_PATTERNS = {
    "乳品類": re.compile(r"調味|強化"),
}

# Map TFDA categories onto the category labels already used by jp_foods.json so the UI groups them together.
CATEGORY_MAP = {
    "魚貝類": ("魚貝類", "Fish and Shellfish", "魚介類"),
    "蔬菜類": ("蔬菜", "Vegetables", "野菜類"),
    "水果類": ("水果", "Fruits", "果実類"),
    "肉類": ("肉類", "Meats", "肉類"),
    "調味料及香辛料類": ("調味料與辛香料", "Seasonings and Spices", "調味料及び香辛料類"),
    "穀物類": ("穀類", "Cereals", "穀類"),
    "乳品類": ("乳制品", "Dairy Products", "乳類"),
    "蛋類": ("蛋類", "Eggs", "卵類"),
    "菇類": ("菇類", "Mushrooms", "きのこ類"),
    "澱粉類": ("薯類及澱粉", "Potatoes and Starches", "いも及びでん粉類"),
    "豆類": ("豆類", "Legumes", "豆类"),
    "油脂類": ("油脂類", "Fats and Oils", "油脂類"),
    "堅果及種子類": ("堅果及種子", "Nuts and Seeds", "種実類"),
    "藻類": ("海藻類", "Seaweeds", "藻類"),
    "糖類": ("糖類及甜味品", "Sugars and Sweeteners", "砂糖及び甘味類"),
}

# TFDA analysis item -> (app nutrient key, unit). The first 8 keys are NutrientKeys.STANDARD_KEYS;
# the rest are kept for search/RAG and future flavour features (sweet/umami/salty estimation).
NUTRIENT_MAP = OrderedDict([
    ("修正熱量", ("Energy", "kcal")),
    ("粗蛋白", ("Protein", "g")),
    ("總碳水化合物", ("Carbohydrate, by difference", "g")),
    ("膳食纖維", ("Fiber, total dietary", "g")),
    ("粗脂肪", ("Total lipid (fat)", "g")),
    ("鈉", ("Sodium, Na", "mg")),
    ("膽固醇", ("Cholesterol", "mg")),
    ("水分", ("Water", "g")),
    ("飽和脂肪", ("Fatty acids, total saturated", "g")),
    ("糖質總量", ("Sugars, total", "g")),
    ("鉀", ("Potassium, K", "mg")),
    ("鈣", ("Calcium, Ca", "mg")),
    ("鐵", ("Iron, Fe", "mg")),
    ("維生素C", ("Vitamin C, total ascorbic acid", "mg")),
    ("麩胺酸(Glu)", ("Glutamic acid", "mg")),
])

ID_RE = re.compile(r"^([A-Z])(\d{5}|\d{7})$")
ID_BASE = 900_000_000  # keeps TW ids clear of JP (1001-18052) and USDA (<3,000,000) ids


def stable_id(code: str) -> int:
    m = ID_RE.match(code)
    if not m:
        raise ValueError(f"Unexpected 整合編號: {code}")
    letter, digits = m.groups()
    return ID_BASE + (ord(letter) - 65) * 20_000_000 + (10_000_000 if len(digits) == 5 else 0) + int(digits)


def num(value):
    if value is None:
        return None
    try:
        return float(str(value).strip())
    except ValueError:
        return None


def clean_name(name: str) -> str:
    # "大番茄平均值(紅色系)" -> "大番茄(紅色系)（平均值）"
    if "平均值" in name:
        name = name.replace("平均值", "") + "（平均值）"
    return name.strip()


MONTH_RE = re.compile(r"\(\d{1,2}月取樣\)")
AVG_SUFFIX = "（平均值）"


def base_name(name: str) -> str:
    return MONTH_RE.sub("", name).replace(AVG_SUFFIX, "").strip()


def average_records(recs):
    keep = dict(recs[0])
    protos = OrderedDict()
    for r in recs:
        for k, v in r["nutrients"].items():
            protos.setdefault(k, v)
    nutrients = OrderedDict()
    for k, proto in protos.items():
        vals = [r["nutrients"][k]["amount"] for r in recs
                if k in r["nutrients"] and r["nutrients"][k]["amount"] is not None]
        if vals:
            entry = dict(proto)
            entry["amount"] = round(sum(vals) / len(vals), 2)
            nutrients[k] = entry
    keep["nutrients"] = nutrients
    keep["description"] = f"{len(recs)} 個月份取樣之平均; " + (keep.get("description") or "")
    return keep


def collapse_monthly_samples(records):
    """Merge "(N月取樣)" siblings and official averages into one record per food."""
    groups = OrderedDict()
    for rec in records:
        groups.setdefault((rec["categoryZh"], base_name(rec["nameZh"])), []).append(rec)

    result, dropped_count = [], 0
    for (_, base), recs in groups.items():
        monthly = [r for r in recs if MONTH_RE.search(r["nameZh"])]
        averages = [r for r in recs if r["nameZh"].endswith(AVG_SUFFIX)]
        if not monthly and not averages:
            result.extend(recs)
            continue
        plain = [r for r in recs if r not in monthly and r not in averages]
        if averages:
            keep = dict(averages[0])
            dropped = monthly + [r for r in plain if r["nameZh"] == base]
            others = [r for r in plain if r["nameZh"] != base]
            merged_codes = [averages[0]["sourceCode"]] + [r["sourceCode"] for r in dropped]
        else:
            keep = average_records(monthly)
            dropped, others = monthly[1:], plain
            merged_codes = [r["sourceCode"] for r in monthly]
        keep["nameZh"] = base
        if len(merged_codes) > 1:
            keep["mergedFrom"] = merged_codes
        result.append(keep)
        result.extend(others)
        dropped_count += len(dropped)
    return result, dropped_count


def main(src: str, dst: str) -> None:
    with open(src, encoding="utf-8-sig") as f:
        rows = json.load(f)

    samples = OrderedDict()
    for r in rows:
        code = r["整合編號"]
        s = samples.get(code)
        if s is None:
            s = samples[code] = {"meta": r, "values": {}}
        s["values"][r["分析項"]] = num(r["每100克含量"])

    out, skipped = [], 0
    for code, s in samples.items():
        meta = s["meta"]
        cat = meta["食品分類"]
        name = meta["樣品名稱"] or ""
        pattern = EXCLUDED_NAME_PATTERNS.get(cat)
        if cat in EXCLUDED_CATEGORIES or cat not in CATEGORY_MAP or (pattern and pattern.search(name)):
            skipped += 1
            continue

        vals = s["values"]
        nutrients = OrderedDict()
        for src_key, (key, unit) in NUTRIENT_MAP.items():
            v = vals.get(src_key)
            if src_key == "修正熱量" and v is None:
                v = vals.get("熱量")
            if v is not None:
                nutrients[key] = {"amount": v, "unit": unit}
        carb, fib = vals.get("總碳水化合物"), vals.get("膳食纖維")
        if carb is not None:
            nutrients["Available carbohydrate"] = {
                "amount": round(max(carb - (fib or 0.0), 0.0), 2),
                "unit": "g",
                "note": "Estimated as (Carbohydrate by difference - Fiber)",
            }

        zh, en, ja = CATEGORY_MAP[cat]
        name_en = (meta.get("樣品英文名稱") or "").split(";")[0].strip()
        aliases = [a.strip() for a in (meta.get("俗名") or "").split(",") if a.strip()]
        out.append(OrderedDict([
            ("fdcId", stable_id(code)),
            ("source", "TW"),
            ("sourceCode", code),
            ("nameEn", name_en or clean_name(name)),
            ("nameZh", clean_name(name)),
            ("nameJa", ""),
            ("categoryEn", en),
            ("categoryZh", zh),
            ("categoryJa", ja),
            ("aliasesZh", aliases),
            ("description", (meta.get("內容物描述") or "").strip()),
            ("refusePercent", num(meta.get("廢棄率"))),
            ("nutrients", nutrients),
        ]))

    out, merged = collapse_monthly_samples(out)
    ids = [o["fdcId"] for o in out]
    assert len(ids) == len(set(ids)), "duplicate ids"
    with open(dst, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
    print(f"samples={len(samples)} written={len(out)} skipped={skipped} merged_monthly={merged} -> {dst}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "data/sources/tw_foods.json")
