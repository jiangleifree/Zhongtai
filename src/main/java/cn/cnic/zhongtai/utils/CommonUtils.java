package cn.cnic.zhongtai.utils;

import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

public class CommonUtils {

    private static Logger logger = LoggerUtil.getLogger();

    /**
     * uuid(32位的)
     *
     * @return
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    public static String getCalenderTimeInMillis(){
        return "" + Calendar.getInstance().getTimeInMillis();
    }

    /**
     * 判断对象是否存在有值字段
     *
     * @param object
     * @return
     */
    public static boolean isObjectFieldEmpty(Object object) {
        boolean flag = false;
        if (object != null) {
            Class<?> entity = object.getClass();
            Field[] fields = entity.getDeclaredFields();//获取该类的所有成员变量（私有的）
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    if (field.get(object) != null && !"".equals(field.get(object))) {
                        flag = true;
                       // break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }


    /**
     * 判断对象属性是否存在空值
     *
     * @param object
     * @return
     */
    public static boolean isObjectFieldNoEmpty(Object object) {
        boolean flag = false;
        if (object != null) {
            Class<?> entity = object.getClass();
            Field[] fields = entity.getDeclaredFields();//获取该类的所有成员变量（私有的）
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    //过滤dataMappingName不做校验
                   if(field.getName().equals("dataMappingName")){
                       continue;
                   }
                    if (field.get(object) == null || "".equals(field.get(object))) {
                        flag = true;
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }


    public static String getCode(int n) {
        String string = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//保存数字0-9 和 大小写字母
        char[] ch = new char[n]; //声明一个字符数组对象ch 保存 验证码
        for (int i = 0; i < n; i++) {
            Random random = new Random();//创建一个新的随机数生成器
            int index = random.nextInt(string.length());//返回[0,string.length)范围的int值    作用：保存下标
            ch[i] = string.charAt(index);//charAt() : 返回指定索引处的 char 值   ==》保存到字符数组对象ch里面
        }
        //将char数组类型转换为String类型保存到result
        //String result = new String(ch);//方法一：直接使用构造方法      String(char[] value) ：分配一个新的 String，使其表示字符数组参数中当前包含的字符序列。
        String result = String.valueOf(ch);//方法二： String方法   valueOf(char c) ：返回 char 参数的字符串表示形式。
        return result;
    }

}
