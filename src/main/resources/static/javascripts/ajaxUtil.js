


/**
 * ajax工具js
 * @param pageUrl
 * @param jsonData
 * @param backFunc
 */
function requestPostAjax(url, status, jsonData, backFunc, errBackFunc) {
    $.ajax({
        type: "POST",
        async: status,
        url:   url,
        data: jsonData,
        dataType: 'json',
        success: function (data) {
            if (data.code === 403) {
                window.location.href = '/login';
                return;
            }
            if (backFunc && $.isFunction(backFunc)) {
                backFunc(data);
            }
        },
        error: function (request) {//请求失败之后的操作
            if (errBackFunc && $.isFunction(errBackFunc)) {
                errBackFunc();
            }
            return;
        }
    });
};
function requestPostAjaxByJson(url, status, jsonData,contentType, backFunc, errBackFunc) {
    $.ajax({
        type: "POST",
        async: status,
        url:   url,
        data: jsonData,
        dataType: 'json',
        contentType:contentType,
        success: function (data) {
            if (data.code === 403) {
                window.location.href = '/login';
                return;
            }
            if (backFunc && $.isFunction(backFunc)) {
                backFunc(data);
            }
        },
        error: function (request) {//请求失败之后的操作
            if (errBackFunc && $.isFunction(errBackFunc)) {
                errBackFunc();
            }
            return;
        }
    });
};


function requestGetAjax(url, backFunc, errBackFunc) {
    $.ajax({
        type: "get",
        url:    url,
        success: function (data) {
            data = JSON.parse(data);
            if (data.code === 403) {
                window.location.href = '/login';
                return;
            }
            if (backFunc && $.isFunction(backFunc)) {
                backFunc(data);
            }
        }, error: function (request) {//请求失败之后的操作
            if (errBackFunc && $.isFunction(errBackFunc)) {
                errBackFunc();
            }
            return;
        }
    });
};


/**
 * 弹窗通用
 * @param msg
 */
function openPopup(msg) {

    layer.open({
        title: '信息提示',
        content: '<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + msg + '',
        area: ['600px'],
        shade: 0.5
    });
}


//解析url可获取指定参数值
function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURIComponent(r[2]);
    return null;
}