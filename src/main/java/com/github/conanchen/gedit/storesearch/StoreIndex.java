package com.github.conanchen.gedit.storesearch;

public interface StoreIndex {
    String INDEX = "STORES";
    String TYPE = "STORE";

    String FIELD_uuid = "uuid";
    String FIELD_name = "name";
    String FIELD_logo = "logo";
    String FIELD_geoPoint = "geoPoint";
    String FIELD_type = "type";
    String FIELD_desc = "desc";

    String FIELD_pointsRate = "pointsRate";
    String FIELD_amapAdCode = "amapAdCode";
    String FIELD_amapAoiName = "amapAoiName";
    String FIELD_amapBuildingId = "amapBuildingId";
    String FIELD_amapStreet = "amapStreet";
    String FIELD_amapStreetNum = "amapStreetNum";
    String FIELD_amapDistrict = "amapDistrict";
    String FIELD_amapCityCode = "amapCityCode";
    String FIELD_amapCity = "amapCity";
    String FIELD_amapProvince = "amapProvince";
    String FIELD_amapCountry = "amapCountry";


//    string id = 1; //store id
//    string name = 2;
//    string logo = 3;
//    int64 lat = 4;
//    int64 lon = 5;
//    string type = 6;
//    string desc = 7;
//    int32 pointsRate = 8;
// 参照AMapLocation.java, 一下amapXXX字段与location配合做附近搜索使用
//    AdCode:510107, AoiName=心族宾馆, BuildingId=, Street=人民南路四段, StreetNum=34号, District=武侯区, CityCode=028, City=成都市, Province=四川省, Country=中国
//    string amapAdCode = 20;
//    string amapAoiName = 21;
//    string amapBuildingId = 22;
//    string amapStreet = 23;
//    string amapStreetNum = 24;
//    string amapDistrict = 25;
//    string amapCityCode = 26;
//    string amapCity = 27;
//    string amapProvince = 28;
//    string amapCountry = 29;

}
