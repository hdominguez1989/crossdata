/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.core.executor;

import com.stratio.deep.context.DeepSparkContext;
import com.stratio.meta.common.actor.ActorResultListener;
import com.stratio.meta.common.data.CassandraResultSet;
import com.stratio.meta.common.result.QueryResult;
import com.stratio.meta.core.engine.EngineConfig;
import com.stratio.meta.core.statements.SelectStatement;
import com.stratio.meta.streaming.MetaStream;
import com.stratio.streaming.api.IStratioStreamingAPI;

import org.apache.spark.SparkEnv;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

/**
 * Created by dhiguero on 20/06/14.
 */
public class StreamingExecutionThread implements Runnable {

  private final SelectStatement statement;

  private final EngineConfig config;

  private final String queryId;

  private final IStratioStreamingAPI stratioStreamingAPI;

  private final ActorResultListener callbackActor;

  private final DeepSparkContext deepSparkContext;

  public StreamingExecutionThread(
      String queryId,
      SelectStatement ss,
      EngineConfig config,
      IStratioStreamingAPI stratioStreamingAPI,
      DeepSparkContext deepSparkContext,
      ActorResultListener callbackActor){
    statement = ss;
    this.config = config;
    this.queryId = queryId;
    this.stratioStreamingAPI = stratioStreamingAPI;
    this.deepSparkContext = deepSparkContext;
    this.callbackActor = callbackActor;
  }

  @Override
  public void run() {
    JavaStreamingContext newContext = MetaStream.createSparkStreamingContext(config, deepSparkContext, 3);
    MetaStream.listenStream(queryId, stratioStreamingAPI, statement, config, newContext, callbackActor);
    QueryResult r = QueryResult.createQueryResult(new CassandraResultSet());
    r.setLastResultSet();
    r.setQueryId(queryId);
    callbackActor.processResults(r);
  }

  public void stopStreamingQuery(){
    StreamExecutor.stopContext(queryId);
  }
}
