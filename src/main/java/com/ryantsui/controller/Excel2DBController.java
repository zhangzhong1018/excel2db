package com.ryantsui.controller;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ryantsui.config.DBConfig;
import com.ryantsui.entity.JsonMessage;
import com.ryantsui.service.Excel2DBService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 控制类.
 * Created by xufy on 2018/5/8.
 */
@Controller
@RequestMapping("file")
public class Excel2DBController {
    private static final Logger logger = LoggerFactory.getLogger(Excel2DBController.class);

    @Autowired
    private Excel2DBService excel2DBService;

    /**
     * 用户自定义数据库连接.
     * @param driver 驱动名称
     * @param url 地址
     * @param username 用户名
     * @param password 密码
     * @return JsonMessage
     * @throws ClassNotFoundException 异常
     * @throws SQLException 异常
     */
    @RequestMapping(value = "/listDBTables",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public JsonMessage connectDataBase(@RequestParam(value = "driver") String driver,
                                  @RequestParam(value = "url") String url,
                                  @RequestParam(value = "username") String username,
                                  @RequestParam(value = "password") String password)
            throws ClassNotFoundException,SQLException {
        DBConfig.getInstance().put("driver",driver);
        DBConfig.getInstance().put("url",url);
        DBConfig.getInstance().put("username",username);
        DBConfig.getInstance().put("password",password);
        List<String> tables = excel2DBService.listAllTables();
        return new JsonMessage().success(tables);
    }


    /**
     * 获取数据库某个表的所有字段.
     * @param tableName 表名
     * @return JsonMessage
     * @throws ClassNotFoundException 异常
     * @throws SQLException 异常
     */
    @RequestMapping(value="listTableColumns",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public JsonMessage listTableColumns(@RequestParam(value="tableName") String tableName) throws ClassNotFoundException,SQLException{
        if (!StringUtils.isNotBlank(tableName)) {
            return new JsonMessage().failure("表名不能为空");
        }
        String tableSql = null;
        String driver = (String)DBConfig.getInstance().get("driver");
        if (driver.contains("mysql")) {
            tableSql = "select * from " + tableName +" limit 10";
        } else if (driver.contains("oracle")) {
            tableSql = "select * from " + tableName + " where rownum < 10";
        }
        List<String> list = excel2DBService.listTableAllColumns(tableSql);
        return new JsonMessage().success(list);
    }

    /**
     * 创建新表.
     * @param tableName 表名
     * @param data 字段数据
     * @return JsonMessage
     * @throws ClassNotFoundException 异常
     * @throws SQLException 异常
     */
    @RequestMapping(value="createNewTable",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public JsonMessage createNewTable(String tableName,String data) throws ClassNotFoundException,SQLException{
        List<Map<String,String>> DO = JSON.parseObject(data,new TypeReference<List<Map<String,String>>>(){});
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("create table " + tableName + " (");
        Map<String,String> tempMap = null;
        for (int i = 0; i < DO.size(); i++) {
            tempMap = DO.get(i);
            stringBuffer.append(tempMap.get("name") + " " + tempMap.get("type"));
            if ("VARCHAR".equals(tempMap.get("type")) || "VARCHAR2".equals(tempMap.get("type")) || "CHAR".equals(tempMap.get("type")) ){
                if (StringUtils.isNotBlank(tempMap.get("length"))) {
                    stringBuffer.append("(" + tempMap.get("length") + ")");
                }
            }
            if (i != DO.size() - 1) {
                stringBuffer.append(",");
            }
        }
        stringBuffer.append(")");
        excel2DBService.createNewTable(stringBuffer.toString());
        return new JsonMessage().success();
    }

    /**
     *  插入数据到数据库.
     * @param tableName 表名
     * @param columns 列名
     * @param data 数据
     * @return JsonMessage
     * @throws ClassNotFoundException 异常
     * @throws SQLException 异常
     */
    @RequestMapping(value = "saveData",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public JsonMessage saveData(String tableName,String columns,String data) throws ClassNotFoundException,SQLException{
        List<List<String>> dataList = JSON.parseObject(data, new TypeReference< List<List<String>>>() {});
        String[] columnsArr = columns.split(",");
        if (columnsArr.length != dataList.get(0).size()) {
            return new JsonMessage().failure("列数不匹配！");
        }
        //50条数据进行一次提交
        int num = 50;
        if (dataList.size() <= num) {
            excel2DBService.saveDataIns(tableName,columns,dataList);
        } else {
            int fromIndex = 0,toIndex = num;
            while(true) {
                if (toIndex > dataList.size()) {
                    toIndex = dataList.size();
                }
                excel2DBService.saveDataIns(tableName,columns,dataList.subList(fromIndex,toIndex));
                if (toIndex == dataList.size()) {
                    break;
                }
                fromIndex = toIndex;
                toIndex += num;
            }
        }
        return new JsonMessage().success();
    }


}