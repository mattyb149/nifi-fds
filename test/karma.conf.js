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

module.exports = function (config) {

    var appBase = 'platform/';       // app JS and map files

    config.set({
        basePath: '',
        browserNoActivityTimeout: 9999999, //default 10000
        browserDisconnectTimeout: 999999, // default 2000
        browserDisconnectTolerance: 1, // default 0
        captureTimeout: 999999,
        frameworks: ['jasmine'],
        customLaunchers: {
            Chrome_travis_ci: {
                base: 'ChromeHeadless',
                flags: ['--no-sandbox']
            }
        },
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-spec-reporter'),
            require('karma-coverage')
        ],

        client: {
            builtPaths: [appBase], // add more spec base paths as needed
            clearContext: false // leave Jasmine Spec Runner output visible in browser
        },

        files: [
            // System.js for module loading
            'node_modules/systemjs/dist/system.src.js',

            // Polyfills
            'node_modules/core-js/client/shim.js',

            // zone.js
            'node_modules/zone.js/dist/zone.js',
            'node_modules/zone.js/dist/long-stack-trace-zone.js',
            'node_modules/zone.js/dist/proxy.js',
            'node_modules/zone.js/dist/sync-test.js',
            'node_modules/zone.js/dist/jasmine-patch.js',
            'node_modules/zone.js/dist/async-test.js',
            'node_modules/zone.js/dist/fake-async-test.js',

            // RxJs
            {pattern: 'node_modules/rxjs/**/*.js', included: false, watched: false},
            {pattern: 'node_modules/rxjs/**/*.js.map', included: false, watched: false},

            // Paths loaded via module imports:
            {pattern: 'node_modules/systemjs/**/*.js.map', included: false, watched: false},
            {pattern: 'node_modules/@angular/**/*.js', included: false, watched: false},
            {pattern: 'node_modules/@angular/**/*.js.map', included: false, watched: false},
            {pattern: 'node_modules/jquery/**/*.js', included: false, watched: false},
            {pattern: 'node_modules/systemjs-plugin-text/text.js', included: false, watched: false},

            {pattern: appBase + 'systemjs.spec.config.js', included: false, watched: false},
            'karma-test-shim.js', // optionally extend SystemJS mapping e.g., with barrels

            // Include the Flow Design System templates in the test suite.
            {
                pattern: 'platform/core/**/*.html',
                included: true,
                watched: true,
                served: true
            },

            // Paths for debugging with source maps in dev tools
            {pattern: appBase + '**/*.js', included: false, watched: false}
        ],

        // Proxied base paths for loading assets
        proxies: {
            // required for modules fetched by SystemJS
            '/base/fds/node_modules/': '/base/node_modules/'
        },

        exclude: [],
        preprocessors: {
            'platform/**/!(*spec|*mock|*stub|*config|*extras|).js': 'coverage'
        },
        reporters: ['kjhtml', 'spec', 'coverage'],
        coverageReporter: {
            type: 'html',
            dir: 'coverage/'
        },
        specReporter: {
            failFast: false
        },
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: ['Chrome'],
        singleRun: false
    });

    if (process.env.TRAVIS) {
        config.set({
            browsers: ['Chrome_travis_ci']
        });

        // Override base config
        config.set({
            singleRun: true,
            autoWatch: false,
            reporters: ['spec', 'coverage'],
            specReporter: {
                failFast: true
            }
        });
    }
}
