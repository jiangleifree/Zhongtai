var flag = false;

$(document).ready(function() {
    if(!flag){
        refreshTables();
    }
});

//虚拟目录
$('#browseInput').on('click', function(){

    layer.open({
        type: 2,
        // area: ['550px', '300px'],
        area: ['750px', '550px'],
        title:'neo4j选择目录',
        fixed: false, //不固定
        maxmin: true,
        // content: 'dataAddBrowse/replaceOrSkip.html'
        content: '/view/dataAddBrowse/browse.html'
    });
});

$('#getInfoByName').on('click', function(){
   var tableName = $("#tableName").val();
   if (tableName && ""!= tableName){
       getDataByTableName( tableName);
   } else {
       layer.alert(" tableName 为空,无法查询！", {icon: 2});
       return false;
   }
});



function refreshTables() {
    var url = "/dataClean/hiveTableRefreshToMysql";
    requestGetAjax(url, function (resData) {
        if (200 == resData.code) {
            flag = true;
        }
    });
}




/**
 * hive 切换表
 */
function getDataByTableName(tableName) {
   layer.msg('数据加载中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: 'auto', time:100000});
    window.location.href = "/dataClean/page?tableName="+tableName;
}

//清洗页面显示自适应
window.onload=function () {
    //宽度自适应
    let num = $('.column-0').length
    for(let i = 0;i<num;i++){
        let width = $('.centerMainTable').find('tr').eq(0).find('td').eq(i).width()
        if(width!=null && width>=220){
            $('.column-0').eq(i).width(width)
            $('.pensonOrg').find('.titleItem').eq(i).width(width)

        }

    }

    // 可以拖动
    $('.column-resize-handle').on('mousedown',function (e) {
        let index = $(this).parents('.column-0').index()
        let $this = $(this).parents('.column-0')
        let width = $(this).parents('.column-0').width()
        var startX = e.clientX;
        // alert(width)
        document.onmousemove = function(e){
            let newWidth = e.clientX-startX
            // if(newWidth>0){
            console.log(e.clientX-startX)

            $this.width(width+newWidth)
            $('.centerMainTable').find('tr').find('td').eq(index).width(width+newWidth)
            $('.pensonOrg').find('.titleItem').eq(index).width(width+newWidth)
            // }

            // $('.centerMainTable').find('td').eq(index).css('background','gray')

        }
        document.onmouseup = function(evt){
            document.onmousemove = null;
            document.onmouseup = null;
            // $('.centerMainTable').find('td').eq(index).css('background','#fff')
            // resize.releaseCapture && resize.releaseCapture();
        }
        // alert(index)
    })
}




var dsq ;
// 清洗任务提交
$('#run-job').on('click',function(){

    if(null == result || null == result.tableName){
        layer.alert("暂无参数可提交！", {icon: 2});
        return false;
    }
    var flag =false;

    layer.confirm('本次执行清洗数据是否要覆盖本表？', {
        btn: ['不覆盖','覆盖'] //按钮
        ,title :'重要提示'
    }, function(){
        //不覆盖填写表名操作
        layer.prompt({
            title: 'please enter the new table name',
            formType: 0,
            btn: ['submit', 'cancel']
        }, function (text, index) {
            var index = layer.load(1, {
                shade: [0.1, '#fff'] //0.1透明度的白色背景
            });
            //表名校验是否存在
            $.ajax({
                type: "get",
                url: "/hive/checkTableInHive",
                data: {"tableName":text},
                error: function (request) {
                    console.log(" check hive table error");
                    return;
                },
                success: function (data) {
                    layer.close(index);
                    var dataMap = JSON.parse(data);
                    if (200 === dataMap.code ) {
                        if(!dataMap.data){
                            flag = true;
                            layer.closeAll();
                            runTask(text);
                        }else{
                            layer.msg("table "+text+"已经存在", {icon: 2, shade: 0, time: 2000}, function () {
                            });
                        }
                    }
                }
            });
        });
    }, function(){
        //覆盖操作
        flag = true;
        runTask("");
    });
    if(!flag){
        return false ;
    }




})


//最终任务提交
function  runTask(newTableName){

    var y = layer.msg('正在提交任务中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: 'auto', time:100000});
    var DataClearVO = {};
    DataClearVO.newTableName = newTableName;
    DataClearVO.tableName =result.tableName;

    var dataOp = new Array();

    for (var i = 0; i < result.dataOperation.length; i++) {
        var operation = result.dataOperation[i];
        dataOp.push(operation);
    }
    DataClearVO.dataOperation = dataOp;
    var url = "/dataClean/runJob";

    console.log(DataClearVO);
    $.ajax({
        type: "POST",
        async: true,
        url:   url,
        data: JSON.stringify(DataClearVO),
        dataType: 'json',
        contentType:"application/json",

        success: function (data) {
            if(data.code == 200){
                closeLoad(y);
                //询问框
                layer.confirm(data.msg+',可在任务管理中查看进度,是否调整到任务列表？', {
                        btn: ['跳转', '取消'] //按钮
                        , title: '重要提示'
                    }, function () {
                        //   window.location.href='/view/task-list.html';
                        parent.xadmin.add_tab('任务管理','/view/task-list.html',true)
                    }, function(){
                        $('#taskStatusP').css('display','block');
                        //开始定时器查询状态
                        if(null != data.taskId && "" != data.taskId){
                            dsq = setInterval(function(){
                                queryTaskStatu(data.taskId);
                            },3000)
                        }
                    }
                );
            }else{
                closeLoad(y);
                layer.alert(data.msg, {icon: 2})
            }
        },
        error: function (request) {//请求失败之后的操作
            layer.alert(data.msg, {icon: 2})
            layer.close(index);
        }
    });


}


//任务状态查询
function queryTaskStatu(taskId) {
    $.ajax({
        url: '/task/queryTaskStatuById',
        type:'post',
        dataType:'json',
        data:{"taskId":taskId},
        success:function (data,statusText) {
            if(data.code===200){
                console.log(data);
                var task =  data.task;
                $('#taskStatusInfo').html(task.status);
                //done状态关闭定时器
                if("done" == task.status){
                    window.clearInterval(dsq);
                    console.log("clear dsq ");

                    var y = layer.msg('准备数据刷新中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: 'auto', time:100000});
                    setTimeout(function(){
                        var tableName = $('#modelTypeId').attr("na");
                        closeLoad(y);
                        window.location.reload(true);//刷新一遍在跳转
                        window.location.href = "/dataClean/page?tableName="+tableName ;

                    }, 3000);
                }
            } else {
                layer.msg(data.errMsg);
            }
        },
        'error':function () {
            layer.msg('任务状态查询异常');
        }
    });


}


function closeLoad(index) {
    layer.close(index);
}

$('#formatSelect').change(function(){
    var formatValue = $("#formatSelect option:selected").attr("value");
    if("addPrefix" == formatValue){
        $('#formatContent').css('display','block');
        $('#formatDate').css('display','none');
    }else if("addSuffix" == formatValue){
        $('#formatContent').css('display','block');
        $('#formatDate').css('display','none');
    }else if("date_format" == formatValue){
        $('#formatContent').css('display','none');
        $('#formatDate').css('display','block');
    }else {
        $('#formatContent').css('display','none');
        $('#formatDate').css('display','none');
    }
})

$('#operationColumnSelect').change(function(){
    var formatValue = $("#operationColumnSelect option:selected").attr("value");
    if("addColumn" == formatValue){
        $('#addColumnContent').css('display','block');
    }else {
        $('#addColumnContent').css('display','none');
    }
})

//右侧面板统一关闭代码
$('.delete.tricon').on('click',function(){
    $('#pane2').css('display','none');
    $('#pane3').css('display','none');
    $('#pane4').css('display','none');
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeReplace',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeSplit',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeJobList',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeMergeCustom',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeFormat',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeReplaceStr',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeReplaceRegular',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeFunctions',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeOperationColumn',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeParticiple',function () {
    $('#pane5').css('display','none');
})
$('#pane5Cards').on('click','#closeSplitIntermediateTable',function () {
    $('#pane5').css('display','none');
})

//页面点击显示画板开始
$("#Replace").click(function () {
    $("#menu-1").slideToggle();
});

$("#Split").click(function () {
    $('#rightTitle').html('Split by delimiter')
    $('#rightSubTitle').html('split')
    // $("#pane3").slideToggle();
    $('#formSplit')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane3').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','block')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')
});

$("#MergeCustom").click(function () {
    $('#rightTitle').html('Merge columns')
    $('#rightSubTitle').html(' merge')
    // $("#pane3").slideToggle();
    $('#formMergeCustom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane5').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','block')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')
});
$("#Format").click(function () {

    $('#rightTitle').html('Text format')
    $('#rightSubTitle').html('Format')
    // $("#pane3").slideToggle();
    $('#formFormat')[0].reset()
    // $('#pane5').slideToggle();
    $('#formatDate').css('display','none');
    $('#formatContent').css('display','none');
    if($('#itemPane6').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','block')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')
});



$("#replaceAll").click(function () {
    $('#rightTitle').html('Replace text or patterns')
    $('#rightSubTitle').html('replaceAll')
    // $('#pane2').slideToggle();
    $("#menu-1").slideToggle();
    $('#replaceFrom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane2').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane2').css('display','block')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')

});

$("#replaceByStr").click(function () {
    $('#rightTitle').html('Replace text or patterns')
    $('#rightSubTitle').html('replace by string')
    // $('#pane2').slideToggle();
    $("#menu-1").slideToggle();
    $('#replaceByStrFrom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane7').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane7').css('display','block')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')

});
$("#replaceByRegular").click(function () {
    $('#rightTitle').html('Replace text or patterns')
    $('#rightSubTitle').html('replace by regular expression')
    // $('#pane2').slideToggle();
    $("#menu-1").slideToggle();
    $('#replaceByRegularFrom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane8').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane8').css('display','block')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')

});

