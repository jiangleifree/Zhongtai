package cn.cnic.zhongtai.system.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class EchartsRelationVo {

    private List<Node> data;
    private List<Link> links;
    private List<Category> categories;


    @Data
    @Builder
    public static class Category{
        private String name;
    }

    @Data
    @Builder
    public static class Label{
        private Normal normal;
    }

    @Data
    @Builder
    public static class Normal{
        private boolean show;
        private String formatter;
    }

    @Data
    @Builder
    public static class Node{
        private String name;
        private String category;
        private Label label;


        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            final Node node = (Node) obj;
            if (this == node) {
                return true;
            } else {
                return (this.name.equals(node.name));
            }
        }
        @Override
        public int hashCode() {
            int hashCode = (name == null ? 0 : name.hashCode());
            return hashCode;
        }
    }

    @Data
    @Builder
    public static class Link{
        private String source;
        private String target;
        private Label label;
        private String value;
    }
}
