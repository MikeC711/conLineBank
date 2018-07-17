const express = require('express'),
    http = require('http'),
    path = require('path'),
    assert = require('assert'),
    fs = require('fs'),
    mongodb = require('mongodb'),
    util = require('util'),
    bodyParser = require('body-parser')
const bankLog = util.debuglog('banking');
var bankTrace, REST_COBURL = '/api/conlinebank';
var traceStr = process.env.NODE_DEBUG;
var bankTrace = (traceStr.indexOf("bank") >= 0);
var verbose = (traceStr.indexOf("verbose") >= 0);

var url = "mongodb://127.0.0.1:27017/",
    dbnm = "conLineBank";
var MongoClient = mongodb.MongoClient;

var app = express();
app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true }));

app.set('port', process.env.BANKPORT || 3000);
app.use(express.static(__dirname + '/public'));
if (bankTrace) bankLog("MC: created connection pool: " + REST_COBURL);

/*********************************************************************************************************************************
  Retrieve names of collections
*********************************************************************************************************************************/
app.get(REST_COBURL + '/listCollections', function(request, response) {
    if (verbose) bankLog("Called listCollections");

    MongoClient.connect(url, function(cerr, db) {
        if (cerr) {
            var errString = "Failure to get connection: " + JSON.stringify(cerr);
            bankLog("Error: %s", errString);
            response.write(JSON.stringify({ statusCode: 500, userMessage: errString }));
        } else {
            var dbo = db.db(dbnm);
            dbo.collection("myNewCollection1").findOne({}, function(err, result) {
                if (err) {
                    var errString = "Failure to find record: " + JSON.stringify(err);
                    bankLog("Error: %s", errString);
                    response.write(JSON.stringify({ statusCode: 500, userMessage: errString }));
                    console.log(JSON.stringify(err));
                } else {
                    console.log("Row: %s", JSON.stringify(result));
                    response.write(JSON.stringify({ statusCode: 200, userMessage: "", row: result }));
                }
            });
        }
        response.end();
        db.close();
    });
});


/*********************************************************************************************************************************
 Start the express appServer going
*********************************************************************************************************************************/
http.createServer(app).listen(app.get('port'), function() {
    bankLog('App:createServer Express server listening on port %s', app.get('port'));
});