$("#functions").click(function () {
    $('#rightTitle').html('New formula')
    $('#rightSubTitle').html('generate new columns based on calculation')
    $('#formFunctions')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane9').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane9').css('display','block')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')

});


$("#operationColumn").click(function () {
    $('#rightTitle').html('Operation column')
    $('#rightSubTitle').html('Column modification')
    $('#formOperationColumn')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane10').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane10').css('display','block')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane11').css('display','none')
    $('#itemPane12').css('display','none')

});

$("#participle").click(function () {
    $('#rightTitle').html('participle column')
    $('#rightSubTitle').html('Column modification')
    $('#participleFrom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane11').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane11').css('display','block')
    $('#itemPane10').css('display','none')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane12').css('display','none')

});

$("#splitIntermediateTable").click(function () {
    $('#rightTitle').html('participle column')
    $('#rightSubTitle').html('Column modification')
    $('#participleFrom')[0].reset()
    // $('#pane5').slideToggle();
    if($('#itemPane12').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane12').css('display','block')
    $('#itemPane11').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','none')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')

});

//页面点击显示画板结束



//逐个清洗规则代码参数组合开始
var result =   new Object();
result.dataOperation = [];
function submitInfo() {

    // 获取页面已有的一个form表单
    var form = document.getElementById("replaceFrom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var contents = formData.get("newContents");

    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == contents) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;

    var obj = {};
    obj.mode = "replaceAll";
    obj.column = selectColumn;
    obj.type = "replace";
    obj.content = contents;
    result.dataOperation.push(obj);
    console.log(result);
    $('#pane5').css('display', 'none');
}

