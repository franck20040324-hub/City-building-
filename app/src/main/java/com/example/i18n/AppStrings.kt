package com.example.i18n

import com.example.model.AppLanguage
import com.example.model.BuildCategory
import com.example.model.BuildingType
import com.example.model.CraftRecipe
import com.example.model.Season

object AppStrings {
    fun isAr(lang: AppLanguage) = lang == AppLanguage.ARABIC

    fun levelLabel(lang: AppLanguage, lvl: Int) =
        if (isAr(lang)) "مستوى $lvl" else "Lvl $lvl"

    fun popLabel(lang: AppLanguage) = if (isAr(lang)) "السكان" else "Pop"
    fun taxRateLabel(lang: AppLanguage, perMin: Int) =
        if (isAr(lang)) "+$perMin/د" else "+$perMin/m"

    fun storageLabel(lang: AppLanguage) = if (isAr(lang)) "المخزن" else "Storage"
    fun happinessLabel(lang: AppLanguage) = if (isAr(lang)) "السعادة" else "Happy"

    fun seasonName(lang: AppLanguage, s: Season): String = when (s) {
        Season.SPRING -> if (isAr(lang)) "الربيع" else "Spring"
        Season.SUMMER -> if (isAr(lang)) "الصيف" else "Summer"
        Season.AUTUMN -> if (isAr(lang)) "الخريف" else "Autumn"
        Season.WINTER -> if (isAr(lang)) "الشتاء" else "Winter"
    }

    fun categoryName(lang: AppLanguage, c: BuildCategory): String = when (c) {
        BuildCategory.ROADS_TRANSIT -> if (isAr(lang)) "الطرق والنقل" else "Roads & Transit"
        BuildCategory.RESIDENTIAL -> if (isAr(lang)) "المساكن" else "Homes"
        BuildCategory.CIVIC_SAFETY -> if (isAr(lang)) "الخدمات" else "Civic & Fun"
        BuildCategory.COMMERCIAL_ADMIN -> if (isAr(lang)) "التجارة والإدارة" else "Commerce"
        BuildCategory.FACTORIES_FARMS -> if (isAr(lang)) "المصانع والزراعة" else "Factories & Farm"
        BuildCategory.LANDMARKS_DECOR -> if (isAr(lang)) "المعالم والتجميل" else "Landmarks & Art"
        BuildCategory.TERRAIN_TOOLS -> if (isAr(lang)) "التضاريس" else "Terrain"
    }

