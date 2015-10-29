#What is this#
在Hive中直接读写MySQL数据，而不用通过Sqoop导入导出，实现通用的数据处理链路。
在原开源版本上修正了大量bug

#Features#
* 修正NPE异常
* 修正同一session中不能多次查询的问题
* 增加了hadoop2的支持
* 增加drop table时truncate功能
即在创建表时加上"mapred.jdbc.truncate.on.drop"="true"即可在drop表时一并将mysql数据清除

#Hive Storage Handler for JDBC#

Forked from Qubole(https://github.com/qubole/Hive-JDBC-Storage-Handler).

Fixed bugs and enhanced some features.


##Building from Source##

* Build using Maven (add ```-DskipTests``` to build without running tests):

```
  $ mvn clean package -Phadoop-2 -DskipTests
```


##Usage##
* Copy the JAR to the Hive lib directory and modify hive-site.xml, 
append jar file path to the section 'hive.aux.jars.path' .
* Copy mysql connector jar and modify config as above as well.

* How to create a mysql table-based hive table ?

```
CREATE EXTERNAL TABLE HiveTable
ROW FORMAT SERDE 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcSerDe'
STORED BY 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcStorageHandler'
TBLPROPERTIES (
  "mapred.jdbc.driver.class"="com.mysql.jdbc.Driver",
  "mapred.jdbc.url"="jdbc:mysql://localhost:3306/rstore",
  "mapred.jdbc.username"="root",
  "mapred.jdbc.input.table.name"="JDBCTable",
  "mapred.jdbc.output.table.name" = "JDBCTable",
  "mapred.jdbc.password"="",
  "mapred.jdbc.hive.lazy.split"= "false"
  "mapred.jdbc.truncate.on.drop"="false"
);
```

```
NOTE: "mapred.jdbc.hive.lazy.split"= "true" property enables 
       split computation to be done by mappers internally.
```

#### Queries to insert data into DB ####
```
* Insert Into Table HiveTable_1 select * from HiveTable_2;
*Insert Into Table HiveTable_1 select * from HiveTable_2 
 where id > 50000 and upper(names) = 'ANN';
```

## Support For FilterPushDown ##

* Support for FilterPushDown has been added to the jar as described in the following [wiki] (https://cwiki.apache.org/confluence/display/Hive/FilterPushdownDev)
* To disable FilterPushDown 
```
 set hive.optimize.ppd = false;
```
##Contributions##
* https://github.com/myui/HiveJdbcStorageHandler
* https://github.com/hava101
* https://github.com/stagraqubole
* https://github.com/divyanshu25
