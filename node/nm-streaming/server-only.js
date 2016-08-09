/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var http = require('http');
var path = require('path');
var request = require('request');
var request2 = require('request');

var express = require('express');
var bodyParser = require('body-parser');
var session = require('express-session');
var EventSource = require('eventsource');
//var mdns = require('mdns');

var SUPER_SECRET_KEY = 'bmVzdG1hbmdlcnRvbmVzdG8NCg==';

var NEST_API_URL = 'https://developer-api.nest.com';
var app = express();

//var ad = mdns.createAdvertisement(mdns.tcp('Nest-Event-Srvc'), 3000, ({ name: 'Nest Web Manager' }));
//ad.start();

app.post('/stream', function(req, res) {
    var token = req.headers.token;
    //console.log('AuthToken: ' + token)
    var streamPath = req.headers.callback;
    //console.log('StreamUrl: ' + streamPath)
    var streamPort = req.headers.port;
    var stToken = req.headers.stToken;
    console.log('ST_Token: ' + stToken);
    var devSource = new EventSource(NEST_API_URL + '/devices?auth=' + token);
    var strSource = new EventSource(NEST_API_URL + '/structures?auth=' + token);


    strSource.addEventListener('put', function(e) {
        var data = JSON.parse(e.data);
        var options = {
            uri: streamPath + '/receiveStructData?access_token=' + stToken,
            method: 'POST',
            body: data
        };
        console.log(data);

        request(options, function(error, response, body) {
            if (!error && response.statusCode == 200) {
                console.log(body.id);
            }
        });
    });

    strSource.addEventListener('open', function(e) {
        console.log('Structure Connection opened!');
        res.send('Structure Connected');
    });

    strSource.addEventListener('auth_revoked', function(e) {
        console.log('Authentication token was revoked.');
    });

    strSource.addEventListener('error', function(e) {
        if (e.readyState == EventSource.CLOSED) {
            console.error('Structure Connection was closed! ', e);
        } else {
            console.error('StructureAn unknown error occurred: ', e);
        }
    }, false);

    devSource.addEventListener('put', function(e) {
        var data = JSON.parse(e.data);
        var options = {
            uri: streamPath + '/receiveDeviceData?access_token=' + stToken,
            method: 'POST',
            body: data
        };
        console.log(data);

        request2(options, function(error, response, body) {
            if (!error && response.statusCode == 200) {
                console.log(body.id);
            }
        });
    });

    devSource.addEventListener('open', function(e) {
        console.log('Device Connection opened!');
        res.send('Device Stream Connected');
    });

    devSource.addEventListener('auth_revoked', function(e) {
        console.log('Authentication token was revoked.');
    });

    devSource.addEventListener('error', function(e) {
        if (e.readyState == EventSource.CLOSED) {
            console.error('Device Connection was closed! ', e);
        } else {
            console.error('An unknown error occurred: ', e);
        }
    }, false);
});

app.post('/cmd', function(req, res) {
    var exitCmd = req.headers.exitCmd;
    console.log('Cmd: ' + exitCmd);
    //var source = new EventSource(NEST_API_URL + '?auth=' + token);
    server.close();
});


app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(session({
    secret: SUPER_SECRET_KEY,
    resave: false,
    saveUninitialized: false
}));


var port = process.env.PORT || 3000;
app.set('port', port);

var server = http.createServer(app);
server.listen(port);
console.info('Server is Running on Port: ' + port);
console.info('Send a Post Command to http://localhost:' + port + '/stream with this Body (token: "your_auth_token") to Start the Stream');