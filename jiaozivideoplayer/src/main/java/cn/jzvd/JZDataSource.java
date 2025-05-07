package cn.jzvd;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 视频数据源管理类
 * 用于管理视频播放器的数据源，包括URL、标题、请求头等信息
 * 支持单个视频源和多个视频源的切换
 */
public class JZDataSource {

    /** 默认URL的键名 */
    public static final String URL_KEY_DEFAULT = "URL_KEY_DEFAULT";

    /** 当前播放的URL索引 */
    public int currentUrlIndex;
    /** 存储视频URL的LinkedHashMap，保持插入顺序 */
    public LinkedHashMap urlsMap = new LinkedHashMap();
    /** 视频标题 */
    public String title = "";
    /** 请求头信息 */
    public HashMap<String, String> headerMap = new HashMap<>();
    /** 是否循环播放 */
    public boolean looping = false;
    /** 自定义对象数组 */
    public Object[] objects;

    /**
     * 使用单个URL构造数据源
     * @param url 视频URL
     */
    public JZDataSource(String url) {
        urlsMap.put(URL_KEY_DEFAULT, url);
        currentUrlIndex = 0;
    }

    /**
     * 使用单个URL和标题构造数据源
     * @param url 视频URL
     * @param title 视频标题
     */
    public JZDataSource(String url, String title) {
        urlsMap.put(URL_KEY_DEFAULT, url);
        this.title = title;
        currentUrlIndex = 0;
    }

    /**
     * 使用单个对象URL构造数据源
     * @param url 视频URL对象
     */
    public JZDataSource(Object url) {
        urlsMap.put(URL_KEY_DEFAULT, url);
        currentUrlIndex = 0;
    }

    /**
     * 使用URL映射构造数据源
     * @param urlsMap 包含多个URL的LinkedHashMap
     */
    public JZDataSource(LinkedHashMap urlsMap) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
        currentUrlIndex = 0;
    }

    /**
     * 使用URL映射和标题构造数据源
     * @param urlsMap 包含多个URL的LinkedHashMap
     * @param title 视频标题
     */
    public JZDataSource(LinkedHashMap urlsMap, String title) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
        this.title = title;
        currentUrlIndex = 0;
    }

    /**
     * 获取当前播放的URL
     * @return 当前URL对象
     */
    public Object getCurrentUrl() {
        return getValueFromLinkedMap(currentUrlIndex);
    }

    /**
     * 获取当前播放URL的键名
     * @return 当前URL的键名
     */
    public Object getCurrentKey() {
        return getKeyFromDataSource(currentUrlIndex);
    }

    /**
     * 根据索引获取URL的键名
     * @param index URL索引
     * @return 对应索引的URL键名
     */
    public String getKeyFromDataSource(int index) {
        int currentIndex = 0;
        for (Object key : urlsMap.keySet()) {
            if (currentIndex == index) {
                return key.toString();
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 根据索引获取URL的值
     * @param index URL索引
     * @return 对应索引的URL值
     */
    public Object getValueFromLinkedMap(int index) {
        int currentIndex = 0;
        for (Object key : urlsMap.keySet()) {
            if (currentIndex == index) {
                return urlsMap.get(key);
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 检查是否包含指定的URL
     * @param object 要检查的URL对象
     * @return 如果包含返回true，否则返回false
     */
    public boolean containsTheUrl(Object object) {
        if (object != null) {
            return urlsMap.containsValue(object);
        }
        return false;
    }

    /**
     * 克隆当前数据源
     * @return 新的JZDataSource实例
     */
    public JZDataSource cloneMe() {
        LinkedHashMap map = new LinkedHashMap();
        map.putAll(urlsMap);
        return new JZDataSource(map, title);
    }
}
