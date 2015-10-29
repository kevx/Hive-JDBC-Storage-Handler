/*
 * Copyright 2013-2015 Qubole
 * Copyright 2013-2015 Makoto YUI
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

package org.apache.hadoop.hive.jdbc.storagehandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

import org.apache.hadoop.hive.jdbc.storagehandler.exceptions.PredicateMissingException;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.lib.db.DBInputFormat;
import org.apache.hadoop.hive.ql.plan.TableScanDesc;

import org.apache.hadoop.hive.wrapper.InputFormatWrapper;

public class JdbcInputFormat extends InputFormatWrapper {
    private static final Log LOG = LogFactory.getLog(JdbcInputFormat.class);

    private boolean jobConfSet = false;
    private DBInputFormat delegate;

    public JdbcInputFormat() {
        super(new JdbcDBInputFormat());
    }

    @Override
    public RecordReader getRecordReader(InputSplit split, JobConf job,
            Reporter reporter) throws IOException {
        if ((job.get(TableScanDesc.FILTER_TEXT_CONF_STR) == null ||
                job.get(TableScanDesc.FILTER_TEXT_CONF_STR).length() == 0) &&
                job.getBoolean(Constants.PREDICATE_REQUIRED, false)) {
            throw new PredicateMissingException();
        }
        JdbcSerDeHelper.setFilters(job);
        ((org.apache.hadoop.mapreduce.lib.db.DBInputFormat) realInputFormat)
                .setConf(job);
        return super.getRecordReader(split, job, reporter);
    }

    @Override
    public InputSplit[] getSplits(JobConf job, int numSplits)
            throws IOException {
        JdbcSerDeHelper.setFilters(job);
        job.setInt("mapred.map.tasks", numSplits);
        try {
            //why do i do this? In one hive session you may run the query without
            //any exception for the first time, but the jdbc connection will by closed
            //soon. So the second time you just get a 'MySQLNonTransientConnectionException'
            //After explicitly calling closeConnection Hadoop will try to get a new connection
            //and everything is ok now.
            Method method = DBInputFormat.class.getDeclaredMethod("closeConnection");
            method.setAccessible(true);
            method.invoke(realInputFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ((org.apache.hadoop.mapreduce.lib.db.DBInputFormat) realInputFormat)
                .setConf(job);
        return super.getSplits(job, numSplits);

    }
}
