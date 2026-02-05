# NEW FILE
translations = {
    "en": {
        "report_title": "📊 Sales Analysis Report",
        "total_revenue": "Total revenue generated: ₹{total_revenue:,.2f}.",
        "total_orders": "Total number of orders: {total_orders}.",
        "total_products_sold": "Total products sold: {total_products_sold}.",
        "best_day": "Best performing day: {best_day} (₹{total_sales:.2f}).",
        "best_week": "Best week: {best_week} (₹{revenue:.2f}).",
        "best_month": "Best month: {best_month} (₹{revenue:.2f}).",
        "top_products_title": "Top selling products:",
        "top_product_line": "- {name} ({category}) sold {qty} units, revenue {revenue:.2f}"
    },
    "hi": {
        "report_title": "📊 बिक्री विश्लेषण रिपोर्ट",
        "total_revenue": "कुल राजस्व उत्पन्न: ₹{total_revenue:,.2f}।",
        "total_orders": "ऑर्डर की कुल संख्या: {total_orders}।",
        "total_products_sold": "कुल बिके उत्पाद: {total_products_sold}।",
        "best_day": "सबसे अच्छा प्रदर्शन करने वाला दिन: {best_day} (₹{total_sales:.2f})।",
        "best_week": "सबसे अच्छा सप्ताह: {best_week} (₹{revenue:.2f})।",
        "best_month": "सबसे अच्छा महीना: {best_month} (₹{revenue:.2f})।",
        "top_products_title": "सबसे ज्यादा बिकने वाले उत्पाद:",
        "top_product_line": "- {name} ({category}) ने {qty} इकाइयाँ बेचीं, राजस्व {revenue:.2f}"
    },
    "mr": {
        "report_title": "📊 विक्री विश्लेषण अहवाल",
        "total_revenue": "एकूण महसूल: ₹{total_revenue:,.2f}।",
        "total_orders": "एकूण ऑर्डरची संख्या: {total_orders}।",
        "total_products_sold": "एकूण विकलेली उत्पादने: {total_products_sold}।",
        "best_day": "सर्वोत्तम कामगिरीचा दिवस: {best_day} (₹{total_sales:.2f}).",
        "best_week": "सर्वोत्तम आठवडा: {best_week} (₹{revenue:.2f}).",
        "best_month": "सर्वोत्तम महिना: {best_month} (₹{revenue:.2f}).",
        "top_products_title": " सर्वाधिक विकली जाणारी उत्पादने:",
        "top_product_line": "- {name} ({category}) ने {qty} युनिट्स विकले, महसूल {revenue:.2f}"
    },
    "te": {
        "report_title": "📊 అమ్మకాల విశ్లేషణ నివేదిక",
        "total_revenue": "మొత్తం ఆదాయం: ₹{total_revenue:,.2f}।",
        "total_orders": "మొత్తం ఆర్డర్ల సంఖ్య: {total_orders}।",
        "total_products_sold": "మొత్తం అమ్ముడైన ఉత్పత్తులు: {total_products_sold}।",
        "best_day": "ఉత్తమ పనితీరు కనబరిచిన రోజు: {best_day} (₹{total_sales:.2f}).",
        "best_week": "ఉత్తమ వారం: {best_week} (₹{revenue:.2f}).",
        "best_month": "ఉత్తమ నెల: {best_month} (₹{revenue:.2f}).",
        "top_products_title": "అత్యధికంగా అమ్ముడైన ఉత్పత్తులు:",
        "top_product_line": "- {name} ({category}) {qty} యూనిట్లు అమ్ముడయ్యాయి, ఆదాయం {revenue:.2f}"
    }
}

def get_translation(lang_code, key):
    """
    Fetches a translation string for a given language and key.
    Falls back to English if the language or key is not found.
    """
    # Use 'en' as default
    lang = lang_code if lang_code in translations else "en"

    # Get the dictionary for the language
    lang_dict = translations.get(lang)

    # Get the specific string, falling back to English if the key is missing
    translation_string = lang_dict.get(key)
    if translation_string is None:
        # Fallback to English if key doesn't exist in the target language
        translation_string = translations.get("en", {}).get(key, key)

    return translation_string