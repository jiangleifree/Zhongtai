
$(document).ready(function() {
    $("#modelTypeId").select2({
        placeholder: '<span style="font-weight:100">点击此处进行选择</span>',
        multiple: false,
        allowClear: true,
        id: function (rs) {
            return rs.tableId;
        },
        ajax: {
            url: "/dataImport/modelListByTableName",
            dataType: 'json',
            type: 'POST',
            quietMillis: 250,
            data: function (term, page) {
                return {tableName: term};
            },
            results: function (data, page) {
                return {results: data};
            },
            cache: false
        },
        initSelection: function (element, callback) {
            var tableName = $("#modelTypeId").val();
            callback({name: tableName});
        },
        formatResult: modelFormatResult,
        formatSelection: modelFormatSelection,
        escapeMarkup: function (m) {
            return m;
        },
        minimumInputLength: 0
    });
    $("#SQLmodelTypeId").select2({
        placeholder: '<span style="font-weight:100">点击此处进行选择</span>',
        multiple: false,
        allowClear: true,
        id: function (rs) {
            return rs.tableId;
        },
        ajax: {
            url: "/dataImport/modelListByTableName",
            dataType: 'json',
            type: 'POST',
            quietMillis: 250,
            data: function (term, page) {
                return {tableName: term};
            },
            results: function (data, page) {
                return {results: data};
            },
            cache: false
        },
        initSelection: function (element, callback) {
            var tableName = $("#SQLmodelTypeId").val();
            callback({name: tableName});
        },
        formatResult: modelFormatResult,
        formatSelection: modelFormatSelection1,
        escapeMarkup: function (m) {
            return m;
        },
        minimumInputLength: 0
    });
});



function modelFormatResult(repo) {
    var markup = '<div class="row-fluid">' +
        '<div class="span6">' + repo.tableName + '</div>';
    markup += '</div>';
    return markup;
}

function modelFormatSelection(repo) {
    $('#modelTypeId').attr("na", repo.tableName);
    return repo.tableName;
}

function modelFormatSelection1(repo) {
    $('#SQLmodelTypeId').attr("na", repo.tableName);
    return repo.tableName;
}


//上传
$(window).load(function () {
    //提交按钮样式
    var chtml = "<button onclick='dataImportFile();' type='button' tabindex='500' title='提交' class='btn btn-default'><i class='glyphicon glyphicon-upload text-info'></i> <span class='hidden-xs'>&nbsp;提交</span></button>"
    $(".input-group-btn").append(chtml);
});


function dataImportFile() {
    var modelName = $('#modelTypeId').attr("na");
    if(!modelName || "undefined" == modelName || "" == modelName){
        layer.alert("请您选择一个要接入的目标数据表", {icon: 2});
        return false;
    }

    let parentId = $('#parentId').val();
    if(parentId == ''){
        layer.alert('请先选择虚拟目录!', {icon: 2});
        return false;
    }

    if (FileTypeCheck($("#fileType").val())) {
        //加载层
        var index = layer.load(1, {shade: [0.5,'#000']}); //0代表加载的风格，支持0-2,shade透明度的背景
        $("#csvOrJsonForm").ajaxSubmit({
            type: 'post',
            url: "/dataImport/importCsvOrJsonFile",
            contentType: "application/x-www-form-urlencoded; charset=utf-8",
            success: function (data) {
                data = JSON.parse(data);
                if (data.code === 200) {
                    layer.close(index);
                    layer.alert(data.msg, {
                            icon: 6
                        },
                        function() {
                            // 获得frame索引
                            var index = parent.layer.getFrameIndex(window.name);
                            //关闭当前frame
                            parent.layer.close(index);
                            // 可以对父窗口进行刷新
                            xadmin.father_reload();
                        });

                } else {
                    layer.close(index);
                    layer.alert(data.msg, {icon: 2});
                }

            }, error: function (XmlHttpRequest, textStatus, errorThrown) {
                layer.close(index);
                layer.alert("服务器内部错误,请稍后再试！", {icon: 2});
            }
        });
    }
    //防止表单非ajax方式提交
    return false;
}


function FileTypeCheck(type) {
    var obj = document.getElementById('i-file');
    fileSize = obj.size;
    var size = fileSize / 1024;
    if (size > 10000) {
        layer.alert("附件不能大于10M", {icon: 2});
        return false;   //阻止submit提交
    }
    if (obj.value == null || obj.value == '') {
        layer.alert("请选择csv文件 或者json文件后再提交", {icon: 2});
        this.focus()
        return false;
    }
    var length = obj.value.length;
    var charindex = obj.value.lastIndexOf(".");
    var ExtentName = obj.value.substring(charindex, charindex + 4);
    if("json" == type){
        ExtentName = ExtentName+"n";
    }
    if (ExtentName != "."+type ) {
        layer.alert("文件格式错误,请确认文件格式为csv文件 或者json文件", {icon: 2});
        this.focus()
        return false;
    }
    return true;
}

/**
 * 添加在线收割
 */
function dataAddToFtp() {

    let parentId = $('#parentId').val();
    if(parentId == ''){
        layer.alert('请先选择虚拟目录!', {icon: 2});
        return false;
    }
    var index = layer.load(1, {shade: [0.5,'#000']}); //0代表加载的风格，支持0-2,shade透明度的背景
    var data = $("#dataFtpFrom").serializeArray();
    var url = "/dataImport/data_add_ftp";
    requestPostAjax(url, true, data, function (resData) {
        if (resData.code === 200) {
            layer.close(index);
            layer.alert(resData.msg, {
                    icon: 6
                },
                function() {
                    // 获得frame索引
                    var index = parent.layer.getFrameIndex(window.name);
                    //关闭当前frame
                    parent.layer.close(index);
                    // 可以对父窗口进行刷新
                    xadmin.father_reload();
                });

        } else {
            layer.close(index);
            layer.alert(resData.msg, {icon: 2});
        }
    });
}




function dataAddToSQL() {
    var index = layer.load(1, {shade: [0.5,'#000']}); //0代表加载的风格，支持0-2,shade透明度的背景
    var data = $("#dataSqlForm").serializeArray();
    var url = "/dataImport/data_add_sql";
    requestPostAjax(url, true, data, function (resData) {
        if (resData.code === 200) {
            layer.close(index);
            layer.alert(resData.msg, {
                    icon: 6
                },
                function() {
                    // 获得frame索引
                    var index = parent.layer.getFrameIndex(window.name);
                    //关闭当前frame
                    parent.layer.close(index);
                    // 可以对父窗口进行刷新
                    xadmin.father_reload();
                });

        } else {
            layer.close(index);
            layer.alert(resData.msg, {icon: 2});
        }
    });
}