function submitReplaceStrInfo() {

    // 获取页面已有的一个form表单
    var form = document.getElementById("replaceByStrFrom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var prefix = formData.get("prefix"); //替换什么
    var suffix = formData.get("suffix");//替换成什么

    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == prefix || "" == suffix) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;

    var obj = {};
    obj.mode = "replaceToStr";
    obj.column = selectColumn;
    obj.type = "replace";
    obj.prefix = prefix;
    obj.suffix = suffix;
    result.dataOperation.push(obj);
    console.log(result);
    $('#pane5').css('display', 'none');

}
function submitReplaceRegularInfo() {

    // 获取页面已有的一个form表单
    var form = document.getElementById("replaceByRegularFrom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var sign = formData.get("sign"); //正则表达式
    var content = formData.get("content");//替换成什么

    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == sign || "" == content) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;

    var obj = {};
    obj.mode = "replaceByRegular";
    obj.column = selectColumn;
    obj.type = "replace";
    obj.sign = sign;
    obj.content = content;
    result.dataOperation.push(obj);
    console.log(result);
    $('#pane5').css('display', 'none');

}
function submitSplitInfo() {

    // 获取页面已有的一个form表单
    var form = document.getElementById("formSplit");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var sign = formData.get("sign");
    var splitLength = formData.get("splitLength");
    var columnContent = formData.get("columnContent");

    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == sign || "" == splitLength || "" == columnContent) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;

    var obj = {};
    obj.mode = "split";
    obj.column = selectColumn;
    obj.type = "split";
    obj.sign = sign;
    obj.length = splitLength;
    obj.split_column = columnContent;
    result.dataOperation.push(obj);
    console.log(result);
    $('#pane5').css('display', 'none');

}
function submitMergeCustomInfo(){

    // 获取页面已有的一个form表单
    var form = document.getElementById("formMergeCustom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var selectColumn2 = formData.get("selectColumn2");

    var sign = formData.get("sign");
    var columnContent = formData.get("columnContent");

    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == sign || "" == columnContent || "" == selectColumn2) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;

    var obj = {};
    obj.mode = "merge";
    obj.column = selectColumn + "," + selectColumn2;
    obj.type = "merge";
    obj.sign = sign;
    obj.content = columnContent;
    result.dataOperation.push(obj);
    console.log(result);
    $('#pane5').css('display', 'none');

}
//标准化
function submitFormatInfo(){

    // 获取页面已有的一个form表单
    var form = document.getElementById("formFormat");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    //获取目前选中的id和name
    var tableName = $("#tableName").val();
    //获取选中项
    var formatValue = $("#formatSelect option:selected").attr("value");
    //添加前后缀操作
    if("addPrefix" == formatValue || "addSuffix" == formatValue){
        var contents = formData.get("formatContent");
        if(""== tableName || "Select column" == selectColumn || "" == contents){
            layer.alert("required item cannot be empty ", {icon: 2});
            return false;
        }
        debugger;
        result.tableName= tableName;
        var obj = {};
        obj.mode = formatValue;
        obj.column = selectColumn;
        obj.type = "format";
        obj.content = contents;
        result.dataOperation.push(obj);
        console.log(result);

    } else if("date_format" == formatValue ) { //日期格式化
        var formatDateSelect = $("#formatDateSelect option:selected").attr("value");
        if(""== tableName || "Select column" == selectColumn || !formatValue || !formatDateSelect){
            layer.alert("required item cannot be empty ", {icon: 2});
            return false;
        }
        result.tableName= tableName;
        var obj = {};
        obj.mode = formatValue;
        obj.column = selectColumn;
        obj.type = "format";
        obj.sign = formatDateSelect;
        result.dataOperation.push(obj);
        console.log(result);
    }else{ //大小写操作
        if(""== tableName || "Select column" == selectColumn || !formatValue){
            layer.alert("required item cannot be empty ", {icon: 2});
            return false;
        }
        result.tableName= tableName;
        var obj = {};
        obj.mode = formatValue;
        obj.column = selectColumn;
        obj.type = "format";
        result.dataOperation.push(obj);
        console.log(result);
    }
    $('#pane5').css('display','none');
}

