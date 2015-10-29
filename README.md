#Hive Storage Handler for JDBC#

Forked from Qubole.

Fixed bugs and enhanced some features.


##Building from Source##

* Build using Maven (add ```-DskipTests``` to build without running tests):

```
  $ mvn clean install -Phadoop-1
```

* The JARs for the storage handler can be found in the ```target/``` folder. Use ```qubole-hive-JDBC-0.0.4.jar``` in the hive session (see below).

##Usage##
* Add the JAR to the Hive session. ```<path-to-jar>``` is the path to the above mentioned JAR. For using this with Qubole hive, upload the JAR to an S3 bucket and provide its path.
  
``` 
  ADD JAR <path-to-jar>;
```

* Each record in the JDBC corresponds to a row in the Hive table.

* While creating the Hive table, use 
  
```
  STORED BY 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcStorageHandler'
```
  
##Table Creation##
Table can be created in 2 ways:
* First is, Column mappings can be explicitly given along with the table creation statement.

```
DROP TABLE HiveTable;
CREATE EXTERNAL TABLE HiveTable(
  id INT,
  id_double DOUBLE,
  names STRING,
  test INT
)
STORED BY 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcStorageHandler'
TBLPROPERTIES (
  "mapred.jdbc.driver.class"="com.mysql.jdbc.Driver",
  "mapred.jdbc.url"="jdbc:mysql://localhost:3306/rstore",
  "mapred.jdbc.username"="root",
  "mapred.jdbc.input.table.name"="JDBCTable",
  "mapred.jdbc.output.table.name"="JDBCTable",
  "mapred.jdbc.password"="",
  "mapred.jdbc.hive.lazy.split"= "false"
);

```

* Second is, no table mappings are specified, SerDe class automatically generates those mappings.

```
CREATE EXTERNAL TABLE HiveTable
row format serde 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcSerDe'
STORED BY 'org.apache.hadoop.hive.jdbc.storagehandler.JdbcStorageHandler'
TBLPROPERTIES (
  "mapred.jdbc.driver.class"="com.mysql.jdbc.Driver",
  "mapred.jdbc.url"="jdbc:mysql://localhost:3306/rstore",
  "mapred.jdbc.username"="root",
  "mapred.jdbc.input.table.name"="JDBCTable",
  "mapred.jdbc.output.table.name" = "JDBCTable",
  "mapred.jdbc.password"="",
  "mapred.jdbc.hive.lazy.split"= "false"
);
```

```
NOTE: "mapred.jdbc.hive.lazy.split"= "true" property enables 
       split computation to be done by mappers internally.
```

##Sample Queries##

HIVE-JDBC Storage Handeler supports alomost all types of possible SQL queries. Some examples of supported queries are:

####Queries to Read from DB ####
```
* select * from HiveTable;
* Select count(*) from HiveTable;
* select id from HiveTable where id > 50000;
* select names from HiveTable;
* select * from HiveTable where names like ‘D%’;
* SELECT * FROM HiveTable ORDER BY id DESC;
```
####Group By Queries ####
```
* select id, sum(id_double) as sum_double from HiveTable group by id;

```
#### Join Queries ####
```
* select HiveTable_1.*, HiveTable_2.* from HiveTable_1 a join HiveTable_2 b 
  on (a.id = b.id) where a.id > 90000 and b.id > 97000 ;
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
