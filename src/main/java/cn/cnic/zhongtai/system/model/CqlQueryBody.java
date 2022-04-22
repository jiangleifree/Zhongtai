package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CqlQueryBody {

    private int skip;
    private int limit;
    private String label;
    private RelationShip relationShip;

    public static List<CqlQueryBody> generate(int total, int limit, String label){

        List<CqlQueryBody> list = new ArrayList<>();
        int c = (total % limit) == 0 ? total / limit : total / limit + 1;
        for (int i = 0; i < c; i++) {
            CqlQueryBody cqlQueryBody = new CqlQueryBody();
            cqlQueryBody.setLabel(label);
            cqlQueryBody.setLimit(limit);
            cqlQueryBody.setSkip(i * limit);
            list.add(cqlQueryBody);
        }
        return list;
    }

    public static List<CqlQueryBody> generate(int total, int limit, RelationShip relationShip){
        List<CqlQueryBody> list = new ArrayList<>();
        int c = (total % limit) == 0 ? total / limit : total / limit + 1;
        for (int i = 0; i < c; i++) {
            CqlQueryBody cqlQueryBody = new CqlQueryBody();
            cqlQueryBody.setLimit(limit);
            cqlQueryBody.setSkip(i * limit);
            cqlQueryBody.setRelationShip(relationShip);
            list.add(cqlQueryBody);
        }
        return list;
    }
}
