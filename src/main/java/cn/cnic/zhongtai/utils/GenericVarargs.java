package cn.cnic.zhongtai.utils;

import java.util.ArrayList;
import java.util.List;

public class GenericVarargs {
    /**
     * 数组转集合
     *
     * @param args
     * @param <T>
     * @return
     */
    public static <T> List<T> makeList(T... args) {
        List<T> result = new ArrayList<>();
        for (T item : args)
            result.add(item);
        return result;
    }
}