//计算加减乘除
function submitFunctionsInfo(){
    // 获取页面已有的一个form表单
    var form = document.getElementById("formFunctions");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var sign = formData.get("sign");

    //获取目前选中的id和name
    var tableName = $("#tableName").val();
    //获取选中项
    var formatValue = $("#functionsSelect option:selected").attr("value");
    var contents = formData.get("content");
    if ("" == tableName || "Select column" == selectColumn || "" == contents || "" == formatValue || "" == sign) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    result.tableName = tableName;
    var obj = {};
    obj.mode = formatValue;
    obj.column = selectColumn;
    obj.type = "functions";
    obj.content = contents;
    obj.sign = sign;
    result.dataOperation.push(obj);
    console.log(result);

    $('#pane5').css('display', 'none');
}

//操作列
function submitOperationColumnInfo(){
    // 获取页面已有的一个form表单
    var form = document.getElementById("formOperationColumn");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    //获取目前选中的id和name
    var tableName = $("#tableName").val();
    //获取选中项
    var formatValue = $("#operationColumnSelect option:selected").attr("value");
    debugger;
    //添加列操作
    if ("addColumn" == formatValue) {
        var contents = formData.get("content");
        var contentColumn = formData.get("contentColumn");

        if ("" == tableName || "" == contentColumn || "" == contents) {
            layer.alert("required item cannot be empty ", {icon: 2});
            return false;
        }
        result.tableName = tableName;
        var obj = {};
        obj.mode = formatValue;
        obj.column = contentColumn;
        obj.type = "operationColumn";
        obj.content = contents;
        result.dataOperation.push(obj);
        console.log(result);

    } else { //删除操作
        if ("" == tableName || "Select column" == selectColumn || !formatValue) {
            layer.alert("required item cannot be empty ", {icon: 2});
            return false;
        }
        result.tableName = tableName;
        var obj = {};
        obj.mode = formatValue;
        obj.column = selectColumn;
        obj.type = "operationColumn";
        result.dataOperation.push(obj);
        console.log(result);
    }
    $('#pane5').css('display', 'none');
}


