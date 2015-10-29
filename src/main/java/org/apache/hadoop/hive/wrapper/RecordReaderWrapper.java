/*
 * Copyright 2013-2015 Qubole
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.wrapper;

import java.io.IOException;

import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.lib.db.DBConfiguration;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.hive.shims.ShimLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import org.apache.hadoop.hive.jdbc.storagehandler.Constants;
import org.apache.hadoop.hive.jdbc.storagehandler.JdbcDBInputSplit;
public class RecordReaderWrapper<K, V> implements RecordReader<K, V> {

    private static final Log LOG = LogFactory.getLog(RecordReaderWrapper.class);

    private org.apache.hadoop.mapreduce.RecordReader<K, V> realReader;
    private long splitLen; // for getPos()

    // expect readReader return same Key & Value objects (common case)
    // this avoids extra serialization & deserialazion of these objects
    private K keyObj = null;
    protected V valueObj = null;

    private boolean firstRecord = false;
    private boolean eof = false;
    private Connection conn = null;
    private String tblname = null;
    private DBConfiguration delegate = null;
    private long taskIdMapper = 0;
    private boolean lazySplitActive = false;
    private long count = 0;
    private int chunks = 0;
    public RecordReaderWrapper(InputFormat<K, V> newInputFormat,
            InputSplit oldSplit, JobConf oldJobConf, Reporter reporter)
            throws IOException {
    
        TaskAttemptID taskAttemptID = TaskAttemptID.forName(oldJobConf
                .get("mapred.task.id"));
        if(oldJobConf.get(Constants.LAZY_SPLIT) != null &&
                (oldJobConf.get(Constants.LAZY_SPLIT)).toUpperCase().equals("TRUE")){
            lazySplitActive = true;
            ResultSet results = null;  
            Statement statement = null;
            delegate = new DBConfiguration(oldJobConf);
            try{    
                conn = delegate.getConnection();
           
                statement = conn.createStatement();

                results = statement.executeQuery("Select Count(*) from " + oldJobConf.get("mapred.jdbc.input.table.name"));
                results.next();

                count = results.getLong(1);
                chunks = oldJobConf.getInt("mapred.map.tasks", 1);
                LOG.info("Total numer of records: " + count + ". Total number of mappers: " + chunks );
                splitLen = count/chunks;
                if((count%chunks) != 0)
                    splitLen++;
                LOG.info("Split Length is "+ splitLen);
                results.close();
                statement.close();
                
            }
            catch(Exception e){
                // ignore Exception
            }
        }
        org.apache.hadoop.mapreduce.InputSplit split;
        
        if(lazySplitActive){
            
            ((JdbcDBInputSplit)(((InputSplitWrapper)oldSplit).realSplit)).setStart(splitLen);
            ((JdbcDBInputSplit)(((InputSplitWrapper)oldSplit).realSplit)).setEnd(splitLen);
        }

        if (oldSplit.getClass() == FileSplit.class) {
            split = new org.apache.hadoop.mapreduce.lib.input.FileSplit(
                    ((FileSplit) oldSplit).getPath(),
                    ((FileSplit) oldSplit).getStart(),
                    ((FileSplit) oldSplit).getLength(), oldSplit.getLocations());
        } else {
            split = ((InputSplitWrapper) oldSplit).realSplit;
        }
        if (taskAttemptID == null) {
            taskAttemptID = new TaskAttemptID();
        }
        //holy shit!! NPE will be thrown here...
        //LOG.info("Task attempt id is >> " + taskAttemptID.toString());

        // create a MapContext to pass reporter to record reader (for counters)
        TaskAttemptContext taskContext = ShimLoader.getHadoopShims()
                .newTaskAttemptContext(oldJobConf,
                        new ReporterWrapper(reporter));

        try {
            realReader = newInputFormat.createRecordReader(split, taskContext);
            realReader.initialize(split, taskContext);

            // read once to gain access to key and value objects
            if (realReader.nextKeyValue()) {
                firstRecord = true;
                keyObj = realReader.getCurrentKey();
                valueObj = realReader.getCurrentValue();
            } else {
                eof = true;
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        realReader.close();
    }

    @Override
    public K createKey() {
        return keyObj;
    }

    @Override
    public V createValue() {
        return valueObj;
    }

    @Override
    public long getPos() throws IOException {
        return (long) (splitLen * getProgress());
    }

    @Override
    public float getProgress() throws IOException {
        try {
            return realReader.getProgress();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean next(K key, V value) throws IOException {
        if (eof) {
            return false;
        }

        if (firstRecord) { // key & value are already read.
            firstRecord = false;
            return true;
        }

        try {
            if (realReader.nextKeyValue()) {
                keyObj = realReader.getCurrentKey();
                valueObj = realReader.getCurrentValue();

                if (key != keyObj) {

                    throw new IOException(
                            "InputFormatWrapper can not "
                                    + "support RecordReaders that don't return same key & value "
                                    + "objects. current reader class : "
                                    + realReader.getClass());
                }
                return true;
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        eof = true; // strictly not required, just for consistency
        return false;
    }
}
