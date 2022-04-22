package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 图库
 */
@Data
public class MapStorage {

    private int mapId;
    //图库名称
    private String mapName;
    //图库对应的英文
    private String mapNameEn;
    //创建时间
    private Date createTime;
    //描述说明
    private String mapComment;
    //图库启动后对用的地址
    private String mapUrl;
    //图库状态 已启动 doing | 已关闭 closed | 未初始化 not init
    private String status;
    private List<MapStorageTable> mapTables;
    //对应的attarch_id
    private String attarchId;
    //图库对应的端口号
    private String httpPort;
    //bolt端口
    private String boltPort;
    public static final String NOT_INIT = "not init";
    public static final String CLOSED = "closed";
    public static final String DOING = "doing";
}