//分词和拆分中间表
var participleAndcreateMiddleTable =   new Object();
participleAndcreateMiddleTable.dataOperation = [];
//分词功能
function participle() {
     participleAndcreateMiddleTable =   new Object();
    participleAndcreateMiddleTable.dataOperation = [];
    // 获取页面已有的一个form表单
    var form = document.getElementById("participleFrom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");
    var columnContent = formData.get("columnContent");
    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == columnContent  ) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    $('#pane5').css('display', 'none');

    participleAndcreateMiddleTable.pid = $("#parentId").val();
    participleAndcreateMiddleTable.dataMappingName = $("#parentName").val();
    participleAndcreateMiddleTable.tableName = tableName;
    var obj = {};
    obj.mode = "participlee";
    obj.column = selectColumn;
    obj.type = "participlee";
    obj.content = columnContent ;
    participleAndcreateMiddleTable.dataOperation.push(obj);
    console.log(participleAndcreateMiddleTable);

    layer.confirm('现在提交分词功能？', {
        btn: ['确定','取消'] //按钮
    }, function(){


        layer.confirm('本次执行清洗数据是否要覆盖本表？', {
            btn: ['不覆盖','覆盖'] //按钮
            ,title :'重要提示'
        }, function(){
            //不覆盖填写表名操作
            layer.prompt({
                title: 'please enter the new table name',
                formType: 0,
                btn: ['submit', 'cancel']
            }, function (text, index) {
                var index = layer.load(1, {
                    shade: [0.1, '#fff'] //0.1透明度的白色背景
                });
                //表名校验是否存在
                $.ajax({
                    type: "get",
                    url: "/hive/checkTableInHive",
                    data: {"tableName":text},
                    error: function (request) {
                        console.log(" check hive table error");
                        return;
                    },
                    success: function (data) {
                        layer.close(index);
                        var dataMap = JSON.parse(data);
                        if (200 === dataMap.code ) {
                            if(!dataMap.data){
                                layer.closeAll();
                                participleAndcreateMiddleTable.newTableName = text;
                                rowToColumn();
                            }else{
                                layer.msg("table "+text+"已经存在", {icon: 2, shade: 0, time: 2000}, function () {
                                });
                            }
                        }
                    }
                });
            });
        }, function(){
            participleAndcreateMiddleTable.newTableName = "";
            rowToColumn();
        });

    }, function(){
    });


}
function splitIntermediateTable() {
    participleAndcreateMiddleTable =   new Object();
    participleAndcreateMiddleTable.dataOperation = [];
    // 获取页面已有的一个form表单
    var form = document.getElementById("splitIntermediateTableFrom");
    // 用表单来初始化
    var formData = new FormData(form);
    // 我们可以根据name来访问表单中的字段
    var selectColumn = formData.get("selectColumn");//操作列
    var selectColumn2 = formData.get("selectColumn2");//行转列的字段
    var columnContent = formData.get("columnContent");//新字段名
    var sign = formData.get("sign"); //分割字符
    //获取目前选中的id和name
    var tableName = $("#tableName").val();

    if ("" == tableName || "Select column" == selectColumn || "" == columnContent || "" == selectColumn2 || "" == sign) {
        layer.alert("required item cannot be empty ", {icon: 2});
        return false;
    }
    $('#pane5').css('display', 'none');

    participleAndcreateMiddleTable.tableName = tableName;
    participleAndcreateMiddleTable.pid = $("#parentId").val();
    participleAndcreateMiddleTable.dataMappingName = $("#parentName").val();
    var obj = {};
    obj.mode = "splitKeywords";
    obj.column = selectColumn;
    obj.type = "createMiddleTable";
    obj.content = columnContent;
    obj.prefix = selectColumn2;
    obj.sign = sign;
    participleAndcreateMiddleTable.dataOperation.push(obj);
    console.log(participleAndcreateMiddleTable);


    layer.confirm('现在提交行转列功能？', {
        btn: ['确定','取消'] //按钮
    }, function(){



        layer.prompt({
            title: 'please enter the new table name',
            formType: 0,
            btn: ['submit', 'cancel']
        }, function (text, index) {
            var index = layer.load(1, {
                shade: [0.1, '#fff'] //0.1透明度的白色背景
            });
            //表名校验是否存在
            $.ajax({
                type: "get",
                url: "/hive/checkTableInHive",
                data: {"tableName":text},
                error: function (request) {
                    console.log(" check hive table error");
                    return;
                },
                success: function (data) {
                    layer.close(index);
                    var dataMap = JSON.parse(data);
                    if (200 === dataMap.code ) {
                        if(!dataMap.data){
                            flag = true;
                            layer.closeAll();
                            participleAndcreateMiddleTable.newTableName = text;
                            rowToColumn();
                        }else{
                            layer.msg("table "+text+"已经存在", {icon: 2, shade: 0, time: 2000}, function () {
                            });
                        }
                    }
                }
            });
        });
    }, function(){
    });


}

