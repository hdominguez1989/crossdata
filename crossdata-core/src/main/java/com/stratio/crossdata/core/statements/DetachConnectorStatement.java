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

package com.stratio.crossdata.core.statements;

import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.data.ConnectorName;
import com.stratio.crossdata.core.validator.requirements.ValidationRequirements;
import com.stratio.crossdata.core.validator.requirements.ValidationTypes;

/**
 * DetachConnectorStatement class.
 */
public class DetachConnectorStatement extends MetadataStatement {

    private ConnectorName connectorName;
    private ClusterName clusterName;

    /**
     * Constructor class.
     * @param connectorName The connector name.
     * @param clusterName The cluster name where the connector will be detached.
     */
    public DetachConnectorStatement(ConnectorName connectorName, ClusterName clusterName) {
        this.connectorName = connectorName;
        this.clusterName = clusterName;
    }

    @Override
    public String toString() {
        return "DETACH CONNECTOR " + connectorName.getQualifiedName() + " FROM " + clusterName.getQualifiedName();
    }

    @Override
    public ValidationRequirements getValidationRequirements() {
        return new ValidationRequirements().add(ValidationTypes.MUST_EXIST_CLUSTER)
                .add(ValidationTypes.MUST_EXIST_ATTACH_CONNECTOR_CLUSTER).add(ValidationTypes.MUST_EXIST_CONNECTOR);
    }

    public ConnectorName getConnectorName() {
        return connectorName;
    }

    public ClusterName getClusterName() {
        return clusterName;
    }
}
