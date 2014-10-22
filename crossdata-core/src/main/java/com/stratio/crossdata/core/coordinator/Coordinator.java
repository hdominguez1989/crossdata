/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.stratio.crossdata.core.coordinator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.stratio.crossdata.common.executionplan.MetadataWorkflow;
import com.stratio.crossdata.common.result.CommandResult;
import com.stratio.crossdata.common.metadata.ConnectorMetadata;
import com.stratio.crossdata.common.result.ErrorType;
import com.stratio.crossdata.common.result.Result;
import com.stratio.crossdata.communication.AttachCluster;
import com.stratio.crossdata.communication.AttachConnector;
import com.stratio.crossdata.communication.DetachCluster;
import com.stratio.crossdata.communication.DetachConnector;
import com.stratio.crossdata.communication.ManagementOperation;
import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.data.ConnectorName;
import com.stratio.crossdata.common.data.DataStoreName;
import com.stratio.crossdata.common.data.IndexName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.metadata.CatalogMetadata;
import com.stratio.crossdata.common.metadata.ClusterAttachedMetadata;
import com.stratio.crossdata.common.metadata.ClusterMetadata;
import com.stratio.crossdata.common.metadata.ConnectorAttachedMetadata;
import com.stratio.crossdata.common.metadata.DataStoreMetadata;
import com.stratio.crossdata.common.metadata.IndexMetadata;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.selectors.Selector;
import com.stratio.crossdata.core.metadata.MetadataManager;

public class Coordinator implements Serializable {

    /**
     * Class logger.
     */
    private static final Logger LOG = Logger.getLogger(Coordinator.class);

    public void persist(MetadataWorkflow metadataWorkflow) {

       switch (metadataWorkflow.getExecutionType()) {
        case CREATE_CATALOG:
            persistCreateCatalog(metadataWorkflow.getCatalogMetadata());
            break;
        case CREATE_INDEX:
            persistCreateIndex(metadataWorkflow.getIndexMetadata());
            break;
        case CREATE_TABLE_AND_CATALOG:
        case CREATE_TABLE:
            persistCreateTable(metadataWorkflow.getTableMetadata());
            break;
        case DROP_CATALOG:
            persistDropCatalog(metadataWorkflow.getCatalogName());
            break;
        case DROP_INDEX:
            persistDropIndex(metadataWorkflow.getIndexMetadata().getName());
            break;
        case DROP_TABLE:
            persistDropTable(metadataWorkflow.getTableName());
            break;
        default:
            LOG.info("unknown statement detected");
            break;
        }
    }

    public Result executeManagementOperation(ManagementOperation workflow){
        Result result = Result.createErrorResult(ErrorType.COORDINATION, "Wrong management operation");

        if(AttachCluster.class.isInstance(workflow)){
            AttachCluster ac = AttachCluster.class.cast(workflow);
            result = persistAttachCluster(ac.targetCluster(), ac.datastoreName(), ac.options());
        }else if(DetachCluster.class.isInstance(workflow)){
            result = Result.createErrorResult(ErrorType.NOT_SUPPORTED, "Not supported");
        }else if(AttachConnector.class.isInstance(workflow)){
            AttachConnector ac = AttachConnector.class.cast(workflow);
            result = persistAttachConnector(ac.targetCluster(), ac.connectorName(), ac.options());
        } else if(DetachConnector.class.isInstance(workflow)){
            result = Result.createErrorResult(ErrorType.NOT_SUPPORTED, "Not supported");
        }

        result.setQueryId(workflow.queryId());

        return result;
    }

