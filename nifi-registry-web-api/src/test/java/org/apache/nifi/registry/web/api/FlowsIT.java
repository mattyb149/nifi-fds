/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.web.api;

import org.apache.nifi.registry.bucket.BucketItemType;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.jdbc.Sql;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:db/clearDB.sql", "classpath:db/FlowsIT.sql"})
public class FlowsIT extends UnsecuredITBase {

    @Test
    public void testGetFlowsEmpty() throws Exception {

        // Given: an empty bucket with id "3" (see FlowsIT.sql)
        final String emptyBucketId = "3";

        // When: the /buckets/{id}/flows endpoint is queried

        final VersionedFlow[] flows = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", emptyBucketId)
                .request()
                .get(VersionedFlow[].class);

        // Then: an empty array is returned

        assertNotNull(flows);
        assertEquals(0, flows.length);
    }

    @Test
    public void testGetFlows() throws Exception {

        // Given: a few buckets and flows have been populated in the DB (see FlowsIT.sql)

        final String prePopulatedBucketId = "1";
        final String expected = "[" +
                "{\"identifier\":\"1\"," +
                "\"name\":\"Flow 1\"," +
                "\"description\":\"This is flow 1\"," +
                "\"bucketIdentifier\":\"1\"," +
                "\"createdTimestamp\":1505091360000," +
                "\"modifiedTimestamp\":1505091360000," +
                "\"type\":\"FLOW\"," +
                "\"link\":{\"params\":{\"rel\":\"self\"},\"href\":\"buckets/1/flows/1\"}}," +
                "{\"identifier\":\"2\",\"name\":\"Flow 2\"," +
                "\"description\":\"This is flow 2\"," +
                "\"bucketIdentifier\":\"1\"," +
                "\"createdTimestamp\":1505091360000," +
                "\"modifiedTimestamp\":1505091360000," +
                "\"type\":\"FLOW\"," +
                "\"versionCount\":0," +
                "\"link\":{\"params\":{\"rel\":\"self\"},\"href\":\"buckets/1/flows/2\"}}" +
                "]";

        // When: the /buckets/{id}/flows endpoint is queried

        final String flowsJson = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", prePopulatedBucketId)
                .request()
                .get(String.class);

        // Then: the pre-populated list of flows is returned

        JSONAssert.assertEquals(expected, flowsJson, false);
    }

    @Test
    public void testCreateFlowGetFlow() throws Exception {

        // Given: an empty bucket with id "3" (see FlowsIT.sql)

        long testStartTime = System.currentTimeMillis();
        final String bucketId = "3";

        // When: a flow is created

        final VersionedFlow flow = new VersionedFlow();
        flow.setBucketIdentifier(bucketId);
        flow.setName("Test Flow");
        flow.setDescription("This is a flow created by an integration test.");

        final VersionedFlow createdFlow = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", bucketId)
                .request()
                .post(Entity.entity(flow, MediaType.APPLICATION_JSON), VersionedFlow.class);

        // Then: the server returns the created flow, with server-set fields populated correctly

        assertFlowsEqual(flow, createdFlow, false);
        assertNotNull(createdFlow.getIdentifier());
        assertNotNull(createdFlow.getBucketName());
        assertEquals(0, createdFlow.getVersionCount());
        assertEquals(createdFlow.getType(), BucketItemType.FLOW);
        assertTrue(createdFlow.getCreatedTimestamp() - testStartTime > 0L); // both server and client in same JVM, so there shouldn't be skew
        assertEquals(createdFlow.getCreatedTimestamp(), createdFlow.getModifiedTimestamp());
        assertNotNull(createdFlow.getLink());
        assertNotNull(createdFlow.getLink().getUri());

        // And when .../flows is queried, then the newly created flow is returned in the list

        final VersionedFlow[] flows = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", bucketId)
                .request()
                .get(VersionedFlow[].class);
        assertNotNull(flows);
        assertEquals(1, flows.length);
        assertFlowsEqual(createdFlow, flows[0], true);

        // And when the link URI is queried, then the newly created flow is returned

        final VersionedFlow flowByLink = client
                .target(createURL(flows[0].getLink().getUri().toString()))
                .request()
                .get(VersionedFlow.class);
        assertFlowsEqual(createdFlow, flowByLink, true);

        // And when the bucket is queried by .../flows/ID, then the newly created flow is returned

        final VersionedFlow flowById = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", createdFlow.getIdentifier())
                .request()
                .get(VersionedFlow.class);
        assertFlowsEqual(createdFlow, flowById, true);

    }

