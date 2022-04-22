package cn.cnic.zhongtai.system.model;

public class UniqueTag {
    private int id;
    private String tagName;

    public int getId() {
        return id;
    }

    public UniqueTag setId(int id) {
        this.id = id;
        return this;
    }

    public String getTagName() {
        return tagName;
    }

    public UniqueTag setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }
}