    public Result persistAttachCluster(ClusterName clusterName, DataStoreName datastoreName,
            Map<Selector, Selector> options) {

        // Create and persist Cluster metadata
        ClusterMetadata clusterMetadata = new ClusterMetadata(clusterName, datastoreName, options, new HashMap<ConnectorName, ConnectorAttachedMetadata>());
        MetadataManager.MANAGER.createCluster(clusterMetadata, false);

        // Add new attachment to DataStore
        DataStoreMetadata datastoreMetadata =
                MetadataManager.MANAGER.getDataStore(datastoreName);

        Map<ClusterName, ClusterAttachedMetadata> clusterAttachedRefs =
                datastoreMetadata.getClusterAttachedRefs();

        ClusterAttachedMetadata value =
                new ClusterAttachedMetadata(clusterName, datastoreName, options);

        clusterAttachedRefs.put(clusterName, value);
        datastoreMetadata.setClusterAttachedRefs(clusterAttachedRefs);

        MetadataManager.MANAGER.createDataStore(datastoreMetadata, false);
        return CommandResult.createCommandResult("Cluster attached successfully");
    }

    public Result persistDetachCluster(ClusterName clusterName, DataStoreName datastoreName,
            Map<Selector, Selector> options) {
        //TODO Move this type of operations to MetadataManager in order to use a single lock
        DataStoreMetadata datastoreMetadata =
                MetadataManager.MANAGER.getDataStore(datastoreName);

        Map<ClusterName, ClusterAttachedMetadata> clusterAttachedRefs =
                datastoreMetadata.getClusterAttachedRefs();

        clusterAttachedRefs.remove(clusterName);
        datastoreMetadata.setClusterAttachedRefs(clusterAttachedRefs);

        MetadataManager.MANAGER.createDataStore(datastoreMetadata, false);
        return CommandResult.createCommandResult("CLUSTER attached successfully");
    }

    public void persistCreateCatalog(CatalogMetadata catalog) {
        MetadataManager.MANAGER.createCatalog(catalog);
    }

    public void persistCreateTable(TableMetadata table) {
        MetadataManager.MANAGER.createTable(table);
    }

    public void persistCreateIndex(IndexMetadata index) {
        // TODO move to MetadataManager
        TableMetadata table = MetadataManager.MANAGER.getTable(index.getName().getTableName());
        table.addIndex(index.getName(), index);
        MetadataManager.MANAGER.createTable(table, false);
    }

    public void persistDropCatalog(CatalogName catalog) {
        MetadataManager.MANAGER.deleteCatalog(catalog);
    }

    public void persistDropTable(TableName table) {
        MetadataManager.MANAGER.deleteTable(table);
    }

    public void persistDropIndex(IndexName index) {
        // TODO move to MetadataManager
        TableMetadata table = MetadataManager.MANAGER.getTable(index.getTableName());
        table.deleteIndex(index);
        MetadataManager.MANAGER.createTable(table, false);
    }

    public Result persistAttachConnector(ClusterName clusterName, ConnectorName connectorName,
            Map<Selector, Selector> options) {

        // Update information in Cluster
        ClusterMetadata clusterMetadata =
                MetadataManager.MANAGER.getCluster(clusterName);

        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs =
                clusterMetadata.getConnectorAttachedRefs();

        ConnectorAttachedMetadata value =
                new ConnectorAttachedMetadata(connectorName, clusterName, options);

        connectorAttachedRefs.put(connectorName, value);

        clusterMetadata.setConnectorAttachedRefs(connectorAttachedRefs);

        MetadataManager.MANAGER.createCluster(clusterMetadata, false);

        // Update information in Connector
        ConnectorMetadata connectorMetadata = MetadataManager.MANAGER.getConnector(connectorName);

        connectorMetadata.addClusterProperties(clusterName, options);

        MetadataManager.MANAGER.createConnector(connectorMetadata, false);


        return CommandResult.createCommandResult("CONNECTOR attached successfully");
    }

    public Result persistDetachConnector(ClusterName clusterName, ConnectorName connectorName,
            Map<Selector, Selector> options) {
        ClusterMetadata clusterMetadata =
                MetadataManager.MANAGER.getCluster(clusterName);

        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs =
                clusterMetadata.getConnectorAttachedRefs();

        connectorAttachedRefs.remove(connectorName);
        clusterMetadata.setConnectorAttachedRefs(connectorAttachedRefs);

        MetadataManager.MANAGER.createCluster(clusterMetadata, false);
        return CommandResult.createCommandResult("Connector attached successfully");
    }
}