    @Test
    public void testUpdateFlow() throws Exception {

        // Given: a flow exists on the server

        final String bucketId = "3";
        final VersionedFlow flow = new VersionedFlow();
        flow.setBucketIdentifier(bucketId);
        flow.setName("Test Flow");
        flow.setDescription("This is a flow created by an integration test.");
        final VersionedFlow createdFlow = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", bucketId)
                .request()
                .post(Entity.entity(flow, MediaType.APPLICATION_JSON), VersionedFlow.class);

        // When: the flow is modified by the client and updated on the server

        createdFlow.setName("Renamed Flow");
        createdFlow.setDescription("This flow has been updated by an integration test.");

        final VersionedFlow updatedFlow = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", createdFlow.getIdentifier())
                .request()
                .put(Entity.entity(createdFlow, MediaType.APPLICATION_JSON), VersionedFlow.class);

        // Then: the server returns the updated flow, with a new modified timestamp

        assertTrue(updatedFlow.getModifiedTimestamp() > createdFlow.getModifiedTimestamp());
        createdFlow.setModifiedTimestamp(updatedFlow.getModifiedTimestamp());
        assertFlowsEqual(createdFlow, updatedFlow, true);

    }

    @Test
    public void testDeleteBucket() throws Exception {

        // Given: a flow exists on the server

        final String bucketId = "3";
        final VersionedFlow flow = new VersionedFlow();
        flow.setBucketIdentifier(bucketId);
        flow.setName("Test Flow");
        flow.setDescription("This is a flow created by an integration test.");
        final VersionedFlow createdFlow = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", bucketId)
                .request()
                .post(Entity.entity(flow, MediaType.APPLICATION_JSON), VersionedFlow.class);

        // When: the flow is deleted

        final VersionedFlow deletedFlow = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", createdFlow.getIdentifier())
                .request()
                .delete(VersionedFlow.class);

        // Then: the body of the server response matches the flow that was deleted
        //  and: the flow is no longer accessible (resource not found)

        createdFlow.setLink(null); // self URI will not be present in deletedBucket
        assertFlowsEqual(createdFlow, deletedFlow, true);

        final Response response = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", createdFlow.getIdentifier())
                .request()
                .get();
        assertEquals(404, response.getStatus());

    }