    fun buildingName(lang: AppLanguage, b: BuildingType): String = if (isAr(lang)) when (b) {
        BuildingType.ROAD_TWO_WAY -> "طريق اتجاهين (١+١ حارة)"
        BuildingType.ROAD_FOUR_LANE -> "طريق مزدوج (٢+٢ حارة)"
        BuildingType.ROAD_HIGHWAY -> "طريق سريع ٣ حارات + طوارئ"
        BuildingType.FUEL_STATION -> "محطة وقود"
        BuildingType.HIGHWAY_TOLL -> "محطة تحصيل ضرائب"
        BuildingType.BUS_STATION -> "محطة حافلات"
        BuildingType.TAXI_STATION -> "موقف سيارات أجرة"
        BuildingType.TRAM_STATION -> "محطة ترام"
        BuildingType.TRAIN_STATION -> "محطة قطار"
        BuildingType.METRO_STATION -> "محطة مترو أنفاق"
        BuildingType.SEAPORT -> "ميناء سفن"
        BuildingType.AIRPORT -> "مطار دولي"
        BuildingType.GROUND_HOUSE -> "بيت أرضي"
        BuildingType.SMALL_APARTMENT -> "عمارة سكنية صغيرة"
        BuildingType.LARGE_APARTMENT -> "عمارة سكنية كبيرة"
        BuildingType.LUXURY_APARTMENT -> "عمارة فاخرة كبيرة"
        BuildingType.SKYSCRAPER -> "ناطحة سحاب"
        BuildingType.SHOP -> "متجر"
        BuildingType.RESTAURANT -> "مطعم"
        BuildingType.EMPLOYMENT_OFFICE -> "مكتب توظيف"
        BuildingType.COMPANY -> "شركة"
        BuildingType.LUXURY_ADMIN_BUILDING -> "مبنى إداري فاخر"
        BuildingType.SCHOOL -> "مدرسة"
        BuildingType.WATER_PLANT -> "محطة بنية تحتية للمياه"
        BuildingType.FIRE_STATION -> "مركز إطفاء"
        BuildingType.POLICE_STATION -> "مركز شرطة"
        BuildingType.HOSPITAL -> "مستشفى"
        BuildingType.PRISON -> "سجن"
        BuildingType.AMUSEMENT_PARK -> "مدينة ملاهي برية"
        BuildingType.WATER_AMUSEMENT_PARK -> "مدينة ملاهي مائية"
        BuildingType.CROP_FIELD -> "حقل محاصيل زراعية"
        BuildingType.LIVESTOCK_PASTURE -> "مرعى أغنام وأبقار"
        BuildingType.FOREST_GROVE -> "غابة أشجار"
        BuildingType.FOOD_FACTORY -> "مصنع أغذية"
        BuildingType.CLOTHING_FACTORY -> "مصنع ملابس"
        BuildingType.EQUIPMENT_FACTORY -> "مصنع معدات"
        BuildingType.MEDICINE_FACTORY -> "مصنع أدوية"
        BuildingType.RECYCLING_FACTORY -> "مصنع تكرير وتدوير"
        BuildingType.TRANSPORT_FACTORY -> "مصنع مركبات ونقل"
        BuildingType.LANDMARK_EIFFEL -> "برج إيفل"
        BuildingType.LANDMARK_BURJ_KHALIFA -> "برج خليفة"
        BuildingType.LANDMARK_STATUE_LIBERTY -> "تمثال الحرية"
        BuildingType.LANDMARK_BIG_BEN -> "ساعة بيغ بن"
        BuildingType.LANDMARK_COLOSSEUM -> "الكولوسيوم"
        BuildingType.LANDMARK_PYRAMID -> "هرم الجيزة الأكبر"
        BuildingType.COSMETIC_TREE_GARDEN -> "حديقة أشجار موسمية"
        BuildingType.COSMETIC_PARK_BENCH -> "مقاعد وفوانيس منتزه"
        BuildingType.COSMETIC_FOUNTAIN -> "نافورة ساحة"
        BuildingType.COSMETIC_ARCH -> "قوس النصر التجميلي"
    } else when (b) {
        BuildingType.ROAD_TWO_WAY -> "2-Way Road (1+1 Lane)"
        BuildingType.ROAD_FOUR_LANE -> "Double-Way Road (2+2)"
        BuildingType.ROAD_HIGHWAY -> "3-Lane Highway + Shoulder"
        BuildingType.FUEL_STATION -> "Fuel Station"
        BuildingType.HIGHWAY_TOLL -> "Highway Tax Toll"
        BuildingType.BUS_STATION -> "Bus Station"
        BuildingType.TAXI_STATION -> "Taxi Station"
        BuildingType.TRAM_STATION -> "Tram Station"
        BuildingType.TRAIN_STATION -> "Train Station"
        BuildingType.METRO_STATION -> "Underground Metro"
        BuildingType.SEAPORT -> "Animated Seaport"
        BuildingType.AIRPORT -> "City Airport"
        BuildingType.GROUND_HOUSE -> "Ground House"
        BuildingType.SMALL_APARTMENT -> "Small Apartment"
        BuildingType.LARGE_APARTMENT -> "Large Apartment"
        BuildingType.LUXURY_APARTMENT -> "Luxury Apartment"
        BuildingType.SKYSCRAPER -> "Skyscraper"
        BuildingType.SHOP -> "Shop"
        BuildingType.RESTAURANT -> "Restaurant"
        BuildingType.EMPLOYMENT_OFFICE -> "Employment Office"
        BuildingType.COMPANY -> "Corporate Company"
        BuildingType.LUXURY_ADMIN_BUILDING -> "Luxury Admin Palace"
        BuildingType.SCHOOL -> "School"
        BuildingType.WATER_PLANT -> "Water Works Plant"
        BuildingType.FIRE_STATION -> "Fire Station"
        BuildingType.POLICE_STATION -> "Police Station"
        BuildingType.HOSPITAL -> "Hospital"
        BuildingType.PRISON -> "Correctional Prison"
        BuildingType.AMUSEMENT_PARK -> "Land Amusement Park"
        BuildingType.WATER_AMUSEMENT_PARK -> "Water Amusement Park"
        BuildingType.CROP_FIELD -> "Crop Field"
        BuildingType.LIVESTOCK_PASTURE -> "Sheep & Cow Pasture"
        BuildingType.FOREST_GROVE -> "Forest Grove"
        BuildingType.FOOD_FACTORY -> "Food Factory"
        BuildingType.CLOTHING_FACTORY -> "Clothing Factory"
        BuildingType.EQUIPMENT_FACTORY -> "Equipment Factory"
        BuildingType.MEDICINE_FACTORY -> "Medicine Factory"
        BuildingType.RECYCLING_FACTORY -> "Refining & Recycling"
        BuildingType.TRANSPORT_FACTORY -> "Transport Factory"
        BuildingType.LANDMARK_EIFFEL -> "Eiffel Tower"
        BuildingType.LANDMARK_BURJ_KHALIFA -> "Burj Khalifa"
        BuildingType.LANDMARK_STATUE_LIBERTY -> "Statue of Liberty"
        BuildingType.LANDMARK_BIG_BEN -> "Big Ben"
        BuildingType.LANDMARK_COLOSSEUM -> "Colosseum"
        BuildingType.LANDMARK_PYRAMID -> "Great Pyramid"
        BuildingType.COSMETIC_TREE_GARDEN -> "Seasonal Trees"
        BuildingType.COSMETIC_PARK_BENCH -> "Plaza Benches"
        BuildingType.COSMETIC_FOUNTAIN -> "City Fountain"
        BuildingType.COSMETIC_ARCH -> "Monument Arch"
    }

