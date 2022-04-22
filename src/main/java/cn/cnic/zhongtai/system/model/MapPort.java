package cn.cnic.zhongtai.system.model;

import lombok.Data;

@Data
public class MapPort {
    private int id;
    private int boltPort;
    private int httpPort;
    private int httpsPort;
    private String mapName;

}