    @Test
    public void testGetFlowVersionsEmpty() throws Exception {

        // Given: a Bucket "2" containing a flow "3" with no snapshots (see FlowsIT.sql)
        final String bucketId = "2";
        final String flowId = "3";

        // When: the /buckets/{id}/flows/{id}/versions endpoint is queried

        final VersionedFlowSnapshot[] flowSnapshots = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}/versions"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", flowId)
                .request()
                .get(VersionedFlowSnapshot[].class);

        // Then: an empty array is returned

        assertNotNull(flowSnapshots);
        assertEquals(0, flowSnapshots.length);
    }

    @Test
    public void testGetFlowVersions() throws Exception {

        // Given: a bucket "1" with flow "1" with existing snapshots has been populated in the DB (see FlowsIT.sql)

        final String prePopulatedBucketId = "1";
        final String prePopulatedFlowId = "1";
        final String expected = "[" +
                "{\"bucketIdentifier\":\"1\"," +
                "\"flowIdentifier\":\"1\"," +
                "\"version\":1," +
                "\"timestamp\":1505091420000," +
                "\"author\" : \"user1\"," +
                "\"comments\":\"This is flow 1 snapshot 1\"," +
                "\"link\":{\"params\":{\"rel\":\"content\"},\"href\":\"buckets/1/flows/1/versions/1\"}}," +
                "{\"bucketIdentifier\":\"1\"," +
                "\"flowIdentifier\":\"1\"," +
                "\"version\":2," +
                "\"timestamp\":1505091480000," +
                "\"author\" : \"user2\"," +
                "\"comments\":\"This is flow 1 snapshot 2\"," +
                "\"link\":{\"params\":{\"rel\":\"content\"},\"href\":\"buckets/1/flows/1/versions/2\"}}]";

        // When: the /buckets/{id}/flows/{id}/versions endpoint is queried

        final String flowSnapshotsJson = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}/versions"))
                .resolveTemplate("bucketId", prePopulatedBucketId)
                .resolveTemplate("flowId", prePopulatedFlowId)
                .request()
                .get(String.class);

        // Then: the pre-populated list of flow versions is returned

        JSONAssert.assertEquals(expected, flowSnapshotsJson, false);
    }

    @Test
    public void testCreateFlowVersionGetFlowVersion() throws Exception {

        // Given: an empty Bucket "3" (see FlowsIT.sql) with a newly created flow

        long testStartTime = System.currentTimeMillis();
        final String bucketId = "2";
        final VersionedFlow flow = new VersionedFlow();
        flow.setBucketIdentifier(bucketId);
        flow.setName("Test Flow for creating snapshots");
        flow.setDescription("This is a randomly named flow created by an integration test for the purpose of holding snapshots.");
        final VersionedFlow createdFlow = client
                .target(createURL("buckets/{bucketId}/flows"))
                .resolveTemplate("bucketId", bucketId)
                .request()
                .post(Entity.entity(flow, MediaType.APPLICATION_JSON), VersionedFlow.class);
        final String flowId = createdFlow.getIdentifier();

        // When: an initial flow snapshot is created *without* a version

        final VersionedFlowSnapshotMetadata flowSnapshotMetadata = new VersionedFlowSnapshotMetadata();
        flowSnapshotMetadata.setBucketIdentifier("2");
        flowSnapshotMetadata.setFlowIdentifier(flowId);
        flowSnapshotMetadata.setComments("This is snapshot 1, created by an integration test.");
        final VersionedFlowSnapshot flowSnapshot = new VersionedFlowSnapshot();
        flowSnapshot.setSnapshotMetadata(flowSnapshotMetadata);
        flowSnapshot.setFlowContents(new VersionedProcessGroup()); // an empty root process group

        WebTarget clientRequestTarget = client
                .target(createURL("buckets/{bucketId}/flows/{flowId}/versions"))
                .resolveTemplate("bucketId", bucketId)
                .resolveTemplate("flowId", flowId);
        final Response response =
                clientRequestTarget.request().post(Entity.entity(flowSnapshot, MediaType.APPLICATION_JSON), Response.class);

        // Then: an error is returned because version != 1

        assertEquals(400, response.getStatus());

        // But When: an initial flow snapshot is created with version == 1

        flowSnapshot.getSnapshotMetadata().setVersion(1);
        final VersionedFlowSnapshot createdFlowSnapshot =
                clientRequestTarget.request().post(Entity.entity(flowSnapshot, MediaType.APPLICATION_JSON), VersionedFlowSnapshot.class);

        // Then: the server returns the created flow snapshot, with server-set fields populated correctly :)

        assertFlowSnapshotsEqual(flowSnapshot, createdFlowSnapshot, false);
        assertTrue(createdFlowSnapshot.getSnapshotMetadata().getTimestamp() - testStartTime > 0L); // both server and client in same JVM, so there shouldn't be skew
        assertEquals("anonymous", createdFlowSnapshot.getSnapshotMetadata().getAuthor());
        assertNotNull(createdFlowSnapshot.getSnapshotMetadata().getLink());
        assertNotNull(createdFlowSnapshot.getSnapshotMetadata().getLink().getUri());

        // And when .../flows/{id}/versions is queried, then the newly created flow snapshot is returned in the list

        final VersionedFlowSnapshotMetadata[] versionedFlowSnapshots =
                clientRequestTarget.request().get(VersionedFlowSnapshotMetadata[].class);
        assertNotNull(versionedFlowSnapshots);
        assertEquals(1, versionedFlowSnapshots.length);
        assertFlowSnapshotMetadataEqual(createdFlowSnapshot.getSnapshotMetadata(), versionedFlowSnapshots[0], true);

        // And when the link URI is queried, then the newly created flow snapshot is returned

        final VersionedFlowSnapshot flowSnapshotByLink = client
                .target(createURL(versionedFlowSnapshots[0].getLink().getUri().toString()))
                .request()
                .get(VersionedFlowSnapshot.class);
        assertFlowSnapshotsEqual(createdFlowSnapshot, flowSnapshotByLink, true);
        assertNotNull(flowSnapshotByLink.getFlow());
        assertNotNull(flowSnapshotByLink.getBucket());

        // And when the bucket is queried by .../versions/{v}, then the newly created flow snapshot is returned

        final VersionedFlowSnapshot flowSnapshotByVersionNumber = clientRequestTarget.path("/1").request().get(VersionedFlowSnapshot.class);
        assertFlowSnapshotsEqual(createdFlowSnapshot, flowSnapshotByVersionNumber, true);

        // And when the latest URI is queried, then the newly created flow snapshot is returned

        final VersionedFlowSnapshot flowSnapshotByLatest = clientRequestTarget.path("/latest").request().get(VersionedFlowSnapshot.class);
        assertFlowSnapshotsEqual(createdFlowSnapshot, flowSnapshotByLatest, true);
        assertNotNull(flowSnapshotByLatest.getFlow());
        assertNotNull(flowSnapshotByLatest.getBucket());

    }

    private static void assertFlowsEqual(VersionedFlow expected, VersionedFlow actual, boolean checkServerSetFields) {
        assertNotNull(actual);
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getBucketIdentifier(), actual.getBucketIdentifier());
        if (checkServerSetFields) {
            assertEquals(expected.getIdentifier(), actual.getIdentifier());
            assertEquals(expected.getVersionCount(), actual.getVersionCount());
            assertEquals(expected.getCreatedTimestamp(), actual.getCreatedTimestamp());
            assertEquals(expected.getModifiedTimestamp(), actual.getModifiedTimestamp());
            assertEquals(expected.getType(), actual.getType());
            assertEquals(expected.getLink(), actual.getLink());
        }
    }

    private static void assertFlowSnapshotsEqual(VersionedFlowSnapshot expected, VersionedFlowSnapshot actual, boolean checkServerSetFields) {

        assertNotNull(actual);

        if (expected.getSnapshotMetadata() != null) {
            assertFlowSnapshotMetadataEqual(expected.getSnapshotMetadata(), actual.getSnapshotMetadata(), checkServerSetFields);
        }

        if (expected.getFlowContents() != null) {
            assertVersionedProcessGroupsEqual(expected.getFlowContents(), actual.getFlowContents());
        }

    }

    private static void assertFlowSnapshotMetadataEqual(
            VersionedFlowSnapshotMetadata expected, VersionedFlowSnapshotMetadata actual, boolean checkServerSetFields) {

        assertNotNull(actual);
        assertEquals(expected.getBucketIdentifier(), actual.getBucketIdentifier());
        assertEquals(expected.getFlowIdentifier(), actual.getFlowIdentifier());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getComments(), actual.getComments());
        if (checkServerSetFields) {
            assertEquals(expected.getTimestamp(), actual.getTimestamp());
        }
    }

    private static void assertVersionedProcessGroupsEqual(VersionedProcessGroup expected, VersionedProcessGroup actual) {
        assertNotNull(actual);

        assertEquals(((VersionedComponent)expected), ((VersionedComponent)actual));

        // Poor man's set equality assertion as we are only checking the base type and not doing a recursive check
        // TODO, this would be a stronger assertion by replacing this with a true VersionedProcessGroup.equals() method that does a deep equality check
        assertSetsEqual(expected.getProcessGroups(), actual.getProcessGroups());
        assertSetsEqual(expected.getRemoteProcessGroups(), actual.getRemoteProcessGroups());
        assertSetsEqual(expected.getProcessors(), actual.getProcessors());
        assertSetsEqual(expected.getInputPorts(), actual.getInputPorts());
        assertSetsEqual(expected.getOutputPorts(), actual.getOutputPorts());
        assertSetsEqual(expected.getConnections(), actual.getConnections());
        assertSetsEqual(expected.getLabels(), actual.getLabels());
        assertSetsEqual(expected.getFunnels(), actual.getFunnels());
        assertSetsEqual(expected.getControllerServices(), actual.getControllerServices());
    }


    private static void assertSetsEqual(Set<? extends VersionedComponent> expected, Set<? extends VersionedComponent> actual) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.size(), actual.size());
            assertTrue(actual.containsAll(expected));
        }
    }

}