    fun recipeName(lang: AppLanguage, r: CraftRecipe): String = if (isAr(lang)) when (r) {
        CraftRecipe.FRESH_BREAD -> "خبز طازج"
        CraftRecipe.ORGANIC_MEAL -> "وجبات عضوية"
        CraftRecipe.COTTON_APPAREL -> "ملابس قطنية"
        CraftRecipe.LUXURY_SUIT -> "بدلة فاخرة"
        CraftRecipe.STEEL_TOOLS -> "أدوات معدنية"
        CraftRecipe.HEAVY_GEAR -> "معدات ثقيلة"
        CraftRecipe.FIRST_AID_KIT -> "حقيبة إسعاف"
        CraftRecipe.BIOTECH_VACCINE -> "لقاح طبي"
        CraftRecipe.RECYCLED_ALLOY -> "سبائك معاد تدويرها"
        CraftRecipe.CLEAN_BIOFUEL -> "وقود حيوي نظيف"
        CraftRecipe.CITY_EV_PARTS -> "قطع مركبات"
        CraftRecipe.LUXURY_COACH -> "حافلة نقل فاخرة"
    } else when (r) {
        CraftRecipe.FRESH_BREAD -> "Fresh Bread"
        CraftRecipe.ORGANIC_MEAL -> "Organic Meal Box"
        CraftRecipe.COTTON_APPAREL -> "Cotton Apparel"
        CraftRecipe.LUXURY_SUIT -> "Designer Coat"
        CraftRecipe.STEEL_TOOLS -> "Steel Tools"
        CraftRecipe.HEAVY_GEAR -> "Heavy Machinery"
        CraftRecipe.FIRST_AID_KIT -> "First Aid Kit"
        CraftRecipe.BIOTECH_VACCINE -> "Biotech Vaccine"
        CraftRecipe.RECYCLED_ALLOY -> "Recycled Alloy"
        CraftRecipe.CLEAN_BIOFUEL -> "Clean Bio-Fuel"
        CraftRecipe.CITY_EV_PARTS -> "Vehicle Parts"
        CraftRecipe.LUXURY_COACH -> "Luxury Transit Coach"
    }

    fun formatDuration(sec: Int): String {
        if (sec <= 0) return "0s"
        val m = sec / 60
        val s = sec % 60
        return when {
            m > 0 && s > 0 -> "${m}m ${s}s"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }
}