function rowToColumn() {
    var y = layer.msg('正在提交任务中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: 'auto', time:100000});

     console.log(JSON.stringify(participleAndcreateMiddleTable));
    $.ajax({
        type: "POST",
        async: true,
        url:   "/dataClean/participleeColumn",
        data: JSON.stringify(participleAndcreateMiddleTable),
        dataType: 'json',
        contentType:"application/json",

        success: function (data) {
            if(data.code == 200){
                closeLoad(y);
                //询问框
                layer.confirm(data.msg+',可在任务管理中查看进度,是否调整到任务列表？', {
                        btn: ['跳转', '取消'] //按钮
                        , title: '重要提示'
                    }, function () {
                        parent.xadmin.add_tab('任务管理','/view/task-list.html',true)
                    }, function(){

                    }
                );
            }else{
                closeLoad(y);
                layer.alert(data.msg, {icon: 2})
            }
        },
        error: function (request) {//请求失败之后的操作
            layer.alert(data.msg, {icon: 2})
            layer.close(index);
        }
    });


}

function changeSelect() {
    var str=[];
    var obj = document.getElementById("selectMultipleColumn");
    for(var i=0;i<obj.options.length;i++){
        if(obj.options[i].selected){
            str.push(obj.options[i].value);// 收集选中项
        }
    }
    var selectMonth = document.getElementById("selectMultiple");
    selectMonth.value = str;
}



//逐个清洗规则代码参数组合结束


//清洗任务列表信息开始
$("#OpenRecipePanel").click(function () {
    $('#rightTitle').html('job list')
    $('#rightSubTitle').html('job list')
    $("#jobListId").html("");
    // debugger;
    var str ="";
    if(result.dataOperation ){
        for(var i = 0;i< result.dataOperation.length; i++){
            if("replaceAll" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ " with "+result.dataOperation[i].content+" </span><br>";
            }else  if("split" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ " with  "+result.dataOperation[i].split_column+" by "+result.dataOperation[i].sign+"  </span><br>";
            }else  if("merge" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ " with  "+result.dataOperation[i].content+"  </span><br>";
            }else  if("uppercase" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "    </span><br>";
            }else  if("lowercase" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "    </span><br>";
            }else  if("addPrefix" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"  </span><br>";
            }else  if("replaceToStr" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].prefix+" by "+result.dataOperation[i].suffix+"   </span><br>";
            }else  if("replaceByRegular" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+" by "+result.dataOperation[i].sign+"  </span><br>";
            }else  if("removeSymbols" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "   </span><br>";
            }else  if("removeSpaces" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "   </span><br>";
            }else  if("date_format" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].sign+"  </span><br>";
            }else  if("addColumn" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"  </span><br>";
            }else  if("removeColumn" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "    </span><br>";
            }else  if("math_add" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"   </span><br>";
            }else  if("math_subtract" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"   </span><br>";
            }else  if("math_multiply" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"   </span><br>";
            }else  if("math_divide" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"   </span><br>";
            }else  if("addSuffix" == result.dataOperation[i].mode){
                str += " <span>"+(i+1)+"："+result.dataOperation[i].mode+" matches  "+result.dataOperation[i].column+ "  with  "+result.dataOperation[i].content+"  </span><br>";
            }

        }
        $("#jobListId").append(str);
    }
    // $("#pane4").slideToggle();

    if($('#itemPane4').css('display') == 'block' && $('#pane5').css('display') == 'block'){
        $('#pane5').css('display','none');
    }else{
        $('#pane5').css('display','block');
    }
    $('#itemPane2').css('display','none')
    $('#itemPane3').css('display','none')
    $('#itemPane4').css('display','block')
    $('#itemPane5').css('display','none')
    $('#itemPane6').css('display','none')
    $('#itemPane7').css('display','none')
    $('#itemPane8').css('display','none')
    $('#itemPane9').css('display','none')
    $('#itemPane10').css('display','none')
    $('#itemPane11').css('display','none')

});

//清洗任务列表信息结束