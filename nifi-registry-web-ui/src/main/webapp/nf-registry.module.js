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

var ngCore = require('@angular/core');
var ngMoment = require('angular2-moment');
var NfRegistryRoutes = require('nifi-registry/nf-registry.routes.js');
var FdsDemo = require('nifi-registry/components/fluid-design-system/fds-demo.js');
var NfRegistry = require('nifi-registry/nf-registry.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/add/nf-registry-add-user.js');
var NfRegistryUserDetails = require('nifi-registry/components/administration/users/details/nf-registry-user-details.js');
var NfRegistryUserPermissions = require('nifi-registry/components/administration/users/permissions/nf-registry-user-permissions.js');
var NfRegistryBucketPermissions = require('nifi-registry/components/administration/workflow/buckets/permissions/nf-registry-bucket-permissions.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');
var ngHttp = require('@angular/http');

function NfRegistryModule() {
};

NfRegistryModule.prototype = {
    constructor: NfRegistryModule
};

NfRegistryModule.annotations = [
    new ngCore.NgModule({
        imports: [
            ngMoment.MomentModule,
            fdsCore,
            ngHttp.HttpModule,
            ngHttp.JsonpModule,
            NfRegistryRoutes
        ],
        declarations: [
            FdsDemo,
            NfRegistry,
            NfRegistryExplorer,
            NfRegistryAdministration,
            NfRegistryUsersAdministration,
            NfRegistryUserDetails,
            NfRegistryUserPermissions,
            NfRegistryBucketPermissions,
            NfRegistryAddUser,
            NfRegistryWorkflowAdministration,
            NfRegistryGridListViewer,
            NfRegistryBucketGridListViewer,
            NfRegistryDropletGridListViewer,
            NfPageNotFoundComponent],
        providers: [
            NfRegistryService,
            NfRegistryApi],
        bootstrap: [NfRegistry]
    })
];

module.exports = NfRegistryModule;