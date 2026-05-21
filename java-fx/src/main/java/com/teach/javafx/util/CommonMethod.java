package com.teach.javafx.util;

import com.teach.javafx.request.OptionItem;

import java.util.*;

/**
 * CommonMethod 公共处理方法实例类（仓储系统适配版）
 */
public class CommonMethod {
    public static String[] getStrings(Map<String,Object> data,String key){
        Object obj = data.get(key);
        if(obj == null)
            return new String[]{};
        if(obj instanceof String[])
            return (String[])obj;
        return new String[]{};
    }

    public static String getString(Map<String,Object> data,String key){
        if(data == null)
            return "";
        Object obj = data.get(key);
        if(obj == null)
            return "";
        if(obj instanceof String)
            return (String)obj;
        return obj.toString();
    }

    public static Boolean getBoolean(Map<String,Object> data,String key){
        if(data == null)
            return null;
        Object obj = data.get(key);
        if(obj == null)
            return false;
        if(obj instanceof Boolean)
            return (Boolean)obj;
        return "true".equals(obj.toString());
    }

    public static List<?> getList(Map<String,Object> data, String key){
        if(data == null)
            return new ArrayList<>();
        Object obj = data.get(key);
        if(obj == null)
            return new ArrayList<>();
        if(obj instanceof List)
            return (List<?>)obj;
        else
            return new ArrayList<>();
    }

    public static Map<String,Object> getMap(Map<String,Object> data,String key){
        if(data == null)
            return new HashMap<>();
        Object obj = data.get(key);
        if(obj == null)
            return new HashMap<>();
        if(obj instanceof Map)
            return (Map<String,Object>)obj;
        else
            return new HashMap<>();
    }

    public static Integer getIntegerFromObject(Object obj) {
        if(obj == null)
            return null;
        if(obj instanceof Integer)
            return (Integer)obj;
        String str = obj.toString();
        try {
            return (int)Double.parseDouble(str);
        }catch(Exception e) {
            return null;
        }
    }

    public static Integer getInteger(Map<String,Object> data,String key) {
        if(data == null)
            return null;
        Object obj = data.get(key);
        if(obj == null)
            return null;
        if(obj instanceof Integer)
            return (Integer)obj;
        String str = obj.toString();
        try {
            return (int)Double.parseDouble(str);
        }catch(Exception e) {
            return null;
        }
    }

    public static Integer getInteger0(Map<String,Object> data,String key) {
        if(data == null)
            return 0;
        Object obj = data.get(key);
        if(obj == null)
            return 0;
        if(obj instanceof Integer)
            return (Integer)obj;
        String str = obj.toString();
        try {
            return (int)Double.parseDouble(str);
        }catch(Exception e) {
            return 0;
        }
    }

    public static Double getDouble(Map<String,Object> data,String key) {
        if(data == null)
            return null;
        Object obj = data.get(key);
        if(obj == null)
            return null;
        if(obj instanceof Double)
            return (Double)obj;
        String str = obj.toString();
        try {
            return Double.parseDouble(str);
        }catch(Exception e) {
            return null;
        }
    }

    public static Double getDouble0(Map<String,Object> data,String key) {
        Double d0 = 0d;
        if(data == null)
            return d0;
        Object obj = data.get(key);
        if(obj == null)
            return d0;
        if(obj instanceof Double)
            return (Double)obj;
        String str = obj.toString();
        try {
            return Double.parseDouble(str);
        }catch(Exception e) {
            return d0;
        }
    }

    public static Date getDate(Map<String,Object> data, String key) {
        if(data == null)
            return null;
        Object obj = data.get(key);
        if(obj == null)
            return null;
        if(obj instanceof Date)
            return (Date)obj;
        String str = obj.toString();
        return DateTimeTool.formatDateTime(str,"yyyy-MM-dd");
    }

    public static Date getTime(Map<String,Object> data,String key) {
        if(data == null)
            return null;
        Object obj = data.get(key);
        if(obj == null)
            return null;
        if(obj instanceof Date)
            return (Date)obj;
        String str = obj.toString();
        return DateTimeTool.formatDateTime(str,"yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将 Map 列表转为 OptionItem 列表（仓储系统适配：使用 id 和 name）
     */
    public static List<OptionItem> changeMapListToOptionItemList(List<Map<String,Object>> mapList) {
        List<OptionItem> itemList = new ArrayList<>();
        if(mapList == null) return itemList;
        for(Map<String,Object> m:mapList){
            Integer id = getInteger(m, "id");
            String name = getString(m, "name");
            itemList.add(new OptionItem(id, name));
        }
        return itemList;
    }

    public static int getOptionItemIndexById(List<OptionItem>itemList, Integer id){
        if(id == null || itemList == null)
            return -1;
        for(int i = 0; i < itemList.size();i++) {
            if(itemList.get(i).getId() != null && itemList.get(i).getId().equals(id))
                return i;
        }
        return -1;
    }

    /**
     * 根据 name 查找 OptionItem 索引
     */
    public static int getOptionItemIndexByName(List<OptionItem>itemList, String name){
        if(name == null || name.isEmpty() || itemList == null)
            return -1;
        for(int i = 0; i < itemList.size();i++) {
            if(itemList.get(i).getName() != null && itemList.get(i).getName().equals(name))
                return i;
        }
        return -1;
    }

    /**
     * 根据 id 获取 OptionItem 的 name
     */
    public static String getOptionItemNameById(List<OptionItem>itemList, Integer id){
        if(id == null || itemList == null)
            return "";
        for (OptionItem optionItem : itemList) {
            if (optionItem.getId() != null && optionItem.getId().equals(id))
                return optionItem.getName();
        }
        return "";
    }

    public static String addThingies(String s) {
        return "'" + mysql_real_escape_string(s) + "'";
    }

    //SQLi protection
    public static String mysql_real_escape_string(String str) {
        if (str == null) {
            return null;
        }

        if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").length() < 1) {
            return str;
        }

        String clean_string = str;
        clean_string = clean_string.replaceAll("\\\\", "\\\\\\\\");
        clean_string = clean_string.replaceAll("\\n", "\\\\n");
        clean_string = clean_string.replaceAll("\\r", "\\\\r");
        clean_string = clean_string.replaceAll("\\t", "\\\\t");
        clean_string = clean_string.replaceAll("\\00", "\\\\0");
        clean_string = clean_string.replaceAll("'", "\\\\'");
        clean_string = clean_string.replaceAll("\\\"", "\\\\\"");

        if (clean_string.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]", "").length() < 1) {
            return clean_string;
        }
        return str;
    }

    /**
     * 将 Map 列表转为 OptionItem 列表（使用 OptionItem(Map) 构造器）
     */
    public static List<OptionItem> changeMapToOptionItemList(List<Map<String,Object>> mList) {
        List<OptionItem> iList = new ArrayList<>();
        if(mList == null)
            return iList;
        for( Map<String,Object> m : mList) {
            iList.add(new OptionItem(m));
        }
        return iList;
    }
}
