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

var express = require('express');
var bodyParser = require('body-parser');
var session = require('express-session');
var EventSource = require('eventsource');
var mdns = require('mdns');

var SUPER_SECRET_KEY = 'bmVzdG1hbmdlcnRvbmVzdG8NCg==';

var NEST_API_URL = 'https://developer-api.nest.com';
var app = express();

var ad = mdns.createAdvertisement(mdns.tcp('Nest-Event-Srvc'), 3000, ({ name: 'Nest Web Manager' }));
ad.start();

//var Server = require("upnpserver");

//var server = new Server({ name: 'nestWebService', dlnaSupport: false }, [
//'/',
//{ path: '/' },
//]);

//server.start();

app.post('/stream', function(req, res) {
    var token = req.headers.token;
    //console.log('AuthToken: ' + token)
    var streamPath = req.headers.callback;
    //console.log('StreamUrl: ' + streamPath)
    var streamPort = req.headers.port;
    var stToken = req.headers.accessToken;
    var source = new EventSource(NEST_API_URL + '?auth=' + token);

    source.addEventListener('put', function(e) {
        var data = e.data;
        var options = {
            uri: streamPath + '?access_token=' + stToken,
            method: 'POST',
            body: data
        };
        //var camera_data = data.data.devices.cameras;
        //var tstat_data = data.data.devices.thermostats;
        //var prot_data = data.data.devices.protects;
        //console.log(tstat_data);
        //console.log(prot_data);
        //console.log(camera_data);
        console.log(data);

        request(options, function(error, response, body) {
            if (!error && response.statusCode == 200) {
                console.log(body.id) // Print the shortened url.
            }
        });
    });

    source.addEventListener('open', function(e) {
        console.log('Connection opened!');
        res.send('Connected');
    });

    source.addEventListener('auth_revoked', function(e) {
        console.log('Authentication token was revoked.');
    });

    source.addEventListener('error', function(e) {
        if (e.readyState == EventSource.CLOSED) {
            console.error('Connection was closed! ', e);
        } else {
            console.error('An unknown error occurred: ', e);
        }
    }, false);
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