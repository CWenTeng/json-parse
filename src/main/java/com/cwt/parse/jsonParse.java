package com.cwt.parse;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;

public class jsonParse {

    final static String PATH = "path";
    final static String KEYS = "keys";
    final static String SUBS = "subs";
    final static String SELF_KEY = "@Self@Key";

    /**
     * 根据key 获取对象 中 value
     *
     * @param o
     * @param key
     * @return
     */
    public Object getValueByKey(Object o, String key) {
        if (Objects.isNull(o)) {
            return null;
        }
        if (o instanceof Map) {
            Map<String, Object> oMap = (Map<String, Object>) o;
            if (oMap.containsKey(key)) {
                return oMap.get(key);
            }
        } else if (o instanceof List) {
            try {
                Integer num = Integer.valueOf(key);
                List oList = (List) o;
                if (oList.size() > num) {
                    return oList.get(num);
                }
            } catch (Exception e) {
                System.out.println("key is not number");
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * 解析
     *
     * @param responseData
     * @param parseRule
     * @param pathNum
     * @param result
     * @param resultList
     * @param selfKey
     */
    public void parse(Object responseData, Map parseRule, Integer pathNum, Map result, List resultList, Object selfKey) {
        List<Object> path = (List<Object>) parseRule.get(PATH);
        // 如当前迭代对象为list类型
        if (path.size() > pathNum && responseData instanceof List) {
            List responseList = (List) responseData;
            // 判断迭代全部全部元素 or 指定对象
            if ("*".equals(path.get(pathNum))) {
                for (Object response : responseList) {
                    Map tmp = (Map) SerializationUtils.clone((Serializable) result);
                    parse(response, parseRule, pathNum + 1, tmp, resultList, selfKey);
                }
            } else if (path.get(pathNum) instanceof Integer) {
                Integer num = (Integer) path.get(pathNum);
                if (responseList.size() > num) {
                    Map tmp = (Map) SerializationUtils.clone((Serializable) result);
                    parse(responseList.get(num), parseRule, pathNum + 1, tmp, resultList, selfKey);
                }
            }
            return;
        } else if (path.size() > pathNum && responseData instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) responseData;
            // 判断迭代全部全部元素 or 指定对象
            if ("*".equals(path.get(pathNum))) {
                for (Map.Entry<String, Object> entry : responseMap.entrySet()) {
                    Map tmp = (Map) SerializationUtils.clone((Serializable) result);
                    parse(entry.getValue(), parseRule, pathNum + 1, tmp, resultList, entry.getKey());
                }
                return;
            }
        }

        // 递归到底组装key
        if (pathNum == path.size()) {
            Map<String, String> keys = (Map<String, String>) parseRule.get(KEYS);
            for (Map.Entry<String, String> keyEntry : keys.entrySet()) {
                Object value = responseData;
                // 提取 self key
                if (SELF_KEY.equals(keyEntry.getValue())) {
                    result.put(keyEntry.getKey(), selfKey);
                    continue;
                }
                // 拆分 key 提取 value
                for (String s : keyEntry.getValue().split("\\.")) {
                    value = getValueByKey(value, s);
                }
                result.put(keyEntry.getKey(), value);
            }

            List<Map> subs = (List) parseRule.get(SUBS);
            Map tmp = (Map) SerializationUtils.clone((Serializable) result);
            // 迭代到底
            if (Objects.isNull(subs) || subs.isEmpty()) {
                // 组装输出行
                resultList.add(tmp);
            } else {
                // 迭代子集
                for (Map sub : subs) {
                    parse(responseData, sub, 0, tmp, resultList, selfKey);
                }
            }
            return;
        }

        parse(getValueByKey(responseData, path.get(pathNum).toString()), parseRule, pathNum + 1, result, resultList, path.get(pathNum));
    }


    /**
     * 抽取 keys
     * @param parseRule
     * @param keysMap
     * @return
     */
    public Map extractionKeys(Map parseRule, Map keysMap) {
        if (Objects.isNull(parseRule)) {
            return keysMap;
        }
        keysMap.putAll((Map) getValueByKey(parseRule, KEYS));
        List subs = (List) getValueByKey(parseRule, SUBS);
        if (!Objects.isNull(subs) && !subs.isEmpty()) {
            for (Object sub : subs) {
                extractionKeys((Map) sub, keysMap);
            }
        }
        return keysMap;
    }
}
