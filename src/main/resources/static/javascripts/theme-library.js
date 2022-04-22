$(document).ready(function () {
    getTables();
});
var tables = []

/**
 * 获取所有hive表
 */
function getTables() {
    var url = "/genTable/model_list";
    requestGetAjax(url, function (resData) {
        console.info(resData);
        if ("0" == resData.code) {
            console.info(resData.data);
            if (resData.data.length > 0)
                setTables(resData.data);
        } else {
            layer.close(index);
            layer.alert(resData.msg, {icon: 2});
        }
    });
}

/**
 * 设置表格
 * @param data
 */
function setTables(data) {
    var content = "";

    for (let i = 0; i < data.length; i++) {
        let tableName = data[i].tableName;
        let tableComment = data[i].tableComment;
        let labl = tableName + "(" + tableComment + ")";
        let tab = {
            "tableName": tableName,
            "tableComment": tableComment
        }
        tables.push(tab)
        content += "<div class='layui-colla-item'>";
        content += "<h2 class='layui-colla-title'>" + labl +"</h2>"
        content += "<div class='layui-colla-content'>"
        content += "<table class='layui-hide' id='" + tableName + "' lay-filter='" + tableName + "' ></table>"
        content += "</div>"
        content += "</div>"
    }

    $("#tableList").append(content)

    layui.use(['table', 'layer', 'element'], function () {
        let table = layui.table;
        let layer = layui.layer;
        let element = layui.element;
        for (var i = 0; i < data.length; i++) {
            var tableName = data[i].tableName;
            var tableId = data[i].tableId;
            table.render({
                elem: '#' + tableName,
                url: '/genTableColumn/modelColumnByTableId/' + tableId
                , defaultToolbar: ['filter', 'exports', 'print', { //自定义头部工具栏右侧图标。如无需自定义，去除该参数即可
                    title: '提示'
                    , layEvent: 'LAYTABLE_TIPS'
                    , icon: 'layui-icon-tips'
                }]
                , title: tableName
                , cols: [[
                    {type: 'checkbox'}
                    , {field: 'columnName', width: 80, title: '列名'}
                    , {field: 'columnType', width: 80, title: '类型'}
                    , {field: 'columnLength', width: 80, title: '长度'}
                    , {field: 'columnComment', width: 80, title: '描述'}
                ]]
                , page: false
            });
        }

    });

    layui.element.init();//初始化画板
}

//获取所有页面的checkbox的值
function getChecked() {
    var topicName = $("#topicName").val()
    var topicComment = $("#topicComment").val()
    var topicData = {
        "topicName": topicName,
        "comment": topicComment,
        "tables": []
    }
    var table = layui.table;
    for (var i = 0; i < tables.length; i++) {

        var cols = table.checkStatus(tables[i].tableName).data;
        var tab = {
            "tableName": tables[i].tableName,
            "tableComment": tables[i].tableComment,
            "genTableColumn": cols
        }
        if (tab.genTableColumn.length != 0) {
            topicData.tables.push(tab);
        }
    }
    var jsonTopicData = JSON.stringify(topicData);
    $.ajax({
        type: "POST",
        async: false,
        url: "/topic/createTopic",
        data: jsonTopicData,
        dataType: 'json',
        contentType: 'application/json',
        success: function (data) {
            if (data.code == 200) {
                alert("创建成功")
            } else {
                alert(data.errMsg)
            }
        },
        error: function (request) {//请求失败之后的操作

        }
    });

};





